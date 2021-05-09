package Imm.ASM.VFP.Memory;

import Imm.ASM.Memory.ASMLdrLabel;
import Imm.ASM.Util.Operands.LabelOp;
import Imm.ASM.Util.Operands.RegOp;
import Imm.AST.Statement.Declaration;
import Snips.CompilerDriver;

public class ASMVLdrLabel extends ASMLdrLabel {
	
			/* ---< CONSTRUCTORS >--- */
	/** Example Usage: ldr r0, a_label */
	public ASMVLdrLabel(RegOp target, LabelOp op0, Declaration dec) {
		super(target, op0, null);
		this.dec = dec;
	}
	
			/* ---< METHODS >--- */
	public String build() {
		String s = CompilerDriver.printDepth + "vldr " + target.toString();
		s += ", " + this.prefix;
		if (!this.prefix.equals("")) {
			String s0 = this.op0.toString();
			if (s0.startsWith(".")) s0 = s0.substring(1);
			s += s0;
		}
		else s += this.op0.toString();
		
		return s;
	}
	
	public ASMVLdrLabel clone() {
		RegOp r = new RegOp(this.target.reg);
		LabelOp l = new LabelOp(((LabelOp) this.op0).label);
		return new ASMVLdrLabel(r, l, this.dec);
	}
	
} 
