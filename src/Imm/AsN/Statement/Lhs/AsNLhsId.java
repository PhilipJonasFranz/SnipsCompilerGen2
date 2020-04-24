package Imm.AsN.Statement.Lhs;

import java.util.ArrayList;
import java.util.List;

import CGen.MemoryMap;
import CGen.RegSet;
import CGen.StackSet;
import Exc.CGEN_EXCEPTION;
import Imm.ASM.ASMInstruction;
import Imm.ASM.Memory.Stack.ASMPopStack;
import Imm.ASM.Memory.Stack.ASMPushStack;
import Imm.ASM.Processing.Arith.ASMAdd;
import Imm.ASM.Processing.Arith.ASMAnd;
import Imm.ASM.Processing.Arith.ASMEor;
import Imm.ASM.Processing.Arith.ASMLsl;
import Imm.ASM.Processing.Arith.ASMLsr;
import Imm.ASM.Processing.Arith.ASMMult;
import Imm.ASM.Processing.Arith.ASMOrr;
import Imm.ASM.Processing.Arith.ASMSub;
import Imm.ASM.Util.Operands.RegOperand;
import Imm.ASM.Util.Operands.RegOperand.REGISTER;
import Imm.AST.Lhs.ArraySelectLhsId;
import Imm.AST.Lhs.LhsId;
import Imm.AST.Lhs.PointerLhsId;
import Imm.AST.Lhs.SimpleLhsId;
import Imm.AST.Lhs.StructSelectLhsId;
import Imm.AST.Statement.Assignment;
import Imm.AST.Statement.Assignment.ASSIGN_ARITH;
import Imm.AsN.Statement.AsNStatement;

public class AsNLhsId extends AsNStatement {

	/**
	 * Casting an lhs will cause the value stored in R0 or on the stack to be stored at the target
	 * location specified by the lhs.
	 */
	public static AsNLhsId cast(LhsId lhs, RegSet r, MemoryMap map, StackSet st) throws CGEN_EXCEPTION {
		/* Relay to statement type cast */
		AsNLhsId id = null;
		
		if (lhs instanceof SimpleLhsId) {
			id = AsNSimpleLhsId.cast((SimpleLhsId) lhs, r, map, st);
		}
		else if (lhs instanceof ArraySelectLhsId) {
			id = AsNArraySelectLhsId.cast((ArraySelectLhsId) lhs, r, map, st);
		}
		else if (lhs instanceof PointerLhsId) {
			id = AsNPointerLhsId.cast((PointerLhsId) lhs, r, map, st);
		}
		else if (lhs instanceof StructSelectLhsId) {
			id = AsNStructSelectLhsId.cast((StructSelectLhsId) lhs, r, map, st);
		}
		else throw new CGEN_EXCEPTION(lhs.getSource(), "No injection cast available for " + lhs.getClass().getName());
	
		lhs.castedNode = id;
		return id;
	}
	
