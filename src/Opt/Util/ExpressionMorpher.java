package Opt.Util;

import Exc.SNIPS_EXC;
import Imm.AST.Expression.Atom;
import Imm.AST.Expression.Expression;
import Imm.AST.Expression.IDRef;
import Imm.AST.Expression.IDRefWriteback;
import Imm.AST.Expression.NFoldExpression;
import Tools.ASTNodeVisitor;

public class ExpressionMorpher {

	public static void morphExpression(Expression source, ASTNodeVisitor visitor, Expression replacement) {
		if (source instanceof Atom) return;
		else if (source instanceof IDRef) return;
		else if (source instanceof IDRefWriteback) return;
		else if (source instanceof NFoldExpression) {
			NFoldExpression nfold = (NFoldExpression) source;
			
			for (int i = 0; i < nfold.operands.size(); i++) {
				Expression e0 = nfold.operands.get(i);
				if (visitor.visit(e0)) 
					nfold.operands.set(i, replacement);
			}
		}
		else throw new SNIPS_EXC("Failed to morph from source '" + source.codePrint() + "', add morphing capabilities for " + source.getClass().getSimpleName());
	}
	
}
