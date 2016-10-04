package au.com.agic.deploymentsync.synchronizer.deployment;

import au.com.agic.deploymentsync.core.deployment.Deployment;

import java.util.List;

/**
 * Result of <code>DeploymentListsComparator.compare</code>
 */
public interface DeploymentListsComparisonResult {

	/**
	 * Returns the list of application deployments ready to deploy
	 * to remote server
	 *
	 * @return list of deployments
	 */
	List<Deployment> getListToDeploy();

	/**
	 * Returns the list of application deployments ready to undeploy
	 * from remote server
	 *
	 * @return list of deployments
	 */
	List<Deployment> getListToUndeploy();
}
