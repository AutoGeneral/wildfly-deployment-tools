package au.com.agic.deploymentsync.core.deployment;

/**
 * A service that aids in working with deployments
 */
public interface DeploymentUtils {

	/**
	 * We build WARs that have Tomcat file names like whatever#path##version.war. These need to be
	 * sanitised before being deployed in Wildfly
	 * @param str The Tomcat filename
	 * @return The sanitised filename
	 */
	String sanitizeName(final String str);
}
