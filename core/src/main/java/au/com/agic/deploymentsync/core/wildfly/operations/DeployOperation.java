package au.com.agic.deploymentsync.core.wildfly.operations;

import au.com.agic.deploymentsync.core.deployment.Deployment;
import au.com.agic.deploymentsync.core.exceptions.WildflyOperationExecutionException;

import org.jboss.as.cli.Util;
import org.jboss.as.cli.operation.OperationFormatException;
import org.jboss.dmr.ModelNode;

import java.io.File;

/**
 * Wildfly Native API deploy operation constructor <p> This operation basically was copy pasted from
 * protected method in wildfly-core/cli package See <a href="https://github.com/wildfly/wildfly-core/blob/master/cli/src/main/java/org/jboss/as/cli/handlers/DeployHandler.java">DeployHandler.java</a>
 * to get more info
 */
public class DeployOperation extends ModelNode {

	private static final long serialVersionUID = -7777171342348198384L;

	public DeployOperation(final Deployment deployment, final String serverGroup) {

		ModelNode deployRequest = new ModelNode();
		deployRequest.get(Util.OPERATION).set(Util.COMPOSITE);
		deployRequest.get(Util.ADDRESS).setEmptyList();

		final ModelNode steps = deployRequest.get(Util.STEPS);
		steps.add(Util.configureDeploymentOperation(Util.ADD, deployment.getName(), serverGroup));
		steps
			.add(Util.configureDeploymentOperation(Util.DEPLOY, deployment.getName(), serverGroup));

		get(Util.OPERATION).set(Util.COMPOSITE);
		get(Util.ADDRESS).setEmptyList();

		final ModelNode deploymentSteps = get(Util.STEPS);
		try {
			deploymentSteps.add(buildAddRequest(
				new File(deployment.getLocalPath()), deployment.getName(), deployment.getName()));
		} catch (OperationFormatException ex) {
			throw new WildflyOperationExecutionException(ex.getMessage());
		}
		deploymentSteps.add(deployRequest);
	}

	private ModelNode buildAddRequest(final File file, final String name, final String runtimeName)
		throws OperationFormatException {

		final ModelNode request = new ModelNode();
		request.get(Util.OPERATION).set(Util.ADD);
		request.get(Util.ADDRESS, Util.DEPLOYMENT).set(name);
		if (runtimeName != null) {
			request.get(Util.RUNTIME_NAME).set(runtimeName);
		}
		final ModelNode content = request.get(Util.CONTENT).get(0);
		byte[] bytes = Util.readBytes(file);
		content.get(Util.BYTES).set(bytes);
		return request;
	}
}
