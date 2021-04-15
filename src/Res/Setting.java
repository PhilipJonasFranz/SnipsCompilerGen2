package Res;

/**
 * Contains AST-Optimizer Settings.
 */
public class Setting {

			/* ---< CONTEXT CHECKING >--- */
	/**
	 * Substitution replaces for example an IDRef with the current
	 * value of the declaration.
	 */
	public static final String SUBSTITUTION = "substitution";
	
	/**
	 * If this setting is active, the OPT_DONE flag should not be
	 * set in the scope of the setting. Used to test if under a given
	 * condition an optimization is done for example.
	 */
	public static final String PROBE = "probe";
	
}
