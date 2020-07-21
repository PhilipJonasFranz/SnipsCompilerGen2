package Imm.AsN.Expression;

import CGen.MemoryMap;
import CGen.RegSet;
import CGen.StackSet;
import Exc.CGEN_EXC;
import Imm.ASM.ASMInstruction.OPT_FLAG;
import Imm.ASM.Branch.ASMBranch;
import Imm.ASM.Branch.ASMBranch.BRANCH_TYPE;
import Imm.ASM.Processing.Arith.ASMAdd;
import Imm.ASM.Processing.Arith.ASMMov;
import Imm.ASM.Structural.Label.ASMLabel;
import Imm.ASM.Util.Operands.ImmOp;
import Imm.ASM.Util.Operands.LabelOp;
import Imm.ASM.Util.Operands.RegOp;
import Imm.ASM.Util.Operands.RegOp.REG;
import Imm.AST.Expression.FunctionRef;

public class AsNFunctionRef extends AsNExpression {

			/* --- METHODS --- */
	public static AsNFunctionRef cast(FunctionRef i, RegSet r, MemoryMap map, StackSet st, int target) throws CGEN_EXC {
		AsNFunctionRef ref = new AsNFunctionRef();
		i.castedNode = ref;
		
		ref.instructions.add(new ASMAdd(new RegOp(REG.R10), new RegOp(REG.PC), new ImmOp(8)));
		
		String label = "lambda_" + i.origin.path.build() + i.origin.manager.getPostfix(i.proviso);
		
		ASMBranch branch = new ASMBranch(BRANCH_TYPE.B, new LabelOp(new ASMLabel(label)));
		branch.optFlags.add(OPT_FLAG.SYS_JMP);
		ref.instructions.add(branch);
		
		/* Reset R10 */
		ref.instructions.add(new ASMMov(new RegOp(REG.R10), new ImmOp(0)));
		
		return ref;
	}
	
}
