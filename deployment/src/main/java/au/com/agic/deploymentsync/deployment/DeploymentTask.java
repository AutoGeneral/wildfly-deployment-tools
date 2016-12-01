package au.com.agic.deploymentsync.deployment;

import au.com.agic.deploymentsync.core.aws.Inventory;
import au.com.agic.deploymentsync.core.aws.impl.DynamicInventory;
import au.com.agic.deploymentsync.core.constants.Constants;
import au.com.agic.deploymentsync.core.deployment.Deployment;
import au.com.agic.deploymentsync.core.deployment.DeploymentValidator;
import au.com.agic.deploymentsync.core.deployment.impl.DeploymentValidatorImpl;
import au.com.agic.deploymentsync.core.deployment.impl.StandardDeployment;
import au.com.agic.deploymentsync.core.exceptions.WildflyCommunicationException;
import au.com.agic.deploymentsync.core.utils.CoreUtils;
import au.com.agic.deploymentsync.core.wildfly.DomainControllerClient;
import au.com.agic.deploymentsync.core.wildfly.impl.DomainControllerClientImpl;

import com.amazonaws.services.ec2.model.Instance;
import com.amazonaws.services.ec2.model.InstanceStateName;

import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.io.FileUtils;
import org.jboss.dmr.ModelNode;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.Callable;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

public class DeploymentTask implements Callable<String> {

	private static final Logger LOGGER = Logger.getLogger(DeploymentTask.class.getName());
	private final Configuration configuration;

	public DeploymentTask(final Configuration configuration) {
		this.configuration = configuration;
	}

	@Override
	public String call() throws UnknownHostException {
		final Inventory inventory = new DynamicInventory(configuration);
		final List<Instance> instances =
			inventory.getWildflyDomainControllers()
				.stream()
				.filter(instance ->
					InstanceStateName.Running.toString().equals(instance.getState().getName())
				)
				.collect(Collectors.toList());

		Deployment deployment = null;

		try {
			deployment = createDeployment(configuration.getDeploymentPath());
		} catch (IOException ex) {
			LOGGER.log(Level.SEVERE, ex.toString(), ex);
			System.exit(1);
		}

		if (instances.isEmpty()) {
			LOGGER.log(Level.SEVERE, "No running domain controller instances found");
			System.exit(1);
		}

		// Iterate through domain controllers and deploy artifact
		for (final Instance instance : instances) {

			// create a domain controller client
			final InetAddress serverAddress = InetAddress.getByName(instance.getPrivateIpAddress());
			final DomainControllerClient client =
				new DomainControllerClientImpl(serverAddress, configuration);

			try {
				// check if deployment already there
				if (client.hasDeployment(deployment)) {
					deployment.setForceFlag(true);
				}
			} catch (WildflyCommunicationException ex) {
				LOGGER.log(Level.SEVERE, ex.toString(), ex);
				System.exit(1);
			}

			LOGGER.info(deployment.getName() + " app will be DEPLOYED to "
				+ client.getServerAddress().getHostAddress() + " domain, forced: "
				+ deployment.getForceFlag());

			if (!configuration.getDryRun()) {
				final Optional<ModelNode> result = client.deploy(deployment);

				if (CoreUtils.isOperationFailed(result)) {
					LOGGER.log(Level.SEVERE, result.get().toString());
					System.exit(1); // exit with fire
				}
			}
			client.close();
		}

		final DeploymentValidator deploymentValidator = new DeploymentValidatorImpl();
		final List<String> problems = deploymentValidator.validateDeployment(instances, deployment);

		if (problems.isEmpty()) {
			LOGGER.info("Deployment completed successfully");
		} else {
			problems.forEach(problem -> LOGGER.log(Level.WARNING, problem));
			System.exit(1);
		}

		return "Ready!";
	}


	/**
	 * Creates Deployment object for local artifact
	 *
	 * @param filePath path to an artifact
	 * @return deployment object
	 * @throws IOException if there are problems with file access
	 */
	private static Deployment createDeployment(final String filePath) throws IOException {
		final File file;

		// Check if it's directory and grab first matched artifact if it is
		if (Files.isDirectory(Paths.get(filePath))) {
			final String directoryPath = Paths.get(filePath).toAbsolutePath().toString();
			Collection<File> files = FileUtils.listFiles(
				FileUtils.getFile(directoryPath),
				Constants.APP_EXTENSIONS.split("\\|"),
				true
			);

			if (files.isEmpty()) {
				throw new IOException("Directory " + directoryPath + " is empty");
			} else {
				file = files.iterator().next();
				LOGGER.info(file.getAbsolutePath() + " artifact will be deployed");
			}
		} else {
			file = new File(filePath);
		}

		final byte[] sha1 = DigestUtils.sha(new FileInputStream(file));
		return new StandardDeployment(file.getName(), sha1, file.getAbsolutePath());
	}
}
