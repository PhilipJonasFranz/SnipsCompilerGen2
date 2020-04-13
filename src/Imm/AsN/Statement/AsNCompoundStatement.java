package Imm.AsN.Statement;

import java.util.ArrayList;
import java.util.List;

import CGen.MemoryMap;
import CGen.RegSet;
import CGen.StackSet;
import Exc.CGEN_EXCEPTION;
import Imm.ASM.Memory.Stack.ASMPushStack;
import Imm.ASM.Processing.Arith.ASMAdd;
import Imm.ASM.Structural.ASMComment;
import Imm.ASM.Util.Operands.ImmOperand;
import Imm.ASM.Util.Operands.RegOperand;
import Imm.ASM.Util.Operands.RegOperand.REGISTER;
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
import Imm.AST.Expression.TypeCast;
import Imm.AST.Expression.UnaryExpression;
import Imm.AST.Expression.Boolean.Ternary;
import Imm.AST.Statement.AssignWriteback;
import Imm.AST.Statement.Assignment;
import Imm.AST.Statement.BreakStatement;
import Imm.AST.Statement.CaseStatement;
import Imm.AST.Statement.Comment;
import Imm.AST.Statement.CompoundStatement;
import Imm.AST.Statement.ConditionalCompoundStatement;
import Imm.AST.Statement.ContinueStatement;
import Imm.AST.Statement.Declaration;
import Imm.AST.Statement.FunctionCall;
import Imm.AST.Statement.ReturnStatement;
import Imm.AST.Statement.Statement;
import Imm.AST.Statement.SwitchStatement;

public abstract class AsNCompoundStatement extends AsNStatement {

