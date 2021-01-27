package Imm.AsN;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

import CGen.MemoryMap;
import CGen.RegSet;
import CGen.StackSet;
import CGen.Util.LabelUtil;
import CGen.Util.LiteralUtil;
import CGen.Util.StackUtil;
import Exc.CGEN_EXC;
import Exc.CTX_EXC;
import Imm.ASM.ASMInstruction;
import Imm.ASM.ASMInstruction.OPT_FLAG;
import Imm.ASM.Branch.ASMBranch;
import Imm.ASM.Branch.ASMBranch.BRANCH_TYPE;
import Imm.ASM.Directive.ASMDirective;
import Imm.ASM.Memory.ASMLdr;
import Imm.ASM.Memory.ASMLdrLabel;
import Imm.ASM.Memory.ASMStr;
import Imm.ASM.Memory.Stack.ASMPopStack;
import Imm.ASM.Memory.Stack.ASMPushStack;
import Imm.ASM.Processing.Arith.ASMAdd;
import Imm.ASM.Processing.Arith.ASMMov;
import Imm.ASM.Structural.ASMComment;
import Imm.ASM.Structural.ASMSectionAnnotation;
import Imm.ASM.Structural.ASMSectionAnnotation.SECTION;
import Imm.ASM.Structural.ASMSeperator;
import Imm.ASM.Structural.Label.ASMDataLabel;
import Imm.ASM.Structural.Label.ASMLabel;
import Imm.ASM.Util.Operands.ImmOp;
import Imm.ASM.Util.Operands.LabelOp;
import Imm.ASM.Util.Operands.RegOp;
import Imm.ASM.Util.Operands.RegOp.REG;
import Imm.ASM.Util.Operands.Memory.MemoryWordOp;
import Imm.ASM.Util.Operands.Memory.MemoryWordRefOp;
import Imm.AST.Function;
import Imm.AST.Program;
import Imm.AST.SyntaxElement;
import Imm.AST.Expression.Atom;
import Imm.AST.Statement.Comment;
import Imm.AST.Statement.Declaration;
import Imm.AST.Typedef.InterfaceTypedef;
import Imm.AST.Typedef.StructTypedef;
import Imm.AsN.Expression.AsNExpression;
import Imm.AsN.Expression.AsNIDRef;
import Imm.AsN.Statement.AsNComment;
import Imm.AsN.Typedef.AsNInterfaceTypedef;
import Snips.CompilerDriver;
import Util.Source;
import Util.Logging.ProgressMessage;

public class AsNBody extends AsNNode {

	public static ProgressMessage progress;
	
	/* Label to the stack copy routine */
	public static ASMLabel stackCopyRoutine = new ASMLabel("_routine_stack_copy_");
	
	/* Set to true to signal that a part of the program used the routine */
	public static boolean usedStackCopyRoutine = false;
	
	public static LiteralUtil literalManager;
	
	public static List<ASMInstruction> instructionAppenix = new ArrayList();
	
