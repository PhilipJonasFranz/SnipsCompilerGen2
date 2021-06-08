package Imm.AsN.Expression;

import CGen.MemoryMap;
import CGen.RegSet;
import CGen.StackSet;
import Imm.ASM.Memory.ASMLdrLabel;
import Imm.ASM.Structural.Label.ASMDataLabel;
import Imm.ASM.Util.REG;
import Imm.ASM.Util.Operands.LabelOp;
import Imm.ASM.Util.Operands.RegOp;
import Imm.ASM.Util.Operands.Memory.MemoryWordOp;
import Imm.AST.Expression.FunctionRef;

public class AsNFunctionRef extends AsNExpression {

			/* ---< METHODS >--- */
	public static AsNFunctionRef cast(FunctionRef i, RegSet r, MemoryMap map, StackSet st, int target) {
		AsNFunctionRef ref = new AsNFunctionRef().pushCreatorStack(i);

		/* Construct label name for function lambda target with provided provisos */
		String label = i.origin.path + ((i.origin.requireUIDInLabel)? "_" + i.origin.UID : "") + i.origin.getProvisoPostfix(i.proviso);
		
		ASMDataLabel entry = new ASMDataLabel(label, new MemoryWordOp(0));
		
		LabelOp operand = new LabelOp(entry);
		ref.instructions.add(new ASMLdrLabel(new RegOp(REG.R0), operand, null));

		return ref.popCreatorStack();
	}
	
} 
