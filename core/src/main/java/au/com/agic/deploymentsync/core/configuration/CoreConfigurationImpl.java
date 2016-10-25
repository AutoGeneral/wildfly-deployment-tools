package au.com.agic.deploymentsync.core.configuration;

import au.com.agic.deploymentsync.core.constants.Constants;
import au.com.agic.deploymentsync.core.constants.Defaults;

import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.util.StringUtils;

/**
 * Configuration object
 */
public class CoreConfigurationImpl implements CoreConfiguration {

	private final String wildflyLogin;
	private final String wildflyPassword;
	private final String environment;
	private final String stability;
	private final BasicAWSCredentials awsCredentials;
	private final Boolean dryRun;
	private final Integer timeout;

	@Override
	public final String getWildflyLogin() {
		return wildflyLogin;
	}

	@Override
	public final String getWildflyPassword() {
		return wildflyPassword;
	}

	@Override
	public final String getEnvironment() {
		return environment;
	}

	@Override
	public final String getStability() {
		return stability;
	}

	@Override
	public final BasicAWSCredentials getAwsCredentials() {
		return awsCredentials;
	}

	@Override
	public final Boolean getDryRun() {
		return dryRun;
	}

	@Override
	public final Integer getTimeout() {
		return timeout;
	}

	/**
	 * Read system properties, check them and apply defaults if needed
	 */
	public CoreConfigurationImpl() {
		wildflyLogin =
			System.getProperty(Constants.WILDFLY_LOGIN_ARGUMENT_NAME, Defaults.WILDFLY_LOGIN);
		wildflyPassword = System.getProperty(Constants.WILDFLY_PASSWORD_ARGUMENT_NAME);

		final String awsAccessKeyId = System.getProperty(Constants.AWS_ACCESS_KEY_ID_ARGUMENT_NAME);
		final String
			awsSecretAccessKey =
			System.getProperty(Constants.AWS_SECRET_ACCESS_KEY_ARGUMENT_NAME);

		if (awsAccessKeyId != null && awsSecretAccessKey != null) {
			awsCredentials = new BasicAWSCredentials(awsAccessKeyId, awsSecretAccessKey);
		} else {
			awsCredentials = null;
		}

		environment = System.getProperty(Constants.ENVIRONMENT_ARGUMENT_NAME, Defaults.ENVIRONMENT);
		stability = System.getProperty(Constants.STABILITY_ARGUMENT_NAME, Defaults.STABILITY);
		dryRun = Boolean.getBoolean(Constants.DRY_RUN_ARGUMENT_NAME);
		timeout = Integer.getInteger(Constants.TIMEOUT_ARGUMENT_NAME, Defaults.TIMEOUT);

		if (StringUtils.isNullOrEmpty(wildflyPassword)) {
			throw new IllegalArgumentException(
				"\"" + Constants.WILDFLY_PASSWORD_ARGUMENT_NAME + "\" argument must be specified");
		}
	}
}
