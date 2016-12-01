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
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;


final class Main {

	private static final Logger LOGGER = Logger.getLogger(Main.class.getName());
	private static final Configuration CONFIGURATION = new Configuration();
	private static final long MS = 1000;

	private Main() {

	}

	public static void main(final String... args) {
		final Inventory inventory = new DynamicInventory(CONFIGURATION);
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

		if (latestDeployment.isPresent()) {
			final long acceptableTime = latestDeployment.get().getEnabledTime()
				+ CONFIGURATION.getInactiveTimeout() * MS;

			if (acceptableTime < Instant.now().toEpochMilli()) {
				LOGGER.log(Level.WARNING, "Domain controllers have not been used for more than "
					+ CONFIGURATION.getInactiveTimeout() + " seconds and will be turned off...");

				stopInstances(instances);
			} else {
				LOGGER.log(Level.INFO, "Domain controllers are reporting the last deployment was "
					+ (Instant.now().toEpochMilli() - latestDeployment.get().getEnabledTime()) / MS
					+ " seconds ago for " + latestDeployment.get().getName() + " artifact");
			}
		}
	}

	private static Optional<Deployment> getTheLatestDeployment(List<Instance> domainControllers) {

		final List<Deployment> latestEnabledDeployments = new ArrayList<>();

		for (final Instance instance : domainControllers) {
			try {
				final InetAddress serverAddress = InetAddress.getByName(instance.getPrivateIpAddress());
				final DomainControllerClient client = new DomainControllerClientImpl(serverAddress, CONFIGURATION);

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

	private static void stopInstances(final List<Instance> instances) {
		final AmazonEC2 ec2;

		// Use credentials from arguments if possible, otherwise use default .aws/credentials
		if (CONFIGURATION.getAwsCredentials() != null) {
			ec2 = new AmazonEC2Client(CONFIGURATION.getAwsCredentials());
		} else {
			ec2 = new AmazonEC2Client();
		}
		ec2.setRegion(Defaults.AWS_REGION);

		final StopInstancesRequest stopInstancesRequest = new StopInstancesRequest()
			.withInstanceIds(instances.stream().map(Instance::getInstanceId).collect(Collectors.toList()));

		ec2.stopInstances(stopInstancesRequest);
	}
}
