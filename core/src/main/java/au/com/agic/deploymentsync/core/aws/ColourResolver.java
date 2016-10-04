package au.com.agic.deploymentsync.core.aws;

/**
 * We have a few different colour segments in our environments. One of them has stability name
 * 'stable', another - 'unstable'. There 1-to-1 mapping between colours and stability names and we
 * need to get colours based on stability name dynamically.
 */
@FunctionalInterface
public interface ColourResolver {

	/**
	 * Returns a colour based on stability name
	 *
	 * @param stability name of the stability
	 * @return colour
	 */
	String getColourForStability(final String stability);
}
