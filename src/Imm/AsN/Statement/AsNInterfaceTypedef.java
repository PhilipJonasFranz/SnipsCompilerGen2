package Imm.AsN.Statement;

import CGen.LabelGen;
import CGen.MemoryMap;
import CGen.RegSet;
import CGen.StackSet;
import Exc.CGEN_EXC;
import Imm.ASM.ASMInstruction.OPT_FLAG;
import Imm.ASM.Branch.ASMBranch;
import Imm.ASM.Branch.ASMBranch.BRANCH_TYPE;
import Imm.ASM.Memory.ASMLdr;
import Imm.ASM.Processing.Arith.ASMAdd;
import Imm.ASM.Processing.Arith.ASMLsl;
import Imm.ASM.Processing.Arith.ASMMov;
import Imm.ASM.Processing.Logic.ASMCmp;
import Imm.ASM.Structural.Label.ASMLabel;
import Imm.ASM.Util.Cond;
import Imm.ASM.Util.Cond.COND;
import Imm.ASM.Util.Operands.ImmOp;
import Imm.ASM.Util.Operands.LabelOp;
import Imm.ASM.Util.Operands.RegOp;
import Imm.ASM.Util.Operands.RegOp.REG;
import Imm.AST.Function;
import Imm.AST.Statement.InterfaceTypedef;
import Imm.AST.Statement.StructTypedef;
import Imm.AsN.AsNNode;

public class AsNInterfaceTypedef extends AsNNode {

	public ASMLabel tableHead;
	
	public static AsNInterfaceTypedef cast(InterfaceTypedef def, RegSet r, MemoryMap map, StackSet st) throws CGEN_EXC {
		AsNInterfaceTypedef intf = new AsNInterfaceTypedef();
		def.castedNode = intf;
		
		/* Create relay table */
		intf.tableHead = new ASMLabel(LabelGen.getLabel());
		
		ASMLabel tableEnd = new ASMLabel(LabelGen.getLabel());
		
		intf.instructions.add(intf.tableHead);
		
		/* Load SID from pointer into R10 */
		intf.instructions.add(new ASMLsl(new RegOp(REG.R10), new RegOp(REG.R0), new ImmOp(2)));
		intf.instructions.add(new ASMLdr(new RegOp(REG.R10), new RegOp(REG.R10)));
		
		int cnt = 0;
		
		/* Generate the table */
		for (StructTypedef s : def.implementers) {
			intf.instructions.add(new ASMCmp(new RegOp(REG.R10), new ImmOp(s.SID)));
			intf.instructions.add(new ASMMov(new RegOp(REG.R10), new ImmOp(cnt++ * 4 * def.functions.size()), new Cond(COND.EQ)));
			intf.instructions.add(new ASMBranch(BRANCH_TYPE.B, new Cond(COND.EQ), new LabelOp(tableEnd)));
		}
		
		intf.instructions.add(tableEnd);
		
		/* Add selected function to offset */
		intf.instructions.add(new ASMAdd(new RegOp(REG.R10), new RegOp(REG.R10), new RegOp(REG.R12)));
		intf.instructions.add(new ASMAdd(new RegOp(REG.R10), new RegOp(REG.R10), new ImmOp(4)));
		intf.instructions.add(new ASMMov(new RegOp(REG.R12), new ImmOp(0)));
		
		intf.instructions.add(new ASMAdd(new RegOp(REG.PC), new RegOp(REG.PC), new RegOp(REG.R10)));
		
		for (StructTypedef s : def.implementers) {
			for (Function f : s.functions) {
				/* Branch to function */
				String target = f.path.build(); // + f.getProvisoPostfix(f.provisosCalls.get(0).provisoMapping);
				
				ASMLabel functionLabel = new ASMLabel(target);
				
				ASMBranch b = new ASMBranch(BRANCH_TYPE.B, new LabelOp(functionLabel));
				b.optFlags.add(OPT_FLAG.SYS_JMP);
				
				intf.instructions.add(b);
			}
		}
		
		return intf;
	}
	
} 
