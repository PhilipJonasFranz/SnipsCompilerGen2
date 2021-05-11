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
		super(target, op0, dec);
	}
	
			/* ---< METHODS >--- */
	public String build() {
		return CompilerDriver.printDepth + "v" + super.toString().trim();
	}
	
	public ASMVLdrLabel clone() {
		RegOp r = new RegOp(this.target.reg);
		LabelOp l = new LabelOp(((LabelOp) this.op0).label);
		return new ASMVLdrLabel(r, l, this.dec);
	}
	
} 
