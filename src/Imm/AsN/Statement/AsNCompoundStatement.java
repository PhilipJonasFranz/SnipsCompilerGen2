package Imm.AsN.Statement;

import java.util.ArrayList;
import java.util.List;

import CGen.MemoryMap;
import CGen.RegSet;
import CGen.StackSet;
import Exc.CGEN_EXC;
import Imm.ASM.ASMInstruction.OPT_FLAG;
import Imm.ASM.Memory.Stack.ASMPushStack;
import Imm.ASM.Processing.Arith.ASMAdd;
import Imm.ASM.Structural.ASMComment;
import Imm.ASM.Util.Operands.ImmOp;
import Imm.ASM.Util.Operands.RegOp;
import Imm.ASM.Util.Operands.RegOp.REG;
import Imm.AST.Statement.CompoundStatement;
import Imm.AST.Statement.ConditionalCompoundStatement;
import Imm.AST.Statement.Declaration;
import Imm.AST.Statement.ForEachStatement;
import Imm.AST.Statement.Statement;
import Imm.AST.Statement.TryStatement;
import Imm.AsN.AsNNode;
import Res.Const;
import Tools.Matchers;

public abstract class AsNCompoundStatement extends AsNStatement {

	public static AsNCompoundStatement cast(CompoundStatement s, RegSet r, MemoryMap map, StackSet st) throws CGEN_EXC {
		/* Relay to statement type cast */
		AsNCompoundStatement node = null;
		
		if (s instanceof ConditionalCompoundStatement) {
			node = AsNConditionalCompoundStatement.cast((ConditionalCompoundStatement) s, r, map, st);
		}
		else if (s instanceof TryStatement) {
			node = AsNTryStatement.cast((TryStatement) s, r, map, st);
		}
		else if (s instanceof ForEachStatement) {
			node = AsNForEachStatement.cast((ForEachStatement) s, r, map, st);
		}
		else throw new CGEN_EXC(s.getSource(), Const.NO_INJECTION_CAST_AVAILABLE, s.getClass().getName());	
		
		s.castedNode = node;
		return node;
	}
	
	/**
	 * Free all loaded declarations that were made in the body of the statement, since
	 * the scope is popped.
	 * @param s The CapsuledStatement containing the Statements.
	 * @param r The current RegSet
	 * @param close If set to true, the declarations are removed, if not, only the offsets are calculated and added to the stack.
	 */
	public static void popDeclarationScope(AsNNode node, CompoundStatement s, RegSet r, StackSet st, boolean close) {
		if (close) {
			List<Declaration> declarations = new ArrayList(); 
			
			/* Collect declarations from statements, ignore sub-compounds, they will do the same */
			for (Statement s0 : s.body) {
				if (s0 instanceof Declaration) 
					declarations.add((Declaration) s0);
			}
			
			/* Delete declarations out of the registers */
			for (Declaration d : declarations) {
				if (r.declarationLoaded(d)) {
					int loc = r.declarationRegLocation(d);
					r.getReg(loc).free();
				}
			}
		}
		
		/* Set the stack pointer to the new stack size */
		int add = st.closeScope(s, close);
		
		if (add != 0) {
			ASMAdd add0 = new ASMAdd(new RegOp(REG.SP), new RegOp(REG.SP), new ImmOp(add));
			if (!close) add0.optFlags.add(OPT_FLAG.LOOP_BREAK_RESET);
			node.instructions.add(add0);
		}
	}
	
	/**
	 * Opens a new scope, inserts the body of the compound statement, closes the scope 
	 * and resets the stack if nessesary.
	 */
	protected void addBody(CompoundStatement a, RegSet r, MemoryMap map, StackSet st) throws CGEN_EXC {
		/* Open a new Scope in the stack */
		st.openScope(a);
		
		/* Body */
		for (int i = 0; i < a.body.size(); i++) {
			Statement s = a.body.get(i);
			
			/* Free operand regs */
			r.free(0, 1, 2);
			
			this.loadStatement(a, s, r, map, st);
		}
		
		/* Free all declarations in scope */
		popDeclarationScope(this, a, r, st, true);
	}
	
	/**
	 * Casts given statement into this.instructions. If the given statement is a declaration,
	 * the method checks with {@link #hasAddressReference(Statement, Declaration)} if the dec
	 * has an address reference in the compound statement. If yes, the declaration needs to be
	 * forced on the stack to make it addressable. This is done by just injecting a push instruction,
	 * if the declaration will not be automatically loaded on the stack. If the statement is not
	 * a declaration, the standard AsNStatement.cast() method is used for the injection cast.
	 */
	public void loadStatement(CompoundStatement a, Statement s, RegSet r, MemoryMap map, StackSet st) throws CGEN_EXC {
		if (s instanceof Declaration) {
			Declaration dec = (Declaration) s;
			this.instructions.addAll(AsNDeclaration.cast(dec, r, map, st).getInstructions());
			
			if (r.declarationLoaded(dec)) {
				boolean hasAddress = Matchers.hasAddressReference(a, dec); 
				if (hasAddress) {
					int location = r.declarationRegLocation(dec);
					
					ASMPushStack push = new ASMPushStack(new RegOp(location));
					push.comment = new ASMComment("Push declaration on stack, referenced by addressof.");
					this.instructions.add(push);
					
					st.push(dec);
					r.free(location);
				}
			}
		}
		else this.instructions.addAll(AsNStatement.cast(s, r, map, st).getInstructions());
	}
	
} 
