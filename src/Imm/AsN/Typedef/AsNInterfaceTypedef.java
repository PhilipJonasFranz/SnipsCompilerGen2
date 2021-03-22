package Imm.AsN.Typedef;

import java.util.ArrayList;
import java.util.List;

import CGen.MemoryMap;
import CGen.RegSet;
import CGen.StackSet;
import CGen.Util.LabelUtil;
import Exc.CGEN_EXC;
import Imm.ASM.ASMInstruction;
import Imm.ASM.ASMInstruction.OPT_FLAG;
import Imm.ASM.Branch.ASMBranch;
import Imm.ASM.Branch.ASMBranch.BRANCH_TYPE;
import Imm.ASM.Memory.ASMLdr;
import Imm.ASM.Memory.Stack.ASMPopStack;
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
import Imm.AST.Typedef.InterfaceTypedef;
import Imm.AST.Typedef.InterfaceTypedef.InterfaceProvisoMapping;
import Imm.AST.Typedef.StructTypedef;
import Imm.AST.Typedef.StructTypedef.StructProvisoMapping;
import Imm.AsN.AsNNode;
import Imm.TYPE.TYPE;
import Imm.TYPE.COMPOSIT.INTERFACE;

public class AsNInterfaceTypedef extends AsNNode {

	public ASMLabel tableHead;
	
