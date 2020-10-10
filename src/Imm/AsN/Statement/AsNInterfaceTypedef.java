package Imm.AsN.Statement;

import java.util.ArrayList;
import java.util.List;

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
import Imm.ASM.Structural.ASMComment;
import Imm.ASM.Structural.ASMSeperator;
import Imm.ASM.Structural.Label.ASMLabel;
import Imm.ASM.Util.Cond;
import Imm.ASM.Util.Cond.COND;
import Imm.ASM.Util.Operands.ImmOp;
import Imm.ASM.Util.Operands.LabelOp;
import Imm.ASM.Util.Operands.RegOp;
import Imm.ASM.Util.Operands.RegOp.REG;
import Imm.AST.Function;
import Imm.AST.Statement.InterfaceTypedef;
import Imm.AST.Statement.InterfaceTypedef.InterfaceProvisoMapping;
import Imm.AST.Statement.StructTypedef;
import Imm.AsN.AsNNode;
import Imm.TYPE.TYPE;

public class AsNInterfaceTypedef extends AsNNode {

	public ASMLabel tableHead;
	
	public static AsNInterfaceTypedef cast(InterfaceTypedef def, RegSet r, MemoryMap map, StackSet st) throws CGEN_EXC {
		AsNInterfaceTypedef intf = new AsNInterfaceTypedef();
		def.castedNode = intf;

		/* Create relay table */
		intf.tableHead = new ASMLabel(LabelGen.getLabel());

		/*
		 * Change names of proviso mappings if the types are word-size equal.
		 * The translation will result in the same assembly, so we dont have to do it again.
		 * The name is set to the first occurrence of the similar mappings. Down below
		 * the name is checked if its already in a list of translated mappings.
		 * 
		 * This algorithm is equal to the one in AsNFunction, and thus produces the same
		 * results as its counterpart, this means that equal proviso mappings are merged
		 * the same way on both sides.
		 */
		for (int i = 0; i < def.registeredMappings.size(); i++) {
			InterfaceProvisoMapping call0 = def.registeredMappings.get(i);
			for (int a = i + 1; a < def.registeredMappings.size(); a++) {
				InterfaceProvisoMapping call1 = def.registeredMappings.get(a);
				
				if (!call0.provisoPostfix.equals(call1.provisoPostfix)) {
					boolean equal = true;
					
					/* Check for equal parameter types */
					for (int k = 0; k < call0.providedHeadProvisos.size(); k++) 
						equal &= call0.providedHeadProvisos.get(k).wordsize() == call1.providedHeadProvisos.get(k).wordsize();
					
					/* 
					 * Mappings are of equal types, set label gen postfix of 
					 * this one to the other equal one 
					 */
					if (equal) 
						call1.provisoPostfix = call0.provisoPostfix;
				}
			}
		}
		
		List<String> createdTable = new ArrayList();
		
		/* For all proviso mappings, create a relay-table */
		for (InterfaceProvisoMapping mapping : def.registeredMappings) {
			/* 
			 * Check if a table has been created for this 'proviso group' 
			 * has been created already, if yes, simply continue. During function
			 * casting, the exact algorithm will be executed, resulting in correct
			 * and equal results on both ends.
			 */
			if (createdTable.contains(mapping.provisoPostfix))
				continue;
			else 
				createdTable.add(mapping.provisoPostfix);
			
			/* Generate comment with function name and potential proviso types */
			String s = "Interface : " + def.path.build();
			
			List<TYPE> types = mapping.providedHeadProvisos;
			
			if (!types.isEmpty())
				s += ", Provisos: ";
				
			for (int x = 0; x < types.size(); x++) 
				s += types.get(x).provisoFree().typeString() + ", ";
			
			if (!mapping.providedHeadProvisos.isEmpty())
				s = s.trim().substring(0, s.trim().length() - 1);
			
			ASMComment com = new ASMComment(s);
			
			/* Get the current proviso postfix. If only the default mapping exists, let postfix empty */
			String postfix = mapping.provisoPostfix;
			if (def.registeredMappings.size() == 1 && mapping.providedHeadProvisos.isEmpty())
				postfix = "";
			
			/* Head of the table for this proviso mapping */
			ASMLabel tableHeadProviso = new ASMLabel(intf.tableHead.name + postfix);
			
			tableHeadProviso.comment = com;
			
			/* Label at the end of the table multiplexing section */
			ASMLabel tableEnd = new ASMLabel(LabelGen.getLabel());
			
			intf.instructions.add(tableHeadProviso);
			
			/* Load SID from pointer into R10 */
			intf.instructions.add(new ASMLsl(new RegOp(REG.R10), new RegOp(REG.R0), new ImmOp(2)));
			intf.instructions.add(new ASMLdr(new RegOp(REG.R10), new RegOp(REG.R10)));
			
			int cnt = 0;
			
			/* Generate the table */
			for (StructTypedef struct : def.implementers) {
				intf.instructions.add(new ASMCmp(new RegOp(REG.R10), new ImmOp(struct.SID)));
				intf.instructions.add(new ASMMov(new RegOp(REG.R10), new ImmOp(cnt++ * 4 * def.functions.size()), new Cond(COND.EQ)));
				intf.instructions.add(new ASMBranch(BRANCH_TYPE.B, new Cond(COND.EQ), new LabelOp(tableEnd)));
			}
			
			intf.instructions.add(tableEnd);
			
			/* Add selected function to offset */
			intf.instructions.add(new ASMAdd(new RegOp(REG.R10), new RegOp(REG.R10), new RegOp(REG.R12)));
			intf.instructions.add(new ASMAdd(new RegOp(REG.R10), new RegOp(REG.R10), new ImmOp(4)));
			intf.instructions.add(new ASMMov(new RegOp(REG.R12), new ImmOp(0)));
			
			intf.instructions.add(new ASMAdd(new RegOp(REG.PC), new RegOp(REG.PC), new RegOp(REG.R10)));
			
			for (StructTypedef struct : def.implementers) {
				for (Function f : def.functions) {
					Function f0 = null;
					
					for (int i = 0; i < struct.functions.size(); i++)
						if (struct.functions.get(i).path.getLast().equals(f.path.getLast()))
							f0 = struct.functions.get(i);
					
					/* Branch to function */
					String target = f0.path.build();
					
					String post = f0.getProvisoPostfix(mapping.providedHeadProvisos);
					
					if (post == null) {
						ASMAdd add = new ASMAdd(new RegOp(REG.R10), new RegOp(REG.R10), new RegOp(REG.R10));
						add.comment = new ASMComment("Function was not called, use as placeholder");
						
						intf.instructions.add(add);
					}
					else {
						target += post;
						
						ASMLabel functionLabel = new ASMLabel(target);
						
						ASMBranch b = new ASMBranch(BRANCH_TYPE.B, new LabelOp(functionLabel));
						b.optFlags.add(OPT_FLAG.SYS_JMP);
						
						intf.instructions.add(b);
					}
				}
			}
			
			if (!mapping.equals(def.registeredMappings.get(def.registeredMappings.size() - 1)) && def.registeredMappings.size() > 1)
				intf.instructions.add(new ASMSeperator());
		}
		
		return intf;
	}
	
} 
