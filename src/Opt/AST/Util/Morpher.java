package Opt.AST.Util;

import Exc.SNIPS_EXC;
import Imm.AST.Expression.ArrayInit;
import Imm.AST.Expression.ArraySelect;
import Imm.AST.Expression.Atom;
import Imm.AST.Expression.Expression;
import Imm.AST.Expression.IDRef;
import Imm.AST.Expression.IDRefWriteback;
import Imm.AST.Expression.InlineCall;
import Imm.AST.Expression.NFoldExpression;
import Imm.AST.Expression.StructureInit;
import Imm.AST.Expression.TempAtom;
import Imm.AST.Expression.Boolean.Ternary;
import Tools.ASTNodeVisitor;

public class Morpher {

	public static Expression morphExpression(Expression source, ASTNodeVisitor visitor, Expression replacement) {
		if (source instanceof Atom) return source;
		else if (source instanceof IDRef) return source;
		else if (source instanceof IDRefWriteback) return source;
		else if (source instanceof NFoldExpression nfold) {
			for (int i = 0; i < nfold.operands.size(); i++) {
				Expression e0 = nfold.operands.get(i);
				if (visitor.visit(e0)) 
					nfold.operands.set(i, replacement.clone());
				else 
					nfold.operands.set(i, morphExpression(nfold.operands.get(i), visitor, replacement));
			}
		}
		else if (source instanceof ArrayInit init) {
			for (int i = 0; i < init.elements.size(); i++) {
				Expression e0 = init.elements.get(i);
				if (visitor.visit(e0)) 
					init.elements.set(i, replacement.clone());
				else 
					init.elements.set(i, morphExpression(init.elements.get(i), visitor, replacement));
			}
		}
		else if (source instanceof StructureInit init) {
			for (int i = 0; i < init.elements.size(); i++) {
				Expression e0 = init.elements.get(i);
				if (visitor.visit(e0)) 
					init.elements.set(i, replacement.clone());
				else 
					init.elements.set(i, morphExpression(init.elements.get(i), visitor, replacement));
			}
		}
		else if (source instanceof TempAtom atom) {
			if (atom.base != null && visitor.visit(atom.base)) {
				atom.base = replacement.clone();
			}
		}
		else if (source instanceof Ternary tern) {
			if (visitor.visit(tern.condition)) tern.condition = replacement.clone();
			else tern.condition = morphExpression(tern.condition, visitor, replacement);
			
			if (visitor.visit(tern.left)) tern.left = replacement.clone();
			else tern.left = morphExpression(tern.left, visitor, replacement);
			
			if (visitor.visit(tern.right)) tern.right = replacement.clone();
			else tern.right = morphExpression(tern.right, visitor, replacement);
		}
		else if (source instanceof InlineCall in) {
			for (int i = 0; i < in.parameters.size(); i++) {
				Expression e0 = in.parameters.get(i);
				if (visitor.visit(e0)) 
					in.parameters.set(i, replacement.clone());
				else 
					in.parameters.set(i, morphExpression(e0, visitor, replacement));
			}
		}
		else if (source instanceof ArraySelect s) {
			if (visitor.visit(s.idRef)) s.idRef = (IDRef) replacement.clone();
			else s.idRef = (IDRef) morphExpression(s.idRef, visitor, replacement);
			
			for (int i = 0; i < s.selection.size(); i++) {
				Expression e0 = s.selection.get(i);
				if (visitor.visit(e0)) 
					s.selection.set(i, replacement.clone());
				else 
					s.selection.set(i, morphExpression(e0, visitor, replacement));
			}
		}
		else throw new SNIPS_EXC("Failed to morph from source '" + source.codePrint() + "', add morphing capabilities for " + source.getClass().getSimpleName());
	
		return source;
	}
	
}
