package au.com.agic.deploymentsync.core.wildfly.operations;

import au.com.agic.deploymentsync.core.deployment.Deployment;
import au.com.agic.deploymentsync.core.exceptions.WildflyOperationExecutionException;

import org.jboss.as.cli.Util;
import org.jboss.as.cli.operation.OperationFormatException;
import org.jboss.dmr.ModelNode;

import java.io.File;

/**
 * Wildfly Native API redeploy (deploy --force) operation constructor <p> This operation basically
 * was copy pasted from protected method in wildfly-core/cli package See <a
 * href="https://github.com/wildfly/wildfly-core/blob/master/cli/src/main/java/org/jboss/as/cli/handlers/DeployHandler.java">DeployHandler.java</a>
 * to get more info
 */
public class RedeployOperation extends ModelNode {

	private static final long serialVersionUID = 6624427219640799582L;

	public RedeployOperation(final Deployment deployment) {

		get(Util.OPERATION).set(Util.FULL_REPLACE_DEPLOYMENT);
		get(Util.NAME).set(deployment.getName());
		get(Util.RUNTIME_NAME).set(deployment.getName());

		final ModelNode content = get(Util.CONTENT).get(0);
		byte[] bytes;

		try {
			bytes = Util.readBytes(new File(deployment.getLocalPath()));
		} catch (OperationFormatException ex) {
			throw new WildflyOperationExecutionException(ex.getMessage());
		}
		content.get(Util.BYTES).set(bytes);
	}
}
