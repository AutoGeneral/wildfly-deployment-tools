package au.com.agic.deploymentsync.synchronizer;

import au.com.agic.deploymentsync.core.aws.Inventory;
import au.com.agic.deploymentsync.core.aws.impl.DynamicInventory;
import au.com.agic.deploymentsync.core.deployment.Deployment;
import au.com.agic.deploymentsync.core.exceptions.WildflyCommunicationException;
import au.com.agic.deploymentsync.synchronizer.deployment.DeploymentSynchronizer;
import au.com.agic.deploymentsync.synchronizer.deployment.impl.DeploymentSynchronizerImpl;
import au.com.agic.deploymentsync.synchronizer.exeptions.SynchronizationException;
import au.com.agic.deploymentsync.synchronizer.utlis.DeploymentUtils;

import com.amazonaws.services.ec2.model.Instance;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

final class Main {

	private static final Logger LOGGER = Logger.getLogger(Main.class.getName());
	private static final Configuration CONFIGURATION = new Configuration();

	private Main() {

	}

	public static void main(final String... args) {
		final Inventory inventory = new DynamicInventory(CONFIGURATION);
		final DeploymentSynchronizer deploymentSynchronizer =
			new DeploymentSynchronizerImpl(CONFIGURATION);

		final List<Deployment> localDeployments = getLocalDeployments();
		final List<Instance> instances = inventory.getWildflyDomainControllers();

		if (instances.isEmpty()) {
			LOGGER.log(Level.SEVERE, "No domain controller instances found");
			System.exit(1);
		}

		for (final Instance instance : instances) {
			try {
				final InetAddress serverAddress = InetAddress.getByName(instance.getPrivateIpAddress());
				deploymentSynchronizer.synchronize(
					serverAddress, localDeployments, CONFIGURATION.getDryRun());
			} catch (WildflyCommunicationException | UnknownHostException | SynchronizationException ex) {
				LOGGER.log(Level.SEVERE, ex.getMessage(), ex);
				System.exit(1);
			}
		}
	}

	private static List<Deployment> getLocalDeployments() {
		final Set<String> ignoredArtifacts = CONFIGURATION.getIgnoreArtifacts();

		return DeploymentUtils
			.getLocalDeploymentsList(CONFIGURATION.getLocalAppLibraryPath())
			.stream()
			.filter(deployment -> !ignoredArtifacts.contains(deployment.getName()))
			.collect(Collectors.toList());
	}
}
