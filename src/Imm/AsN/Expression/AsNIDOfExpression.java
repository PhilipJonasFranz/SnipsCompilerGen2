package Imm.AsN.Expression;

import CGen.MemoryMap;
import CGen.RegSet;
import CGen.StackSet;
import Exc.CGEN_EXC;
import Imm.ASM.Memory.ASMLdrLabel;
import Imm.ASM.Util.Operands.LabelOp;
import Imm.ASM.Util.Operands.RegOp;
import Imm.ASM.Util.Operands.RegOp.REG;
import Imm.AST.Expression.IDOfExpression;
import Imm.TYPE.COMPOSIT.STRUCT;

public class AsNIDOfExpression extends AsNExpression {

			/* ---< METHODS >--- */
	public static AsNIDOfExpression cast(IDOfExpression soe, RegSet r, MemoryMap map, StackSet st, int target) throws CGEN_EXC {
		AsNIDOfExpression s = new AsNIDOfExpression();
		soe.castedNode = s;
		
		r.free(0);
		
		STRUCT struct = (STRUCT) soe.type;
		
		LabelOp operand = new LabelOp(struct.getTypedef().SIDLabel);
		s.instructions.add(new ASMLdrLabel(new RegOp(REG.R0), operand, null));
		
		return s;
	}
	
} 
