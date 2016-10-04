package au.com.agic.deploymentsync.core.deployment;

import com.amazonaws.services.ec2.model.Instance;

import java.util.List;

public interface DeploymentValidator {

	/**
	 * Validates all deployments across domain controllers instances
	 * @param instances domain controllers
	 * @return list of problems
	 */
	List<String> validateDeployments(List<Instance> instances);

	/**
	 * Validates deployment across domain controllers instances
	 * @param instances domain controllers
	 * @param deployment deployment to validate
	 * @return list of problems
	 */
	List<String> validateDeployment(List<Instance> instances, final Deployment deployment);
}
