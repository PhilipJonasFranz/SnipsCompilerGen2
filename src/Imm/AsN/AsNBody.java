package Imm.AsN;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import CGen.LabelGen;
import CGen.LiteralManager;
import CGen.MemoryMap;
import CGen.RegSet;
import CGen.StackSet;
import Exc.CGEN_EXCEPTION;
import Exc.CTX_EXCEPTION;
import Imm.ASM.ASMInstruction;
import Imm.ASM.ASMInstruction.OPT_FLAG;
import Imm.ASM.Branch.ASMBranch;
import Imm.ASM.Branch.ASMBranch.BRANCH_TYPE;
import Imm.ASM.Memory.ASMLdr;
import Imm.ASM.Memory.ASMLdrLabel;
import Imm.ASM.Memory.ASMStr;
import Imm.ASM.Memory.Stack.ASMLdrStack;
import Imm.ASM.Memory.Stack.ASMPopStack;
import Imm.ASM.Memory.Stack.ASMPushStack;
import Imm.ASM.Memory.Stack.ASMStackOp.MEM_OP;
import Imm.ASM.Processing.Arith.ASMAdd;
import Imm.ASM.Processing.Arith.ASMMov;
import Imm.ASM.Processing.Arith.ASMSub;
import Imm.ASM.Processing.Logic.ASMCmp;
import Imm.ASM.Structural.ASMComment;
import Imm.ASM.Structural.ASMSectionAnnotation;
import Imm.ASM.Structural.ASMSectionAnnotation.SECTION;
import Imm.ASM.Structural.ASMSeperator;
import Imm.ASM.Structural.Label.ASMDataLabel;
import Imm.ASM.Structural.Label.ASMLabel;
import Imm.ASM.Util.Cond;
import Imm.ASM.Util.Cond.COND;
import Imm.ASM.Util.Operands.ImmOperand;
import Imm.ASM.Util.Operands.LabelOperand;
import Imm.ASM.Util.Operands.RegOperand;
import Imm.ASM.Util.Operands.RegOperand.REGISTER;
import Imm.ASM.Util.Operands.Memory.MemoryWordOperand;
import Imm.ASM.Util.Operands.Memory.MemoryWordRefOperand;
import Imm.AST.Function;
import Imm.AST.Program;
import Imm.AST.SyntaxElement;
import Imm.AST.Expression.Atom;
import Imm.AST.Statement.Comment;
import Imm.AST.Statement.Declaration;
import Imm.AsN.Expression.AsNExpression;
import Imm.AsN.Expression.AsNIdRef;
import Imm.AsN.Statement.AsNAssignment;
import Imm.AsN.Statement.AsNComment;
import Snips.CompilerDriver;
import Util.Logging.ProgressMessage;

public class AsNBody extends AsNNode {

	public static ProgressMessage progress;
	
	/* Label to the stack copy routine */
	public static ASMLabel stackCopyRoutine = new ASMLabel("_routine_stack_copy_");
	
	/* Set to true to signal that a part of the program used the routine */
	public static boolean usedStackCopyRoutine = false;
	
	public static LiteralManager literalManager;
	
