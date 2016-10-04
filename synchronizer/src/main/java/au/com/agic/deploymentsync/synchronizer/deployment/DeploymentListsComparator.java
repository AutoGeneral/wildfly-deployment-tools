package au.com.agic.deploymentsync.synchronizer.deployment;

import au.com.agic.deploymentsync.core.deployment.Deployment;

import java.util.List;

@FunctionalInterface
public interface DeploymentListsComparator {

	/**
	 * Compare two lists of deployments and returns an object with result of that comparison
	 *
	 * @param localList  list of deployments stored locally
	 * @param remoteList list of deployments on remote Wildfly domain controller
	 * @return object that includes list of deployments to deploy/undeploy
	 */
	DeploymentListsComparisonResult compare(List<Deployment> localList, List<Deployment> remoteList);
}
