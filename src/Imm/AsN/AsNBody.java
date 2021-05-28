package Imm.AsN;

import CGen.MemoryMap;
import CGen.RegSet;
import CGen.StackSet;
import CGen.Util.LabelUtil;
import CGen.Util.LiteralUtil;
import CGen.Util.StackUtil;
import Ctx.Util.ProvisoUtil;
import Exc.CGEN_EXC;
import Exc.CTEX_EXC;
import Imm.ASM.ASMInstruction;
import Imm.ASM.ASMInstruction.OPT_FLAG;
import Imm.ASM.Branch.ASMBranch;
import Imm.ASM.Branch.ASMBranch.BRANCH_TYPE;
import Imm.ASM.Memory.ASMLdrLabel;
import Imm.ASM.Memory.ASMStr;
import Imm.ASM.Memory.Stack.ASMPopStack;
import Imm.ASM.Memory.Stack.ASMPushStack;
import Imm.ASM.Processing.Arith.ASMAdd;
import Imm.ASM.Processing.Arith.ASMMov;
import Imm.ASM.Structural.ASMComment;
import Imm.ASM.Structural.ASMSectionAnnotation.SECTION;
import Imm.ASM.Structural.ASMSeperator;
import Imm.ASM.Structural.Label.ASMDataLabel;
import Imm.ASM.Structural.Label.ASMLabel;
import Imm.ASM.Util.Operands.ImmOp;
import Imm.ASM.Util.Operands.LabelOp;
import Imm.ASM.Util.Operands.Memory.MemoryOperand;
import Imm.ASM.Util.Operands.Memory.MemoryWordOp;
import Imm.ASM.Util.Operands.Memory.MemoryWordRefOp;
import Imm.ASM.Util.Operands.RegOp;
import Imm.ASM.Util.REG;
import Imm.AST.Expression.ArrayInit;
import Imm.AST.Expression.Atom;
import Imm.AST.Function;
import Imm.AST.Program;
import Imm.AST.Statement.Comment;
import Imm.AST.Statement.Declaration;
import Imm.AST.SyntaxElement;
import Imm.AST.Typedef.InterfaceTypedef;
import Imm.AST.Typedef.InterfaceTypedef.InterfaceProvisoMapping;
import Imm.AST.Typedef.StructTypedef;
import Imm.AST.Typedef.StructTypedef.StructProvisoMapping;
import Imm.AsN.Expression.AsNExpression;
import Imm.AsN.Expression.AsNIDRef;
import Imm.AsN.Statement.AsNComment;
import Imm.AsN.Typedef.AsNInterfaceTypedef;
import Imm.TYPE.COMPOSIT.ARRAY;
import Imm.TYPE.PRIMITIVES.CHAR;
import Imm.TYPE.TYPE;
import PreP.PreProcessor;
import Res.Manager.FileUtil;
import Res.Manager.RessourceManager;
import Res.Manager.TranslationUnit;
import Snips.CompilerDriver;
import Util.Logging.ProgressMessage;
import Util.MODIFIER;
import Util.Source;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map.Entry;

public class AsNBody extends AsNNode {

	public static ProgressMessage progress;
	
	/* Label to the stack copy routine */
	public static ASMLabel stackCopyRoutine;
	
	/* Set to true to signal that a part of the program used the routine */
	public static boolean usedStackCopyRoutine = false;
	
	public static LiteralUtil literalManager;
	
	public static HashMap<String, TranslationUnit> translationUnits = new HashMap();
	
	public TranslationUnit originUnit;

	private static MemoryMap map;
	
