package Imm.AsN.Expression;

import CGen.MemoryMap;
import CGen.RegSet;
import CGen.StackSet;
import Exc.CGEN_EXC;
import Exc.SNIPS_EXC;
import Imm.ASM.Memory.ASMLdr;
import Imm.ASM.Memory.ASMLdrLabel;
import Imm.ASM.Memory.Stack.ASMPopStack;
import Imm.ASM.Memory.Stack.ASMPushStack;
import Imm.ASM.Processing.Arith.ASMAdd;
import Imm.ASM.Processing.Arith.ASMLsl;
import Imm.ASM.Processing.Arith.ASMMov;
import Imm.ASM.Processing.Arith.ASMSub;
import Imm.ASM.Structural.ASMComment;
import Imm.ASM.Structural.Label.ASMDataLabel;
import Imm.ASM.Util.Operands.ImmOp;
import Imm.ASM.Util.Operands.LabelOp;
import Imm.ASM.Util.Operands.PatchableImmOp;
import Imm.ASM.Util.Operands.PatchableImmOp.PATCH_DIR;
import Imm.ASM.Util.Operands.RegOp;
import Imm.ASM.Util.Operands.RegOp.REG;
import Imm.AST.Expression.ArraySelect;
import Imm.AST.Expression.Expression;
import Imm.AST.Expression.IDRef;
import Imm.AST.Expression.InlineCall;
import Imm.AST.Expression.StructSelect;
import Imm.AST.Expression.TypeCast;
import Imm.AsN.AsNNode;
import Imm.AsN.Statement.AsNFunctionCall;
import Imm.TYPE.TYPE;
import Imm.TYPE.COMPOSIT.ARRAY;
import Imm.TYPE.COMPOSIT.POINTER;
import Imm.TYPE.COMPOSIT.STRUCT;
import Res.Const;
import Snips.CompilerDriver;

public class AsNStructSelect extends AsNExpression {
	
			/* --- METHODS --- */
	public static AsNStructSelect cast(StructSelect s, RegSet r, MemoryMap map, StackSet st) throws CGEN_EXC {
		AsNStructSelect sel = new AsNStructSelect();
		s.castedNode = sel;
		
		if (s.selection instanceof InlineCall) {
			/* Struct Select is nested struct call, simply extract called function and call it */
			InlineCall ic = (InlineCall) s.selection;
			AsNFunctionCall.call(ic.calledFunction, ic.anonTarget, ic.proviso, ic.parameters, ic, sel, r, map, st);
		}
		else {
			/* Create a address loader that points to the first word of the target */
			boolean directLoad = injectAddressLoader(sel, s, r, map, st, false);
			
			if (!directLoad) {
				/* Copy result on the stack, push dummy values on stack set */
				if (s.getType() instanceof STRUCT || s.getType() instanceof ARRAY) {
					/* Copy memory section */
					AsNArraySelect.subStructureCopy(sel, s.getType().wordsize());
					
					/* Create dummy stack entries for newly copied struct on stack */
					for (int i = 0; i < s.getType().wordsize(); i++) st.push(REG.R0);
				}
				else {
					/* Load in register */
					ASMLdr load = new ASMLdr(new RegOp(REG.R0), new RegOp(REG.R1));
					load.comment = new ASMComment("Load field from struct");
					sel.instructions.add(load);
				}
			}
		}
		
		return sel;
	}
	
