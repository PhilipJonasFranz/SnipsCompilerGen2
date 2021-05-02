package Opt.AST.Util;

public enum OPT_METRIC {

	/**
	 * Use the amount of AST nodes. Generally, lesser
	 * AST nodes will mean fewer instructions generated.
	 */
	AST_SIZE,
	
	/**
	 * Use the expected instructions metric. This metric
	 * attempts to guess how many asm instructions are required
	 * to cast the current AST. This metric is heuristic, and
	 * thus not always perfect.
	 */
	EXPECTED_INSTRUCTIONS,
	
	/**
	 * Use the expected cpu cycles metric. This metric
	 * attempts to guess how many cpu cycles are required
	 * to cast the current AST. This metric is heuristic, and
	 * thus not always perfect.
	 */
	EXPECTED_CPU_CYCLES;
	
}
