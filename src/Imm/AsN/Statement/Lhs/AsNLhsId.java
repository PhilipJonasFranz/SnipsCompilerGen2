package Imm.AsN.Statement.Lhs;

import java.util.ArrayList;
import java.util.List;

import CGen.MemoryMap;
import CGen.RegSet;
import CGen.StackSet;
import Exc.CGEN_EXC;
import Exc.SNIPS_EXC;
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
import Imm.ASM.Util.Operands.ImmOp;
import Imm.ASM.Util.Operands.LabelOp;
import Imm.ASM.Util.Operands.RegOp;
import Imm.ASM.Util.Operands.RegOp.REG;
import Imm.AST.Lhs.ArraySelectLhsId;
import Imm.AST.Lhs.LhsId;
import Imm.AST.Lhs.PointerLhsId;
import Imm.AST.Lhs.SimpleLhsId;
import Imm.AST.Lhs.StructSelectLhsId;
import Imm.AST.Statement.Assignment;
import Imm.AST.Statement.Assignment.ASSIGN_ARITH;
import Imm.AsN.Statement.AsNStatement;
import Res.Const;

public class AsNLhsId extends AsNStatement {

	/**
	 * Casting an lhs will cause the value stored in R0 or on the stack to be stored at the target
	 * location specified by the lhs.
	 */
	public static AsNLhsId cast(LhsId lhs, RegSet r, MemoryMap map, StackSet st) throws CGEN_EXC {
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
		else throw new CGEN_EXC(lhs.getSource(), Const.NO_INJECTION_CAST_AVAILABLE, lhs.getClass().getName());
	
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
				inj.add(new ASMAdd(new RegOp(REG.R0), new RegOp(sourceOperand), new RegOp(combineOperand)));
			}
			else inj.add(new ASMAdd(new RegOp(sourceOperand), new RegOp(sourceOperand), new RegOp(combineOperand)));
		}
		else if (a.assignArith == ASSIGN_ARITH.SUB_ASSIGN) {
			if (!directInjection) {
				if (otherOp == 0) save = true;
				inj.add(new ASMSub(new RegOp(REG.R0), new RegOp(sourceOperand), new RegOp(combineOperand)));
			}
			else inj.add(new ASMSub(new RegOp(sourceOperand), new RegOp(sourceOperand), new RegOp(combineOperand)));
		}
		else if (a.assignArith == ASSIGN_ARITH.MUL_ASSIGN) {
			if (!directInjection) {
				if (otherOp == 0) save = true;
				inj.add(new ASMMult(new RegOp(REG.R0), new RegOp(sourceOperand), new RegOp(combineOperand)));
			}
			else inj.add(new ASMMult(new RegOp(sourceOperand), new RegOp(sourceOperand), new RegOp(combineOperand)));
		}
		else if (a.assignArith == ASSIGN_ARITH.DIV_ASSIGN || a.assignArith == ASSIGN_ARITH.MOD_ASSIGN) {
			/* Move operands, so sourceOperand = 0 and combineOperand = 1 */
			if (sourceOperand != 0) {
				if (combineOperand == 0) {
					if (sourceOperand != 1) {
						inj.add(new ASMMov(new RegOp(REG.R1), new RegOp(REG.R0)));
						inj.add(new ASMMov(new RegOp(REG.R0), new RegOp(sourceOperand)));
					}
					else {
						/* Swap */
						inj.add(new ASMMov(new RegOp(REG.R2), new RegOp(REG.R0)));
						inj.add(new ASMMov(new RegOp(REG.R0), new RegOp(REG.R1)));
						inj.add(new ASMMov(new RegOp(REG.R1), new RegOp(REG.R2)));
					}
				}
				else {
					inj.add(new ASMMov(new RegOp(REG.R0), new RegOp(sourceOperand)));
					inj.add(new ASMMov(new RegOp(REG.R1), new RegOp(combineOperand)));
				}
			}
			else if (combineOperand != 1) 
				inj.add(new ASMMov(new RegOp(REG.R1), new RegOp(combineOperand)));
			
			/* Branch to subroutine */
			if (a.assignArith == ASSIGN_ARITH.DIV_ASSIGN) 
				inj.add(new ASMBranch(BRANCH_TYPE.BL, new LabelOp(new ASMLabel("__op_div"))));
			else 
				inj.add(new ASMBranch(BRANCH_TYPE.BL, new LabelOp(new ASMLabel("__op_mod"))));
			
			/* Move result to target */
			if (directInjection) 
				inj.add(new ASMMov(new RegOp(sourceOperand), new RegOp(REG.R0)));
		}
		else if (a.assignArith == ASSIGN_ARITH.LSL_ASSIGN) {
			if (!directInjection) {
				if (otherOp == 0) save = true;
				inj.add(new ASMLsl(new RegOp(REG.R0), new RegOp(sourceOperand), new RegOp(combineOperand)));
			}
			else inj.add(new ASMLsl(new RegOp(sourceOperand), new RegOp(sourceOperand), new RegOp(combineOperand)));
		}
		else if (a.assignArith == ASSIGN_ARITH.LSR_ASSIGN) {
			if (!directInjection) {
				if (otherOp == 0) save = true;
				inj.add(new ASMLsr(new RegOp(REG.R0), new RegOp(sourceOperand), new RegOp(combineOperand)));
			}
			else inj.add(new ASMLsr(new RegOp(sourceOperand), new RegOp(sourceOperand), new RegOp(combineOperand)));
		}
		else if (a.assignArith == ASSIGN_ARITH.BIT_ORR_ASSIGN) {
			if (!directInjection) {
				if (otherOp == 0) save = true;
				inj.add(new ASMOrr(new RegOp(REG.R0), new RegOp(sourceOperand), new RegOp(combineOperand)));
			}
			else inj.add(new ASMOrr(new RegOp(sourceOperand), new RegOp(sourceOperand), new RegOp(combineOperand)));
		}
		else if (a.assignArith == ASSIGN_ARITH.BIT_AND_ASSIGN) {
			if (!directInjection) {
				if (otherOp == 0) save = true;
				inj.add(new ASMAnd(new RegOp(REG.R0), new RegOp(sourceOperand), new RegOp(combineOperand)));
			}
			else inj.add(new ASMAnd(new RegOp(sourceOperand), new RegOp(sourceOperand), new RegOp(combineOperand)));
		}
		else if (a.assignArith == ASSIGN_ARITH.BIT_XOR_ASSIGN) {
			if (!directInjection) {
				if (otherOp == 0) save = true;
				inj.add(new ASMEor(new RegOp(REG.R0), new RegOp(sourceOperand), new RegOp(combineOperand)));
			}
			else inj.add(new ASMEor(new RegOp(sourceOperand), new RegOp(sourceOperand), new RegOp(combineOperand)));
		}
		else if (a.assignArith == ASSIGN_ARITH.AND_ASSIGN) {
			ASMAdd and0 = new ASMAdd(new RegOp(combineOperand), new RegOp(combineOperand), new ImmOp(0));
			and0.updateConditionField = true;
			inj.add(and0);
			
			inj.add(new ASMMov(new RegOp(combineOperand), new ImmOp(1), new Cond(COND.NE)));
			
			inj.add(new ASMCmp(new RegOp(sourceOperand), new ImmOp(0)));
			
			if (!directInjection) {
				inj.add(new ASMMov(new RegOp(REG.R0), new RegOp(combineOperand), new Cond(COND.NE)));
				inj.add(new ASMMov(new RegOp(REG.R0), new ImmOp(0), new Cond(COND.EQ)));
			}
			else {
				inj.add(new ASMMov(new RegOp(sourceOperand), new RegOp(combineOperand), new Cond(COND.NE)));
				inj.add(new ASMMov(new RegOp(sourceOperand), new ImmOp(0), new Cond(COND.EQ)));
			}
		}
		else if (a.assignArith == ASSIGN_ARITH.ORR_ASSIGN) {
			ASMOrr orr = new ASMOrr(new RegOp(sourceOperand), new RegOp(sourceOperand), new RegOp(combineOperand));
			orr.updateConditionField = true;
			inj.add(orr);
			
			if (!directInjection) {
				inj.add(new ASMMov(new RegOp(REG.R0), new ImmOp(1), new Cond(COND.NE)));
				inj.add(new ASMMov(new RegOp(REG.R0), new ImmOp(0), new Cond(COND.EQ)));
			}
			else {
				inj.add(new ASMMov(new RegOp(sourceOperand), new ImmOp(1), new Cond(COND.NE)));
				inj.add(new ASMMov(new RegOp(sourceOperand), new ImmOp(0), new Cond(COND.EQ)));
			}
		}
		else throw new SNIPS_EXC(Const.OPERATION_NOT_IMPLEMENTED);
		
		/* Pop Last Operand Register if Operand Register was used */
		if (clearOtherOperandReg && save) {
			inj.add(0, new ASMPushStack(new RegOp(otherOp)));
			inj.add(new ASMPopStack(new RegOp(otherOp)));
		}
		
		return inj;
	}
	
} 
