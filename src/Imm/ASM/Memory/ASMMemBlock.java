package Imm.ASM.Memory;

import java.util.List;

import Imm.ASM.ASMInstruction;
import Imm.ASM.Util.Cond;
import Imm.ASM.Util.Operands.RegOperand;
import Snips.CompilerDriver;

public class ASMMemBlock extends ASMInstruction {

			/* --- NESTED --- */
	public enum MEM_BLOCK_MODE {
		LDMED, LDMFD, LDMEA, LDMFA,
		
		STMFA, STMEA, STMFD, STMED;
	}
	
			/* --- FIELDS --- */
	public MEM_BLOCK_MODE mode;
	
	public boolean writeback;
	
	/** The target of the memory operation, or the origin when loading */
	public RegOperand target;
	
	public List<RegOperand> registerList;
	
	/* Set to true when op1 is reg Operand and is supposed to be subtracted from base */
	public boolean subFromBase = false;
	
	
			/* --- CONSTRUCTORS --- */
	/** Example Usage: ldr/str r0, [r1, #2] */
	public ASMMemBlock(MEM_BLOCK_MODE mode, boolean writeback, RegOperand target, List<RegOperand> registerList) {
		this.target = target;
		this.registerList = registerList;
		this.mode = mode;
		this.writeback = writeback;
	}
	
	/** Example Usage: ldr/str r0, [r1] */
	public ASMMemBlock(MEM_BLOCK_MODE mode, boolean writeback, RegOperand target, List<RegOperand> registerList, Cond cond) {
		super(cond);
		this.target = target;
		this.registerList = registerList;
		this.mode = mode;
		this.writeback = writeback;
	}
	
	
			/* --- METHODS --- */
	public static boolean checkInOrder(List<RegOperand> operands) {
		if (operands.size() == 1) return true;
		else {
			for (int i = 1; i < operands.size(); i++) {
				if (RegOperand.toInt(operands.get(i - 1).reg) > RegOperand.toInt(operands.get(i).reg)) 
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
				streak = RegOperand.toInt(this.registerList.get(i).reg);
			}
			else {
				if (RegOperand.toInt(this.registerList.get(i).reg) != RegOperand.toInt(this.registerList.get(i - 1).reg) + 1) {
					/* End Streak */
					if (RegOperand.toInt(this.registerList.get(i - 1).reg) - streak < 2) {
						op += RegOperand.toReg(streak) + ", ";
						if (RegOperand.toInt(this.registerList.get(i - 1).reg) != streak) op += this.registerList.get(i - 1).reg.toString() + ", ";
					}
					else {
						op += RegOperand.toReg(streak) + "-";
						if (RegOperand.toInt(this.registerList.get(i - 1).reg) != streak) op += this.registerList.get(i - 1).reg.toString() + ", ";
					}
					
					streak = RegOperand.toInt(this.registerList.get(i).reg);
				}
				else if (i == this.registerList.size() - 1) {
					/* End Streak */
					if (RegOperand.toInt(this.registerList.get(i).reg) - streak < 2) {
						op += RegOperand.toReg(streak) + ", ";
						op += this.registerList.get(i).reg.toString() + ", ";
					}
					else {
						op += RegOperand.toReg(streak) + "-";
						op += this.registerList.get(i).reg.toString() + ", ";
					}
					
					streak = -1;
					break;
				}
			}
		}
		
		if (streak != -1) {
			op += RegOperand.toReg(streak) + ", ";
		}
		
		op = op.trim();
		if (op.endsWith(",")) op = op.substring(0, op.length() - 1);
		
		op += "}";
		return CompilerDriver.printDepth + op;
	}
	
}
