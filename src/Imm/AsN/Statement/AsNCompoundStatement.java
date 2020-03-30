package Imm.AsN.Statement;

import java.util.ArrayList;
import java.util.List;

import CGen.RegSet;
import CGen.StackSet;
import Exc.CGEN_EXCEPTION;
import Imm.ASM.Processing.ASMAdd;
import Imm.ASM.Util.Operands.ImmOperand;
import Imm.ASM.Util.Operands.RegOperand;
import Imm.ASM.Util.Operands.RegOperand.REGISTER;
import Imm.AST.Statement.CompoundStatement;
import Imm.AST.Statement.ConditionalCompoundStatement;
import Imm.AST.Statement.Declaration;
import Imm.AST.Statement.Statement;

public abstract class AsNCompoundStatement extends AsNStatement {

	public static AsNCompoundStatement cast(CompoundStatement s, RegSet r, StackSet st) throws CGEN_EXCEPTION {
		/* Relay to statement type cast */
		AsNCompoundStatement node = null;
		
		if (s instanceof ConditionalCompoundStatement) {
			node = AsNConditionalCompoundStatement.cast((ConditionalCompoundStatement) s, r, st);
		}
		else throw new CGEN_EXCEPTION(s.getSource(), "No injection cast available for " + s.getClass().getName());	
		
		s.castedNode = node;
		return node;
	}
	
	/**
	 * Free all loaded declarations that were made in the body of the statement, since
	 * the scope is popped.
	 * @param s The CapsuledStatement containing the Statements.
	 * @param r The current RegSet
	 */
	protected void popDeclarationScope(CompoundStatement s, RegSet r, StackSet st) {
		List<Declaration> declarations = new ArrayList(); 
		for (Statement s0 : s.body) {
			if (s0 instanceof Declaration) {
				declarations.add((Declaration) s0);
			}
		}
		
		for (Declaration d : declarations) {
			if (r.declarationLoaded(d)) {
				int loc = r.declarationRegLocation(d);
				r.getReg(loc).free();
			}
		}
		
		int add = st.closeScope();
		if (add != 0) {
			this.instructions.add(new ASMAdd(new RegOperand(REGISTER.SP), new RegOperand(REGISTER.SP), new ImmOperand(add)));
		}
	}
	
}
