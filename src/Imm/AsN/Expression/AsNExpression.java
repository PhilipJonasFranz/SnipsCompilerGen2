package Imm.AsN.Expression;

import CGen.MemoryMap;
import CGen.RegSet;
import CGen.StackSet;
import Exc.CGEN_EXCEPTION;
import Imm.AST.Expression.AddressOf;
import Imm.AST.Expression.ArrayInit;
import Imm.AST.Expression.ArraySelect;
import Imm.AST.Expression.Atom;
import Imm.AST.Expression.BinaryExpression;
import Imm.AST.Expression.Deref;
import Imm.AST.Expression.Expression;
import Imm.AST.Expression.IDRef;
import Imm.AST.Expression.IDRefWriteback;
import Imm.AST.Expression.InlineCall;
import Imm.AST.Expression.SizeOfExpression;
import Imm.AST.Expression.SizeOfType;
import Imm.AST.Expression.StructSelect;
import Imm.AST.Expression.StructureInit;
import Imm.AST.Expression.TypeCast;
import Imm.AST.Expression.UnaryExpression;
import Imm.AST.Expression.Boolean.Ternary;
import Imm.AsN.AsNNode;
import Imm.AsN.Expression.Boolean.AsNTernary;

public abstract class AsNExpression extends AsNNode {

			/* --- METHODS --- */
	public static AsNExpression cast(Expression e, RegSet r, MemoryMap map, StackSet st) throws CGEN_EXCEPTION {
		/* Relay to Expression type */
		AsNExpression node = null;
		
		if (e instanceof BinaryExpression) {
			node = AsNBinaryExpression.cast((BinaryExpression) e, r, map, st);
		}
		else if (e instanceof UnaryExpression) {
			node = AsNUnaryExpression.cast((UnaryExpression) e, r, map, st);
		}
		else if (e instanceof InlineCall) {
			node = AsNInlineCall.cast((InlineCall) e, r, map, st);
		}
		else if (e instanceof ArrayInit) {
			node = AsNArrayInit.cast((ArrayInit) e, r, map, st); 
		}
		else if (e instanceof StructureInit) {
			node = AsNStructureInit.cast((StructureInit) e, r, map, st); 
		}
		else if (e instanceof ArraySelect) {
			node = AsNArraySelect.cast((ArraySelect) e, r, map, st); 
		}
		else if (e instanceof StructSelect) {
			node = AsNStructSelect.cast((StructSelect) e, r, map, st); 
		}
		else if (e instanceof Ternary) {
			node = AsNTernary.cast((Ternary) e, r, map, st);
		}
		else if (e instanceof IDRef) {
			node = AsNIdRef.cast((IDRef) e, r, map, st, 0);
		}
		else if (e instanceof IDRefWriteback) {
			node = AsNIDRefWriteback.cast((IDRefWriteback) e, r, map, st);
		}
		else if (e instanceof Atom) {
			node = AsNAtom.cast((Atom) e, r, map, st, 0); 
		}
		else if (e instanceof SizeOfType) {
			node = AsNSizeOfType.cast((SizeOfType) e, r, map, st, 0);
		}
		else if (e instanceof SizeOfExpression) {
			node = AsNSizeOfExpression.cast((SizeOfExpression) e, r, map, st, 0);
		}
		else if (e instanceof AddressOf) {
			node = AsNAddressOf.cast((AddressOf) e, r, map, st, 0); 
		}
		else if (e instanceof Deref) {
			node = AsNDeref.cast((Deref) e, r, map, st, 0); 
		}
		else if (e instanceof TypeCast) {
			node = AsNTypeCast.cast((TypeCast) e, r, map, st); 
		}
		else throw new CGEN_EXCEPTION(e.getSource(), "No injection cast available for " + e.getClass().getName());
	
		e.castedNode = node;
		return node;
	}
	
}
