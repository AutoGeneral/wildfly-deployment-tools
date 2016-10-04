package au.com.agic.deploymentsync.core.wildfly.operations;

import org.jboss.as.cli.Util;
import org.jboss.dmr.ModelNode;

/**
 * Wildfly Native API get resources operation constructor
 * Result of the operation is a list of available resources
 */
public class GetResourcesOperation extends ModelNode {

	private static final long serialVersionUID = 8855382992911748282L;

	public GetResourcesOperation() {
		get(Util.OPERATION).set(Util.READ_RESOURCE);
	}
}
