package au.com.agic.deploymentsync.deployment;

import static au.com.agic.deploymentsync.deployment.constants.Defaults.WAIT_AFTER_DOMAIN_CONTROLLER_START_ATTEMPT;

import au.com.agic.deploymentsync.core.aws.Inventory;
import au.com.agic.deploymentsync.core.aws.impl.DynamicInventory;
import au.com.agic.deploymentsync.core.constants.Constants;
import au.com.agic.deploymentsync.core.constants.Defaults;
import au.com.agic.deploymentsync.core.deployment.Deployment;
import au.com.agic.deploymentsync.core.deployment.DeploymentValidator;
import au.com.agic.deploymentsync.core.deployment.impl.DeploymentValidatorImpl;
import au.com.agic.deploymentsync.core.deployment.impl.StandardDeployment;
import au.com.agic.deploymentsync.core.exceptions.WildflyCommunicationException;
import au.com.agic.deploymentsync.core.utils.CoreUtils;
import au.com.agic.deploymentsync.core.wildfly.DomainControllerClient;
import au.com.agic.deploymentsync.core.wildfly.impl.DomainControllerClientImpl;

import com.amazonaws.services.ec2.AmazonEC2;
import com.amazonaws.services.ec2.AmazonEC2Client;
import com.amazonaws.services.ec2.model.Instance;
import com.amazonaws.services.ec2.model.InstanceStateName;
import com.amazonaws.services.ec2.model.StartInstancesRequest;

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
	private static final long MS = 1000;

	private final Configuration configuration;
	private final AmazonEC2 ec2;

	private boolean hasDomainControllersStartAttempted = false;

	public DeploymentTask(final Configuration configuration) {
		this.configuration = configuration;

		// Use credentials from arguments if possible, otherwise use default .aws/credentials
		if (configuration.getAwsCredentials() != null) {
			ec2 = new AmazonEC2Client(configuration.getAwsCredentials());
		} else {
			ec2 = new AmazonEC2Client();
		}
		ec2.setRegion(Defaults.AWS_REGION);
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
			LOGGER.log(Level.WARNING, "No running domain controller instances found");
			attemptDomainContollerStart();
			return "";
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
	 * Attempts to start domain controllers and reruns the task
	 *
	 * @throws UnknownHostException
	 */
	private void attemptDomainContollerStart() throws UnknownHostException {

		if (hasDomainControllersStartAttempted) {
			LOGGER.log(Level.SEVERE, "Domain Controller start already attempted. Exiting now...");
			System.exit(1);
		} else {
			LOGGER.log(Level.INFO, "Attempting to start domain controllers...");
			startStoppedWildflyDomainControllers();
			hasDomainControllersStartAttempted = true;
			try {
				Thread.sleep(WAIT_AFTER_DOMAIN_CONTROLLER_START_ATTEMPT * MS);
			} catch (InterruptedException ex) {
				LOGGER.log(Level.SEVERE, "Timer's dead baby. Timer's dead.", ex);
			}
			call();
		}
	}

	/***
	 * Sends start request to AWS EC2 API to start domain controllers
	 */
	private void startStoppedWildflyDomainControllers() {
		final Inventory inventory = new DynamicInventory(configuration);
		final List<Instance> stoppedWildflyDomainControllers =
			inventory.getWildflyDomainControllers()
				.stream()
				.filter(instance ->
					InstanceStateName.Stopped.toString().equals(instance.getState().getName())
				)
				.collect(Collectors.toList());

		if (stoppedWildflyDomainControllers.isEmpty()) {
			// nothing we can do then
		}

		final StartInstancesRequest startInstancesRequest = new StartInstancesRequest()
			.withInstanceIds(stoppedWildflyDomainControllers
				.stream().map(Instance::getInstanceId).collect(Collectors.toList())
			);

		ec2.startInstances(startInstancesRequest);
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
