package Opt.AST.Util;

import Imm.AST.Expression.*;
import Imm.AST.Statement.BreakStatement;
import Imm.AST.Statement.ContinueStatement;
import Imm.AST.Statement.Declaration;
import Imm.AST.Statement.Statement;
import Imm.AST.SyntaxElement;
import Tools.ASTNodeVisitor;

import java.util.ArrayList;
import java.util.List;

/**
 * Contains pre-built AST-Visitor matcher statements.
 */
public class Matcher {

	/**
	 * Checks if in the given subtree defined by the given statement, has an address reference via address of
	 * is made to a variable with given origin declaration. If so, return true.
	 */
	public static boolean hasAddressReference(SyntaxElement s, Declaration dec) {
		return !s.visit(x -> {
			if (x instanceof AddressOf aof) {
				if (aof.expression instanceof IDRef)
					return (((IDRef) aof.expression).origin.equals(dec));
				else if (aof.expression instanceof IDRefWriteback) 
					return (((IDRefWriteback) aof.expression).idRef.origin.equals(dec));
				else if (aof.expression instanceof StructSelect) 
					/* Struct will be on the stack anyway */
					return true;
				else if (aof.expression instanceof StructureInit) 
					return false;
				else if (aof.expression instanceof ArraySelect) {
					return (((ArraySelect) aof.expression).idRef.origin.equals(dec));
				}
			}
			
			return false;
		}).isEmpty();
	}
	
	public static boolean hasRefTo(SyntaxElement s, Declaration dec) {
		return !(s.visit(x -> {
			if (x instanceof IDRef ref) {
				return ref.origin.equals(dec);
			}
			return false;
		}).isEmpty());
	}
	
	public static boolean noStateDependentSubExpression(Expression e, ProgramState state) {
		List<Expression> list = e.visit(x -> {
			if (x instanceof IDRef ref) {
				return state.getWrite(ref.origin);
			}
			else if (x instanceof IDRefWriteback wb) {
				return state.getAll(wb.idRef.origin);
			}
			else return x instanceof InlineCall;
		});
		
		return list.isEmpty();
	}
	
	/**
	 * Returns true if the given expression contains either an 
	 * IDRefWriteback, StructSelectWriteback or InlienCall node.
	 */
	public static boolean containsStateChangingSubExpression(Expression e) {
		return e.visit(x -> x instanceof IDRefWriteback || x instanceof StructSelectWriteback || x instanceof InlineCall).isEmpty();
	}
	
	/**
	 * Check if the given expression is morphable. An expression has to fullfill the following
	 * criteria to be considered morphable:
	 * 
	 * - The expression may not contain a StructSelectWriteback
	 * - The expression may not contain an IDRefWriteback where the origin is equal to the given origin.
	 * 
	 * @param e The expression that should be checked if it is morphable.
	 * @param origin The declaration that is the origin of IDRefs that are the morphing target, 
	 * 		or the sub-expressions that will be replaced.
	 * @return True if the given expression is morphable.
	 */
	public static boolean isMorphable(Expression e, Declaration origin) {
		List<Expression> list = e.visit(x -> {
			if (x instanceof StructSelectWriteback) return true;
			else if (x instanceof IDRefWriteback wb) {
				return wb.idRef.origin.equals(origin);
			}
			else if (x instanceof ArraySelect s) {
				return s.idRef.origin.equals(origin);
			}
			
			return false;
		});
		
		return list.isEmpty();
	}
	
	public static boolean hasLoopUnrollBlockerStatements(List<Statement> body) {
		List<Statement> blockers = Matcher.visitBody(body, x -> x instanceof Declaration || x instanceof ContinueStatement || x instanceof BreakStatement);
		return !blockers.isEmpty();
	}
	
	/**
	 * Check if the given expressions contains any IDRefs where the origin has
	 * been overwritten in the given state.
	 */
	public static boolean hasOverwrittenVariables(Expression e, ProgramState state) {
		List<Expression> list = e.visit(x -> {
			if (e instanceof IDRef ref) {
				return state.getWrite(ref.origin);
			}
			return false;
		});
		
		return !list.isEmpty();
	}
	
	public static <T extends SyntaxElement> List<T> visitBody(List<Statement> body, ASTNodeVisitor<T> visitor) {
		List<T> result = new ArrayList();
		for (Statement s : body) result.addAll(s.visit(visitor));
		return result;
	}
	
	public static boolean isSubExpression(Expression base, Expression sub) {
		return base.visit(x -> true).contains(sub);
	}
	
}
