package au.com.agic.deploymentsync.core.wildfly.operations;

import au.com.agic.deploymentsync.core.deployment.Deployment;
import au.com.agic.deploymentsync.core.exceptions.WildflyOperationExecutionException;

import org.jboss.as.cli.Util;
import org.jboss.as.cli.operation.OperationFormatException;
import org.jboss.as.cli.operation.impl.DefaultOperationRequestBuilder;
import org.jboss.dmr.ModelNode;

/**
 * Wildfly Native API undeploy operation constructor <p> See <a href="https://github.com/wildfly/wildfly-core/blob/master/cli/src/main/java/org/jboss/as/cli/handlers/UndeployHandler.java">UndeployHandler.java</a>
 * to get more info
 */
public class UndeployOperation extends ModelNode {

	private static final long serialVersionUID = 6768889605834269563L;

	public UndeployOperation(final Deployment deployment, final String serverGroup) {

		get(Util.OPERATION).set(Util.COMPOSITE);
		get(Util.ADDRESS).setEmptyList();
		final ModelNode steps = get(Util.STEPS);

		steps.add(
			Util.configureDeploymentOperation(Util.UNDEPLOY, deployment.getName(), serverGroup));
		steps
			.add(Util.configureDeploymentOperation(Util.REMOVE, deployment.getName(), serverGroup));

		DefaultOperationRequestBuilder builder = new DefaultOperationRequestBuilder();
		builder.setOperationName(Util.REMOVE);
		builder.addNode(Util.DEPLOYMENT, deployment.getName());

		try {
			steps.add(builder.buildRequest());
		} catch (OperationFormatException ex) {
			throw new WildflyOperationExecutionException(ex.getMessage());
		}
	}
}
