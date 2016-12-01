package au.com.agic.deploymentsync.integrity;

import au.com.agic.deploymentsync.core.configuration.CoreConfiguration;
import au.com.agic.deploymentsync.core.configuration.CoreConfigurationImpl;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.logging.Logger;

final class Main {

	private static final Logger LOGGER = Logger.getLogger(Main.class.getName());
	private static final CoreConfiguration CONFIGURATION = new CoreConfigurationImpl();

	private Main() {

	}

	public static void main(final String... args) throws ExecutionException, InterruptedException, TimeoutException {
		ExecutorService executor = Executors.newSingleThreadExecutor();
		Future<String> future = executor.submit(new CheckIntegrityTask(CONFIGURATION));

		try {
			LOGGER.info(future.get(CONFIGURATION.getTimeout(), TimeUnit.SECONDS));
		} catch (TimeoutException exception) {
			future.cancel(true);
			LOGGER.warning("Execution interrupted as the time limit exceeded");
			throw new TimeoutException(exception.getMessage());
		}
		executor.shutdownNow();
	}
}