			/* --- METHODS --- */
	public static AsNBody cast(Program p, ProgressMessage progress) throws CGEN_EXCEPTION, CTX_EXCEPTION {
		AsNBody.usedStackCopyRoutine = false;
		AsNBody.literalManager = new LiteralManager();
		
		AsNBody body = new AsNBody();
		p.castedNode = body;
		AsNBody.progress = progress;
		
		MemoryMap map = new MemoryMap();
		
		/* File name comment */
		DateFormat df = new SimpleDateFormat("MM/dd/yyyy HH:mm:ss");
		Date today = Calendar.getInstance().getTime();        
		String todayAsString = df.format(today);
		
		body.instructions.add(new ASMComment("--" + CompilerDriver.file.getName() + ((CompilerDriver.includeMetaInformation)? ", Snips Version: " + CompilerDriver.sys_config.getValue("Version") + ", Date: " + todayAsString : "")));
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
		globalsInit.add(new ASMPushStack(new RegOperand(REGISTER.R0), new RegOperand(REGISTER.R1), new RegOperand(REGISTER.R2), new RegOperand(REGISTER.FP), new RegOperand(REGISTER.LR)));
		
		for (int i = 0; i < p.programElements.size(); i++) {
			SyntaxElement s = p.programElements.get(i);
			if (s instanceof Declaration) {
				Declaration dec = (Declaration) s;
				
				/* Create instruction for .data Section */
				ASMDataLabel dataEntry = new ASMDataLabel(dec.path.build(), new MemoryWordOperand(dec.value));
				body.instructions.add(dataEntry);

				/* Create address reference instruction for .text section */
				ASMDataLabel reference = new ASMDataLabel(LabelGen.mapToAddressName(dec.path.build()), new MemoryWordRefOperand(dataEntry));
				globalVarReferences.add(reference);
				
				/* Add declaration to global memory */
				map.add(dec, reference);
				globals = true;
				
				done++;
				progress.incProgress((double) done / p.programElements.size());
			}
		}
		
		if (CompilerDriver.heap_referenced) {
			globals = true;
			
			Declaration heap = CompilerDriver.HEAP_START;
			
			/* Create instruction for .data Section */
			ASMDataLabel dataEntry = new ASMDataLabel(heap.path.build(), new MemoryWordOperand(heap.value));
			body.instructions.add(dataEntry);
			
			/* Create address reference instruction for .text section */
			ASMDataLabel reference = new ASMDataLabel(LabelGen.mapToAddressName(heap.path.build()), new MemoryWordRefOperand(dataEntry));
			globalVarReferences.add(reference);
			
			/* Add declaration to global memory */
			map.add(heap, reference);
		}
		
		if (CompilerDriver.null_referenced) {
			Declaration nullPtr = CompilerDriver.NULL_PTR;
			
			/* Create instruction for .data Section */
			ASMDataLabel dataEntry = new ASMDataLabel(nullPtr.path.build(), new MemoryWordOperand(nullPtr.value));
			body.instructions.add(dataEntry);
			
			/* Create address reference instruction for .text section */
			ASMDataLabel reference = new ASMDataLabel(LabelGen.mapToAddressName(nullPtr.path.build()), new MemoryWordRefOperand(dataEntry));
			globalVarReferences.add(reference);
			
			/* Add declaration to global memory */
			map.add(nullPtr, reference);
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
		ASMBranch branch = new ASMBranch(BRANCH_TYPE.B, new LabelOperand());
		body.instructions.add(branch);
				
		
		/* --- Inject Stack Copy Routine --- */
		List<ASMInstruction> routine = body.buildStackCopyRoutine();
		body.instructions.addAll(routine);
	
		ASMLabel mainLabel = null;
		
		/* Cast program elements */
		for (SyntaxElement s : p.programElements) {
			if (s instanceof Function) {
				st = new StackSet();
				List<ASMInstruction> ins = AsNFunction.cast((Function) s, new RegSet(), map, st).getInstructions();
				
				/* Ensure that stack was emptied, so no stack shift at compile time occurred */
				assert(st.getStack().isEmpty());
				
				if (!ins.isEmpty()) {
					/* Patch Branch to Main Function */
					if (((Function) s).path.build().equals("main")) {
						((LabelOperand) branch.target).patch((ASMLabel) ins.get(0));
						mainLabel = (ASMLabel) ins.get(0);
					}
				}
				
				body.instructions.addAll(ins);
				body.instructions.add(new ASMSeperator());
				
				done++;
				progress.incProgress((double) done / p.programElements.size());
			}
			else if (s instanceof Comment) {
				body.instructions.addAll(AsNComment.cast((Comment) s, null, map, null).getInstructions());
			
				done++;
				progress.incProgress((double) done / p.programElements.size());
			}
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
					ASMLdrLabel ins = new ASMLdrLabel(new RegOperand(REGISTER.R1), new LabelOperand(label), dec);
					ins.comment = new ASMComment("Load from .data section");
					globalsInit.add(ins);
					
					if (dec.value.getType().wordsize() == 1) {
						globalsInit.add(new ASMStr(new RegOperand(REGISTER.R0), new RegOperand(REGISTER.R1)));
					}
					else {
						/* Quick and dirty way to relay the generated instructions into a AsNNode */
						AsNIdRef ref = new AsNIdRef();
						AsNAssignment.copyStackSection(dec.getType().wordsize(), ref, st);
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
				else if (globalsInit.get(i) instanceof ASMMov && ((ASMMov) globalsInit.get(i)).target.reg == REGISTER.PC) {
					hasCall = true;
				}
			}
			
			if (!hasCall) {
				ASMPushStack push = (ASMPushStack) globalsInit.get(2);
				push.operands.remove(push.operands.size() - 1);
				push.operands.remove(push.operands.size() - 1);
				
				globalsInit.add(new ASMPopStack(new RegOperand(REGISTER.R0), new RegOperand(REGISTER.R1), new RegOperand(REGISTER.R2)));
			}
			else globalsInit.add(new ASMPopStack(new RegOperand(REGISTER.R0), new RegOperand(REGISTER.R1), new RegOperand(REGISTER.R2), new RegOperand(REGISTER.FP), new RegOperand(REGISTER.LR)));
			
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
					((LabelOperand) branch.target).label = initLabel;
					break;
				}
			}
		}
		
