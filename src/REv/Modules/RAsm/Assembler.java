package REv.Modules.RAsm;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map.Entry;

import REv.Modules.Tools.Util;
import Snips.CompilerDriver;
import Util.Logging.LogPoint;
import Util.Logging.LogPoint.Type;
import Util.Logging.Message;

public class Assembler {

	public enum MODE {
		TEXT, DATA
	}
	
	public static MODE mode = MODE.TEXT;
	
	static List<Message> log = new ArrayList();
	
	public static int [] [] assemble(List<String> input, boolean silent, boolean printBinary) throws Exception {
		boolean silent0 = CompilerDriver.silenced;
		CompilerDriver.silenced = silent;
		
		if (input == null || input.isEmpty())new Message("Input is empty!", LogPoint.Type.WARN);
		
		new Message("Starting compilation", LogPoint.Type.INFO);
		
		List<Instruction> in = new ArrayList();
		
		/* ----< Pre-Processor >---- */
		int c = 0;
		for (int i = 0; i < input.size(); i++) {
			String s = input.get(i).toLowerCase();
			String [] sp = s.split("\n");
			for (String s0 : sp) {
				in.add(new Instruction(s0, c++));
			}
		}
		
		/* Remove Comments */
		if (!in.isEmpty()) try {
			for (int i = 0; i < in.size(); i++) {
				if (in.get(i).getInstruction().startsWith("//")) {
					// Remove Comments starting with //
					in.remove(i);
					i--;
					continue;
				}
				
				String [] sp = in.get(i).getInstruction().split("");
				String build = "";
				boolean comment = false, wholeLine = false;
				try {
					for (int a = 0; a < sp.length; a++) {
						// Remove comments of the form /* */ and //
						if (a >= sp.length)break;
						if (sp [a].equals("/")) {
							if (sp [a + 1].equals("/") || sp [a + 1].equals("*")) {
								if (sp [a + 1].equals("/"))wholeLine = true;
								comment = true;
								a += 2;
								continue;
							}
							if (sp [a + 1].equals("/"))break;
						}
						if (sp [a].equals("*")) {
							if (sp [a + 1].equals("/")) {
								if (wholeLine)throw new Exception();
								comment = false;
								a++;
								continue;
							}
						}
						
						if (!comment)build += sp [a];
					}
					
					if (comment && !wholeLine)throw new Exception();
					
					in.get(i).setInstruction(build);
				} catch (Exception e) {
					new Message("Bad comment in line " + in.get(i).getLine() + ": " + in.get(i).getInstruction(), LogPoint.Type.FAIL);
				}
				
				if (in.get(i).isEmpty()) {
					// Remove empty lines
					in.remove(i);
					i--;
					continue;
				}
			}
		} catch (Exception e) {
			new Message("Internal Error while removing comments.", LogPoint.Type.FAIL);
		}
		
		boolean labelText = false, labelData = false;
		
		int l = in.size();
		
		/* Move .data block to the end of the file */
		if (!in.isEmpty())try {
			for (int i = 0; i < in.size(); i++) {
				if (in.get(i).getInstruction().equals(".data")) {
					mode = MODE.DATA;
					labelData = true;
				}
				if (in.get(i).getInstruction().equals(".text")) {
					if (mode == MODE.TEXT) {
						new Message("Found lines outside of any section, potentially missing .data label.", LogPoint.Type.WARN);
					}
					mode = MODE.TEXT;
					labelText = true;
					break;
				}
				
				if (mode.equals(MODE.DATA)) {
					in.add(in.get(i));
					in.remove(i);
					i--;
				}
				
				l--;
				if (l == 0)break;
			}
		} catch (Exception e) {
			log.add(new Message("Internal Error while relocating sections.", LogPoint.Type.FAIL));
		}
		
		if (!in.isEmpty()) {
			if (!labelData && !labelText)new Message("No section labels found. Assuming a global .text section.", LogPoint.Type.WARN);
			if (mode.equals(MODE.DATA))new Message("No .text section found.", LogPoint.Type.WARN);
		}
		
		mode = MODE.DATA;
		
		// Replace push and pop instructions, wrap out .skip
		if (!in.isEmpty())try {
			for (int i = 0; i < in.size(); i++) {
				if (in.get(i).getInstruction().equals(".data")) {
					mode = MODE.DATA;
				}
				if (in.get(i).getInstruction().equals(".text")) {
					mode = MODE.TEXT;
				}
				if (in.get(i).getInstruction().startsWith(".global") ||
						in.get(i).getInstruction().startsWith(".version")) {
					in.remove(i);
					i--;
					continue;
				}
				
				if (in.get(i).getInstruction().startsWith("push")) {
					String [] sp = in.get(i).getInstruction().split(" ");
					in.remove(i);
					
					sp [0] = sp [0].substring(4);
					
					for (int a = 1; a < sp.length; a++) {
						String r = sp [a].trim();
						if (r.equals("") || r.equals("{") || r.equals("}"))continue;
						if (r.startsWith("{"))r = r.substring(1);
						if (r.endsWith("}"))r = r.substring(0, r.length() - 1);
						if (r.endsWith(","))r = r.substring(0, r.length() - 1);
						
						in.add(i, new Instruction("str" + sp [0] + " " + r + ", [sp, #-4]!", i));
					}
				}
				
				if (in.get(i).getInstruction().startsWith("pop")) {
					String [] sp = in.get(i).getInstruction().split(" ");
					in.remove(i);
					
					sp [0] = sp [0].substring(3);
					
					for (int a = sp.length - 1; a >= 1; a--) {
						String r = sp [a].trim();
						if (r.equals("") || r.equals("{") || r.equals("}"))continue;
						if (r.startsWith("{"))r = r.substring(1);
						if (r.endsWith("}"))r = r.substring(0, r.length() - 1);
						if (r.endsWith(","))r = r.substring(0, r.length() - 1);
						
						in.add(i, new Instruction("ldr" + sp [0] + " " + r + ", [sp], #4", i));
					}
				}
				
				if (in.get(i).getInstruction().contains(".skip")) {
					if (mode == MODE.TEXT)log.add(new Message("Misplaced statement in line " + in.get(i).getLine() + ": .skip in .text section", LogPoint.Type.FAIL));
					if (!in.get(i).getInstruction().startsWith(".skip")) {
						// Label: .skip -> .word 0 ...
						String [] sp = in.get(i).getInstruction().split(":");
						int n = Integer.parseInt(sp [1].trim().split(" ") [1]);
						if (n == 0) {
							in.remove(i);
							i--;
						}
						else {
							if (n % 4 != 0) {
								System.out.println("Error: can only bytes in quantity of fours.");
								continue;
							}
							else {
								for (int a = 0; a < n >> 2; a++) {
									if (a == 0)in.get(i).setInstruction(sp [0] + ": .word 0");
									else in.add(i + 1, new Instruction(".word 0", i));
								}
							}
						}
					}
					else {
						// .skip -> .word 0 ...
						int n = Integer.parseInt(in.get(i).getInstruction().trim().split(" ") [1]);
						in.remove(i);
						
						if (n % 4 != 0) {
							System.out.println("Error: can only bytes in quantity of fours.");
							continue;
						}
						else for (int a = 0; a < n >> 2; a++)in.add(i, new Instruction(".word 0", i));
					}
				}
			}
		} catch (Exception e) {
			new Message("Internal Error when replacing push/pop operations and unrolling .skip.", LogPoint.Type.FAIL);
		}
		
		// First pass, get Locations
		HashMap<String, Integer> locations = new HashMap();
		if (!in.isEmpty())try {
			for (int i = 0; i < in.size(); i++) {
				if (in.get(i).getInstruction().contains(":")) {
					if (!in.get(i).getInstruction().contains(".word") && !in.get(i).getInstruction().contains(".skip")) {
						// [label]: -> remove line
						String label = in.get(i).getInstruction().substring(0, in.get(i).getInstruction().length() - 1);
						
						if (locations.containsKey(label)) new Message("Multiple labels with similar name: " + label, LogPoint.Type.FAIL);
						if (label.equals("")) new Message("Label name cannot be empty in line " + in.get(i).getLine() + ".", LogPoint.Type.FAIL);
						if (label == null || label.equals("null")) 
							new Message("Found bad label: '" + label + "' in line " + in.get(i).line, LogPoint.Type.FAIL);
						
						locations.put(label, i * 4);
						in.remove(i);
						i--;
					}
					else {
						// [label]: .[] ... -> .[] ...
						String [] sp = in.get(i).getInstruction().split(":");
						locations.put(sp [0].trim(), i * 4);
						in.get(i).setInstruction(sp [1].trim());
					}
				}
			}
		} catch (Exception e) {
			new Message("Internal Error when retrieving address data.", LogPoint.Type.FAIL);
		}
		
		int posData = 0;
		try {
			if (labelData)for (int i = 0; i < in.size(); i++) {
				if (in.get(i).getInstruction().equals(".data")) {
					posData = i * 4;
					break;
				}
			}
		} catch (Exception e) {
			new Message("Internal Error when retrieving location of .data label.", LogPoint.Type.FAIL);
		}
		
		// Relocate locations after removal of .data and .text labels
		if (!in.isEmpty())try {
			for (Entry<String, Integer> entry : locations.entrySet()) {
				if (labelData && entry.getValue() > posData)entry.setValue(entry.getValue() - 4);
				if (labelText)entry.setValue(entry.getValue() - 4);
			}
		} catch (Exception e) {
			new Message("Internal Error when relocating address data.", LogPoint.Type.FAIL);
		}
		
		// Replace Addresses
		if (!in.isEmpty())try {
			for (int i = 0; i < in.size(); i++) {
				if (in.get(i).getInstruction().startsWith("bl") || (in.get(i).instruction.startsWith("b") && !in.get(i).getInstruction().startsWith("bx"))) {
					// bl there -> bl [x], x = address there
					String [] sp = in.get(i).getInstruction().split(" ");
					
					if (!locations.containsKey(sp [1]) || locations.get(sp [1]) == null)
						 log.add(new Message("Unknown label: " + sp [1] + " in line " + in.get(i).getLine() + ".", LogPoint.Type.FAIL));
					
					sp [1] = "" + locations.get(sp [1]);
					in.get(i).setInstruction(sp [0] + " " + sp [1]);
				}
			}
		} catch (Exception e) {
			new Message("Internal Error when replacing labels with addresses.", LogPoint.Type.FAIL);
		}
		
		List<String> instr = new ArrayList();
		
		/* Set to true when switching sections for the first time */
		boolean sectionSwitch = false;
		
		Exception error = null;
		
		// Generate Code
		if (!in.isEmpty())for (int i = 0; i < in.size(); i++) {
			try {
				boolean addLine = true;
				
				String [] sp = in.get(i).getInstruction().replace("  ", " ").split(" ");
				for (int a = 0; a < sp.length; a++)sp [a] = sp [a].replace(",", "").trim();
				String app = "";
				
				if (sp [0].equals(".data")) {
					if (i * 4 != posData)new Message("Found multiple .data labels.", LogPoint.Type.FAIL);
					if (mode == MODE.DATA)new Message("Found multiple .data labels.", LogPoint.Type.FAIL);
					mode = MODE.DATA;
					addLine = false;
					sectionSwitch = true;
				}
				else if (sp [0].equals(".text")) {
					if (mode == MODE.TEXT && sectionSwitch)new Message("Found multiple .text labels.", LogPoint.Type.FAIL);
					mode = MODE.TEXT;
					addLine = false;
					sectionSwitch = true;
				}
				else if (sp [0].startsWith("b")) {
					if (sp [0].startsWith("bx")) {
						if (sp.length == 1 || sp [1].equals("null"))new Message("Expected register: bx [Reg] in line " + in.get(i).getLine() + ".", LogPoint.Type.FAIL);
						sp [0] = sp [0].substring(2);
						
						String cond = getCond(sp [0]);
						if (cond != null)sp [0] = sp [0].substring(2);
						else cond = "1110";
						
						app += cond;
						app += "000100101111111111110001";
						app += getReg(sp [1]);
					}
					else {
						if (sp.length == 1 || sp [1].equals("null"))new Message("Expected destination: " + sp [0] + " in line " + in.get(i).getLine() + ".", LogPoint.Type.FAIL);
						String link = "0";
						
						sp [0] = sp [0].substring(1);
						
						if (sp [0].equals("l") || sp [0].length() == 3) {
							sp [0] = sp [0].substring(1);
							link = "1";
						}
						
						String cond = getCond(sp [0]);
						if (cond != null)sp [0] = sp [0].substring(2);
						else cond = "1110";
						
						app += cond;
						app += "101";
						app += link;
						app += Assembler.toBinaryStringLength(sp [1], 24, in.get(i).getLine());
					}
				}
				else if (sp [0].startsWith("mrs")) {
					sp [0] = sp [0].substring(3);
					
					String cond = getCond(sp [0]);
					if (cond != null)sp [0] = sp [0].substring(2);
					else cond = "1110";
					
					app += cond; // Cond
					app += "00010";
					app += "0"; // TODO: CPSR or SPSR
					app += "001111";
					app += getReg(sp [1]);
					app += "000000000000";
				}
				else if (sp [0].startsWith("msr")) {
					// TODO
				}
				else if (sp [0].startsWith("lsl") || sp [0].startsWith("lsr") || sp [0].startsWith("asr") || sp [0].startsWith("ror")) {
					String shift = getShift(sp [0].substring(0, 3));
					sp [0] = sp [0].substring(3);
					
					String cond = getCond(sp [0]);
					if (cond != null)sp [0] = sp [0].substring(2);
					else cond = "1110";
					
					app += cond; // Cond
					app += "00"; // Data Processing
					app += "0"; // I TODO: Shift by reg?
					app += "1101"; // OpCode
					app += "0"; // S
					app += "0000"; // Rn
					app += getReg(sp [1]); //Rd
					if (sp [sp.length - 1].startsWith("#")) {
						app += toBinaryStringLength(sp [sp.length - 1].substring(1), 5, in.get(i).getLine());
						app += shift;
						app += "0";
					}
					else {
						app += getReg(sp [sp.length - 1]);
						app += "0";
						app += shift;
						app += "1";
					}
					
					app += (sp.length > 3)? getReg(sp [2]) : getReg(sp [1]); // Source reg: Rd
				}
				else if (sp [0].startsWith("mov") || sp [0].startsWith("mvn") || sp [0].startsWith("cmp") || sp [0].startsWith("cmn")) {
					String op = getOpCode(sp [0]);
					sp [0] = sp [0].substring(3);
					
					String cond = getCond(sp [0]);
					if (cond != null)sp [0] = sp [0].substring(2);
					else cond = "1110";
					
					app += cond; // Cond
					app += "00";
					app += (sp [2].contains("#"))? "1" : "0";
					app += op; // OpCode
					app += "0"; // s
					app += getReg(sp [1]); // Rn
					app += getReg(sp [1]); // Rd
					if (sp [2].contains("#")) { // Immediate value
						app += "0000"; // TODO: ror
						app += toBinaryStringLength(sp [2].substring(1), 8, in.get(i).getLine());	
					}
					else { // Register Source
						if (sp.length > 3) {
							if (sp.length < 5)new Message("Expected shift operation and operand in line " + in.get(i).getLine() + ", got " + sp [3], LogPoint.Type.FAIL);
							app += getShiftBin(sp [3] + " " + sp [4]); // shamt
						}
						else app += "00000000";
						app += getReg(sp [2]);
					}
				}
				else if (sp [0].startsWith("and") || sp [0].startsWith("eor") || sp [0].startsWith("sub") ||
					 sp [0].startsWith("rsb") || sp [0].startsWith("add") || sp [0].startsWith("adc") ||
					 sp [0].startsWith("sbc") || sp [0].startsWith("rsc") || sp [0].startsWith("tst") ||
					 sp [0].startsWith("teq") || sp [0].startsWith("orr") || sp [0].startsWith("bic")) {
					String op = getOpCode(sp [0]);
					sp [0] = sp [0].substring(3);
					
					String cond = getCond(sp [0]);
					if (cond != null)sp [0] = sp [0].substring(2);
					else cond = "1110";
					
					app += cond; // Cond
					app += "00";
					app += (sp [3].contains("#"))? "1" : "0";
					app += op; // OpCode
					app += (sp [0].startsWith("s"))? "1" : "0"; //s
					app += getReg(sp [2]); //Rn
					app += getReg(sp [1]); // Rd
					if (sp [3].contains("#")) {
						app += "0000"; // TODO: Ror
						app += toBinaryStringLength(sp [3].substring(1), 8, in.get(i).getLine());
					}
					else {
						if (sp.length > 4) {
							if (sp.length < 6)new Message("Expected shift operation and operand in line " + in.get(i).getLine() + ", got " + sp [4], LogPoint.Type.FAIL);
							app += getShiftBin(sp [4] + " " + sp [5]); // shamt
						}
						else app += "00000000";
						app += getReg(sp [3]);
					}
				}
				else if (sp [0].startsWith("mul") || sp [0].startsWith("mla")) {
					int acc = (sp [0].startsWith("mul"))? 0 : 1;
					sp [0] = sp [0].substring(3);
						
					String cond = getCond(sp [0]);
					if (cond != null)sp [0] = sp [0].substring(2);
					else cond = "1110";
					
					app += cond; // Cond
					app += "000000";
					app += "" + acc; // Accumulate
					app += (sp [0].startsWith("s"))? "1" : "0"; // s
					app += getReg(sp [1]); // Rd
					app += (sp.length > 4)? getReg(sp [4]) : "0000"; // Rn
					app += getReg(sp [3]); // Rs
					app += "1001";
					app += getReg(sp [2]); // Rm
				}
				else if (sp [0].startsWith("ldm") || sp [0].startsWith("stm")) {
					/* Block Memory Op Code */
					String op = sp [0].substring(3);
					
					String cond = null;
					
					if (op.length() > 2) {
						cond = getCond(op.substring(op.length() - 2, op.length()));
						op = op.substring(0, op.length() - 2);
					}
					
					String p = "0";
					String u = "0";
					String l0 = "0";
					
					if (sp [0].startsWith("ldm")) {
						if (op.equals("ed")) {
							l0 = "1"; p = "1"; u = "1"; }
						else if (op.equals("fd")) {
							l0 = "1"; p = "0"; u = "1"; }
						else if (op.equals("ea")) {
							l0 = "1"; p = "1"; u = "0"; }
						else if (op.equals("fa")) {
							l0 = "1"; p = "0"; u = "0"; }
					}
					else {
						if (op.equals("fa")) {
							l0 = "0"; p = "1"; u = "1"; }
						else if (op.equals("ea")) {
							l0 = "0"; p = "0"; u = "1"; }
						else if (op.equals("fd")) {
							l0 = "0"; p = "1"; u = "0"; }
						else if (op.equals("ed")) {
							l0 = "0"; p = "0"; u = "0"; }
					}
					
					String wb = "0";
					
					/* Extract Rn, cut away , */
					String rn = sp [1].trim();
					
					if (rn.endsWith("!")) {
						wb = "1";
						rn = rn.substring(0, rn.length() - 1);
					}

					String regListCode = "0000000000000000";
					
					for (int k = 2; k < sp.length; k++) {
						sp [k] = sp [k].trim();
						if (sp [k].endsWith(",")) sp [k] = sp [k].substring(0, sp [k].length() - 1);
						if (sp [k].endsWith("}")) sp [k] = sp [k].substring(0, sp [k].length() - 1);
						if (sp [k].startsWith("{")) sp [k] = sp [k].substring(1);
						
						if (toInt(sp [k]) != -1) {
							regListCode = replaceChar(regListCode, '1', 15 - toInt(sp [k]));
						}
						else {
							String [] sp0 = sp [k].split("-");
							int s = toInt(sp0 [0].trim());
							int e = toInt(sp0 [1].trim());
							
							for (int z = s; z <= e; z++) 
								regListCode = replaceChar(regListCode, '1', 15 - z);
						}
					}
					
					app += (cond != null)? cond : "1110";
					app += "100";
					app += p;
					app += u;
					app += "0";
					app += wb;
					app += l0;
					app += getReg(rn);
					app += regListCode;
				}
				else if (sp [0].startsWith("ldr") || sp [0].startsWith("str")) {
					String ls = (sp [0].startsWith("str"))? "0" : "1";
					sp [0] = sp [0].substring(3);
					
					String cond = getCond(sp [0]);
					if (cond != null)sp [0] = sp [0].substring(2);
					else cond = "1110";
					
					String imm = "1"; // Default: Reg Src
					String immV = "000000000000";
					
					String rn = "0000";
					String rm = null;
					
					String shift = "00000000";
					
					String upDown = "1";
					
					String wb = "0";
					String indexing = "1";
					if (!in.get(i).getInstruction().contains("[")) {
						// Address Label
						if (sp.length < 3)new Message("Expected Address Label or Operands: " + in.get(i).getInstruction(), LogPoint.Type.FAIL);
						String [] labelSplit = sp [2].split("\\+");
						if (locations.containsKey(labelSplit [0])) {
							imm = "0";
							int base = locations.get(labelSplit [0]);
							if (labelSplit.length == 2)base += Integer.parseInt(labelSplit [1]);
							immV = Assembler.toBinaryStringLength("" + (base - (i << 2) + ((labelData)? 4 : 0)), 12, in.get(i).getLine());
							rn = "1111";
						}
						else {
							new Message("Unknown Label: " + sp [2] + " in line " + in.get(i).getLine() + ".", LogPoint.Type.FAIL);
						}
						upDown = "1";
					}
					else if (in.get(i).getInstruction().contains("!")) {
						// ldr r0, [r1, #-4]!
						wb = "1";
						
						sp [2] = sp [2].substring(1);
						sp [sp.length - 1] = sp [sp.length - 1].substring(0, sp [sp.length - 1].length() - 2);
						
						rn = getReg(sp [2]);
						
						if (sp [3].contains("#")) {
							imm = "0";
							int val = Integer.parseInt(sp [3].substring(1));
							if (val < 0) {
								upDown = "0";
								val = -val;
							}
							immV = toBinaryStringLength("" + val, 12, in.get(i).getLine());
						}
						else {
							if (sp.length > 5)shift = getShiftBin(sp [4] + " " + sp [5]);
							
							/* - before rm operand */
							if (sp [3].startsWith("-")) {
								upDown = "0";
								sp [3] = sp [3].substring(1);
							}
							
							rm = shift + getReg(sp [3]);
						}
					}
					else if (sp.length > 3 && sp [sp.length - 1].contains("]")) {
						// ldr r0, [r1, r0]
						sp [2] = sp [2].substring(1);
						if (sp [3].endsWith("]"))sp [3] = sp [3].substring(0, sp [3].length() - 1);
						
						rn = getReg(sp [2]);
						
						if (sp [3].contains("#")) {
							imm = "0";
							int val = Integer.parseInt(sp [3].substring(1));
							if (val < 0) {
								upDown = "0";
								val = -val;
							}
							immV = toBinaryStringLength("" + val, 12, in.get(i).getLine());
						}
						else {
							if (sp.length > 5) shift = getShiftBin(sp [4] + " " + sp [5].substring(0, sp [5].length() - 1));
							
							/* - before rm operand */
							if (sp [3].startsWith("-")) {
								upDown = "0";
								sp [3] = sp [3].substring(1);
							}
							
							rm = shift + getReg(sp [3]);
						}
					}
					else {
						// ldr r0, [r0], r1
						wb = "1";
						indexing = "0";
						
						sp [2] = sp [2].substring(1, sp [2].length() - 1);
						
						rn = getReg(sp [2]);
						
						if (sp.length > 3 && sp [3].contains("#")) {
							imm = "0";
							int val = Integer.parseInt(sp [3].substring(1));
							if (val < 0) {
								upDown = "0";
								val = -val;
							}
							immV = toBinaryStringLength("" + val, 12, in.get(i).getLine());
						}
						else if (sp.length > 3) {
							if (sp.length > 5) {
								if (sp [5].endsWith("]"))sp [5] = sp [5].substring(0, sp [5].length() - 1);
								shift = getShiftBin(sp [4] + " " + sp [5]);
							}
							
							/* - before rm operand */
							if (sp [3].startsWith("-")) {
								upDown = "0";
								sp [3] = sp [3].substring(1);
							}
							
							rm = shift + getReg(sp [3]);
						}
						else {
							imm = "0";
							immV = toBinaryStringLength("0", 12, in.get(i).getLine());
						}
					}
					
					app += cond;
					app += "01";
					app += imm; // Immediate
					app += indexing; // Indexing
					app += upDown; // Up/Down
					app += "0"; // Quantity
					app += wb; // Writeback
					app += ls;
					
					app += rn; // Rn
					app += getReg(sp [1]); // Rd
					
					if (imm.equals("0"))app += immV;
					else app += rm; // Rm
				}
				else if (sp [0].startsWith("swp")) {
					sp [0] = sp [0].substring(3);
					
					String quantity = "0";
					if (sp [0].startsWith("b")) {
						quantity = "1";
						sp [0] = sp [0].substring(1);
					}
					
					String cond = getCond(sp [0]);
					if (cond != null)sp [0] = sp [0].substring(2);
					else cond = "1110";
					
					app += cond;
					app += "00010";
					app += quantity;
					app += "00";
					
					app += getReg(sp [3].substring(1, sp [3].length() - 1)); // Rn, Cut brackets
					app += getReg(sp [1]); // Rd
					app += "00001001";
					app += getReg(sp [2]); // Rm
				}
				else if (sp [0].startsWith(".word")) {
					if (sp.length == 1)new Message("Expected identifier or numeric: " + in.get(i).getInstruction(), LogPoint.Type.FAIL);
					if (mode == MODE.TEXT) {
						// .word [label] -> label address
						try {
							int imm = Integer.parseInt(sp [1]);
							int [] num = Util.toBinary(imm);
							for (int a : num)app = a + app;
						} catch (NumberFormatException e) {
							if (locations.get(sp [1]) == null) {
								throw new Exception("Unknown label in line " + in.get(i).getLine() + ": " + sp [1]);
							}
							
							int [] num = Util.toBinary(locations.get(sp [1]));
							for (int a : num)app = a + app;
						}
					}
					else {
						// Either write word by .word [value] or default to 0, .word [value] -> [value]
						try {
							int [] num = Util.toBinary((sp.length > 1 && !sp [1].equals(""))? Integer.parseInt(sp [1]) : 0);
							for (int a : num)app = a + app;
						} catch (Exception e) {
							int [] num = Util.toBinary(locations.get(sp [1]));
							for (int a : num)app = a + app;
							
							//new Message("Error when parsing label in line " + in.get(i).getLine() + ": Expected numeric value for .word, got label name: " + sp [1], LogPoint.Type.FAIL);
							//new Message("Possible causes: Multiple/misplaced .data labels.", LogPoint.Type.WARN);
						}
					}
				}
				else {
					new Message("Unknown Command in line " + in.get(i).getLine() + ": " + in.get(i).getInstruction(), LogPoint.Type.FAIL);
				}
			
				if (addLine) {
					app = app.trim();
					if (!app.equals("") && printBinary) System.out.println(app + ": " + in.get(i).getInstruction());
					instr.add(app);
				}
			} catch (Exception e) {
				boolean silenced = CompilerDriver.silenced;
				CompilerDriver.silenced = false;
				log.add(new Message("Internal Error when creating machine code in line " + in.get(i).getLine(), LogPoint.Type.FAIL));
				log.stream().forEach(x -> x.flush());
				new Message("Error in line: " + in.get(i).instruction + " line: " + in.get(i).getLine(), Type.FAIL);
				error = e;
				CompilerDriver.silenced = silenced;
			}
		}
		
		if (error != null) throw error;
		
		// To Integer Arrays
		int [] [] code = new int [instr.size()] [32];
		if (!in.isEmpty())try {
			for (int i = 0; i < instr.size(); i++) {
				if (instr.get(i).equals("")) continue;
				String [] sp = instr.get(i).split("");
				for (int a = 0; a < 32; a++)code [i] [31 - a] = Integer.parseInt(sp [a]);
			}
		} catch (Exception e) {
			new Message("Internal Error when formatting to binary data.", LogPoint.Type.FAIL);
		}
		
		int err = (int) log.stream().filter(x -> x.messageType == LogPoint.Type.FAIL).count();
		int warn = (int) log.stream().filter(x -> x.messageType == LogPoint.Type.WARN).count();
		new Message("Compilation finished " + ((err == 0 && warn == 0)? "successfully." : ((err > 0)? "with " + err + " Error" + ((err > 1)? "s" : "") + ((warn > 0)? " and " : "") : "") + ((warn > 0)? "with " + warn + " Warning" + ((warn > 1)? "s" : "") : "") + "."), (err == 0)? LogPoint.Type.INFO : LogPoint.Type.FAIL);
		
		log.clear();
		
		CompilerDriver.silenced = silent0;
		
		return code;
	}
	
