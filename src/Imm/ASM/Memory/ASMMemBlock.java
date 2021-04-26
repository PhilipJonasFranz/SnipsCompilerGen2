package Imm.ASM.Memory;

import java.util.List;

import Imm.ASM.ASMInstruction;
import Imm.ASM.Util.COND;
import Imm.ASM.Util.Operands.RegOp;
import Snips.CompilerDriver;

public class ASMMemBlock extends ASMInstruction {

			/* ---< NESTED >--- */
	public enum MEM_BLOCK_MODE {
		LDMED, LDMFD, LDMEA, LDMFA,
		
		STMFA, STMEA, STMFD, STMED;
	}
	
			/* ---< FIELDS >--- */
	public MEM_BLOCK_MODE mode;
	
	public boolean writeback;
	
	/** The target of the memory operation, or the origin when loading */
	public RegOp target;
	
	public List<RegOp> registerList;
	
	
			/* ---< CONSTRUCTORS >--- */
	/** Example Usage: ldr/str r0, [r1] */
	public ASMMemBlock(MEM_BLOCK_MODE mode, boolean writeback, RegOp target, List<RegOp> registerList, COND cond) {
		super(cond);
		this.target = target;
		this.registerList = registerList;
		this.mode = mode;
		this.writeback = writeback;
	}
	
	
			/* ---< METHODS >--- */
	public static boolean checkInOrder(List<RegOp> operands) {
		if (operands.size() == 1) return true;
		else {
			for (int i = 1; i < operands.size(); i++) {
				if (RegOp.toInt(operands.get(i - 1).reg) > RegOp.toInt(operands.get(i).reg)) 
					return false;
			}
			
			return true;
		}
	}
	
	public String build() {
		String op = this.mode.toString().toLowerCase();
		if (this.cond != null) op += this.cond.getCondPostfix();
		
		op += " ";
		
		op += this.target.toString();
		if (this.writeback) op += "!, {";
		else op += ", {";
		
		int streak = -1;
		for (int i = 0; i < this.registerList.size(); i++) {
			if (streak == -1) {
				streak = RegOp.toInt(this.registerList.get(i).reg);
			}
			else {
				if (RegOp.toInt(this.registerList.get(i).reg) != RegOp.toInt(this.registerList.get(i - 1).reg) + 1) {
					/* End Streak */
					if (RegOp.toInt(this.registerList.get(i - 1).reg) - streak < 2) {
						op += RegOp.toReg(streak) + ", ";
						if (RegOp.toInt(this.registerList.get(i - 1).reg) != streak) op += this.registerList.get(i - 1).reg.toString() + ", ";
					}
					else {
						op += RegOp.toReg(streak) + "-";
						if (RegOp.toInt(this.registerList.get(i - 1).reg) != streak) op += this.registerList.get(i - 1).reg.toString() + ", ";
					}
					
					streak = RegOp.toInt(this.registerList.get(i).reg);
				}
				else if (i == this.registerList.size() - 1) {
					/* End Streak */
					if (RegOp.toInt(this.registerList.get(i).reg) - streak < 2) {
						op += RegOp.toReg(streak) + ", ";
						op += this.registerList.get(i).reg.toString() + ", ";
					}
					else {
						op += RegOp.toReg(streak) + "-";
						op += this.registerList.get(i).reg.toString() + ", ";
					}
					
					streak = -1;
					break;
				}
			}
		}
		
		if (streak != -1) {
			op += RegOp.toReg(streak) + ", ";
		}
		
		op = op.trim();
		if (op.endsWith(",")) op = op.substring(0, op.length() - 1);
		
		op += "}";
		return (CompilerDriver.printDepth + op).toLowerCase();
	}
	
} 
