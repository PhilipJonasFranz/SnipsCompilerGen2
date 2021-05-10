package Imm.ASM.VFP.Memory;

import java.util.List;

import Imm.ASM.Memory.ASMMemBlock;
import Imm.ASM.Util.COND;
import Imm.ASM.Util.Operands.RegOp;
import Snips.CompilerDriver;

public class ASMVMemBlock extends ASMMemBlock {

			/* ---< CONSTRUCTORS >--- */
	public ASMVMemBlock(MEM_BLOCK_MODE mode, boolean writeback, RegOp target, List<RegOp> registerList, COND cond) {
		super(mode, writeback, target, registerList, cond);
	}
	
	
			/* ---< METHODS >--- */
	public String build() {
		return CompilerDriver.printDepth + "v" + super.build().trim();
	}
	
} 