	/**
	 * [Shift Type] [Imm/Reg] -> 0100100...
	 * @throws Exception 
	 */
	public static String getShiftBin(String s) throws Exception {
		String [] sp = s.split(" ");
		String shift = "00000000";
		if (sp [1].startsWith("#")) {
			shift = toBinaryStringLength(sp [1].substring(1), 5, -1);
			shift += getShift(sp [0]);
			shift += "0";
		}
		else {
			try {
				shift = getReg(sp [1]);
			} catch (Exception e) {
				log.add(new Message("Invalid shift operation: " + s, LogPoint.Type.FAIL));
			}
			shift += "0";
			shift += getShift(sp [0]);
			shift += "1";
		}
		
		return shift;
	}
	
	/*
	 * [num] -> |bin(num)| = w
	 */
	public static String toBinaryStringLength(String num, int w, int line) throws Exception {
		try {
			int [] b = Util.toBinary(Integer.parseInt(num));
			String app = "";
			for (int i = 0; i < w; i++)app = b [31 - i] + app;
			return app;
		} catch (NumberFormatException e) {
			throw new Exception("Bad parse input, line " + line + ": " + num);
		}
	}
	
	
	/* Look up Tables */
	public static String getShift(String in) {
		if (in.equals("lsl"))return "00";
		if (in.equals("lsr"))return "01";
		if (in.equals("asr"))return "10";
		if (in.equals("ror"))return "11";
		return null;
	}
	