		/* Routine was not used, remove */
		if (!AsNBody.usedStackCopyRoutine) body.instructions.removeAll(routine);
		
		/* Main function is first function and routine was not used, so the branch to main can be removed */
		if (p.programElements.get(0) instanceof Function && ((Function) p.programElements.get(0)).path.build().equals("main") && !AsNBody.usedStackCopyRoutine) {
			body.instructions.remove(branch);
		}
		
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
						String prefix = LabelGen.literalPoolPrefix();
						
						List<String> added = new ArrayList();
						
						for (ASMLdr l : buffer) {
							if (l instanceof ASMLdrLabel) {
								ASMLdrLabel label = (ASMLdrLabel) l;
								
								/* Apply prefix to load label */
								label.prefix = prefix;
								
								ASMDataLabel l0 = null;
								if (label.dec == null) 
									/* Label is dynamic label, generated by literal manager */
									l0 = AsNBody.literalManager.getValue((LabelOperand) label.op0).clone();
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
		
		progress.incProgress(1);
		
		return body;
	}
	
	public static void branchToCopyRoutine(AsNNode node) throws CGEN_EXCEPTION {
		/* Mark routine as used */
		AsNBody.usedStackCopyRoutine = true;
		
		ASMAdd add = new ASMAdd(new RegOperand(REGISTER.R10), new RegOperand(REGISTER.PC), new ImmOperand(8));
		add.comment = new ASMComment("Setup return address for routine");
		node.instructions.add(add);
		
		ASMBranch branch = new ASMBranch(BRANCH_TYPE.B, new LabelOperand(AsNBody.stackCopyRoutine));
		
		/* 
		 * Signal the optimizer that this is a controlled jump, and that the program will return, 
		 * but not with jump mechanics, but by manipulating the pc.
		 */
		branch.optFlags.add(OPT_FLAG.SYS_JMP);
		node.instructions.add(branch);
	}
	
	public List<ASMInstruction> buildStackCopyRoutine() {
		List<ASMInstruction> routine = new ArrayList();
		routine.add(new ASMComment("System Routine, used to copy memory on the stack"));
		
		routine.add(AsNBody.stackCopyRoutine);
		
		ASMLabel loopEnd = new ASMLabel("_routine_stack_copy_end_");
		
		/* Check if whole sub array was loaded */
		routine.add(new ASMCmp(new RegOperand(REGISTER.R0), new ImmOperand(0)));
		
		/* Branch to loop end */
		routine.add(new ASMBranch(BRANCH_TYPE.B, new Cond(COND.EQ), new LabelOperand(loopEnd)));
		
		routine.add(new ASMLdrStack(MEM_OP.PRE_WRITEBACK, new RegOperand(REGISTER.R2), new RegOperand(REGISTER.R1), new ImmOperand(-4)));
		
		routine.add(new ASMPushStack(new RegOperand(REGISTER.R2)));
		
		/* Decrement counter */
		routine.add(new ASMSub(new RegOperand(REGISTER.R0), new RegOperand(REGISTER.R0), new ImmOperand(4)));
		
		/* Branch to loop start */
		routine.add(new ASMBranch(BRANCH_TYPE.B, new LabelOperand(AsNBody.stackCopyRoutine)));
		
		routine.add(loopEnd);
		
		/* Branch back */
		routine.add(new ASMMov(new RegOperand(REGISTER.PC), new RegOperand(REGISTER.R10)));
		routine.add(new ASMSeperator());
		
		return routine;
	}
	
}
