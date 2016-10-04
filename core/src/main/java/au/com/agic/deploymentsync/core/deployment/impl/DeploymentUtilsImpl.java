package au.com.agic.deploymentsync.core.deployment.impl;

import au.com.agic.deploymentsync.core.deployment.DeploymentUtils;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;

/**
 * implementation of the DeploymentUtils service
 */
public class DeploymentUtilsImpl implements DeploymentUtils {

	@Override
	public String sanitizeName(final String str) {
		if (StringUtils.isBlank(str)) {
			return "";
		}

		return FilenameUtils.getBaseName(str)
			// We want to trim off the version information, which is after a double pound sign.
			.replaceAll("##.*", "")
			// It looks like Wildfly java api URLencodes deployment name so we will
			// replace # pretending that we have an encoded name to avoid Wildfly errors
			.replaceAll("#", "%23")

			+ "." + FilenameUtils.getExtension(str);

	}
}
