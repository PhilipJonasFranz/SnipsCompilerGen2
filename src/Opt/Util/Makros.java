package Opt.Util;

import Imm.AST.Expression.Atom;
import Imm.AST.Expression.Expression;

/**
 * Contains small helper functions and predicates.
 */
public class Makros {

	public static boolean isAlwaysTrue(Expression e) {
		if (e instanceof Atom && e.getType().hasInt()) {
			int a = e.getType().toInt();
			return a != 0;
		}
		return false;
	}
	
	public static boolean isAlwaysFalse(Expression e) {
		if (e instanceof Atom && e.getType().hasInt()) {
			int a = e.getType().toInt();
			return a == 0;
		}
		return false;
	}
	
}
