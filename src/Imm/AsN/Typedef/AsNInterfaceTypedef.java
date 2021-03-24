package Imm.AsN.Typedef;

import java.util.ArrayList;
import java.util.List;

import CGen.Util.LabelUtil;
import Ctx.Util.ProvisoUtil;
import Imm.ASM.ASMInstruction;
import Imm.ASM.ASMInstruction.OPT_FLAG;
import Imm.ASM.Branch.ASMBranch;
import Imm.ASM.Branch.ASMBranch.BRANCH_TYPE;
import Imm.ASM.Memory.Stack.ASMPopStack;
import Imm.ASM.Processing.Arith.ASMAdd;
import Imm.ASM.Processing.Arith.ASMMov;
import Imm.ASM.Processing.Logic.ASMCmp;
import Imm.ASM.Structural.ASMComment;
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
import Imm.TYPE.PROVISO;
import Imm.TYPE.TYPE;
import Imm.TYPE.COMPOSIT.INTERFACE;

public class AsNInterfaceTypedef extends AsNNode {

	/**
	 * Creates a relay table for a struct typedef with context and interface tuple.
	 * @param sdef The struct typedef of the table
	 * @param mapping The struct proviso mapping
	 * @param intf The interface that contains the functions that this table relays to
	 * @return The generated table mapping.
	 */
	public static List<ASMInstruction> createStructFunctionRelay(StructTypedef sdef, StructProvisoMapping mapping, INTERFACE intf) {
		
		InterfaceTypedef idef = intf.getTypedef();
		
		List<ASMInstruction> table = new ArrayList();
		
		String postfix = LabelUtil.getProvisoPostfix(mapping.providedHeadProvisos);
		String name = sdef.path.build() + postfix + "_" + idef.path.build();
		
		ASMLabel relayTableHead = new ASMLabel(name);
		relayTableHead.optFlags.add(OPT_FLAG.LABEL_USED);
		relayTableHead.comment = new ASMComment("Relay: " + idef.path.build() + " -> " + sdef.path.build());
		table.add(relayTableHead);
		
		mapping.resolverLabelMap.put(idef, relayTableHead);
		
		table.add(new ASMMov(new RegOp(REG.R10), new ImmOp(0)));
		
		/* Pop function offset */
		table.add(new ASMPopStack(new RegOp(REG.R12)));
		
		table.add(new ASMAdd(new RegOp(REG.R12), new RegOp(REG.R12), new ImmOp(4)));
		table.add(new ASMAdd(new RegOp(REG.PC), new RegOp(REG.PC), new RegOp(REG.R12)));
		
		boolean hasCalls = false;
		
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
			
			/* Get the proviso postfix from the function in the struct with the current mapping */
			String post = f0.getProvisoPostfix(mapping.providedHeadProvisos);
			
			/* 
			 * The mapping has not been registered in the function, this means that this function
			 * has not been called with this specific provisos. We do have to add a placeholder,
			 * so that the offsets of the function order is preserved.
			 */
			if (post == null) {
				ASMAdd add = new ASMAdd(new RegOp(REG.R10), new RegOp(REG.R10), new RegOp(REG.R10));
				add.comment = new ASMComment("Function was not called, use as placeholder");
				add.optFlags.add(OPT_FLAG.IS_PADDING);
				add.optFlags.add(OPT_FLAG.SYS_JMP);
				table.add(add);
			}
			else {
				/* Get function head label from function with given postfix */
				ASMLabel functionLabel = f0.headLabelMap.get(postfix);
				
				/* Create system jump to function */
				ASMBranch b = new ASMBranch(BRANCH_TYPE.B, new LabelOp(functionLabel));
				b.optFlags.add(OPT_FLAG.SYS_JMP);
				table.add(b);
				
				hasCalls = true;
			}
		}
		
		if (!hasCalls) {
			/*
			 * We can clear the table here, but we still need the head label since
			 * it will be referenced by the resolver table.
			 */
			table.clear();
			
			relayTableHead.comment.comment += " (Unused)";
			table.add(relayTableHead);
		}
		
		table.get(table.size() - 1).optFlags.add(OPT_FLAG.BX_SEMI_EXIT);
		return table;
	}
	
	/**
	 * Creates a resolver table for the given struct typedef for a given struct proviso mapping.
	 * @param sdef The struct typedef.
	 * @param mapping The proviso mapping for this table.
	 * @return The generated instructions.
	 */
	public static List<ASMInstruction> createStructInterfaceRelay(StructTypedef sdef, StructProvisoMapping mapping) {
		
		/* Head proviso of struct typedef with context from mapping */
		List<TYPE> sdefProviso = new ArrayList();
		for (int a = 0; a < sdef.proviso.size(); a++) {
			PROVISO headClone = (PROVISO) sdef.proviso.get(a).clone();
			headClone.setContext(mapping.providedHeadProvisos.get(a).clone());
			sdefProviso.add(headClone);
		}
		
		List<ASMInstruction> table = new ArrayList();
		
		String postfix = LabelUtil.getProvisoPostfix(mapping.providedHeadProvisos);
		String name = sdef.path.build() + postfix + "_resolver";
		
		/* Create table head */
		ASMLabel relayTableHead = new ASMLabel(name);
		relayTableHead.optFlags.add(OPT_FLAG.LABEL_USED);
		relayTableHead.comment = new ASMComment("Relay: " + sdef.path.build() + " -> INTF");
		table.add(relayTableHead);
		
		for (int i = 0; i < sdef.implemented.size(); i++) {
			INTERFACE intf = sdef.implemented.get(i);
		
			List<String> added = new ArrayList();
			
			/*
			 * Translate the provisos provided by the struct typedef to the interface
			 * head provisos, essentially re-creating the mapping of the interface.
			 * We can use this mapping to check if the imapping is a valid mapping by
			 * comparing the proviso postfixes.
			 */
			List<TYPE> intfTypes = new ArrayList();
			for (int a = 0; a < intf.proviso.size(); a++) {
				PROVISO intfClone = (PROVISO) intf.proviso.get(a).clone();
				ProvisoUtil.mapNTo1(intfClone, sdefProviso);
				intfTypes.add(intfClone);
			}
			
			postfix = LabelUtil.getProvisoPostfix(intfTypes);
			
			for (InterfaceProvisoMapping imapping : intf.getTypedef().registeredMappings) {
				
				/*
				 * Check if the mapping is valid - this can be done by checking if the 
				 * imapping can be derived from the given struct mapping.
				 */
				boolean isValidMapping = LabelUtil.getProvisoPostfix(imapping.providedHeadProvisos).equals(postfix);
				
				if (!added.contains(postfix) && isValidMapping) {
					added.add(postfix);
					
					table.addAll(intf.getTypedef().loadIIDInReg(REG.R12, intfTypes));
					table.add(new ASMCmp(new RegOp(REG.R10), new RegOp(REG.R12)));
					table.add(new ASMBranch(BRANCH_TYPE.B, new Cond(COND.EQ), new LabelOp(mapping.resolverLabelMap.get(intf.getTypedef()))));
				}
			}
		}
		
		table.get(table.size() - 1).optFlags.add(OPT_FLAG.BX_SEMI_EXIT);
		return table;
	}
	
} 
