package au.com.agic.deploymentsync.synchronizer.constants;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public final class Defaults {

	public static final String LOCAL_APPS_LIBRARY_PATH = "./tmp";

	/**
	 * Default argument values
	 */

	private static final Set<String> IGNORED_ARTIFACTS = new HashSet<>();

	static {
		IGNORED_ARTIFACTS.add("activemq-rar-5.13.0.rar");
		IGNORED_ARTIFACTS.add("services.war");
		IGNORED_ARTIFACTS.add("systemcheck.war");
	}

	/**
	 * AWS related defaults
	 */

	private Defaults() {
	}

	public static Set<String> getIgnoredArtifacts() {
		return Collections.unmodifiableSet(IGNORED_ARTIFACTS);
	}

}
