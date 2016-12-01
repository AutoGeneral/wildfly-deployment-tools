package au.com.agic.deploymentsync.synchronizer.utlis;

import static com.google.common.io.Files.getFileExtension;

import au.com.agic.deploymentsync.core.constants.Constants;
import au.com.agic.deploymentsync.core.deployment.Deployment;
import au.com.agic.deploymentsync.core.deployment.impl.StandardDeployment;

import org.apache.commons.codec.digest.DigestUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;

public final class DeploymentUtils {

	private static final Logger LOGGER = Logger.getLogger(DeploymentUtils.class.getName());
	private static final Pattern COMPILE = Pattern.compile(Constants.APP_EXTENSIONS);

	private DeploymentUtils() {
	}

	/**
	 * Goes through the list of folders (non-recursively) and generate a list of deployments
	 * with SHA1 checksums
	 *
	 * @param deploymentsPaths list of folders split by ','
	 * @return list of deployments in paths
	 */
	public static List<Deployment> getLocalDeploymentsList(final String deploymentsPaths) {
		List<Deployment> deployments = new ArrayList<>();

		try {
			for (String path : deploymentsPaths.split(",")) {
				Files
					.walk(Paths.get(path), 1)
					.filter(Files::isRegularFile)
					.filter(filePath ->
						COMPILE.matcher(getFileExtension(filePath.toString())).matches())
					.map(DeploymentUtils::readDeployment)
					.filter(Optional::isPresent)
					.forEach(deployment -> deployments.add(deployment.get()));
			}
		} catch (IOException ex) {
			LOGGER.log(Level.INFO, "Can't read local deployments", ex);
		}

		return deployments;
	}

	private static Optional<Deployment> readDeployment(final Path filePath) {
		try {
			final File file = new File(filePath.toString());
			final InputStream inputStream = new FileInputStream(file);
			final byte[] sha1 = DigestUtils.sha(inputStream);
			return Optional.of(new StandardDeployment(
				filePath.getFileName().toString(),
				sha1,
				filePath.toString()
			));
		} catch (IOException ex) {
			LOGGER.log(Level.INFO, "Can't read local deployments", ex);
			return Optional.empty();
		}
	}
}