	/**
	 * Loads the address of the target of the selection into R1.
	 */
	public static boolean injectAddressLoader(AsNNode node, StructSelect select, RegSet r, MemoryMap map, StackSet st, boolean addressLoader) throws CGEN_EXC {
		
		Expression base = select.selector;
		if (base instanceof TypeCast) {
			base = ((TypeCast) base).expression;
		}
		
		/* Load base address */
		if (base instanceof IDRef) {
			IDRef ref = (IDRef) base;
			
			/**
			 * When a pointer is the base, and the pointer is in the stack, we have a 'remote'
			 * address. This means that our current address points to somewhere in the stack
			 * where the pointer is located. The value of this pointer is pointing to the structure
			 * we are actually interested in. So, we need to load from the pointer location.
			 * 
			 * This is done by setting this flag to true. It overrides the addressLoader flag, since
			 * they are having the same goal, which is loading the location of the substructure.
			 */
			boolean isInStack = false;
			
			if (r.declarationLoaded(ref.origin)) {
				int loc = r.declarationRegLocation(ref.origin);
				
				/* Struct is one word large and stored in register, this means that the only value requested
				 * can be the one in the location. So just move the value into R0 and return true that the
				 * value was directley loaded. */
				if (ref.getType() instanceof STRUCT) {
					node.instructions.add(new ASMMov(new RegOp(REG.R0), new RegOp(loc)));
					return true;
				}
				/* IDRef must point to a pointer, so move the pointer value into R1 */
				else node.instructions.add(new ASMMov(new RegOp(REG.R1), new RegOp(loc)));
			}
			else if (st.getDeclarationInStackByteOffset(ref.origin) != -1) {
				/* In Local Stack */
				int offset = st.getDeclarationInStackByteOffset(ref.origin);
				offset += (ref.origin.getType().wordsize() - 1) * 4;
				
				/* Load offset of array in memory */
				node.instructions.add(new ASMSub(new RegOp(REG.R1), new RegOp(REG.FP), new ImmOp(offset)));
				
				isInStack = true;
			}
			else if (st.getParameterByteOffset(ref.origin) != -1) {
				/* In Parameter Stack */
				int offset = st.getParameterByteOffset(ref.origin);
				
				ASMAdd start = new ASMAdd(new RegOp(REG.R1), new RegOp(REG.FP), new PatchableImmOp(PATCH_DIR.UP, offset));
				start.comment = new ASMComment("Start of structure in stack");
				node.instructions.add(start);
				
				isInStack = true;
			}
			else if (map.declarationLoaded(ref.origin)) {
				/* In Global Memory */
				ASMDataLabel label = map.resolve(ref.origin);
				
				/* Load data label */
				node.instructions.add(new ASMLdrLabel(new RegOp(REG.R1), new LabelOp(label), ref.origin));
			}
			
			/*
			 * When loading the address of a pointer substructure, we are interested in the address 
			 * of the substructure. If the IDRef is a pointer, with the code above, we just loaded the location
			 * of the pointer in the stack. We need to load the value from the stack to recieve the 
			 * base address of the structure. The offsets are added in the following code.
			 * 
			 * The addressLoader flag is overridden by the isInStack flag.
			 */
			if ((addressLoader || isInStack) && ref.getType() instanceof POINTER) 
				node.instructions.add(new ASMLdr(new RegOp(REG.R1), new RegOp(REG.R1)));
		}
		else if (base instanceof ArraySelect) {
			ArraySelect arr = (ArraySelect) base;
			
			/*
			 * This case can only happen if the object that is selected from is a heaped array.
			 * In this case we can just load the pointer from a register.
			 */
			if (r.declarationLoaded(arr.idRef.origin)) {
				int loc = r.declarationRegLocation(arr.idRef.origin);
				
				/* Just move in R1 */
				node.instructions.add(new ASMMov(new RegOp(REG.R1), new RegOp(loc)));
			}
			/* In Local Stack */
			else if (st.getDeclarationInStackByteOffset(arr.idRef.origin) != -1) {
				int offset = st.getDeclarationInStackByteOffset(arr.idRef.origin);
				offset += (arr.idRef.origin.getType().wordsize() - 1) * 4;
				
				/* Load offset of array in memory */
				node.instructions.add(new ASMSub(new RegOp(REG.R1), new RegOp(REG.FP), new ImmOp(offset)));
			}
			/* In Parameter Stack */
			else if (st.getParameterByteOffset(arr.idRef.origin) != -1) {
				int offset = st.getParameterByteOffset(arr.idRef.origin);
				
				ASMAdd start = new ASMAdd(new RegOp(REG.R1), new RegOp(REG.FP), new PatchableImmOp(PATCH_DIR.UP, offset));
				start.comment = new ASMComment("Start of structure in stack");
				node.instructions.add(start);
			}
			/* In Global Memory */
			else if (map.declarationLoaded(arr.idRef.origin)) {
				ASMDataLabel label = map.resolve(arr.idRef.origin);
				
				/* Load data label */
				node.instructions.add(new ASMLdrLabel(new RegOp(REG.R1), new LabelOp(label), arr.idRef.origin));
			}
			
			/* Already convert to bytes here, since loadSumR2 loads the offset in bytes */
			if (select.deref) {
				ASMLsl lsl = new ASMLsl(new RegOp(REG.R1), new RegOp(REG.R1), new ImmOp(2));
				lsl.comment = new ASMComment("Convert to bytes");
				node.instructions.add(lsl);
			}
			
			/* Push current */
			node.instructions.add(new ASMPushStack(new RegOp(REG.R1)));
			
			/* Load the struture offset in R2 */
			if (arr.getType().wordsize() > 1)
				AsNArraySelect.loadSumR2(node, arr, r, map, st, true);
			else AsNArraySelect.loadSumR2(node, arr, r, map, st, false);
			
			/* Pop Current */
			node.instructions.add(new ASMPopStack(new RegOp(REG.R1)));
			
			/* Add sum to current */
			node.instructions.add(new ASMAdd(new RegOp(REG.R1), new RegOp(REG.R1), new RegOp(REG.R2)));
		}
		else throw new SNIPS_EXC(Const.OPERATION_NOT_IMPLEMENTED);
		
		/* 
		 * Only convert to bytes if select does deref and selector is not a array select since array select will
		 * deref by itself. 
		 */
		if (select.deref && !(base instanceof ArraySelect)) {
			ASMLsl lsl = new ASMLsl(new RegOp(REG.R1), new RegOp(REG.R1), new ImmOp(2));
			lsl.comment = new ASMComment("Convert to bytes");
			node.instructions.add(lsl);
		}
		
		if (!node.instructions.isEmpty())
			node.instructions.get(0).comment = new ASMComment("Load field location");
		
		/* Base address is now in R1 */
		
		/* Base Type */
		StructSelect sel0 = select;
		
		while (true) {
			/* Current selector type */
			TYPE type = sel0.selector.getType();
			
			/* Current selection */
			Expression selection = sel0.selection;
			
			/* Unwrap pointer */
			if (type instanceof POINTER && sel0.deref) {
				POINTER p0 = (POINTER) type;
				type = p0.targetType;
			}
			
			if (type instanceof STRUCT) {
				/* Cast to struct type */
				STRUCT struct = (STRUCT) type;
				
				if (selection instanceof StructSelect) {
					StructSelect sel1 = (StructSelect) selection;
					
					if (sel1.selector instanceof IDRef) {
						injectIDRef(node, struct, (IDRef) sel1.selector);
					}
					else if (sel1.selector instanceof ArraySelect) {
						if (!CompilerDriver.disableStructSIDHeaders) {
							/* Add offset for header */
							node.instructions.add(new ASMAdd(new RegOp(REG.R1), new RegOp(REG.R1), new ImmOp(4)));
						}
						
						injectArraySelect(node, (ArraySelect) sel1.selector, r, map, st);
					}
					else throw new SNIPS_EXC(Const.OPERATION_NOT_IMPLEMENTED);
				}
				/* Base Case */
				else if (selection instanceof IDRef) {
					IDRef ref = (IDRef) selection;
					injectIDRef(node, struct, (IDRef) ref);
				}
				else if (selection instanceof ArraySelect) {
					ArraySelect arrSel = (ArraySelect) selection;
					
					int offset = struct.getFieldByteOffset(arrSel.idRef.path);
					node.instructions.add(new ASMAdd(new RegOp(REG.R1), new RegOp(REG.R1), new ImmOp(offset)));
					
					injectArraySelect(node, arrSel, r, map, st);
				}
				else throw new SNIPS_EXC(Const.OPERATION_NOT_IMPLEMENTED);
			}
			
			/* If current selection derefs and its not the last selection in the chain */
			if (sel0.selection instanceof StructSelect) {
				StructSelect sel1 = (StructSelect) sel0.selection;
				if (sel1.deref) {
					/* Deref, just load current address */
					node.instructions.add(new ASMLdr(new RegOp(REG.R1), new RegOp(REG.R1)));
					node.instructions.add(new ASMLsl(new RegOp(REG.R1), new RegOp(REG.R1), new ImmOp(2)));
				}
			}
			
			/* Keep selecting */
			if (selection instanceof StructSelect) 
				sel0 = (StructSelect) sel0.selection;
			
			/* Base Case reached */
			else break;
		}
		
		return false;
	}
	
	private static void injectArraySelect(AsNNode node, ArraySelect arr, RegSet r, MemoryMap map, StackSet st) throws CGEN_EXC {
		/* Push current on stack */
		node.instructions.add(new ASMPushStack(new RegOp(REG.R1)));
		
		/* Load the struture offset in R2 */
		if (arr.getType().wordsize() > 1)
			AsNArraySelect.loadSumR2(node, arr, r, map, st, true);
		else AsNArraySelect.loadSumR2(node, arr, r, map, st, false);
		
		node.instructions.add(new ASMPopStack(new RegOp(REG.R1)));
		
		/* Add sum to current */
		node.instructions.add(new ASMAdd(new RegOp(REG.R1), new RegOp(REG.R1), new RegOp(REG.R2)));
	}
	
	private static void injectIDRef(AsNNode node, STRUCT struct, IDRef ref) {
		int offset = struct.getFieldByteOffset(ref.path);
		if (offset != 0) node.instructions.add(new ASMAdd(new RegOp(REG.R1), new RegOp(REG.R1), new ImmOp(offset)));
	}
	
} 
