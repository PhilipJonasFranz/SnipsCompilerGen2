package Opt.AST.Util;

public enum OPT_STRATEGY {

	/**
	 * Only accept optimizations that reduce the complexity
	 * of the current program.
	 */
	ON_IMPROVEMENT,
	
	/**
	 * Always accept optimizations.
	 */
	ALWAYS
	
}
