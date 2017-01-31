package au.com.agic.deploymentsync.core.wildfly.impl;

import au.com.agic.deploymentsync.core.configuration.CoreConfiguration;
import au.com.agic.deploymentsync.core.constants.Defaults;
import au.com.agic.deploymentsync.core.deployment.Deployment;
import au.com.agic.deploymentsync.core.deployment.impl.StandardDeployment;
import au.com.agic.deploymentsync.core.exceptions.WildflyCommunicationException;
import au.com.agic.deploymentsync.core.utils.CoreUtils;
import au.com.agic.deploymentsync.core.wildfly.DomainControllerClient;
import au.com.agic.deploymentsync.core.wildfly.ModelControllerClientFactory;
import au.com.agic.deploymentsync.core.wildfly.operations.DeployOperation;
import au.com.agic.deploymentsync.core.wildfly.operations.GetDeploymentsForHostNameOperation;
import au.com.agic.deploymentsync.core.wildfly.operations.GetDetailedDeploymentsStatusOperation;
import au.com.agic.deploymentsync.core.wildfly.operations.GetResourcesOperation;
import au.com.agic.deploymentsync.core.wildfly.operations.RedeployOperation;
import au.com.agic.deploymentsync.core.wildfly.operations.RemoveOperation;
import au.com.agic.deploymentsync.core.wildfly.operations.RestartSlaveOperation;
import au.com.agic.deploymentsync.core.wildfly.operations.UndeployOperation;

import org.jboss.as.cli.Util;
import org.jboss.as.controller.client.ModelControllerClient;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.Property;

