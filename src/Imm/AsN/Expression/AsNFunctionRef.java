package Imm.AsN.Expression;

import CGen.MemoryMap;
import CGen.RegSet;
import CGen.StackSet;
import Exc.CGEN_EXCEPTION;
import Imm.ASM.ASMInstruction.OPT_FLAG;
import Imm.ASM.Branch.ASMBranch;
import Imm.ASM.Branch.ASMBranch.BRANCH_TYPE;
import Imm.ASM.Processing.Arith.ASMAdd;
import Imm.ASM.Processing.Arith.ASMMov;
import Imm.ASM.Structural.Label.ASMLabel;
import Imm.ASM.Util.Operands.ImmOperand;
import Imm.ASM.Util.Operands.LabelOperand;
import Imm.ASM.Util.Operands.RegOperand;
import Imm.ASM.Util.Operands.RegOperand.REGISTER;
import Imm.AST.Expression.FunctionRef;

public class AsNFunctionRef extends AsNExpression {

			/* --- METHODS --- */
	public static AsNFunctionRef cast(FunctionRef i, RegSet r, MemoryMap map, StackSet st, int target) throws CGEN_EXCEPTION {
		AsNFunctionRef ref = new AsNFunctionRef();
		i.castedNode = ref;
		
		ref.instructions.add(new ASMAdd(new RegOperand(REGISTER.R10), new RegOperand(REGISTER.PC), new ImmOperand(8)));
		
		String label = "lambda_" + i.origin.path.build() + i.origin.manager.getPostfix(i.proviso);
		
		ASMBranch branch = new ASMBranch(BRANCH_TYPE.B, new LabelOperand(new ASMLabel(label)));
		branch.optFlags.add(OPT_FLAG.SYS_JMP);
		ref.instructions.add(branch);
		
		/* Reset R10 */
		ref.instructions.add(new ASMMov(new RegOperand(REGISTER.R10), new ImmOperand(0)));
		
		return ref;
	}
	
}
