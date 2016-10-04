package au.com.agic.deploymentsync.synchronizer.deployment;

import au.com.agic.deploymentsync.core.deployment.Deployment;

import java.net.InetAddress;
import java.util.List;

@FunctionalInterface
public interface DeploymentSynchronizer {

	/**
	 * Synchronizes local deployments with remote Wildfly domain controller server
	 *
	 * @param IPAddress        IP of Wildfly domain controller
	 * @param localDeployments list of local deployments
	 * @param dryRun           is it a dry run or what?
	 */
	void synchronize(final InetAddress ipAddress, final List<Deployment> localDeployments, boolean dryRun);
}
