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

		/* Not a single mapping was registered, interface was not used. */
		if (def.registeredMappings.isEmpty())
			return intf;
		
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
		
		/* Check if anything from the interface was used. If not, the interface table does not have to be generated. */
		boolean hasCalls = false;
		
		/* Only one mapping and only one function, table will have only one entry, directly relay to single function target. */
		if (def.registeredMappings.size() == 1 && def.registeredMappings.get(0).providedHeadProvisos.isEmpty() && def.implementers.size() == 1 && def.functions.size() == 1) {
			/* Generate comment with function name and potential proviso types */
			String s = "Interface : " + def.path.build();
			
			/* Head of the table for this proviso mapping */
			ASMLabel tableHeadProviso = new ASMLabel(intf.tableHead.name);
			tableHeadProviso.comment =  new ASMComment(s);
			intf.instructions.add(tableHeadProviso);
			
			hasCalls |= intf.injectStructTypedefTableMapping(def, def.implementers.get(0), def.registeredMappings.get(0));
		}
		else {
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
				else createdTable.add(mapping.provisoPostfix);
				
				/* Generate comment with function name and potential proviso types */
				String s = "Interface : " + def.path.build();
				
				List<TYPE> types = mapping.providedHeadProvisos;
				if (!types.isEmpty()) s += ", Provisos: ";
				for (int x = 0; x < types.size(); x++) 
					s += types.get(x).provisoFree().typeString() + ", ";
				if (s.isEmpty()) s = s.trim().substring(0, s.trim().length() - 1);
				
				/* Get the current proviso postfix. If only the default mapping exists, let postfix empty */
				String postfix = mapping.provisoPostfix;
				if (def.registeredMappings.size() == 1 && mapping.providedHeadProvisos.isEmpty())
					postfix = "";
				
				/* Head of the table for this proviso mapping */
				ASMLabel tableHeadProviso = new ASMLabel(intf.tableHead.name + postfix);
				tableHeadProviso.comment =  new ASMComment(s);
				intf.instructions.add(tableHeadProviso);
				
				/**
				 * Only one struct implemented this interface, so the SID must be the one from 
				 * this struct. This allows to precalculate some values, which is done below.
				 */
				if (def.implementers.size() == 1) {
					
					/* Compute the offset to be jumped to select the correct function */
					intf.instructions.add(new ASMAdd(new RegOp(REG.R10), new RegOp(REG.R12), new ImmOp(4)));
					intf.instructions.add(new ASMMov(new RegOp(REG.R12), new ImmOp(0)));
	
					/* Jump to the correct function */
					intf.instructions.add(new ASMAdd(new RegOp(REG.PC), new RegOp(REG.PC), new RegOp(REG.R10)));
					
					StructTypedef sdef = def.implementers.get(0);
					
					/* Inject for only struct typedef */
					hasCalls |= intf.injectStructTypedefTableMapping(def, sdef, mapping);
				}
				/**
				 * Multiple structs implement this interface, so the SID needs to be loaded and mapped
				 * to the index of the struct typedef in the InterfaceTypedef.implementes list.
				 */
				else {
					/* Label at the end of the table multiplexing section */
					ASMLabel tableEnd = new ASMLabel(LabelGen.getLabel());
					
					/* Load SID from pointer into R10 */
					intf.instructions.add(new ASMLsl(new RegOp(REG.R10), new RegOp(REG.R0), new ImmOp(2)));
					intf.instructions.add(new ASMLdr(new RegOp(REG.R10), new RegOp(REG.R10)));
					
					int cnt = 0;
					
					/* Generate the SID-to-Index mapper */
					for (StructTypedef struct : def.implementers) {
						intf.instructions.add(new ASMCmp(new RegOp(REG.R10), new ImmOp(struct.SID)));
						intf.instructions.add(new ASMMov(new RegOp(REG.R10), new ImmOp(cnt++ * 4 * def.functions.size()), new Cond(COND.EQ)));
						intf.instructions.add(new ASMBranch(BRANCH_TYPE.B, new Cond(COND.EQ), new LabelOp(tableEnd)));
					}
					
					intf.instructions.add(tableEnd);
					
					/* Compute the offset to be jumped to select the correct function */
					intf.instructions.add(new ASMAdd(new RegOp(REG.R10), new RegOp(REG.R10), new RegOp(REG.R12)));
					intf.instructions.add(new ASMAdd(new RegOp(REG.R10), new RegOp(REG.R10), new ImmOp(4)));
					intf.instructions.add(new ASMMov(new RegOp(REG.R12), new ImmOp(0)));
					
					/* Jump to the correct function */
					intf.instructions.add(new ASMAdd(new RegOp(REG.PC), new RegOp(REG.PC), new RegOp(REG.R10)));
					
					/* Generate the relay-table */
					for (StructTypedef struct : def.implementers) 
						hasCalls |= intf.injectStructTypedefTableMapping(def, struct, mapping);
				}
				
				intf.instructions.add(new ASMSeperator());
			}
		}
		
		/* No calls were made to the interface, and the only registered mapping 
		 * is the default mapping, interface table is not needed. 
		 */
		if (!hasCalls && def.registeredMappings.size() == 1 && def.registeredMappings.get(0).providedHeadProvisos.isEmpty())
			intf.instructions.clear();
		
		return intf;
	}
	
	/**
	 * Injects for a given struct typedef the function relays in the table. For example, 
	 * the generated instructions by this function could look like:<br>
	 * <br>
	 * b X.set<br>
	 * b X.get<br>
	 * add r10, r10, r10 ...<br>
	 * b X.foo<br>
	 */
	public boolean injectStructTypedefTableMapping(InterfaceTypedef typedef, StructTypedef def, InterfaceProvisoMapping mapping) {
		boolean hasCalls = false;
		
		for (Function f : typedef.functions) {
			Function f0 = null;
			
			/*
			 * Search function from interface in struct typedef, and set reference to it.
			 * This is done since the acutal proviso mappings of this function are stored
			 * in the function located in the struct typedef, and not in the function in
			 * the interface typedef.
			 */
			for (int i = 0; i < def.functions.size(); i++)
				if (def.functions.get(i).path.getLast().equals(f.path.getLast()))
					f0 = def.functions.get(i);
			
			/* Branch to function */
			String target = f0.path.build();
			
			/* Get the proviso postfix from the function in the struct with the current mapping */
			String post = f0.getProvisoPostfix(mapping.providedHeadProvisos);
			
			/* 
			 * The mapping has not been registered in the function, this means that this function
			 * has not been called with this specific provisos.
			 */
			if (post == null) {
				ASMAdd add = new ASMAdd(new RegOp(REG.R10), new RegOp(REG.R10), new RegOp(REG.R10));
				add.comment = new ASMComment("Function was not called, use as placeholder");
				
				this.instructions.add(add);
			}
			/*
			 * Mapping was found, append to target to create final label and branch to it.
			 */
			else {
				hasCalls |= f.wasCalled;
				
				target += post;
				
				/* Final label to function with proviso postfix */
				ASMLabel functionLabel = new ASMLabel(target);
				
				ASMBranch b = new ASMBranch(BRANCH_TYPE.B, new LabelOp(functionLabel));
				b.optFlags.add(OPT_FLAG.SYS_JMP);
				this.instructions.add(b);
			}
		}
		
		return hasCalls;
	}
	
} 
