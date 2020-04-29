package Imm.ASM.Memory;

import Imm.ASM.Util.Operands.LabelOperand;
import Imm.ASM.Util.Operands.RegOperand;
import Imm.AST.Statement.Declaration;
import Snips.CompilerDriver;

public class ASMLdrLabel extends ASMLdr {

	public String prefix = "";
	
	public Declaration dec;
	
	
			/* --- CONSTRUCTORS --- */
	/** Example Usage: ldr r0, a_label */
	public ASMLdrLabel(RegOperand target, LabelOperand op0, Declaration dec) {
		super(target, op0, null);
		this.dec = dec;
	}
	
			/* --- METHODS --- */
	public String build() {
		String s = CompilerDriver.printDepth + "ldr " + target.toString();
		s += ", " + this.prefix;
		if (!this.prefix.equals("")) {
			String s0 = this.op0.toString();
			if (s0.startsWith(".")) s0 = s0.substring(1);
			s += s0;
		}
		else this.op0.toString();
		return s;
	}
	
	public ASMLdrLabel clone() {
		RegOperand r = new RegOperand(this.target.reg);
		LabelOperand l = new LabelOperand(((LabelOperand) this.op0).label);
		return new ASMLdrLabel(r, l, this.dec);
	}
	
	public String getName() {
		return this.prefix + target.toString();
	}
	
}
