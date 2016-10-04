package au.com.agic.deploymentsync.core.wildfly.operations;

import au.com.agic.deploymentsync.core.deployment.Deployment;

import org.jboss.as.cli.Util;
import org.jboss.dmr.ModelNode;

/**
 * Wildfly Native API remove operation constructor <p> See <a href="https://github.com/wildfly/wildfly-core/blob/master/cli/src/main/java/org/jboss/as/cli/handlers/UndeployHandler.java">UndeployHandler.java</a>
 * to get more info
 */
public class RemoveOperation extends ModelNode {

	private static final long serialVersionUID = 1133166287545061694L;

	public RemoveOperation(final Deployment deployment, final String serverGroup) {
		get(Util.OPERATION).set(Util.REMOVE);
		get(Util.ADDRESS).add(Util.DEPLOYMENT, deployment.getName());
	}
}
