package au.com.agic.deploymentsync.deployment;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.logging.Level;
import java.util.logging.Logger;

final class Main {

	private static final Logger LOGGER = Logger.getLogger(Main.class.getName());
	private static final Configuration CONFIGURATION = new Configuration();

	private Main() {

	}

	public static void main(final String... args) throws ExecutionException, InterruptedException, TimeoutException {
		ExecutorService executor = Executors.newSingleThreadExecutor();
		Future<String> future = executor.submit(new DeploymentTask(CONFIGURATION));

		try {
			LOGGER.info(future.get(CONFIGURATION.getTimeout(), TimeUnit.SECONDS));
		} catch (TimeoutException ex) {
			future.cancel(true);
			LOGGER.log(Level.SEVERE, "Execution interrupted as the time limit exceeded", ex);
			System.exit(1);
		}
		executor.shutdownNow();
	}

}
