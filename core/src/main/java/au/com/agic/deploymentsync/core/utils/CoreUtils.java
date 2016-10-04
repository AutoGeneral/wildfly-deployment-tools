package au.com.agic.deploymentsync.core.utils;

import org.jboss.as.cli.Util;
import org.jboss.dmr.ModelNode;

import java.util.Optional;

public final class CoreUtils {

	private CoreUtils() {
	}

	public static boolean isOperationFailed(final ModelNode result) {
		return "failed".equals(result.get().get(Util.OUTCOME).asString());
	}

	public static boolean isOperationFailed(final Optional<ModelNode> result) {
		return result.isPresent() && isOperationFailed(result.get());
	}
}
