package Imm.AsN.Statement.Lhs;

import java.util.ArrayList;
import java.util.List;

import CGen.MemoryMap;
import CGen.RegSet;
import CGen.StackSet;
import Exc.CGEN_EXCEPTION;
import Imm.ASM.ASMInstruction;
import Imm.ASM.Branch.ASMBranch;
import Imm.ASM.Branch.ASMBranch.BRANCH_TYPE;
import Imm.ASM.Memory.Stack.ASMPopStack;
import Imm.ASM.Memory.Stack.ASMPushStack;
import Imm.ASM.Processing.Arith.ASMAdd;
import Imm.ASM.Processing.Arith.ASMAnd;
import Imm.ASM.Processing.Arith.ASMEor;
import Imm.ASM.Processing.Arith.ASMLsl;
import Imm.ASM.Processing.Arith.ASMLsr;
import Imm.ASM.Processing.Arith.ASMMov;
import Imm.ASM.Processing.Arith.ASMMult;
import Imm.ASM.Processing.Arith.ASMOrr;
import Imm.ASM.Processing.Arith.ASMSub;
import Imm.ASM.Processing.Logic.ASMCmp;
import Imm.ASM.Structural.Label.ASMLabel;
import Imm.ASM.Util.Cond;
import Imm.ASM.Util.Cond.COND;
import Imm.ASM.Util.Operands.ImmOperand;
import Imm.ASM.Util.Operands.LabelOperand;
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
		else if (a.assignArith == ASSIGN_ARITH.DIV_ASSIGN || a.assignArith == ASSIGN_ARITH.MOD_ASSIGN) {
			/* Move operands, so sourceOperand = 0 and combineOperand = 1 */
			if (sourceOperand != 0) {
				if (combineOperand == 0) {
					if (sourceOperand != 1) {
						inj.add(new ASMMov(new RegOperand(REGISTER.R0), new RegOperand(REGISTER.R1)));
						inj.add(new ASMMov(new RegOperand(REGISTER.R0), new RegOperand(sourceOperand)));
					}
					else {
						/* Swap */
						inj.add(new ASMMov(new RegOperand(REGISTER.R0), new RegOperand(REGISTER.R2)));
						inj.add(new ASMMov(new RegOperand(REGISTER.R1), new RegOperand(REGISTER.R0)));
						inj.add(new ASMMov(new RegOperand(REGISTER.R2), new RegOperand(REGISTER.R1)));
					}
				}
				else {
					inj.add(new ASMMov(new RegOperand(REGISTER.R0), new RegOperand(sourceOperand)));
					inj.add(new ASMMov(new RegOperand(REGISTER.R1), new RegOperand(combineOperand)));
				}
			}
			else if (combineOperand != 1) {
				inj.add(new ASMMov(new RegOperand(REGISTER.R1), new RegOperand(combineOperand)));
			}
			
			if (a.assignArith == ASSIGN_ARITH.DIV_ASSIGN) {
				inj.add(new ASMBranch(BRANCH_TYPE.BL, new LabelOperand(new ASMLabel("__op_div"))));
			}
			else {
				inj.add(new ASMBranch(BRANCH_TYPE.BL, new LabelOperand(new ASMLabel("__op_mod"))));
			}
			
			if (directInjection) {
				inj.add(new ASMMov(new RegOperand(sourceOperand), new RegOperand(REGISTER.R0)));
			}
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
		else if (a.assignArith == ASSIGN_ARITH.BIT_ORR_ASSIGN) {
			if (!directInjection) {
				if (otherOp == 0) save = true;
				inj.add(new ASMOrr(new RegOperand(REGISTER.R0), new RegOperand(sourceOperand), new RegOperand(combineOperand)));
			}
			else inj.add(new ASMOrr(new RegOperand(sourceOperand), new RegOperand(sourceOperand), new RegOperand(combineOperand)));
		}
		else if (a.assignArith == ASSIGN_ARITH.BIT_AND_ASSIGN) {
			if (!directInjection) {
				if (otherOp == 0) save = true;
				inj.add(new ASMAnd(new RegOperand(REGISTER.R0), new RegOperand(sourceOperand), new RegOperand(combineOperand)));
			}
			else inj.add(new ASMAnd(new RegOperand(sourceOperand), new RegOperand(sourceOperand), new RegOperand(combineOperand)));
		}
		else if (a.assignArith == ASSIGN_ARITH.BIT_XOR_ASSIGN) {
			if (!directInjection) {
				if (otherOp == 0) save = true;
				inj.add(new ASMEor(new RegOperand(REGISTER.R0), new RegOperand(sourceOperand), new RegOperand(combineOperand)));
			}
			else inj.add(new ASMEor(new RegOperand(sourceOperand), new RegOperand(sourceOperand), new RegOperand(combineOperand)));
		}
		else if (a.assignArith == ASSIGN_ARITH.AND_ASSIGN) {
			/* Perform and */
			ASMAdd and0 = new ASMAdd(new RegOperand(combineOperand), new RegOperand(combineOperand), new ImmOperand(0));
			and0.updateConditionField = true;
			inj.add(and0);
			
			inj.add(new ASMMov(new RegOperand(combineOperand), new ImmOperand(1), new Cond(COND.NE)));
			
			inj.add(new ASMCmp(new RegOperand(sourceOperand), new ImmOperand(0)));
			
			if (!directInjection) {
				inj.add(new ASMMov(new RegOperand(REGISTER.R0), new RegOperand(combineOperand), new Cond(COND.NE)));
				inj.add(new ASMMov(new RegOperand(REGISTER.R0), new ImmOperand(0), new Cond(COND.EQ)));
			}
			else {
				inj.add(new ASMMov(new RegOperand(sourceOperand), new RegOperand(combineOperand), new Cond(COND.NE)));
				inj.add(new ASMMov(new RegOperand(sourceOperand), new ImmOperand(0), new Cond(COND.EQ)));
			}
		}
		else if (a.assignArith == ASSIGN_ARITH.ORR_ASSIGN) {
			/* Perform and */
			ASMOrr orr = new ASMOrr(new RegOperand(sourceOperand), new RegOperand(sourceOperand), new RegOperand(combineOperand));
			orr.updateConditionField = true;
			inj.add(orr);
			
			if (!directInjection) {
				inj.add(new ASMMov(new RegOperand(REGISTER.R0), new ImmOperand(1), new Cond(COND.NE)));
				inj.add(new ASMMov(new RegOperand(REGISTER.R0), new ImmOperand(0), new Cond(COND.EQ)));
			}
			else {
				inj.add(new ASMMov(new RegOperand(sourceOperand), new ImmOperand(1), new Cond(COND.NE)));
				inj.add(new ASMMov(new RegOperand(sourceOperand), new ImmOperand(0), new Cond(COND.EQ)));
			}
		}
		
		/* Pop Last Operand Register if Operand Register was used */
		if (clearOtherOperandReg && save) {
			inj.add(0, new ASMPushStack(new RegOperand(otherOp)));
			inj.add(new ASMPopStack(new RegOperand(otherOp)));
		}
		
		return inj;
	}
	
}
