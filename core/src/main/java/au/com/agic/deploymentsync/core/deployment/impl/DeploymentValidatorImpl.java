package au.com.agic.deploymentsync.core.deployment.impl;

import au.com.agic.deploymentsync.core.configuration.CoreConfiguration;
import au.com.agic.deploymentsync.core.configuration.CoreConfigurationImpl;
import au.com.agic.deploymentsync.core.deployment.Deployment;
import au.com.agic.deploymentsync.core.deployment.DeploymentValidator;
import au.com.agic.deploymentsync.core.exceptions.WildflyCommunicationException;
import au.com.agic.deploymentsync.core.wildfly.DomainControllerClient;
import au.com.agic.deploymentsync.core.wildfly.impl.DomainControllerClientImpl;

import com.amazonaws.services.ec2.model.Instance;

import org.apache.commons.codec.binary.Hex;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

public class DeploymentValidatorImpl implements DeploymentValidator {

	private static final Logger LOGGER = Logger.getLogger(DeploymentValidatorImpl.class.getName());
	private static final CoreConfiguration CONFIGURATION = new CoreConfigurationImpl();

	@Override
	public final List<String> validateDeployments(final List<Instance> instances) {
		LOGGER.log(Level.INFO, "Validating deployments...");
		final Map<String, List<Deployment>> deployments = getDeploymentsData(instances);
		return findProblems(deployments);
	}

	@Override
	public final List<String> validateDeployment(final List<Instance> instances, final Deployment deployment) {
		LOGGER.log(Level.INFO, "Validating deployment...");
		final Map<String, List<Deployment>> deployments = getDeploymentsData(instances);
		return findProblems(deployments, deployment);
	}

	/**
	 * Gets data about deployments from all the slaves
	 * and stores in static variables
	 * @param instances domain controllers
	 */
	private Map<String, List<Deployment>> getDeploymentsData(final List<Instance> instances) {
		final Map<String, List<Deployment>> deployments = new HashMap<>();

		for (final Instance instance : instances) {
			DomainControllerClient client = null;
			try {
				// Establish connection with the remote Wildfly server
				final InetAddress serverAddress = InetAddress.getByName(instance.getPrivateIpAddress());
				client = new DomainControllerClientImpl(serverAddress, CONFIGURATION);

				// Get list of slaves from wildfly domain controller
				final List<String> slaveIpAddresses = client.getSlavesHostnames();

				for (String slaveIpAddress : slaveIpAddresses) {
					deployments.put(
						slaveIpAddress,
						client.getDeployments(Collections.singletonList(slaveIpAddress))
					);
				}
				client.close();

			} catch (final WildflyCommunicationException | UnknownHostException ex) {
				LOGGER.log(Level.SEVERE, ex.toString(), ex);
				System.exit(1);
			} finally {
				if (client != null) {
					client.close();
				}
			}
		}

		return deployments;
	}

	/**
	 * Find problems with all deployments across all the slaves
	 * @param deployments map of deployments by slave
	 * @return list of problems
	 */
	private List<String> findProblems(final Map<String, List<Deployment>> deployments) {

		// Imagine we have structure like this:
		// Map ('slave1' => List:        ), ('slave2' => List:          ) ...
		//                  - deployment1                  - deployment1
		//                  - deployment2                  - deployment3
		//
		// This stream transformation will create a set of unique deployment names
		// across all slaves like:
		// Set ('deployment1.name', 'deployment2.name', 'deployment3.name') ...

		final Set<String> uniqueDeployments = deployments.keySet().stream()
			.flatMap(slaveName -> deployments.get(slaveName).stream()
				.map(Deployment::getName))
			.collect(Collectors.toSet());

		return findProblems(deployments, uniqueDeployments);
	}


	/**
	 * Find problems with deployment
	 * @param deployments map of deployments by slave
	 * @param deployment deployment to find problems with
	 * @return list of problems
	 */
	private List<String> findProblems(
		final Map<String, List<Deployment>> deployments, final Deployment deployment) {

		final Set<String> deploymentsToCheck = new HashSet<>();
		deploymentsToCheck.add(deployment.getName());
		return findProblems(deployments, deploymentsToCheck);
	}

	/**
	 * Find problems with deployments
	 * @param deployments map of deployments by slave
	 * @param deploymentsToCheck deployments to find problems with
	 * @return list of problems
	 */
	private List<String> findProblems(
		final Map<String, List<Deployment>> deployments, final Set<String> deploymentsToCheck) {
		final List<String> problems = new ArrayList<>();

		for (final String deploymentName : deploymentsToCheck) {
			final List<Deployment> foundInstances = new ArrayList<>();

			for (final String slaveHostname : deployments.keySet()) {
				final Optional<Deployment> deploymentOptional =
					deployments.get(slaveHostname).stream()
						.filter(dep -> dep.getName().equals(deploymentName))
						.findAny();

				if (deploymentOptional.isPresent()) {
					foundInstances.add(deploymentOptional.get());
				} else {
					problems.add(deploymentName + ": "
						+ slaveHostname + " doesn't contain deployment");
				}
			}
			final Boolean shaMatch = foundInstances
				.stream()
				.allMatch(dep -> dep.equals(foundInstances.get(0)));

			if (!shaMatch) {
				final String str = foundInstances
					.stream()
					.map(Deployment::getSha1)
					.map(Hex::encodeHexString)
					.collect(Collectors.joining(", "));

				problems.add(deploymentName + ": sha doesn't match accross slaves. Hashsums: " + str);
			}
		}
		return problems;
	}
}