	public static AsNCompoundStatement cast(CompoundStatement s, RegSet r, MemoryMap map, StackSet st) throws CGEN_EXCEPTION {
		/* Relay to statement type cast */
		AsNCompoundStatement node = null;
		
		if (s instanceof ConditionalCompoundStatement) {
			node = AsNConditionalCompoundStatement.cast((ConditionalCompoundStatement) s, r, map, st);
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
		
		/* Collect declarations from statements, ignore sub-compounds, they will do the same */
		for (Statement s0 : s.body) {
			if (s0 instanceof Declaration) {
				declarations.add((Declaration) s0);
			}
		}
		
		/* Delete declaration out of the registers */
		for (Declaration d : declarations) {
			if (r.declarationLoaded(d)) {
				int loc = r.declarationRegLocation(d);
				r.getReg(loc).free();
			}
		}
		
		/* Set the stack pointer to the new stack size */
		int add = st.closeScope();
		if (add != 0) {
			this.instructions.add(new ASMAdd(new RegOperand(REGISTER.SP), new RegOperand(REGISTER.SP), new ImmOperand(add)));
		}
	}
	
	/**
	 * Opens a new scope, inserts the body of the compound statement, closes the scope and resets the stack.
	 */
	protected void addBody(CompoundStatement a, RegSet r, MemoryMap map, StackSet st) throws CGEN_EXCEPTION {
		/* Open a new Scope in the stack */
		st.openScope();
		
		/* Body */
		for (int i = 0; i < a.body.size(); i++) {
			Statement s = a.body.get(i);
			this.loadStatement(a, s, r, map, st);
		}
		
		/* Free all declarations in scope */
		this.popDeclarationScope(a, r, st);
	}
	
	public void loadStatement(CompoundStatement a, Statement s, RegSet r, MemoryMap map, StackSet st) throws CGEN_EXCEPTION {
		if (s instanceof Declaration) {
			Declaration dec = (Declaration) s;
			this.instructions.addAll(AsNDeclaration.cast(dec, r, map, st).getInstructions());
			
			if (r.declarationLoaded(dec)) {
				boolean hasAddress = this.hasAddressReference(a, dec); 
				if (hasAddress) {
					int location = r.declarationRegLocation(dec);
					
					ASMPushStack push = new ASMPushStack(new RegOperand(location));
					push.comment = new ASMComment("Push declaration on stack, referenced by addressof.");
					this.instructions.add(push);
					
					st.push(dec);
					r.free(location);
				}
			}
		}
		else this.instructions.addAll(AsNStatement.cast(s, r, map, st).getInstructions());
	}
	
	public boolean hasAddressReference(Statement s, Declaration dec) throws CGEN_EXCEPTION {
		if (s instanceof CompoundStatement) {
			CompoundStatement cs = (CompoundStatement) s;
			
			boolean ref = false;
			
			for (Statement s0 : cs.body) {
				ref |= this.hasAddressReference(s0, dec);
			}
			
			return ref;
		}
		else if (s instanceof ReturnStatement) {
			ReturnStatement ret = (ReturnStatement) s;
			if (ret.value == null) return false;
			else return this.hasAddressReference(ret.value, dec);
		}
		else if (s instanceof Declaration) {
			if (s.equals(dec)) return false;
			else return this.hasAddressReference(((Declaration) s).value, dec);
		}
		else if (s instanceof Assignment) {
			return this.hasAddressReference(((Assignment) s).value, dec);
		}
		else if (s instanceof FunctionCall) {
			boolean hasRef = false;
			FunctionCall fc = (FunctionCall) s;
			for (Expression e0 : fc.parameters) {
				hasRef |= this.hasAddressReference(e0, dec);
			}
			return hasRef;
		}
		else if (s instanceof SwitchStatement) {
			boolean hasRef = false;
			SwitchStatement sw = (SwitchStatement) s;
			for (CaseStatement c : sw.cases) {
				hasRef |= this.hasAddressReference(c, dec);
			}
			
			hasRef |= this.hasAddressReference(sw.defaultStatement, dec);
			return hasRef;
		}
		else if (s instanceof AssignWriteback) {
			AssignWriteback awb = (AssignWriteback) s;
			return this.hasAddressReference(awb.getShadowRef(), dec);
		}
		else if (s instanceof BreakStatement || s instanceof ContinueStatement || s instanceof Comment) {
			return false;
		}
		else throw new CGEN_EXCEPTION(s.getSource(), "Cannot check references for " + s.getClass().getName());
	}
	
	public boolean hasAddressReference(Expression e, Declaration dec) throws CGEN_EXCEPTION {
		if (e instanceof BinaryExpression) {
			BinaryExpression b = (BinaryExpression) e;
			return this.hasAddressReference(b.left, dec) || this.hasAddressReference(b.right, dec);
		}
		else if (e instanceof UnaryExpression) {
			UnaryExpression u = (UnaryExpression) e;
			return this.hasAddressReference(u.getOperand(), dec);
		}
		else if (e instanceof InlineCall) {
			boolean hasRef = false;
			InlineCall ic = (InlineCall) e;
			for (Expression e0 : ic.parameters) {
				hasRef |= this.hasAddressReference(e0, dec);
			}
			return hasRef;
		}
		else if (e instanceof Deref) {
			return this.hasAddressReference(((Deref) e).expression, dec);
		}
		else if (e instanceof ArrayInit) {
			boolean hasRef = false;
			ArrayInit init = (ArrayInit) e;
			for (Expression e0 : init.elements) {
				hasRef |= this.hasAddressReference(e0, dec);
			}
			return hasRef;
		}
		else if (e instanceof ArraySelect) {
			boolean hasRef = false;
			ArraySelect sel = (ArraySelect) e;
			for (Expression e0 : sel.selection) {
				hasRef |= this.hasAddressReference(e0, dec);
			}
			return hasRef;
		}
		else if (e instanceof Ternary) {
			boolean hasRef = false;
			Ternary ter = (Ternary) e;
			hasRef |= this.hasAddressReference(ter.condition, dec);
			hasRef |= this.hasAddressReference(ter.leftOperand, dec);
			hasRef |= this.hasAddressReference(ter.rightOperand, dec);
			return hasRef;
		}
		else if (e instanceof AddressOf) {
			AddressOf aof = (AddressOf) e;
			if (aof.expression instanceof IDRef) {
				if (((IDRef) aof.expression).origin.equals(dec)) return true;
			}
			else {
				if (((ArraySelect) aof.expression).idRef.origin.equals(dec)) return true;
			}
			return false;
		}
		else if (e instanceof IDRefWriteback) {
			IDRefWriteback id = (IDRefWriteback) e;
			return this.hasAddressReference(id.getShadowRef(), dec);
		}
		else if (e instanceof SizeOfExpression) {
			SizeOfExpression soe = (SizeOfExpression) e;
			return this.hasAddressReference(soe.expression, dec);
		}
		else if (e instanceof TypeCast) {
			TypeCast tc = (TypeCast) e;
			return this.hasAddressReference(tc.expression, dec);
		}
		else if (e instanceof IDRef || e instanceof Atom || e instanceof SizeOfType) {
			return false;
		}
		else throw new CGEN_EXCEPTION(e.getSource(), "Cannot check references for " + e.getClass().getName());
	}
	
}
