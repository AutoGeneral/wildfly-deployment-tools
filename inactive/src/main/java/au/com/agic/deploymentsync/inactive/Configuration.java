package au.com.agic.deploymentsync.inactive;

import au.com.agic.deploymentsync.core.configuration.CoreConfigurationImpl;
import au.com.agic.deploymentsync.inactive.constants.Constants;
import au.com.agic.deploymentsync.inactive.constants.Defaults;


/**
 * Configuration object
 */
public class Configuration extends CoreConfigurationImpl {

	private final int inactiveTimeout;

	public int getInactiveTimeout() {
		return inactiveTimeout;
	}

	/**
	 * Read system properties, check them and apply defaults if needed
	 */
	public Configuration() {
		inactiveTimeout = Integer.getInteger(Constants.DOMAIN_CONTROLLER_INACTIVITY_ARGUMENT_NAME,
				Defaults.DOMAIN_CONTROLLER_INACTIVITY_TIMEOUT);
	}
}