	public static AsNInterfaceTypedef cast(InterfaceTypedef def, RegSet r, MemoryMap map, StackSet st) throws CGEN_EXC {
		
		AsNInterfaceTypedef intf = new AsNInterfaceTypedef();
		def.castedNode = intf;

		/* Not a single mapping was registered, interface was not used. */
		if (def.registeredMappings.isEmpty())
			return intf;
		
		/* Set as function prefix, so label gen creates label with name reflecting the interface */
		LabelUtil.funcPrefix = def.path.build();
		LabelUtil.funcUID = -1;
		
		/* Create relay table */
		intf.tableHead = new ASMLabel(LabelUtil.getLabel());

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
					/* If there is only one function we can relay directly */
					if (def.functions.size() > 1)
						/* Compute the offset to be jumped to select the correct function */
						intf.instructions.add(new ASMAdd(new RegOp(REG.R10), new RegOp(REG.R12), new ImmOp(4)));
					
					intf.instructions.add(new ASMMov(new RegOp(REG.R12), new ImmOp(0)));
	
					/* 
					 * If only one function, the is no need to jump to the 
					 * correct branch, since there is only one 
					 */
					if (def.functions.size() > 1)
						/* Jump to the correct function */
						intf.instructions.add(new ASMAdd(new RegOp(REG.PC), new RegOp(REG.PC), new RegOp(REG.R10)));
					
					/* Inject for only struct typedef */
					hasCalls |= intf.injectStructTypedefTableMapping(def, def.implementers.get(0), mapping);
				}
				/**
				 * Multiple structs implement this interface, so the SID needs to be loaded and mapped
				 * to the index of the struct typedef in the InterfaceTypedef.implementes list.
				 */
				else {
					/* Label at the end of the table multiplexing section */
					ASMLabel tableEnd = new ASMLabel(LabelUtil.getLabel());
					
					/* Load SID from pointer into R10 */
					intf.instructions.add(new ASMLsl(new RegOp(REG.R10), new RegOp(REG.R0), new ImmOp(2)));
					intf.instructions.add(new ASMLdr(new RegOp(REG.R10), new RegOp(REG.R10)));
					
					int cnt = 0;
					
					/* Generate the SID-to-Index mapper */
					for (StructTypedef struct : def.implementers) {
						struct.loadSIDInReg(intf, REG.R10, mapping.providedHeadProvisos);
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
			
			/* Find the interface type the given struct typedef has implemented */
			INTERFACE inter = null;
			for (TYPE t : def.implemented) 
				if (((INTERFACE) t).getTypedef().equals(typedef)) 
					inter = (INTERFACE) t;
				
			/* 
			 * Create a signature clone of the searched function and translate 
			 * it to the passed provisos in the struct typedef. This is required 
			 * to match proviso types in the signature match.
			 */
			Function signature = f.cloneSignature();
			signature.translateProviso(typedef.proviso, inter.proviso);
			
			/*
			 * Search function from interface in struct typedef, and set reference to it.
			 * This is done since the acutal proviso mappings of this function are stored
			 * in the function located in the struct typedef, and not in the function in
			 * the interface typedef.
			 */
			for (int i = 0; i < def.functions.size(); i++) {
				Function sfunc = def.functions.get(i);
				if (Function.signatureMatch(signature, sfunc, false, false))
					f0 = sfunc;
			}
			
			/* Make sure the function was found */
			assert f0 != null : "Failed to locate function '" + f.path.build() + "'!";
			
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
				
				if (f.requireUIDInLabel)
					target += "@" + f0.UID;
				
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
	
	public static List<ASMInstruction> createStructFunctionRelay(AsNNode node, StructTypedef sdef, StructProvisoMapping mapping, INTERFACE intf) {
		
		InterfaceTypedef idef = intf.getTypedef();
		
		List<ASMInstruction> table = new ArrayList();
		
		String postfix = LabelUtil.getProvisoPostfix(mapping.providedHeadProvisos);
		String name = sdef.path.build() + postfix + "_" + idef.path.build();
		
		ASMLabel relayTableHead = new ASMLabel(name);
		relayTableHead.optFlags.add(OPT_FLAG.LABEL_USED);
		relayTableHead.comment = new ASMComment("Relay: " + idef.path.build() + " -> " + sdef.path.build());
		table.add(relayTableHead);
		
		mapping.resolverLabel = relayTableHead;
		
		boolean wasUsed = false;
		
		table.add(new ASMMov(new RegOp(REG.R10), new ImmOp(0)));
		
		/* Pop function offset */
		table.add(new ASMPopStack(new RegOp(REG.R12)));
		
		table.add(new ASMAdd(new RegOp(REG.R12), new RegOp(REG.R12), new ImmOp(4)));
		table.add(new ASMAdd(new RegOp(REG.PC), new RegOp(REG.PC), new RegOp(REG.R12)));
		
		for (Function f : idef.functions) {
			
			Function f0 = null;
			
			/* 
			 * Create a signature clone of the searched function and translate 
			 * it to the passed provisos in the struct typedef. This is required 
			 * to match proviso types in the signature match.
			 */
			Function signature = f.cloneSignature();
			signature.translateProviso(idef.proviso, intf.proviso);
			
			/*
			 * Search function from interface in struct typedef, and set reference to it.
			 * This is done since the acutal proviso mappings of this function are stored
			 * in the function located in the struct typedef, and not in the function in
			 * the interface typedef.
			 */
			for (int a = 0; a < sdef.functions.size(); a++) {
				Function sfunc = sdef.functions.get(a);
				if (Function.signatureMatch(signature, sfunc, false, false))
					f0 = sfunc;
			}
			
			/* Make sure the function was found */
			assert f0 != null : "Failed to locate function '" + f.path.build() + "'!";
			
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
				
				table.add(add);
			}
			/*
			 * Mapping was found, append to target to create final label and branch to it.
			 */
			else {
				wasUsed = true;
				
				target += post;
				
				/* Final label to function with proviso postfix */
				ASMLabel functionLabel = new ASMLabel(target);
				
				ASMBranch b = new ASMBranch(BRANCH_TYPE.B, new LabelOp(functionLabel));
				b.optFlags.add(OPT_FLAG.SYS_JMP);
				table.add(b);
			}
		}
		
		if (!wasUsed) table.clear();
		return table;
	}
	
	public static List<ASMInstruction> createStructInterfaceRelay(AsNNode node, StructTypedef sdef, StructProvisoMapping mapping) {
		
		List<ASMInstruction> table = new ArrayList();
		
		String postfix = LabelUtil.getProvisoPostfix(mapping.providedHeadProvisos);
		String name = sdef.path.build() + postfix + "_resolver";
		
		ASMLabel relayTableHead = new ASMLabel(name);
		relayTableHead.optFlags.add(OPT_FLAG.LABEL_USED);
		relayTableHead.comment = new ASMComment("Relay: " + sdef.path.build() + " -> INTF");
		table.add(relayTableHead);
		
		for (int i = 0; i < sdef.implemented.size(); i++) {
			INTERFACE intf = sdef.implemented.get(i);
			
			List<String> added = new ArrayList();
			
			for (InterfaceProvisoMapping imapping : intf.getTypedef().registeredMappings) {
				postfix = LabelUtil.getProvisoPostfix(mapping.providedHeadProvisos);
				
				if (!added.contains(postfix)) {
					added.add(postfix);
					
					table.addAll(intf.getTypedef().loadIIDInReg(REG.R12, imapping.providedHeadProvisos));
					table.add(new ASMCmp(new RegOp(REG.R10), new RegOp(REG.R12)));
					table.add(new ASMBranch(BRANCH_TYPE.B, new Cond(COND.EQ), new LabelOp(mapping.resolverLabel)));
				}
			}
		}
		
		return table;
	}
	
} 