	/**
	 * Creates the code that is injected into the assign process.
	 * @param a The assignment node.
	 * @param sourceOperand In which register the original operand is located
	 * @param combineOperand In which register the additional operand is located
	 * @param directInjection Wether to target the source operand directley or to use R0 as target. If set to true, the target reg = sourceOperand.
	 * @return The generated code.
	 */
	public List<ASMInstruction> buildInjector(Assignment a, int sourceOperand, int combineOperand, boolean directInjection, boolean clearOtherOperandReg) {
		int otherOp = 3 - (sourceOperand + combineOperand);
		boolean save = false;
		
		List<ASMInstruction> inj = new ArrayList();
		
		if (a.assignArith == ASSIGN_ARITH.ADD_ASSIGN) {
			if (!directInjection) {
				if (otherOp == 0) save = true;
				inj.add(new ASMAdd(new RegOperand(REGISTER.R0), new RegOperand(sourceOperand), new RegOperand(combineOperand)));
			}
			else inj.add(new ASMAdd(new RegOperand(sourceOperand), new RegOperand(sourceOperand), new RegOperand(combineOperand)));
		}
		else if (a.assignArith == ASSIGN_ARITH.SUB_ASSIGN) {
			if (!directInjection) {
				if (otherOp == 0) save = true;
				inj.add(new ASMSub(new RegOperand(REGISTER.R0), new RegOperand(sourceOperand), new RegOperand(combineOperand)));
			}
			else inj.add(new ASMSub(new RegOperand(sourceOperand), new RegOperand(sourceOperand), new RegOperand(combineOperand)));
		}
		else if (a.assignArith == ASSIGN_ARITH.MUL_ASSIGN) {
			if (!directInjection) {
				if (otherOp == 0) save = true;
				inj.add(new ASMMult(new RegOperand(REGISTER.R0), new RegOperand(sourceOperand), new RegOperand(combineOperand)));
			}
			else inj.add(new ASMMult(new RegOperand(sourceOperand), new RegOperand(sourceOperand), new RegOperand(combineOperand)));
		}
		else if (a.assignArith == ASSIGN_ARITH.DIV_ASSIGN) {
			// TODO
		}
		else if (a.assignArith == ASSIGN_ARITH.MOD_ASSIGN) {
			// TODO
		}
		else if (a.assignArith == ASSIGN_ARITH.LSL_ASSIGN) {
			if (!directInjection) {
				if (otherOp == 0) save = true;
				inj.add(new ASMLsl(new RegOperand(REGISTER.R0), new RegOperand(sourceOperand), new RegOperand(combineOperand)));
			}
			else inj.add(new ASMLsl(new RegOperand(sourceOperand), new RegOperand(sourceOperand), new RegOperand(combineOperand)));
		}
		else if (a.assignArith == ASSIGN_ARITH.LSR_ASSIGN) {
			if (!directInjection) {
				if (otherOp == 0) save = true;
				inj.add(new ASMLsr(new RegOperand(REGISTER.R0), new RegOperand(sourceOperand), new RegOperand(combineOperand)));
			}
			else inj.add(new ASMLsr(new RegOperand(sourceOperand), new RegOperand(sourceOperand), new RegOperand(combineOperand)));
		}
		else if (a.assignArith == ASSIGN_ARITH.ORR_ASSIGN) {
			if (!directInjection) {
				if (otherOp == 0) save = true;
				inj.add(new ASMOrr(new RegOperand(REGISTER.R0), new RegOperand(sourceOperand), new RegOperand(combineOperand)));
			}
			else inj.add(new ASMOrr(new RegOperand(sourceOperand), new RegOperand(sourceOperand), new RegOperand(combineOperand)));
		}
		else if (a.assignArith == ASSIGN_ARITH.AND_ASSIGN) {
			if (!directInjection) {
				if (otherOp == 0) save = true;
				inj.add(new ASMAnd(new RegOperand(REGISTER.R0), new RegOperand(sourceOperand), new RegOperand(combineOperand)));
			}
			else inj.add(new ASMAnd(new RegOperand(sourceOperand), new RegOperand(sourceOperand), new RegOperand(combineOperand)));
		}
		else if (a.assignArith == ASSIGN_ARITH.XOR_ASSIGN) {
			if (!directInjection) {
				if (otherOp == 0) save = true;
				inj.add(new ASMEor(new RegOperand(REGISTER.R0), new RegOperand(sourceOperand), new RegOperand(combineOperand)));
			}
			else inj.add(new ASMEor(new RegOperand(sourceOperand), new RegOperand(sourceOperand), new RegOperand(combineOperand)));
		}
		
		/* Pop Last Operand Register if Operand Register was used */
		if (clearOtherOperandReg && save) {
			inj.add(0, new ASMPushStack(new RegOperand(otherOp)));
			inj.add(new ASMPopStack(new RegOperand(otherOp)));
		}
		
		return inj;
	}
	
}