import java.io.IOException;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class DomainControllerClientImpl implements DomainControllerClient {

	private static final Logger LOGGER = Logger.getLogger(DomainControllerClientImpl.class.getName());
	private static final int PORT = 9990;

	/**
	 * Deployment and Undeployment operations' failures will trigger retry attempt
	 * for MAX_ATTEMPTS_FOR_OPERATION with TIME_BETWEEN_ATTEMPTS
	 */
	private static final int MAX_ATTEMPTS_FOR_OPERATION = 5;
	private static final int TIME_BETWEEN_ATTEMPTS = 5000;
	private static final String UNDEFINED = "undefined";
	private static final String ENABLED_TIME = "enabled-time";
	private static final String SYSTEM_BOOT_STRING = "System boot is in process";
	private static final String IS_ALREADY_REGISTERED_STRING = "is already registered";
	private static final String DUPLICATE_RESOURCE_STRING = "Duplicate resource";
	private static final Pattern NOT_FOUND_PATTERN =
		Pattern.compile(".*Management resource .* not found.*");

	private final InetAddress serverAddress;
	private final ModelControllerClient client;

	public DomainControllerClientImpl(final InetAddress serverInetAddress,
		final CoreConfiguration configuration) {
		serverAddress = serverInetAddress;
		client = ModelControllerClientFactory.createModelControllerClient(
			serverAddress,
			PORT,
			configuration.getWildflyLogin(),
			configuration.getWildflyPassword()
		);
	}

	@Override
	public final InetAddress getServerAddress() {
		return serverAddress;
	}

	/**
	 * @return list of slaves hostnames
	 */
	@Override
	public final List<String> getSlavesHostnames() {
		try {
			ModelNode response = client.execute(new GetResourcesOperation());
			if (!"success".equals(response.get(Util.OUTCOME).asString())) {
				throw new WildflyCommunicationException(
					"Can't get slaves: " + response.get().asString());
			}

			// parse results
			return response
				.get(Util.RESULT)
				.get(Util.HOST)
				.asPropertyList()
				.stream()
				.map(Property::getName)
				.filter(hostname -> !"master".equals(hostname))
				.collect(Collectors.toList());

		} catch (IOException | IllegalArgumentException ex) {
			LOGGER.log(Level.SEVERE, ex.getMessage(), ex);
			throw new WildflyCommunicationException(ex.getMessage());
		}
	}

	/**
	 * Iterates through slaves until it can get list of deployments
	 * @param ipAddresses Wildfly domain slaves IP addresses
	 * @return list of deployments
	 */
	@Override
	public final List<Deployment> getDeployments(final List<String> ipAddresses) {

		for (String ipAddress : ipAddresses) {
			LOGGER.log(Level.INFO, "Getting deployment list from " + ipAddress);

			try {
				ModelNode response = client.execute(new GetDeploymentsForHostNameOperation(ipAddress));
				if (CoreUtils.isOperationFailed(response)) {
					throw new WildflyCommunicationException(response.asString());
				}

				List<Deployment> deployments = new ArrayList<>();
				for (Property property : response.get(Util.RESULT).asPropertyList()) {
					final Deployment deployment = new StandardDeployment(
						property.getName(),
						property.getValue()
							.get(Util.CONTENT)
							.get(0)
							.asProperty()
							.getValue()
							.asBytes()
					);

					// Get enabled time for enabled deployments
					if (!UNDEFINED.equals(property.getValue().get(ENABLED_TIME).toString())) {
						deployment.setEnabledTime(property
							.getValue()
							.get(ENABLED_TIME)
							.asLong());
					}

					deployments.add(deployment);
				}
				return deployments;

			} catch (WildflyCommunicationException | IllegalArgumentException | IOException ex) {
				LOGGER.log(Level.WARNING, "Cannot read data from slave " + ipAddress
					+ " - Going to use another slave if possible", ex);
			}
		}
		throw new WildflyCommunicationException("Cannot get information from slaves. Are they online?");
	}

	@Override
	public final Optional<ModelNode> deploy(final Deployment deployment) {
		Optional<ModelNode> result = Optional.empty();
		int attempts = 0;

		// try operation multiple times
		while (attempts < MAX_ATTEMPTS_FOR_OPERATION) {
			try {
				final ModelNode operation;

				// two different operations depending on force flag
				if (deployment.getForceFlag()) {
					operation = new RedeployOperation(deployment);
				} else {
					operation = new DeployOperation(deployment, Defaults.SERVER_GROUP);
				}

				result = Optional.of(client.execute(operation));

				if (result.isPresent() && CoreUtils.isOperationFailed(result)) {
					final String failureDescription =
						result.get().get(Util.FAILURE_DESCRIPTION).asString();

					LOGGER.log(Level.WARNING, result.get().asString());

					// Trigger remove deployment operation
					// if operation failed because resource is duplicated
					// (that could happened if deploy operation failed to assign deployment
					// to a server group)
					if (failureDescription.contains(DUPLICATE_RESOURCE_STRING)) {
						LOGGER.log(Level.INFO, "Deployment already found");
						remove(deployment);
					}

					// In case of previous errors we may have deployment deployed to one
					// domain controller but not to another. We have to reset a force flag
					// to prevent deployment from failing in that situation
					if (NOT_FOUND_PATTERN.matcher(failureDescription).matches()) {
						LOGGER.log(Level.INFO,
							"Deployment not found. Setting force flag to false...");
						deployment.setForceFlag(false);
					}

					if (failureDescription.contains(IS_ALREADY_REGISTERED_STRING)) {
						LOGGER.log(Level.WARNING,
							"It looks like you're trying to deploy .war file "
							+ "to the enviroment that already has .ear with that resource. "
							+ "Please don't do that.");
					}

					// wait before next iteration
					LOGGER.log(Level.INFO,
						"Retrying deployment in " + TIME_BETWEEN_ATTEMPTS + " ms...");
					Thread.sleep(TIME_BETWEEN_ATTEMPTS);
				} else {
					// exit from loop if operation succeed
					break;
				}
			} catch (IOException | IllegalArgumentException | InterruptedException ex) {
				LOGGER.log(Level.INFO, ex.toString(), ex);
			}
			attempts += 1;
		}
		return result;
	}

	@Override
	public final Optional<ModelNode> undeploy(final Deployment deployment,
		final String serverGroup) {
		Optional<ModelNode> result = Optional.empty();
		int attempts = 0;

		// try operation multiple times
		while (attempts < MAX_ATTEMPTS_FOR_OPERATION) {
			try {
				result =
					Optional.of(client.execute(new UndeployOperation(deployment, serverGroup)));

				// there is not much that we can do if undeployment operation failed
				// basically just hope that issue will resolve itself after timeout
				if (CoreUtils.isOperationFailed(result)) {
					LOGGER.log(Level.WARNING,
						result.get().get(Util.FAILURE_DESCRIPTION).asString());

					// at least we can try to remove deployment if it wasn't assigned
					// (that shouldn't happened)
					remove(deployment);

					// wait before next iteration
					LOGGER.log(Level.INFO,
						"Retrying undeployment in " + TIME_BETWEEN_ATTEMPTS + " ms...");
					Thread.sleep(TIME_BETWEEN_ATTEMPTS);
				} else {
					// exit from loop if operation succeed
					break;
				}
			} catch (IOException | IllegalArgumentException | InterruptedException ex) {
				LOGGER.log(Level.INFO, ex.toString(), ex);
			}
			attempts += 1;
		}
		return result;
	}

	@Override
	public final boolean hasDeployment(final Deployment deployment) {
		// Get list of slaves
		final List<String> ipAddresses = getSlavesHostnames();

		if (ipAddresses.isEmpty()) {
			throw new WildflyCommunicationException("No slaves available for domain controller");
		}

		return getDeployments(ipAddresses)
			.stream()
			.anyMatch(item -> item.getName().equals(deployment.getName()));
	}

	@Override
	public final void close() {
		try {
			client.close();
		} catch (IOException ex) {
			LOGGER.log(Level.WARNING, ex.toString(), ex);
		}
	}

	@Override
	public final Map<String, Boolean> getSlaveActivityStatuses() {

		final List<String> hostnames = getSlavesHostnames();
		final Map<String, Boolean> statuses = new HashMap<>();

		for (String hostname : hostnames) {
			ModelNode deploymentsStatus = null;

			// get detailed deployment information from slave
			// in case of some weird errors it will return response like:
			// {
			//    outcome: success,
			//    result: undefined,
			// }
			// and that means we have to restart it
			try {
				deploymentsStatus = client.execute(new GetDetailedDeploymentsStatusOperation(hostname));
			} catch (IOException ex) {
				LOGGER.log(Level.WARNING, ex.toString(), ex);
			}

			Boolean status = false;
			if (deploymentsStatus != null) {

				// there may be a case when node is already restarting/booting
				// then we will mark it as active assuming it will come back eventually
				if ("failed".equals(deploymentsStatus.get(Util.OUTCOME).asString())
					&& deploymentsStatus.get(Util.FAILURE_DESCRIPTION).asString()
						.contains(SYSTEM_BOOT_STRING)) {

					LOGGER.log(Level.INFO, hostname + " is already restarting...");
					status = true;
				} else {
					status = !"undefined".equals(deploymentsStatus.get(Util.RESULT).asString());
				}
			}
			statuses.put(hostname, status);
		}
		return statuses;
	}

	@Override
	public final Optional<ModelNode> restartSlave(final String hostname) {
		Optional<ModelNode> result = Optional.empty();
		try {
			result = Optional.of(client.execute(new RestartSlaveOperation(hostname)));
		} catch (IOException ex) {
			LOGGER.log(Level.WARNING, ex.toString(), ex);
		}
		return result;
	}

	/**
	 * Remove deployment from domain controller. That is useful when deployment
	 * wasn't assigned to any server group
	 *
	 * @param deployment application's deployment to remove
	 * @throws java.io.IOException if anything went wrong
	 */
	private void remove(final Deployment deployment) throws IOException {
		LOGGER.log(Level.INFO, "Removing deployment " + deployment.getName() + " from domain ...");
		final Optional<ModelNode> result =
			Optional.of(client.execute(new RemoveOperation(deployment, null)));

		if (result.isPresent() && CoreUtils.isOperationFailed(result)) {
			LOGGER.log(Level.WARNING, result.get().get(Util.FAILURE_DESCRIPTION).asString());
		}
	}
}