	private static String originPath;
	
	
			/* ---< METHODS >--- */
	public static AsNBody cast(Program p, ProgressMessage progress) throws CGEN_EXC, CTEX_EXC {
		AsNBody.usedStackCopyRoutine = false;
		AsNBody.literalManager = new LiteralUtil();
		
		AsNBody body = new AsNBody();
		body.pushOnCreatorStack(p);
		p.castedNode = body;
		AsNBody.progress = progress;
		
		stackCopyRoutine = new ASMLabel("_routine_stack_copy_");
		
		originPath = RessourceManager.instance.toASMPath(CompilerDriver.inputFile.getPath());
		body.originUnit = new TranslationUnit(originPath);
		body.originUnit.versionID = FileUtil.computeHashSum(CompilerDriver.inputFile.getPath());
		AsNBody.translationUnits.put(body.originUnit.sourceFile, body.originUnit);
		
		map = new MemoryMap();

		int done = 0;
		
		/* Create new stack set and reg set that can be used during global variable init. */
		StackSet st = new StackSet();
		RegSet r = new RegSet();
		
		/* Instructions generated for global value init */
		boolean addGlobalInit = false;
		List<ASMInstruction> globalsInit = new ArrayList();
		globalsInit.add(new ASMComment("Initialize the global variables"));
		ASMLabel initLabel = new ASMLabel("main_init");
		globalsInit.add(initLabel);
		globalsInit.add(new ASMPushStack(new RegOp(REG.R0), new RegOp(REG.R1), new RegOp(REG.R2), new RegOp(REG.FP), new RegOp(REG.LR)));
		
		
		/* ---< INSERT NULL POINTER DATALABEL >--- */
		AsNBody.createGlobalDataLabelForDeclaration(body, CompilerDriver.HEAP_START);
		
		
				/* ---< INSERT NULL POINTER DATALABEL >--- */
		AsNBody.createGlobalDataLabelForDeclaration(body, CompilerDriver.NULL_PTR);
		
		
		for (int i = 0; i < p.programElements.size(); i++) {
			SyntaxElement s = p.programElements.get(i);
			if (s instanceof Declaration dec) {
				/* Inject instruction for .data Section */
				ASMDataLabel entry = new ASMDataLabel(dec.path.build(), new MemoryWordOp(dec.value));
				
				List<ASMInstruction> dataEntry = Collections.singletonList(entry);
				AsNBody.addToTranslationUnit(dataEntry, s.getSource(), SECTION.DATA);
				
				/* Create address reference instruction for .text section */
				ASMDataLabel reference = new ASMDataLabel(dec.path.build(), new MemoryWordRefOp(entry));

				/* Add declaration to global memory */
				map.add(dec, reference);
				
				done++;
				progress.incProgress((double) done / p.programElements.size());
			}
		}
		
		/* Branch to main Function if main function is not first function, patch target later */
		ASMBranch branch = new ASMBranch(BRANCH_TYPE.B, new LabelOp());
		body.originUnit.append(branch, SECTION.TEXT);
		
		ASMLabel mainLabel = null;
		
		/* Cast program elements */
		for (SyntaxElement s : p.programElements) {
			if (s instanceof Function f) {
				/* Cast a single function */

				st = new StackSet();
				AsNFunction func = AsNFunction.cast(f, map);
				
				/* Ensure that stack was emptied, so no stack shift at compile time occurred */
				assert st.getStack().isEmpty() : "Stack was not empty after casting function!";
				
				if (!func.getInstructions().isEmpty()) {
					/* Patch Branch to Main Function */
					if (((Function) s).path.build().equals("main")) {
						((LabelOp) branch.target).patch((ASMLabel) func.getInstructions().get(1));
						mainLabel = (ASMLabel) func.getInstructions().get(1);
					}
				}
				
				AsNBody.addToTranslationUnit(func.getInstructions(), s.getSource(), SECTION.TEXT);
			}
			else if (s instanceof StructTypedef def) {
				/* Cast all functions defined in the struct typedef */

				List<String> added = new ArrayList();
				
				List<ASMInstruction> dataBlock = new ArrayList();
				
				for (int a = 0; a < def.registeredMappings.size(); a++) {
					StructProvisoMapping mapping = def.registeredMappings.get(a);
					
					List<TYPE> headMapped = ProvisoUtil.mapToHead(def.proviso, mapping.getProvidedProvisos());
					
					String postfix = LabelUtil.getProvisoPostfix(mapping.getProvidedProvisos());
					
					if (!added.contains(postfix)) {
						added.add(postfix);
						
						/* Inject instruction for .data Section */
						MemoryOperand parent = new MemoryWordOp(0);
						if (def.extension != null) {
							
							List<TYPE> extTypes = new ArrayList();
							for (TYPE t : def.extProviso) {
								TYPE t0 = t.clone();
								ProvisoUtil.mapNTo1(t0, headMapped);
								extTypes.add(t0);
							}
							
							String extPostfix = LabelUtil.getProvisoPostfix(extTypes);
							ASMDataLabel entry = new ASMDataLabel(def.extension.path + extPostfix, new MemoryWordOp(0));
							parent = new MemoryWordRefOp(entry);
						}
						
						ASMDataLabel entry = new ASMDataLabel(def.path + postfix, parent);
						dataBlock.add(entry);
						def.SIDLabelMap.put(postfix, entry);

						/* Add label for VTable relay */
						MemoryOperand tableTarget = new MemoryWordRefOp(new ASMDataLabel(def.path + "_vtable" + postfix, new MemoryWordOp(0)));
						if (def.functions.isEmpty()) tableTarget = new MemoryWordOp(0);
						ASMDataLabel vTableEntry = new ASMDataLabel(def.path + "_vrelay" + postfix, tableTarget);
						dataBlock.add(vTableEntry);

						/* Build v-table */
						if (!def.functions.isEmpty()) {
							List<ASMInstruction> vTable = new ArrayList<>();

							ASMLabel vTableHead = new ASMLabel(def.path + "_vtable" + postfix);
							vTableHead.comment = new ASMComment("VTable for " + def.path + ((!mapping.getProvidedProvisos().isEmpty()) ? " proviso " + postfix : ""));
							vTableHead.optFlags.add(OPT_FLAG.LABEL_USED);
							vTable.add(vTableHead);

							vTable.add(new ASMMov(new RegOp(REG.R12), new ImmOp(0)));
							vTable.add(new ASMAdd(new RegOp(REG.PC), new RegOp(REG.PC), new RegOp(REG.R10)));

							for (Function f : def.functions) {

								/* Skip static functions, not associated with struct instance */
								if (f.modifier == MODIFIER.STATIC) continue;

								if (f.provisoTypes.size() == mapping.getProvidedProvisos().size() && f.containsMapping(mapping.getProvidedProvisos())) {
									ASMBranch fBranch;

									/* Inherited Function */
									if (f.body == null)
										fBranch = new ASMBranch(BRANCH_TYPE.B, new LabelOp(new ASMLabel(f.buildInheritedCallLabel(mapping.getProvidedProvisos()))));
									else
										fBranch = new ASMBranch(BRANCH_TYPE.B, new LabelOp(new ASMLabel(f.buildCallLabel(mapping.getProvidedProvisos()))));

									fBranch.optFlags.add(OPT_FLAG.SYS_JMP);
									vTable.add(fBranch);
								}
								else {
									ASMAdd placeholder = new ASMAdd(new RegOp(REG.R10), new RegOp(REG.R10), new RegOp(REG.R10));
									placeholder.comment = new ASMComment("Function '" + f.path + "' not called");
									vTable.add(placeholder);
								}
							}

							AsNBody.addToTranslationUnit(vTable, s.getSource(), SECTION.TEXT);
						}

						if (!def.implemented.isEmpty()) {
							String name = def.path + postfix + "_resolver";
							
							MemoryWordRefOp tableRef = new MemoryWordRefOp(new ASMDataLabel(name, new MemoryWordOp(0)));
							ASMDataLabel relay = new ASMDataLabel(name + "_relay", tableRef);
							dataBlock.add(relay);
						}
					}
				}
				
				for (Function f : def.functions) {
					st = new StackSet();
					AsNFunction func = AsNFunction.cast(f, map);
					
					/* Ensure that stack was emptied, so no stack shift at compile time occurred */
					assert st.getStack().isEmpty() : "Stack was not empty after casting function!";
					
					if (!func.getInstructions().isEmpty()) {
						/* Patch Branch to Main Function */
						if (f.path.build().equals("main")) {
							((LabelOp) branch.target).patch((ASMLabel) func.getInstructions().get(0));
							mainLabel = (ASMLabel) func.getInstructions().get(0);
						}
					}
					
					AsNBody.addToTranslationUnit(func.getInstructions(), s.getSource(), SECTION.TEXT);
				}
				
				AsNBody.addToTranslationUnit(dataBlock, def.getSource(), SECTION.DATA);
				
				added.clear();
				for (int a = 0; a < def.registeredMappings.size(); a++) {
					StructProvisoMapping mapping = def.registeredMappings.get(a);
					
					String postfix = LabelUtil.getProvisoPostfix(mapping.getProvidedProvisos());
					
					if (!added.contains(postfix)) {
						added.add(postfix);
						
						/* Inject Interface Relay-Addresses for .data section */
						for (int i = def.implemented.size() - 1; i >= 0; i--) {
							List<ASMInstruction> relayTable = AsNInterfaceTypedef.createStructFunctionRelay(def, mapping, def.implemented.get(i));
							if (!relayTable.isEmpty())
								AsNBody.addToTranslationUnit(relayTable, def.getSource(), SECTION.TEXT);
						}
						
						if (!def.implemented.isEmpty()) {
							List<ASMInstruction> relayTable = AsNInterfaceTypedef.createStructInterfaceRelay(def, mapping);
							AsNBody.addToTranslationUnit(relayTable, def.getSource(), SECTION.TEXT);
						}
					}
				}
			}
			else if (s instanceof InterfaceTypedef idef) {
				for (InterfaceProvisoMapping mapping : idef.registeredMappings) {
					String postfix = LabelUtil.getProvisoPostfix(mapping.providedHeadProvisos);
					
					if (!idef.IIDLabelMap.containsKey(postfix)) {
						
						/* Inject instruction for .data Section */
						MemoryOperand parent = new MemoryWordOp(0);
						
						ASMDataLabel entry = new ASMDataLabel(idef.path + postfix, parent);
						AsNBody.addToTranslationUnit(entry, idef.getSource(), SECTION.DATA);
						
						idef.IIDLabelMap.put(postfix, entry);
					}
				}
			}
			else if (s instanceof Comment) {
				AsNComment com = AsNComment.cast((Comment) s, null, map, null);
				AsNBody.addToTranslationUnit(com.getInstructions(), s.getSource(), SECTION.TEXT);
			}
			
			done++;
			progress.incProgress((double) done / p.programElements.size());
		}
		
		for (int i = 0; i < p.programElements.size(); i++) {
			SyntaxElement s = p.programElements.get(i);
			if (s instanceof Declaration dec) {

				boolean directInitialization = dec.value instanceof Atom;
				
				/* .asciz initialization */
				if (dec.value instanceof ArrayInit init && ((ARRAY) dec.value.getType()).elementType instanceof CHAR) {
					if (init.elements.stream().allMatch(x -> x instanceof Atom)) {
						directInitialization = true;
					}
				}
				
				/* Has value, cast assembly into globalsInit */
				if (dec.value != null && !directInitialization) {
					addGlobalInit = true;
					
					LabelUtil.funcPrefix = "main.init";
					
					/* Cast Expression before main call */
					globalsInit.addAll(AsNExpression.cast(dec.value, r, map, st).getInstructions());
					
					ASMDataLabel label = map.resolve(dec);
					
					/* Load memory address */
					ASMLdrLabel ins = new ASMLdrLabel(new RegOp(REG.R1), new LabelOp(label), dec);
					ins.comment = new ASMComment("Load from .data section");
					globalsInit.add(ins);
					
					if (dec.value.getType().wordsize() == 1) {
						globalsInit.add(new ASMStr(new RegOp(REG.R0), new RegOp(REG.R1)));
					}
					else {
						/* Quick and dirty way to relay the generated instructions into a AsNNode */
						AsNIDRef ref = new AsNIDRef();
						StackUtil.copyToAddressFromStack(dec.getType().wordsize(), ref, st);
						globalsInit.addAll(ref.instructions);
					}
				}
			}
		}
		
		/* Add global initialization instruction */
		if (addGlobalInit) {
			boolean hasCall = false;
			for (ASMInstruction asmInstruction : globalsInit) {
				if (asmInstruction instanceof ASMBranch && ((ASMBranch) asmInstruction).type == BRANCH_TYPE.BL) {
					hasCall = true;
				} else if (asmInstruction instanceof ASMMov && ((ASMMov) asmInstruction).target.reg == REG.PC) {
					hasCall = true;
				}
			}
			
			if (!hasCall) {
				ASMPushStack push = (ASMPushStack) globalsInit.get(2);
				push.operands.remove(push.operands.size() - 1);
				push.operands.remove(push.operands.size() - 1);
				
				globalsInit.add(new ASMPopStack(new RegOp(REG.R0), new RegOp(REG.R1), new RegOp(REG.R2)));
			}
			else globalsInit.add(new ASMPopStack(new RegOp(REG.R0), new RegOp(REG.R1), new RegOp(REG.R2), new RegOp(REG.FP), new RegOp(REG.LR)));
			
			/* Add seperator before init block to other functions */
			globalsInit.add(0, new ASMSeperator());
			
			List<ASMInstruction> text = body.originUnit.textSection;
			
			/* Inject global init before main function, relay main jump at start to new block */
			for (int i = 0; i < text.size(); i++) {
				if (text.get(i).equals(mainLabel)) {
					/* Remove seperator before main function */
					if (text.get(i) instanceof ASMSeperator) text.remove(i);
					
					/* Inject global init code */
					text.addAll(i, globalsInit);
					
					/* Relay start branch to init block */
					((LabelOp) branch.target).patch(initLabel);
					
					break;
				}
			}
		}
		
		/* --- Inject Stack Copy Routine --- */
		if (AsNBody.usedStackCopyRoutine || !CompilerDriver.buildModulesRecurse) {
			List<ASMInstruction> routine = StackUtil.buildStackCopyRoutine();
			body.originUnit.append(routine, SECTION.TEXT);
		}
		
		/* Add all system libraries that were used */
		for (Entry<String, TranslationUnit> entry : AsNBody.translationUnits.entrySet()) {
	
			TranslationUnit unit = entry.getValue();
			
			/* Imports made by the source of this translation unit */
			List<String> imports = PreProcessor.importsPerFile.get(entry.getValue().sourceFile);
			
			/* Add imports to translation unit */
			if (imports != null) {
				for (String imp : imports) {
					if (!unit.imports.contains(imp)) {
						unit.imports.add(imp);
					}
				}
			}
			
			if (unit.sourceFile.equals(AsNBody.originPath)) {
				List<String> referenced = new ArrayList();
				
				referenced.add("maybe free.s");
				referenced.add("maybe hsize.s");
				referenced.add("maybe init.s");
				referenced.add("maybe isa.s");
				referenced.add("maybe resv.s");
				
				referenced.add("maybe __op_mod.s");
				referenced.add("maybe __op_div.s");
				
				for (String imp : referenced) {
					if (!unit.imports.contains(imp)) {
						
						/* Map path to dynamic imports, check if import was used */
						String iPath = imp.split(" ") [1];
						iPath = iPath.substring(0, iPath.length() - 1) + "sn";
						iPath = RessourceManager.instance.resolve(iPath);
						
						if (!CompilerDriver.buildModulesRecurse || CompilerDriver.driver.referencedLibaries.contains(iPath))
							unit.imports.add(imp);
					}
				}
			}
		}
		
		/* Build the literal pool labels for all created translation units */
		for (Entry<String, TranslationUnit> entry : AsNBody.translationUnits.entrySet()) 
			body.buildLiteralPools(entry.getValue().textSection, map, entry.getValue().sourceFile);
		
		progress.incProgress(1);
		body.registerMetric();
		return body;
	}
	
