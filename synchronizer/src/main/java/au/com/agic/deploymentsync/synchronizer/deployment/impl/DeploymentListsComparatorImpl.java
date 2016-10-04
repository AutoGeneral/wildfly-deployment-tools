package au.com.agic.deploymentsync.synchronizer.deployment.impl;

import au.com.agic.deploymentsync.core.deployment.Deployment;
import au.com.agic.deploymentsync.core.deployment.impl.StandardDeployment;
import au.com.agic.deploymentsync.synchronizer.deployment.DeploymentListsComparator;
import au.com.agic.deploymentsync.synchronizer.deployment.DeploymentListsComparisonResult;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class DeploymentListsComparatorImpl implements DeploymentListsComparator {

	private static final Logger LOGGER = Logger.getLogger(DeploymentListsComparatorImpl.class.getName());

	@Override
	public final DeploymentListsComparisonResult compare(final List<Deployment> localList,
		final List<Deployment> remoteList) {
		// These lists will be a part of result object we will return at the end
		List<Deployment> toDeploy = new ArrayList<>();
		List<Deployment> toUndeploy = new ArrayList<>();

		// Create a hashmaps for faster search of deployments
		Map<String, Deployment> localListHashMap = getDeploymentsMap(localList);
		Map<String, Deployment> remoteListHashMap = getDeploymentsMap(remoteList);

		// Create a set of all the deployments (local and remote)
		Set<String> keys = Stream
			.concat(localListHashMap.keySet().stream(), remoteListHashMap.keySet().stream())
			.collect(Collectors.toSet());

		// Iterate through the set of deployments and check what should we do with deployment:
		// - put to deploy list if it only exists locally
		// - put to deploy list if it exists locally and remotely but checksums don't match
		// - put to undeploy list if it only exists remotely
		// otherwise do nothing
		keys.forEach(key -> {
			Deployment localDeployment = localListHashMap.get(key);
			Deployment remoteDeployment = remoteListHashMap.get(key);

			if (localDeployment == null) {
				toUndeploy.add(remoteDeployment);
			} else {
				if (remoteDeployment == null) {
					toDeploy.add(localDeployment);
				} else if (!localDeployment.equals(remoteDeployment)) {
					Deployment deployment = new StandardDeployment(
						localDeployment.getName(),
						localDeployment.getSha1()
					);
					deployment.setLocalPath(localDeployment.getLocalPath());
					deployment.setForceFlag(true);
					toDeploy.add(deployment);

					LOGGER.info("Hashsums are different for " + deployment.getName()
						+ ", local version will be deployed with --force");
				}
			}
		});

		return new DeploymentListsComparisonResultImpl(toDeploy, toUndeploy);
	}

	/**
	 * Converts list of deployments into the map with key = deployment name
	 * There whould be no duplicates in normal life but we should be able to handle that
	 *
	 * @param deployments list of deployments
	 * @return map of deployments
	 */
	private Map<String, Deployment> getDeploymentsMap(final List<Deployment> deployments) {
		return deployments.stream().collect(Collectors.toMap(Deployment::getName, deployment -> deployment,
			(deployment1, deployment2) -> {
				LOGGER.log(Level.WARNING, "Duplicate deployment found " + deployment1.getName());
				return deployment1;
			}));
	}
}
