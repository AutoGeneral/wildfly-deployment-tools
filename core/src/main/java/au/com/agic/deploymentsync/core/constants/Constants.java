package au.com.agic.deploymentsync.core.constants;

public final class Constants {

	/**
	 * Configuration system properties names
	 */

	public static final String WILDFLY_LOGIN_ARGUMENT_NAME = "wildflyLogin";

	public static final String WILDFLY_PASSWORD_ARGUMENT_NAME = "wildflyPassword";

	public static final String ENVIRONMENT_ARGUMENT_NAME = "environment";

	public static final String STABILITY_ARGUMENT_NAME = "stability";

	public static final String DRY_RUN_ARGUMENT_NAME = "dryRun";

	public static final String AWS_ACCESS_KEY_ID_ARGUMENT_NAME = "awsAccessKeyId";

	public static final String AWS_SECRET_ACCESS_KEY_ARGUMENT_NAME = "awsSecretAccessKey";

	public static final String TIMEOUT_ARGUMENT_NAME = "timeout";

	/**
	 * Stability colours, names
	 */

	public static final String GREEN = "green";

	public static final String BLUE = "blue";

	public static final String STABLE = "stable";

	public static final String UNSTABLE = "unstable";

	/**
	 * AWS related constants
	 */

	public static final String TAG_ENVIRONMENT = "Environment";

	public static final String TAG_COLOUR = "Color";

	public static final String TAG_ROLE = "Role";

	/**
	 * AWS Instance roles
	 */

	public static final String ROLE_WILDFLY_DOMAIN_CONTROLLER = "wildfly-domain";

	public static final String ROLE_WILDFLY_SLAVE = "wildfly-slave";

	/**
	 * Misc
	 */

	public static final String APP_EXTENSIONS = "war|ear|jar";

	public static final String WILDFLY_CLIENT_PROTOCOL = "https-remoting";

	private Constants() {
	}
}