	public static String getCond(String in) {
		if (in.startsWith("eq"))return "0000";
		if (in.startsWith("ne"))return "0001";
		if (in.startsWith("cs"))return "0010";
		if (in.startsWith("cc"))return "0011";
		if (in.startsWith("mi"))return "0100";
		if (in.startsWith("pl"))return "0101";
		if (in.startsWith("vs"))return "0110";
		if (in.startsWith("vc"))return "0111";
		if (in.startsWith("hi"))return "1000";
		if (in.startsWith("ls"))return "1001";
		if (in.startsWith("ge"))return "1010";
		if (in.startsWith("lt"))return "1011";
		if (in.startsWith("gt"))return "1100";
		if (in.startsWith("le"))return "1101";
		if (in.startsWith("al"))return "1110";
		return null;
	}
	
	public static String getOpCode(String in) {
		if (in.startsWith("lsl") || in.startsWith("lsr") || in.startsWith("asr") || in.startsWith("ror"))return "1101";
		if (in.startsWith("and"))return "0000";
		if (in.startsWith("eor"))return "0001";
		if (in.startsWith("sub"))return "0010";
		if (in.startsWith("rsb"))return "0011";
		if (in.startsWith("add"))return "0100";
		if (in.startsWith("adc"))return "0101";
		if (in.startsWith("sbc"))return "0110";
		if (in.startsWith("rsc"))return "0111";
		if (in.startsWith("tst"))return "1000";
		if (in.startsWith("teq"))return "1001";
		if (in.startsWith("cmp"))return "1010";
		if (in.startsWith("cmn"))return "1011";
		if (in.startsWith("orr"))return "1100";
		if (in.startsWith("mov"))return "1101";
		if (in.startsWith("bic"))return "1110";
		if (in.startsWith("mvn"))return "1111";
		return null;
	}
	
