package au.com.agic.deploymentsync.inactive;

import au.com.agic.deploymentsync.core.aws.Inventory;
import au.com.agic.deploymentsync.core.aws.impl.DynamicInventory;
import au.com.agic.deploymentsync.core.constants.Defaults;
import au.com.agic.deploymentsync.core.deployment.Deployment;
import au.com.agic.deploymentsync.core.exceptions.WildflyCommunicationException;
import au.com.agic.deploymentsync.core.wildfly.DomainControllerClient;
import au.com.agic.deploymentsync.core.wildfly.impl.DomainControllerClientImpl;

import com.amazonaws.services.ec2.AmazonEC2;
import com.amazonaws.services.ec2.AmazonEC2Client;
import com.amazonaws.services.ec2.model.Instance;
import com.amazonaws.services.ec2.model.InstanceStateName;
import com.amazonaws.services.ec2.model.StopInstancesRequest;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.Callable;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;


public class CheckInactiveTask implements Callable<String> {

	private static final Logger LOGGER = Logger.getLogger(CheckInactiveTask.class.getName());
	private final Configuration configuration;
	private static final long MS = 1000;

	public CheckInactiveTask(final Configuration configuration) {
		this.configuration = configuration;
	}

	@Override
	public String call() {

		final Inventory inventory = new DynamicInventory(configuration);
		final List<Instance> instances =
			inventory.getWildflyDomainControllers()
				.stream()
				.filter(instance -> InstanceStateName.Running.toString().equals(instance.getState().getName()))
				.collect(Collectors.toList());

		if (instances.isEmpty()) {
			LOGGER.log(Level.INFO, "No running domain controller instances found");
			System.exit(0);
		}

		final Optional<Deployment> latestDeployment = getTheLatestDeployment(instances);
		final long launchTime = instances
			.stream()
			.map(instance -> instance.getLaunchTime().getTime())
			.max(Long::compare)
			.orElse(0L);

		// if domain controllers have been launched long time ago (more than idle limit)
		if (launchTime + configuration.getInactiveTimeout() * MS
			< Instant.now().toEpochMilli()) {

			// If we found deployments - we should inspect them
			if (latestDeployment.isPresent()) {
				final long lastDeploymentTime = latestDeployment.get().getEnabledTime();

				// we will stop instances if the last deployment is old enough
				if (lastDeploymentTime + configuration.getInactiveTimeout() * MS
					< Instant.now().toEpochMilli()) {

					stopInstances(instances);
				} else {
					LOGGER.log(Level.INFO, "Domain controllers are reporting the last deployment was "
						+ (Instant.now().toEpochMilli() - latestDeployment.get().getEnabledTime()) / MS
						+ " seconds ago for " + latestDeployment.get().getName() + " artifact");
				}
			}
			// We will stop instances if they are launched long time ago
			// and still have no deployments
			else {
				stopInstances(instances);
			}
		}
		// Domain controller started not too long ago so ignore them yet
		else {
			LOGGER.log(Level.WARNING, "Domain controllers only started "
				+ (Instant.now().toEpochMilli() - launchTime) / MS
				+ " seconds ago so leave them alone");
		}

		return "Ready!";
	}

	/**
	 * Get the latest deployment deployed to the list of domain controllers
	 * @param domainControllers
	 * @return
	 */
	private Optional<Deployment> getTheLatestDeployment(List<Instance> domainControllers) {

		final List<Deployment> latestEnabledDeployments = new ArrayList<>();

		for (final Instance instance : domainControllers) {
			try {
				final InetAddress serverAddress = InetAddress.getByName(instance.getPrivateIpAddress());
				final DomainControllerClient client = new DomainControllerClientImpl(serverAddress, configuration);

				// Get list of slaves from wildfly domain controller
				final List<String> slaveIpAddresses = client.getSlavesHostnames();

				final Optional<Deployment> latestEnabledDeployment = client.getDeployments(slaveIpAddresses)
					.stream().max(Comparator.comparing(Deployment::getEnabledTime));

				if (latestEnabledDeployment.isPresent()) {
					latestEnabledDeployments.add(latestEnabledDeployment.get());
				} else {
					LOGGER.log(Level.SEVERE,
						"Domain controller " + instance.getPrivateIpAddress() + " returned no deployments");
				}
				client.close();

			} catch (WildflyCommunicationException | UnknownHostException ex) {
				LOGGER.log(Level.SEVERE, ex.getMessage(), ex);
				System.exit(1);
			}
		}
		return latestEnabledDeployments.stream().max(Comparator.comparing(Deployment::getEnabledTime));
	}

	/**
	 * Send AWS EC2 API request to stop instances
	 * @param instances
	 */
	private void stopInstances(final List<Instance> instances) {
		LOGGER.log(Level.WARNING, "Domain controllers have not been used for more than "
			+ configuration.getInactiveTimeout() + " seconds and will be turned off...");

		final AmazonEC2 ec2;

		// Use credentials from arguments if possible, otherwise use default .aws/credentials
		if (configuration.getAwsCredentials() != null) {
			ec2 = new AmazonEC2Client(configuration.getAwsCredentials());
		} else {
			ec2 = new AmazonEC2Client();
		}
		ec2.setRegion(Defaults.AWS_REGION);

		final StopInstancesRequest stopInstancesRequest = new StopInstancesRequest()
			.withInstanceIds(instances.stream().map(Instance::getInstanceId).collect(Collectors.toList()));

		ec2.stopInstances(stopInstancesRequest);
	}
}