	private void buildLiteralPools(List<ASMInstruction> text, MemoryMap map, String fileName) {
		/* Manage and create Literal Pools */
		List<ASMLdrLabel> buffer = new ArrayList();
		
		for (int i = 0; i < text.size(); i++) {
			
			/* Collect ASMLdrLabel instructions in buffer, insert them after next bx instruction */
			if (text.get(i) instanceof ASMLdrLabel load) {
				buffer.add(load);
			}
			else {
				ASMInstruction ins = text.get(i);
				
				/* 
				 * Flush buffer either at BX branch or at instruction with BX_SEMI_EXIT set,
				 * marking the end of a relay table mapping, which has no explicit exit.
				 */
				if ((ins instanceof ASMBranch && ((ASMBranch) ins).type == BRANCH_TYPE.BX) || 
						ins.optFlags.contains(OPT_FLAG.BX_SEMI_EXIT)) {
					
					/* Flush buffer here */
					if (!buffer.isEmpty()) {
						/* Create a new prefix for this literal pool */
						String prefix = LabelUtil.literalPoolPrefix(fileName);
						
						List<String> added = new ArrayList();
						
						for (ASMLdrLabel label : buffer) {
							/* Apply prefix to load label */
							label.prefix = prefix;
							
							ASMDataLabel l0;
							if (label.dec == null) {
								/* Label is dynamic label, generated by literal manager */
								ASMDataLabel dlabel = AsNBody.literalManager.getValue((LabelOp) label.op0);
								
								if (dlabel == null) {
									/* Label is referencing a label in the SID-Symbol table */
									LabelOp op = (LabelOp) label.op0;
									l0 = new ASMDataLabel(op.label.name, new MemoryWordRefOp((ASMDataLabel) op.label));
								}
								else
									l0 = dlabel.clone();
							}
							else 
								/* Get label referenced by load instruction and clone it */
								l0 = map.resolve(label.dec).clone();
							
							/* Apply prefix to label name */
							if (l0.name.startsWith(".")) l0.name = l0.name.substring(1);
							l0.name = prefix + l0.name;
							
							if (added.contains(l0.name)) continue;
							else added.add(l0.name);
							
							/* Inject label clone at target position */
							text.add(i + 1, l0);
						}
						
						buffer.clear();
					}
				}
			}
		}
	}
	
