package au.com.agic.deploymentsync.deployment;

import au.com.agic.deploymentsync.core.configuration.CoreConfigurationImpl;
import au.com.agic.deploymentsync.deployment.constants.Constants;
import au.com.agic.deploymentsync.deployment.constants.Defaults;


/**
 * Configuration object
 */
public class Configuration extends CoreConfigurationImpl {

	private final String deploymentPath;

	/**
	 * Read system properties, check them and apply defaults if needed
	 */
	public Configuration() {
		deploymentPath =
			System.getProperty(Constants.DEPLOYMENT_ARGUMENT_NAME, Defaults.DEPLOYMENT_PATH);
	}

	public final String getDeploymentPath() {
		return deploymentPath;
	}

}
