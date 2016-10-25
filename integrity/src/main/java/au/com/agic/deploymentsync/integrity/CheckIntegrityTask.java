package au.com.agic.deploymentsync.integrity;

import au.com.agic.deploymentsync.core.aws.Inventory;
import au.com.agic.deploymentsync.core.aws.impl.DynamicInventory;
import au.com.agic.deploymentsync.core.configuration.CoreConfiguration;
import au.com.agic.deploymentsync.core.deployment.DeploymentValidator;
import au.com.agic.deploymentsync.core.deployment.impl.DeploymentValidatorImpl;

import com.amazonaws.services.ec2.model.Instance;

import java.util.List;
import java.util.concurrent.Callable;
import java.util.logging.Level;
import java.util.logging.Logger;

public class CheckIntegrityTask implements Callable<String> {

	private static final Logger LOGGER = Logger.getLogger(CheckIntegrityTask.class.getName());
	private final CoreConfiguration configuration;

	public CheckIntegrityTask(final CoreConfiguration configuration) {
		this.configuration = configuration;
	}

	@Override
	public String call() {

		final Inventory inventory = new DynamicInventory(configuration);
		final List<Instance> instances = inventory.getWildflyDomainControllers();
		final DeploymentValidator deploymentValidator = new DeploymentValidatorImpl();

		if (instances.isEmpty()) {
			LOGGER.log(Level.SEVERE, "No domain controller instances found");
			System.exit(1);
		}

		final List<String> problems = deploymentValidator.validateDeployments(instances);

		if (problems.isEmpty()) {
			LOGGER.log(Level.INFO, "No problems found");
		} else {
			problems.forEach(problem -> LOGGER.log(Level.WARNING, problem));
			System.exit(1); // fail if any problems were found
		}

		return "Ready!";
	}
}
