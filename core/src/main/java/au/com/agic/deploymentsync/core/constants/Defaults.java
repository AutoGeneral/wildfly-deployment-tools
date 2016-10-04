package au.com.agic.deploymentsync.core.constants;

import com.amazonaws.regions.Region;
import com.amazonaws.regions.Regions;

public final class Defaults {

	public static final String WILDFLY_LOGIN = "agwildflyadmin";

	public static final String SERVER_GROUP = "main-server-group";

	/**
	 * Default argument values
	 */

	public static final String ENVIRONMENT = "int";

	public static final String STABILITY = "unstable";

	/**
	 * AWS related defaults
	 */

	public static final Region AWS_REGION = Region.getRegion(Regions.AP_SOUTHEAST_2);


	private Defaults() {
	}
}
