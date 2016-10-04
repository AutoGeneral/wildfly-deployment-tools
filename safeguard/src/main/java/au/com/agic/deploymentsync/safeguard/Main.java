package au.com.agic.deploymentsync.safeguard;

import au.com.agic.deploymentsync.core.configuration.CoreConfiguration;
import au.com.agic.deploymentsync.core.aws.Inventory;
import au.com.agic.deploymentsync.core.aws.impl.DynamicInventory;
import au.com.agic.deploymentsync.core.configuration.CoreConfigurationImpl;
import au.com.agic.deploymentsync.core.wildfly.DomainControllerClient;
import au.com.agic.deploymentsync.core.wildfly.impl.DomainControllerClientImpl;

import com.amazonaws.services.ec2.model.Instance;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

public final class Main {

	private static final Logger LOGGER = Logger.getLogger(Main.class.getName());
	private static final CoreConfiguration CONFIGURATION = new CoreConfigurationImpl();

	private Main() {

	}

	public static void main(final String... args) throws UnknownHostException {
		final Inventory inventory = new DynamicInventory(CONFIGURATION);
		final List<Instance> instances = inventory.getWildflyDomainControllers();

		if (instances.isEmpty()) {
			LOGGER.log(Level.SEVERE, "No domain controller instances found");
			System.exit(1);
		}

		for (final Instance instance : instances) {
			final InetAddress serverAddress = InetAddress.getByName(instance.getPrivateIpAddress());
			final DomainControllerClient client =
				new DomainControllerClientImpl(serverAddress, CONFIGURATION);

			// get information about slave statuses, iterates through them
			// and restart if we have to
			Map<String, Boolean> result = client.getSlaveActivityStatuses();
			result.keySet().forEach(hostname -> {
				final Boolean status = result.get(hostname);
				LOGGER.log(Level.INFO, hostname + " slave status is active: " + status);

				if (!status) {
					LOGGER.log(Level.INFO, "Restarting slave: " + hostname);
					if (!CONFIGURATION.getDryRun()) {
						client.restartSlave(hostname);
					}
				}
			});

			client.close();
		}
	}
}