	public HashMap<String, List<ASMInstruction>> external = new HashMap();
	
	
			/* ---< METHODS >--- */
	public static AsNBody cast(Program p, ProgressMessage progress) throws CGEN_EXC, CTX_EXC {
		AsNBody.usedStackCopyRoutine = false;
		AsNBody.literalManager = new LiteralUtil();
		
		stackCopyRoutine = new ASMLabel("_routine_stack_copy_");
		
		AsNBody body = new AsNBody();
		p.castedNode = body;
		AsNBody.progress = progress;
		
		MemoryMap map = new MemoryMap();
		
		/* File name comment */
		DateFormat df = new SimpleDateFormat("MM/dd/yyyy HH:mm:ss");
		Date today = Calendar.getInstance().getTime();        
		String todayAsString = df.format(today);
		
		body.instructions.add(new ASMComment("--" + CompilerDriver.inputFile.getName() + ((CompilerDriver.includeMetaInformation)? ", Snips Version: " + CompilerDriver.sys_config.getValue("Version") + ", Date: " + todayAsString : "")));
		if (CompilerDriver.includeMetaInformation) body.instructions.add(new ASMComment(" SID-Headers: " + ((!CompilerDriver.disableStructSIDHeaders)? "Enabled" : "Disabled") + 
											 ", Optimizer: " + ((!CompilerDriver.disableOptimizer)? "Enabled" : "Disabled") + 
											 ", Modifiers: " + ((!CompilerDriver.disableModifiers)? "Enabled" : "Disabled")));
		
		/* Create .data section annotation */
		ASMSectionAnnotation data = new ASMSectionAnnotation(SECTION.DATA);
		body.instructions.add(data);
		
		/* Count global variables */
		boolean globals = false;
		List<ASMInstruction> globalVarReferences = new ArrayList();
		
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
		
		for (int i = 0; i < p.programElements.size(); i++) {
			SyntaxElement s = p.programElements.get(i);
			if (s instanceof Declaration) {
				Declaration dec = (Declaration) s;
				
				/* Create instruction for .data Section */
				ASMDataLabel dataEntry = new ASMDataLabel(dec.path.build(), new MemoryWordOp(dec.value));
				body.instructions.add(dataEntry);

				/* Create address reference instruction for .text section */
				ASMDataLabel reference = new ASMDataLabel(LabelUtil.mapToAddressName(dec.path.build()), new MemoryWordRefOp(dataEntry));
				globalVarReferences.add(reference);
				
				/* Add declaration to global memory */
				map.add(dec, reference);
				globals = true;
				
				done++;
				progress.incProgress((double) done / p.programElements.size());
			}
		}
		
		if (CompilerDriver.null_referenced) {
			Declaration nullPtr = CompilerDriver.NULL_PTR;
			
			/* Create instruction for .data Section */
			ASMDataLabel dataEntry = new ASMDataLabel(nullPtr.path.build(), new MemoryWordOp(nullPtr.value));
			body.instructions.add(dataEntry);
			
			/* Create address reference instruction for .text section */
			ASMDataLabel reference = new ASMDataLabel(LabelUtil.mapToAddressName(nullPtr.path.build()), new MemoryWordRefOp(dataEntry));
			globalVarReferences.add(reference);
			
			/* Add declaration to global memory */
			map.add(nullPtr, reference);
		}
		
		if (CompilerDriver.heap_referenced) {
			globals = true;
			
			Declaration heap = CompilerDriver.HEAP_START;
			
			/* Create instruction for .data Section */
			ASMDataLabel dataEntry = new ASMDataLabel(heap.path.build(), new MemoryWordOp(heap.value));
			body.instructions.add(dataEntry);
			
			/* Create address reference instruction for .text section */
			ASMDataLabel reference = new ASMDataLabel(LabelUtil.mapToAddressName(heap.path.build()), new MemoryWordRefOp(dataEntry));
			globalVarReferences.add(reference);
			
			/* Add declaration to global memory */
			map.add(heap, reference);
		}
		
		/* No globals, remove .data annotation */
		if (!globals) body.instructions.remove(data);
		
		
		/* Add .text annotation if other sections exist */
		ASMInstruction TEXT = new ASMSectionAnnotation(SECTION.TEXT);
		if (globals) {
			body.instructions.add(new ASMSeperator());
			body.instructions.add(TEXT);
		}
		
		/* Branch to main Function if main function is not first function, patch target later */
		ASMBranch branch = new ASMBranch(BRANCH_TYPE.B, new LabelOp());
		body.instructions.add(branch);
				
		
		/* --- Inject Stack Copy Routine --- */
		List<ASMInstruction> routine = StackUtil.buildStackCopyRoutine();
		body.instructions.addAll(routine);
	
		ASMLabel mainLabel = null;
		
		/* Include imports that are already contained in the output */
		List<String> ext = new ArrayList();
		
		/* Cast program elements */
		for (SyntaxElement s : p.programElements) {
			if (s instanceof Function) {
				/* Cast a single function */
				
				st = new StackSet();
				AsNFunction func = AsNFunction.cast((Function) s, new RegSet(), map, st);
				
				/* Ensure that stack was emptied, so no stack shift at compile time occurred */
				assert st.getStack().isEmpty() : "Stack was not empty after casting function!";
				
				if (!func.getInstructions().isEmpty()) {
					/* Patch Branch to Main Function */
					if (((Function) s).path.build().equals("main")) {
						((LabelOp) branch.target).patch((ASMLabel) func.getInstructions().get(0));
						mainLabel = (ASMLabel) func.getInstructions().get(0);
					}
				}
				
				body.injectASM(func.getInstructions(), ext, s.getSource(), func.source.path.build(), func.translated.stream().toArray(String []::new));
			}
			else if (s instanceof StructTypedef) {
				/* Cast all functions defined in the struct typedef */
				StructTypedef def = (StructTypedef) s;
				
				for (Function f : def.functions) {
					st = new StackSet();
					AsNFunction func = AsNFunction.cast(f, new RegSet(), map, st);
					
					/* Ensure that stack was emptied, so no stack shift at compile time occurred */
					assert st.getStack().isEmpty() : "Stack was not empty after casting function!";
					
					if (!func.getInstructions().isEmpty()) {
						/* Patch Branch to Main Function */
						if (f.path.build().equals("main")) {
							((LabelOp) branch.target).patch((ASMLabel) func.getInstructions().get(0));
							mainLabel = (ASMLabel) func.getInstructions().get(0);
						}
					}
					
					body.injectASM(func.getInstructions(), ext, s.getSource(), func.source.path.build(), func.translated.stream().toArray(String []::new));
				}
			}
			else if (s instanceof InterfaceTypedef) {
				AsNInterfaceTypedef def = AsNInterfaceTypedef.cast((InterfaceTypedef) s, r, map, st);
				body.injectASM(def.getInstructions(), ext, s.getSource(), "");
			}
			else if (s instanceof Comment) {
				AsNComment com = AsNComment.cast((Comment) s, null, map, null);
				body.injectASM(com.getInstructions(), ext, s.getSource(), "");
			}
			
			done++;
			progress.incProgress((double) done / p.programElements.size());
		}
		
		for (int i = 0; i < p.programElements.size(); i++) {
			SyntaxElement s = p.programElements.get(i);
			if (s instanceof Declaration) {
				Declaration dec = (Declaration) s;
				
				/* Has value, cast assembly into globalsInit */
				if (dec.value != null && !(dec.value instanceof Atom)) {
					addGlobalInit = true;
					
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
			for (int i = 0; i < globalsInit.size(); i++) {
				if (globalsInit.get(i) instanceof ASMBranch && ((ASMBranch) globalsInit.get(i)).type == BRANCH_TYPE.BL) {
					hasCall = true;
				}
				else if (globalsInit.get(i) instanceof ASMMov && ((ASMMov) globalsInit.get(i)).target.reg == REG.PC) {
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
			
			/* Only main function present, dont need branch */
			if (p.programElements.stream().filter(x -> x instanceof Function).count() == 1 && !AsNBody.usedStackCopyRoutine) {
				body.instructions.remove(branch);
			}
			else {
				/* Add seperator before init block to other functions */
				globalsInit.add(0, new ASMSeperator());
			}
			
			/* Inject global init before main function, relay main jump at start to new block */
			for (int i = 0; i < body.instructions.size(); i++) {
				if (body.instructions.get(i).equals(mainLabel)) {
					/* Remove seperator before main function */
					if (body.instructions.get(i - 1) instanceof ASMSeperator) body.instructions.remove(i - 1);
					
					/* Inject global init code */
					body.instructions.addAll(i - 1, globalsInit);
					
					/* Relay start branch to init block */
					((LabelOp) branch.target).label = initLabel;
					break;
				}
			}
		}
		
		/* Routine was not used, remove */
		if (!AsNBody.usedStackCopyRoutine) body.instructions.removeAll(routine);
		
		/* Manage and create Literal Pools */
		List<ASMLdr> buffer = new ArrayList();
		for (int i = 0; i < body.instructions.size(); i++) {
			/* Collect ASMLdrLabel instructions in buffer, insert them after next bx instruction */
			if (body.instructions.get(i) instanceof ASMLdrLabel) {
				ASMLdrLabel load = (ASMLdrLabel) body.instructions.get(i);
				buffer.add(load);
			}
			else if (body.instructions.get(i) instanceof ASMBranch) {
				ASMBranch b = (ASMBranch) body.instructions.get(i);
				
				// TODO: Implement Filter to only flush buffer after 4k of text
				
				if (b.type == BRANCH_TYPE.BX) {
					/* Flush buffer here */
					if (!buffer.isEmpty()) {
						/* Create a new prefix for this literal pool */
						String prefix = LabelUtil.literalPoolPrefix();
						
						List<String> added = new ArrayList();
						
						for (ASMLdr l : buffer) {
							if (l instanceof ASMLdrLabel) {
								ASMLdrLabel label = (ASMLdrLabel) l;
								
								/* Apply prefix to load label */
								label.prefix = prefix;
								
								ASMDataLabel l0 = null;
								if (label.dec == null) 
									/* Label is dynamic label, generated by literal manager */
									l0 = AsNBody.literalManager.getValue((LabelOp) label.op0).clone();
								else 
									/* Get label referenced by load instruction and clone it */
									l0 = map.resolve(label.dec).clone();
								
								/* Apply prefix to label name */
								if (l0.name.startsWith(".")) l0.name = l0.name.substring(1);
								l0.name = prefix + l0.name;
								
								if (added.contains(l0.name)) continue;
								else added.add(l0.name);
								
								/* Inject label clone at target position */
								body.instructions.add(i + 1, l0);
							}
						}
						
						buffer.clear();
					}
				}
			}
		}
		
		if (!instructionAppenix.isEmpty()) {
			for (int i = 0; i < body.instructions.size(); i++) {
				if (body.instructions.get(i).equals(branch)) {
					body.instructions.add(i + 1, new ASMSeperator());
					instructionAppenix.add(new ASMSeperator());
					body.instructions.addAll(i + 2, instructionAppenix);
					break;
				}
			}
		}
		
		progress.incProgress(1);
		
		return body;
	}
	
	/**
	 * Adds the generated assembly to the body or inserts an include directive, that
	 * can be processed by the linker.
	 * 
	 * @param ins The generated assembly
	 * @param ext A list of already included files
	 * @param s The source of the ressource that generated the assembly
	 */
	private void injectASM(List<ASMInstruction> ins, List<String> ext, Source s, String prefix, String...incLabels) {
		if (!CompilerDriver.buildObjectFileOnly || CompilerDriver.inputFile.getAbsolutePath().endsWith(s.sourceFile)) {
			this.instructions.addAll(ins);
		}
		else {
			for (String incLabel : incLabels) {
				if (!ext.contains(s.sourceFile + "@" + prefix + incLabel)) {
					this.instructions.add(new ASMDirective(".include " + s.sourceFile + "@" + prefix + incLabel));
					ext.add(s.sourceFile + "@" + prefix + incLabel);
				}
			}
			
			if (!this.external.containsKey(s.sourceFile)) {
				this.external.put(s.sourceFile, ins);
			}
		}
		
		this.instructions.add(new ASMSeperator());
	}
	
	public static void branchToCopyRoutine(AsNNode node) throws CGEN_EXC {
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
