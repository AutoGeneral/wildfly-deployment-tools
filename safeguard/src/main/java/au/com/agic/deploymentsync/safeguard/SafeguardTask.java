package au.com.agic.deploymentsync.safeguard;

import au.com.agic.deploymentsync.core.aws.Inventory;
import au.com.agic.deploymentsync.core.aws.impl.DynamicInventory;
import au.com.agic.deploymentsync.core.configuration.CoreConfiguration;
import au.com.agic.deploymentsync.core.wildfly.DomainControllerClient;
import au.com.agic.deploymentsync.core.wildfly.impl.DomainControllerClientImpl;

import com.amazonaws.services.ec2.model.Instance;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.logging.Level;
import java.util.logging.Logger;

public class SafeguardTask implements Callable<String> {

	private static final Logger LOGGER = Logger.getLogger(SafeguardTask.class.getName());
	private final CoreConfiguration configuration;

	public SafeguardTask(final CoreConfiguration configuration) {
		this.configuration = configuration;
	}

	@Override
	public String call() throws UnknownHostException {

		final Inventory inventory = new DynamicInventory(configuration);
		final List<Instance> instances = inventory.getWildflyDomainControllers();

		if (instances.isEmpty()) {
			LOGGER.log(Level.SEVERE, "No domain controller instances found");
			System.exit(1);
		}

		for (final Instance instance : instances) {
			final InetAddress serverAddress = InetAddress.getByName(instance.getPrivateIpAddress());
			final DomainControllerClient client =
				new DomainControllerClientImpl(serverAddress, configuration);

			// get information about slave statuses, iterates through them
			// and restart if we have to
			Map<String, Boolean> result = client.getSlaveActivityStatuses();
			result.keySet().forEach(hostname -> {
				final Boolean status = result.get(hostname);
				LOGGER.log(Level.INFO, hostname + " slave status is active: " + status);

				if (!status) {
					LOGGER.log(Level.INFO, "Restarting slave: " + hostname);
					if (!configuration.getDryRun()) {
						client.restartSlave(hostname);
					}
				}
			});

			client.close();
		}
		return "Ready!";
	}
}