	public static String getReg(String r) throws Exception {
		r = r.trim();
		if (r.equals("r0"))return "0000";
		if (r.equals("r1"))return "0001";
		if (r.equals("r2"))return "0010";
		if (r.equals("r3"))return "0011";
		if (r.equals("r4"))return "0100";
		if (r.equals("r5"))return "0101";
		if (r.equals("r6"))return "0110";
		if (r.equals("r7"))return "0111";
		if (r.equals("r8"))return "1000";
		if (r.equals("r9"))return "1001";
		if (r.equals("r10"))return "1010";
		if (r.equals("r11"))return "1011";
		if (r.equals("r12"))return "1100";
		if (r.equals("r13"))return "1101";
		if (r.equals("r14"))return "1110";
		if (r.equals("r15"))return "1111";
		if (r.equals("sp"))return "1101";
		if (r.equals("lr"))return "1110";
		if (r.equals("pc"))return "1111";
		if (r.equals("fp"))return "1011";
		log.add(new Message("Not a register: " + r, LogPoint.Type.FAIL));
		throw new Exception();
	}
	
	public static int toInt(String r) {
		r = r.trim();
		if (r.equals("r0"))return 0;
		if (r.equals("r1"))return 1;
		if (r.equals("r2"))return 2;
		if (r.equals("r3"))return 3;
		if (r.equals("r4"))return 4;
		if (r.equals("r5"))return 5;
		if (r.equals("r6"))return 6;
		if (r.equals("r7"))return 7;
		if (r.equals("r8"))return 8;
		if (r.equals("r9"))return 9;
		if (r.equals("r10"))return 10;
		if (r.equals("r11"))return 11;
		if (r.equals("r12"))return 12;
		if (r.equals("r13"))return 13;
		if (r.equals("r14"))return 14;
		if (r.equals("r15"))return 15;
		if (r.equals("sp"))return 13;
		if (r.equals("lr"))return 14;
		if (r.equals("pc"))return 15;
		if (r.equals("fp"))return 11;
		return -1;
	}
	
	public static String replaceChar(String str, char ch, int index) {
	    StringBuilder myString = new StringBuilder(str);
	    myString.setCharAt(index, ch);
	    return myString.toString();
	}

} 
