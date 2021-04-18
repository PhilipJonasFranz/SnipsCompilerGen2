package Opt.Util;

import java.util.ArrayList;
import java.util.List;

import Imm.AST.Expression.Atom;
import Imm.AST.Expression.Expression;
import Imm.AST.Statement.Statement;

/**
 * Contains small helper functions and predicates.
 */
public class Makro {

	/**
	 * Returns true if the given expression is an atom,
	 * the atom type has an integer representation and
	 * this representation has not the value 0.
	 */
	public static boolean isAlwaysTrue(Expression e) {
		if (e instanceof Atom && e.getType().hasInt()) {
			int a = e.getType().toInt();
			return a != 0;
		}
		return false;
	}
	
	/**
	 * Returns true if the given expression is an atom,
	 * the atom type has an integer representation and
	 * this representation has the value 0.
	 */
	public static boolean isAlwaysFalse(Expression e) {
		if (e instanceof Atom && e.getType().hasInt()) {
			int a = e.getType().toInt();
			return a == 0;
		}
		return false;
	}
	
	/**
	 * Returns true if both of the given expressions are
	 * atoms and both Atom types have an integer-representation.
	 */
	public static boolean atomComparable(Expression e0, Expression e1) {
		return e0 instanceof Atom && e0.getType().hasInt() && e1 instanceof Atom && e1.getType().hasInt();
	}
	
	/**
	 * Compares the two given expressions by comparing the integer
	 * representation of their types. Assumes that 
	 * {@link #atomComparable(Expression, Expression)} was checked
	 * and returned true.
	 */
	public static boolean compareAtoms(Expression e0, Expression e1) {
		int val0 = e0.getType().toInt(), val1 = e1.getType().toInt();
		return val0 == val1;
	}
	
	/**
	 * Creates a copy of the given list, and copies the statements
	 * using {@link clone()} of statement.
	 */
	public static List<Statement> copyBody(List<Statement> body) {
		List<Statement> copy = new ArrayList();
		for (Statement s : body) copy.add(s.clone());
		return copy;
	}
	
}
