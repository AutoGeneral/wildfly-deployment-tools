package au.com.agic.deploymentsync.synchronizer;

import au.com.agic.deploymentsync.core.configuration.CoreConfigurationImpl;
import au.com.agic.deploymentsync.synchronizer.constants.Constants;
import au.com.agic.deploymentsync.synchronizer.constants.Defaults;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * Configuration object
 */
public class Configuration extends CoreConfigurationImpl {

	private final String localAppLibraryPath;
	private final Set<String> ignoreArtifacts;
	private final boolean isDeployDisabled;
	private final boolean isUndeployDisabled;

	/**
	 * Read system properties, check them and apply defaults if needed
	 */
	public Configuration() {
		localAppLibraryPath =
			System.getProperty(Constants.LOCAL_APPS_LIBRARY_PATH_ARGUMENT_NAME,
				Defaults.LOCAL_APPS_LIBRARY_PATH);

		isDeployDisabled = Boolean.getBoolean(Constants.DISABLE_DEPLOY_ARGUMENT_NAME);
		isUndeployDisabled = Boolean.getBoolean(Constants.DISABLE_UNDEPLOY_ARGUMENT_NAME);

		if (System.getProperty(Constants.IGNORE_ARTIFACTS_ARGUMENT_NAME) != null) {
			final String[] ignoreArtifactsArguments =
				System.getProperty(Constants.IGNORE_ARTIFACTS_ARGUMENT_NAME, "").split(",");
			ignoreArtifacts = new HashSet<>(Arrays.asList(ignoreArtifactsArguments));
		} else {
			ignoreArtifacts = Defaults.getIgnoredArtifacts();
		}
	}

	public final String getLocalAppLibraryPath() {
		return localAppLibraryPath;
	}

	public final Set<String> getIgnoreArtifacts() {
		return Collections.unmodifiableSet(ignoreArtifacts);
	}

	public final boolean isDeployDisabled() {
		return isDeployDisabled;
	}

	public final boolean isUneployDisabled() {
		return isUndeployDisabled;
	}

}
