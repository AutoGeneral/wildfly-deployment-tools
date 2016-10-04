package au.com.agic.deploymentsync.synchronizer.deployment.impl;

import au.com.agic.deploymentsync.core.constants.Defaults;
import au.com.agic.deploymentsync.core.deployment.Deployment;
import au.com.agic.deploymentsync.core.utils.CoreUtils;
import au.com.agic.deploymentsync.core.wildfly.DomainControllerClient;
import au.com.agic.deploymentsync.core.wildfly.impl.DomainControllerClientImpl;
import au.com.agic.deploymentsync.synchronizer.Configuration;
import au.com.agic.deploymentsync.synchronizer.deployment.DeploymentListsComparator;
import au.com.agic.deploymentsync.synchronizer.deployment.DeploymentListsComparisonResult;
import au.com.agic.deploymentsync.synchronizer.deployment.DeploymentSynchronizer;
import au.com.agic.deploymentsync.synchronizer.exeptions.SynchronizationException;

import org.jboss.as.cli.Util;
import org.jboss.dmr.ModelNode;

import java.net.InetAddress;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

public class DeploymentSynchronizerImpl implements DeploymentSynchronizer {

	private static final Logger LOGGER = Logger.getLogger(DeploymentSynchronizerImpl.class.getName());

	private final Configuration configuration;
	private final DeploymentListsComparator deploymentListsComparator;

	public DeploymentSynchronizerImpl(final Configuration configuration) {
		this.configuration = configuration;
		deploymentListsComparator = new DeploymentListsComparatorImpl();
	}

	@Override
	public final void synchronize(
		final InetAddress ipAddress, final List<Deployment> localDeployments, final boolean dryRun) {

		if (dryRun) {
			LOGGER.info("Dry run mode enabled, nothing will be changed on remote servers");
		}

		// Establish connection with the remote Wildfly server
		DomainControllerClient client = new DomainControllerClientImpl(ipAddress, configuration);

		final DeploymentListsComparisonResult comparisonResult =
			compareDeployments(localDeployments, client);

		// Deploy/Undeploy apps to domain controller
		if (configuration.isDeployDisabled()) {
			LOGGER.log(Level.INFO, "Deployment has been disabled by flag. Skipping...");
		} else {
			deployApplications(client, comparisonResult.getListToDeploy(), dryRun);
		}
		if (configuration.isUneployDisabled()) {
			LOGGER.log(Level.INFO, "Undeployment has been disabled by flag. Skipping...");
		} else {
			undeployApplications(client, comparisonResult.getListToUndeploy(), dryRun);
		}

		client.close();
	}


	/**
	 * Compare local and remote deployments
	 * @param localDeployments list of local deployments
	 * @param client client to remote domain controller
	 * @return comparison result
	 */
	private DeploymentListsComparisonResult compareDeployments(
		final List<Deployment> localDeployments, final DomainControllerClient client) {

		// Get the list of slaves from wildfly domain controller
		final List<String> slaveIpAddresses = client.getSlavesHostnames();
		if (slaveIpAddresses.isEmpty()) {
			throw new SynchronizationException("No slaves were found for "
				+ client.getServerAddress().getHostAddress());
		}

		final Set<String> ignoredArtifacts = configuration.getIgnoreArtifacts();

		// We will use slaves to get information about currently enabled deployments there
		// Filter remote deployments excluding artifacts we should ignore
		final List<Deployment> remoteDeployments = client.getDeployments(slaveIpAddresses)
			.stream()
			.filter(deployment -> !ignoredArtifacts.contains(deployment.getName()))
			.collect(Collectors.toList());

		// Compare the list of local deploymnets with remote deployments
		// It will create a result object with lists of deployments to deploy and undeploy
		return deploymentListsComparator.compare(localDeployments, remoteDeployments);
	}

	/**
	 * Deploy applications to domain controller
	 *
	 * @param client      domain controller client instance
	 * @param deployments list of deployments to deploy
	 * @param dryRun      if true, no apps will be deployed
	 */
	private void deployApplications(
		final DomainControllerClient client, final List<Deployment> deployments, final boolean dryRun) {

		LOGGER.info(deployments.size() + " apps will be DEPLOYED to " + client.getServerAddress()
			.getHostAddress() + " domain");

		deployments.forEach(deployment -> {
			LOGGER.info("Deploying " + deployment.getName() + " to " + client.getServerAddress()
				.getHostAddress());

			if (dryRun) {
				return;
			}

			Optional<ModelNode> result = client.deploy(deployment);
			if (CoreUtils.isOperationFailed(result)) {
				LOGGER.warning(result.get().get(Util.FAILURE_DESCRIPTION).asString());
			}
		});
	}

	/**
	 * Undeploy applications from domain controller
	 *
	 * @param client      domain controller client instance
	 * @param deployments list of deployments to undeploy
	 * @param dryRun      if true, no apps will be undeployed
	 */
	private void undeployApplications(
		final DomainControllerClient client, final List<Deployment> deployments, final boolean dryRun) {

		LOGGER.info(deployments.size() + " apps will be UNDEPLOYED from " + client.getServerAddress()
			.getHostAddress() + " domain");

		deployments.forEach(deployment -> {
			LOGGER.info("Undeploying " + deployment.getName() + " from " + client.getServerAddress()
				.getHostAddress());
			if (dryRun) {
				return;
			}

			Optional<ModelNode> result = client.undeploy(deployment, Defaults.SERVER_GROUP);
			if (CoreUtils.isOperationFailed(result)) {
				LOGGER.warning(result.get().get(Util.FAILURE_DESCRIPTION).asString());
			}
		});
	}
}
