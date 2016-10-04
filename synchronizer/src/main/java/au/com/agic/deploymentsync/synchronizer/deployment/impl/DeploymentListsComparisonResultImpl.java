package au.com.agic.deploymentsync.synchronizer.deployment.impl;

import au.com.agic.deploymentsync.core.deployment.Deployment;
import au.com.agic.deploymentsync.synchronizer.deployment.DeploymentListsComparisonResult;

import java.util.List;

/**
 * Basic implementation of DeploymentListsComparisonResult
 */
public class DeploymentListsComparisonResultImpl implements DeploymentListsComparisonResult {

	private final List<Deployment> listToDeploy;
	private final List<Deployment> listToUndeploy;

	public DeploymentListsComparisonResultImpl(
		final List<Deployment> listToDeploy, final List<Deployment> listToUndeploy) {

		this.listToDeploy = listToDeploy;
		this.listToUndeploy = listToUndeploy;
	}

	@Override
	public final List<Deployment> getListToDeploy() {
		return listToDeploy;
	}

	@Override
	public final List<Deployment> getListToUndeploy() {
		return listToUndeploy;
	}
}
