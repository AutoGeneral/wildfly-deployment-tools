package au.com.agic.deploymentsync.core.aws.impl;

import au.com.agic.deploymentsync.core.aws.ColourResolver;
import au.com.agic.deploymentsync.core.constants.Constants;
import au.com.agic.deploymentsync.core.exceptions.UnknownStabilityException;

/**
 * Static colour resolver
 * - stable environment is always green
 * - unstable environment is always blue
 */
public class StaticColourResolver implements ColourResolver {

	private enum Stability {
		STABLE(Constants.GREEN),
		UNSTABLE(Constants.BLUE);

		private final String color;

		public String getColor() {
			return color;
		}

		Stability(final String color) {
			this.color = color;
		}
	}

	@Override
	public final String getColourForStability(final String stability) {
		try {
			return Stability.valueOf(stability.toUpperCase()).getColor();
		} catch (IllegalArgumentException ex) {
			throw new UnknownStabilityException(ex.getMessage());
		}
	}
}
