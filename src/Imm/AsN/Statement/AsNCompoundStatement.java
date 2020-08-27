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
import Imm.AST.Expression.AddressOf;
import Imm.AST.Expression.ArrayInit;
import Imm.AST.Expression.ArraySelect;
import Imm.AST.Expression.Atom;
import Imm.AST.Expression.BinaryExpression;
import Imm.AST.Expression.Deref;
import Imm.AST.Expression.Expression;
import Imm.AST.Expression.FunctionRef;
import Imm.AST.Expression.IDRef;
import Imm.AST.Expression.IDRefWriteback;
import Imm.AST.Expression.InlineCall;
import Imm.AST.Expression.InstanceofExpression;
import Imm.AST.Expression.RegisterAtom;
import Imm.AST.Expression.SizeOfExpression;
import Imm.AST.Expression.SizeOfType;
import Imm.AST.Expression.StructSelect;
import Imm.AST.Expression.StructureInit;
import Imm.AST.Expression.TempAtom;
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
import Imm.AST.Statement.DirectASMStatement;
import Imm.AST.Statement.ForEachStatement;
import Imm.AST.Statement.FunctionCall;
import Imm.AST.Statement.ReturnStatement;
import Imm.AST.Statement.SignalStatement;
import Imm.AST.Statement.Statement;
import Imm.AST.Statement.SwitchStatement;
import Imm.AST.Statement.TryStatement;
import Imm.AsN.AsNNode;
import Res.Const;
import Util.Pair;

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
				boolean hasAddress = this.hasAddressReference(a, dec); 
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
	
	/**
	 * Checks if in the given statement, an address reference via address of
	 * is made to a variable with given origin declaration. If so, return true.
	 */
	public boolean hasAddressReference(Statement s, Declaration dec) throws CGEN_EXC {
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
			else {
				Declaration d = (Declaration) s;
				if (d.value != null) return this.hasAddressReference(d.value, dec);
				else return false;
			}
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
			return this.hasAddressReference(awb.reference, dec);
		}
		else if (s instanceof DirectASMStatement) {
			DirectASMStatement d = (DirectASMStatement) s;
			
			boolean hasRef = false;
			
			for (Pair<Expression, REG> p : d.dataIn) {
				hasRef |= this.hasAddressReference(p.first, dec);
			}
			
			for (Pair<Expression, REG> p : d.dataOut) {
				hasRef |= this.hasAddressReference(p.first, dec);
			}
			
			return hasRef;
		}
		else if (s instanceof SignalStatement) {
			SignalStatement s0 = (SignalStatement) s;
			return this.hasAddressReference(s0.exceptionInit, dec);
		}
		else if (s instanceof BreakStatement || s instanceof ContinueStatement || s instanceof Comment) {
			return false;
		}
		else throw new CGEN_EXC(s.getSource(), Const.CANNOT_CHECK_REFERENCES, s.getClass().getName());
	}
	
	/**
	 * Checks if in the given expression, an address reference via address of
	 * is made to a variable with given origin declaration. If so, return true.
	 */
	public boolean hasAddressReference(Expression e, Declaration dec) throws CGEN_EXC {
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
			if (aof.expression instanceof IDRef) 
				return (((IDRef) aof.expression).origin.equals(dec));
			else if (aof.expression instanceof StructSelect) 
				/* Struct will be on the stack anyway */
				return true;
			else if (aof.expression instanceof StructureInit) 
				return this.hasAddressReference(aof.expression, dec);
			else return (((ArraySelect) aof.expression).idRef.origin.equals(dec));
		}
		else if (e instanceof IDRefWriteback) {
			IDRefWriteback id = (IDRefWriteback) e;
			return this.hasAddressReference(id.getShadowRef(), dec);
		}
		else if (e instanceof SizeOfExpression) {
			SizeOfExpression soe = (SizeOfExpression) e;
			return this.hasAddressReference(soe.expression, dec);
		}
		else if (e instanceof InstanceofExpression) {
			InstanceofExpression iof = (InstanceofExpression) e;
			return this.hasAddressReference(iof.expression, dec);
		}
		else if (e instanceof TypeCast) {
			TypeCast tc = (TypeCast) e;
			return this.hasAddressReference(tc.expression, dec);
		}
		else if (e instanceof StructureInit) {
			StructureInit s = (StructureInit) e;
			boolean ref = false;
			for (Expression e0 : s.elements) {
				ref |= this.hasAddressReference(e0, dec);
			}
			return ref;
		}
		else if (e instanceof TempAtom) {
			TempAtom a = (TempAtom) e;
			if (a.base == null) return false;
			else return this.hasAddressReference(a.base, dec);
		}
		else if (e instanceof IDRef || e instanceof FunctionRef || e instanceof Atom || e instanceof RegisterAtom || e instanceof SizeOfType || e instanceof StructSelect) {
			return false;
		}
		else throw new CGEN_EXC(e.getSource(), Const.CANNOT_CHECK_REFERENCES, e.getClass().getName());
	}
	
} 
