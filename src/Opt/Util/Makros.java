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
	
	public static boolean atomComparable(Expression e0, Expression e1) {
		return e0 instanceof Atom && e0.getType().hasInt() && e1 instanceof Atom && e1.getType().hasInt();
	}
	
	public static boolean compareAtoms(Expression e0, Expression e1) {
		int val0 = e0.getType().toInt(), val1 = e1.getType().toInt();
		return val0 == val1;
	}
	
}