	/**
	 * Adds the generated assembly to the body or inserts an include directive, that
	 * can be processed by the linker.
	 * 
	 * @param ins The generated assembly
	 * @param ext A list of already included files
	 * @param source The source of the ressource that generated the assembly
	 */
	public static void addToTranslationUnit(List<ASMInstruction> ins, Source source, SECTION section) {
		String path = RessourceManager.instance.toASMPath(source.sourceFile);

		TranslationUnit unit;

		if (!AsNBody.translationUnits.containsKey(path)) {
			unit = new TranslationUnit(path);
			unit.versionID = FileUtil.computeHashSum(source.sourceFile);
			AsNBody.translationUnits.put(unit.sourceFile, unit);
		}
		else unit = AsNBody.translationUnits.get(path);

		unit.append(ins, section);
	}
	
	public static void addToTranslationUnit(ASMInstruction ins, Source source, SECTION section) {
		List<ASMInstruction> list = Collections.singletonList(ins);
		AsNBody.addToTranslationUnit(list, source, section);
	}
	
	public static void createGlobalDataLabelForDeclaration(AsNBody body, Declaration dec) {
		/* Create instruction for .data Section */
		ASMDataLabel dataEntryHeap = new ASMDataLabel(dec.path.build(), new MemoryWordOp(dec.value));
		body.originUnit.append(dataEntryHeap, SECTION.DATA);
		
		/* Create address reference instruction for .text section */
		ASMDataLabel heapReference = new ASMDataLabel(dec.path.build(), new MemoryWordRefOp(dataEntryHeap));

		/* Add declaration to global memory */
		map.add(dec, heapReference);
	}
	
	public static void branchToCopyRoutine(AsNNode node) {
		/* Mark routine as used */
		AsNBody.usedStackCopyRoutine = true;
		
		ASMAdd add = new ASMAdd(new RegOp(REG.R10), new RegOp(REG.PC), new ImmOp(8));
		add.comment = new ASMComment("Setup return address for routine");
		node.instructions.add(add);
		
		ASMBranch branch = new ASMBranch(BRANCH_TYPE.B, new LabelOp(AsNBody.stackCopyRoutine));
		
		/* 
		 * Signal the optimizer that this is a controlled jump, and that the program will return, 
		 * but not with jump mechanics, but by manipulating the pc.
		 */
		branch.optFlags.add(OPT_FLAG.SYS_JMP);
		node.instructions.add(branch);
		
		/* Move 0 into R10 */
		ASMMov resetR10 = new ASMMov(new RegOp(REG.R10), new ImmOp(0));
		node.instructions.add(resetR10);
	}
	
} 
