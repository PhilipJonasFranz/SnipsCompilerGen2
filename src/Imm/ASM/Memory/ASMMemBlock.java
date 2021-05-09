package Imm.ASM.Memory;

import java.util.List;

import Imm.ASM.ASMInstruction;
import Imm.ASM.Util.COND;
import Imm.ASM.Util.REG;
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
				if (operands.get(i - 1).reg.toInt() > operands.get(i).reg.toInt()) 
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
				streak = this.registerList.get(i).reg.toInt();
			}
			else {
				if (this.registerList.get(i).reg.toInt() != this.registerList.get(i - 1).reg.toInt() + 1) {
					/* End Streak */
					if (this.registerList.get(i - 1).reg.toInt() - streak < 2) {
						op += REG.toReg(streak) + ", ";
						if (this.registerList.get(i - 1).reg.toInt() != streak) op += this.registerList.get(i - 1).reg.toString() + ", ";
					}
					else {
						op += REG.toReg(streak) + "-";
						if (this.registerList.get(i - 1).reg.toInt() != streak) op += this.registerList.get(i - 1).reg.toString() + ", ";
					}
					
					streak = this.registerList.get(i).reg.toInt();
				}
				else if (i == this.registerList.size() - 1) {
					/* End Streak */
					if (this.registerList.get(i).reg.toInt() - streak < 2) {
						op += REG.toReg(streak) + ", ";
						op += this.registerList.get(i).reg.toString() + ", ";
					}
					else {
						op += REG.toReg(streak) + "-";
						op += this.registerList.get(i).reg.toString() + ", ";
					}
					
					streak = -1;
					break;
				}
			}
		}
		
		if (streak != -1) {
			op += REG.toReg(streak) + ", ";
		}
		
		op = op.trim();
		if (op.endsWith(",")) op = op.substring(0, op.length() - 1);
		
		op += "}";
		return (CompilerDriver.printDepth + op).toLowerCase();
	}
	
	public int getRequiredCPUCycles() {
		if (this.registerList.stream().filter(x -> x.reg == REG.PC).count() > 0) {
			return this.registerList.size() + 4; // +N +(n-1)S +I +N +2S
		}
		else return this.registerList.size() + 2; // +N +(n-1)S +I +S
	}
	
} 
