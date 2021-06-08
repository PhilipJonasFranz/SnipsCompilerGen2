package REv.CPU;

import Util.FBin;

public class VFPCoProcessor {

	public static boolean DEBUG = true;

	/* 32 * F32 Registers */
	public int [] [] regs = new int [32] [32];
	
	
	public void doInstruction(ProcessorUnit pcu, int [] instr) {

		/* Data-Processing Operation */
		if (instr [27] == 1 && instr [26] == 1 && instr [25] == 1 && instr [24] == 0 && instr [4] == 0) {

			/* Decode Instruction */
			int [] Fd0 = new int [] {instr [15], instr [14], instr [13], instr [12]};
			int [] Fn0 = new int [] {instr [19], instr [18], instr [17], instr [16]};
			int [] Fm0 = new int [] {instr [4], instr [2], instr [1], instr [0]};
			
			int D = instr [22];
			int N = instr [7];
			int M = instr [5];
			
			/* Operand register numbers */
			int Fd = FBin.toDecimal(new int [] {Fd0 [0], Fd0 [1], Fd0 [2], Fd0 [3], D});
			String Fds = "S" + Fd;
			int Fn = FBin.toDecimal(new int [] {Fn0 [0], Fn0 [1], Fn0 [2], Fn0 [3], N});
			String Fns = "S" + Fn;
			int Fm = FBin.toDecimal(new int [] {Fm0 [0], Fm0 [1], Fm0 [2], Fm0 [3], M});
			String Fms = "S" + Fm;

			/* Opcode */
			int p = instr [23];
			int q = instr [21];
			int r = instr [20];
			int s = instr [6];

			/* Float values decoded from regs */
			float vFd = FBin.fromFBin(regs [Fd].clone());
			float vFn = FBin.fromFBin(regs [Fn].clone());
			float vFm = FBin.fromFBin(regs [Fm].clone());

			if (DEBUG) System.out.print("Decoded Data-Processing Instruction, operands: " + Fds + " = " + vFd + ", " + Fns + " = " + vFn + ", " + Fms + " = " + vFm + ", ");

			/* Output, will be written to Fd */
			float out = 0;

			/* True if this instruction is a vcvt.S32.F32 instruction */
			boolean instIsToIntConversion = Fn0[0] == 1 && Fn0[1] == 1 && Fn0[2] == 0 && Fn0[3] == 1 && N == 0;

			/* True if this instruction is a vcmp instruction */
			boolean isCMPInstruction = Fn0[0] == 0 && Fn0[1] == 1 && Fn0[2] == 0 && Fn0[3] == 0 && N == 0;

			/* True if this instruction is a vcmp with zero instruction */
			boolean isCMPZInstruction = Fn0[0] == 0 && Fn0[1] == 1 && Fn0[2] == 0 && Fn0[3] == 1 && N == 0;

			/* FMACS Fd = Fd + (Fn * Fm) */
			if (p == 0 && q == 0 && r == 0 && s == 0) {
				out = vFd + (vFn * vFm);
				if (DEBUG) System.out.print(Fds + " = " + Fds + " + (" + Fns + " * " + Fms + ")" + ", ");
			}
			/* FNMACS Fd = Fd - (Fn * Fm) */
			else if (p == 0 && q == 0 && r == 0 && s == 1) {
				out = vFd - (vFn * vFm);
				if (DEBUG) System.out.print(Fds + " = " + Fds + " - (" + Fns + " * " + Fms + ")" + ", ");
			}
			/* FMSCS Fd = -Fd + (Fn * Fm) */
			else if (p == 0 && q == 0 && r == 1 && s == 0) {
				out = -vFd + (vFn * vFm);
				if (DEBUG) System.out.print(Fds + " = " + "-" + Fds + " + (" + Fns + " * " + Fms + ")" + ", ");
			}
			/* FNMSCS Fd = -Fd - (Fn * Fm) */
			else if (p == 0 && q == 0 && r == 1 && s == 1) {
				out = -vFd - (vFn * vFm);
				if (DEBUG) System.out.print(Fds + " = " + "-" + Fds + " - (" + Fns + " * " + Fms + ")" + ", ");
			}
			/* FMULS Fd = Fn * Fm */
			else if (p == 0 && q == 1 && r == 0 && s == 0) {
				out = vFn * vFm;
				if (DEBUG) System.out.print(Fds + " = " + Fns + " * " + Fms + ", ");
			}
			/* FNMULS Fd = -(Fn * Fm) */
			else if (p == 0 && q == 1 && r == 0 && s == 1) {
				out = -(vFn * vFm);
				if (DEBUG) System.out.print(Fds + " = " + "-(" + Fns + " * " + Fms + ")" + ", ");
			}
			/* FADDS Fd = Fn + Fm */
			else if (p == 0 && q == 1 && r == 1 && s == 0) {
				out = vFn + vFm;
				if (DEBUG) System.out.print(Fds + " = " + Fns + " + " + Fms + ", ");
			}
			/* FSUBS Fd = Fn - Fm */
			else if (p == 0 && q == 1 && r == 1 && s == 1) {
				out = vFn - vFm;
				if (DEBUG) System.out.print(Fds + " = " + Fns + " - " + Fms + ", ");
			}
			/* FDIVS Fd = Fn / Fm */
			else if (p == 1 && q == 0 && r == 0 && s == 0) {
				out = vFn / vFm;
				if (DEBUG) System.out.print(Fds + " = " + Fns + " / " + Fms + ", ");
			}
			/* Extension Instructions */
			else if (p == 1 && q == 1 && r == 1 && s == 1) {
				/* FCPYS Fd = Fm */
				if (Fn0 [0] == 0 && Fn0 [1] == 0 && Fn0 [2] == 0 && Fn0 [3] == 0 && N == 0) {
					out = vFm;
					if (DEBUG) System.out.print(Fds + " = " + Fms + ", ");
				}
				/* FABSS Fd = Fm */
				else if (Fn0 [0] == 0 && Fn0 [1] == 0 && Fn0 [2] == 0 && Fn0 [3] == 0 && N == 1) {
					out = (vFm < 0)? -vFm : vFm;
					if (DEBUG) System.out.print(Fds + " = " + "abs(" + Fms + ")" + ", ");
				}
				/* FNEGS Fd = -Fm */
				else if (Fn0 [0] == 0 && Fn0 [1] == 0 && Fn0 [2] == 0 && Fn0 [3] == 1 && N == 0) {
					out = -vFm;
					if (DEBUG) System.out.print(Fds + " = " + "-" + Fms + ", ");
				}
				/* FSQRTS Fd = sqrt(Fm) */
				else if (Fn0 [0] == 0 && Fn0 [1] == 0 && Fn0 [2] == 0 && Fn0 [3] == 1 && N == 1) {
					out = (float) Math.sqrt(vFm);
					if (DEBUG) System.out.print(Fds + " = " + "sqrt(" + Fms + ")" + ", ");
				}
				/* FCMPS Compare Fd with Fm */
				else if (isCMPInstruction) {
					if (DEBUG) System.out.println("CMP");
					if (vFd == vFm) {
						pcu.cpsr [31] = 0; // Negative
						pcu.cpsr [30] = 1; // Zero
						pcu.cpsr [29] = 1; // Carry
						pcu.cpsr [28] = 0; // Overflow
					}
					else if (vFd < vFm) {
						pcu.cpsr [31] = 1;
						pcu.cpsr [30] = 0;
						pcu.cpsr [29] = 0;
						pcu.cpsr [28] = 0;
					}
					else if (vFd > vFm) {
						pcu.cpsr [31] = 0;
						pcu.cpsr [30] = 0;
						pcu.cpsr [29] = 1;
						pcu.cpsr [28] = 0;
					}
					else {
						pcu.cpsr [31] = 0;
						pcu.cpsr [30] = 0;
						pcu.cpsr [29] = 1;
						pcu.cpsr [28] = 1;
					}
				}
				/* FCMPES Compare Fd with Fm */
				else if (Fn0 [0] == 0 && Fn0 [1] == 1 && Fn0 [2] == 0 && Fn0 [3] == 0 && N == 1) {
					// TODO
					throw new RuntimeException("Not implemented!");
				}
				/* FCMPZS Compare Fd with 0 */
				else if (isCMPZInstruction) {
					if (DEBUG) System.out.println("CMPZ");
					if (vFd == 0) {
						pcu.cpsr [31] = 0; // Negative
						pcu.cpsr [30] = 1; // Zero
						pcu.cpsr [29] = 1; // Carry
						pcu.cpsr [28] = 0; // Overflow
					}
					else if (vFd < 0) {
						pcu.cpsr [31] = 1;
						pcu.cpsr [30] = 0;
						pcu.cpsr [29] = 0;
						pcu.cpsr [28] = 0;
					}
					else if (vFd > 0) {
						pcu.cpsr [31] = 0;
						pcu.cpsr [30] = 0;
						pcu.cpsr [29] = 1;
						pcu.cpsr [28] = 0;
					}
					else {
						pcu.cpsr [31] = 0;
						pcu.cpsr [30] = 0;
						pcu.cpsr [29] = 1;
						pcu.cpsr [28] = 1;
					}
				}
				/* FCMPEZS Compare Fd with Fm */
				else if (Fn0 [0] == 0 && Fn0 [1] == 1 && Fn0 [2] == 0 && Fn0 [3] == 1 && N == 1) {
					// TODO
					throw new RuntimeException("Not implemented!");
				}
				/* FSITOS Signed integer -> floating-point conversions */
				else if (Fn0 [0] == 1 && Fn0 [1] == 0 && Fn0 [2] == 0 && Fn0 [3] == 0 && N == 1) {
					out = FBin.toDecimal(regs [Fm].clone());
					if (DEBUG) System.out.print(Fds + " = " + "(float) " + Fms + ", ");
				}
				else if (!instIsToIntConversion)
					throw new RuntimeException("Not implemented!");
			}
			else throw new RuntimeException("Not implemented!");
			
			/* FTOSIS Floating-point -> signed integer conversions */
			if (instIsToIntConversion) {
				/* Special case, output format not encoded in IEEE 754 */
				regs [Fd] = FBin.toBinI((int) vFm);
				if (DEBUG) System.out.print(Fds + " = " + "(int) " + Fms + ", ");
				out = (int) vFm;
			}
			else if (!isCMPInstruction && !isCMPZInstruction)
				regs [Fd] = FBin.toFBin(out);

			if (DEBUG && !isCMPInstruction && !isCMPZInstruction) System.out.println("Result: " + Fds + " = " + out);
		}
		/* Single-Register-Transfer Instruction */
		else if (instr [27] == 1 && instr [26] == 1 && instr [25] == 1 && instr [24] == 0 && instr [4] == 1) {

			if (DEBUG) System.out.print("Decoded Single-Register-Transfer Instruction: ");

			/* Decode Instruction */
			int Rd = FBin.toDecimal(new int [] {instr [15], instr [14], instr [13], instr [12]});
			String Rds = "R" + Rd;

			/* Operand register numbers */
			int Fn = FBin.toDecimal(new int [] {instr [19], instr [18], instr [17], instr [16], instr [7]});
			String Fns = "S" + Fn;

			/* Opcode */
			int [] opcode = new int [] {instr [23], instr [22], instr [21]};

			int L = instr [20];

			/* Sn = Rd */
			if (opcode [0] == 0 && opcode [1] == 0 && opcode [2] == 0 && L == 0) {
				this.regs [Fn] = pcu.regs [Rd].clone();
				if (DEBUG) System.out.println(Fns + " = " + Rds + ", result: " + Fns + " = " + FBin.toDecimal(this.regs [Fn].clone()));
			}
			/* Rd = Sn */
			else if (opcode [0] == 0 && opcode [1] == 0 && opcode [2] == 0 && L == 1) {
				pcu.regs [Rd] = this.regs [Fn].clone();
				if (DEBUG) System.out.println(Rds + " = " + Fns + ", result: " + Rds + " = " + FBin.toDecimal(pcu.regs [Rd].clone()));
			}
			else throw new RuntimeException("Not implemented!");
		}
		/* Load / Store Instruction */
		else if (instr [27] == 1 && instr [26] == 1 && instr [25] == 0) {

			if (DEBUG) System.out.print("Decoded LDR/STR Instruction: ");

			/* Decode Instruction */
			int [] Fd0 = new int [] {instr [15], instr [14], instr [13], instr [12]};
			int D = instr [22];

			/* Operand register numbers */
			int Fd = FBin.toDecimal(new int [] {Fd0 [0], Fd0 [1], Fd0 [2], Fd0 [3], D});
			String Fds = "S" + Fd;
			int Rn = FBin.toDecimal(new int [] {instr [19], instr [18], instr [17], instr [16]});
			String Rns = "R" + Rn;

			/* Opcode */
			int P = instr [24];
			int U = instr [23];
			int W = instr [21];
			int L = instr [20];

			int vRn = FBin.toDecimal(pcu.regs [Rn].clone());
			int offset = FBin.toDecimal(new int [] {instr [7], instr [6], instr [5], instr [4], instr [3], instr [2], instr [1], instr [0]}) * 4;

			int addr = vRn;

			if (P == 1) {
				/* Pre-indexed addressing */
				if (U == 1) addr += offset;
				else addr -= offset;
			}

			int [] addrBin = FBin.toBinI(addr);

			if (L == 1) {
				/* Load Value */
				int [] value = pcu.readMem(addrBin.clone());
				this.regs [Fd] = value.clone();
				if (DEBUG) System.out.print("Loading " + Fds + " from " + addr + " = " + FBin.toDecimal(value) + " " + offset);
			}
			else {
				/* Store Value */
				pcu.writeMem(addrBin, regs [Fd].clone());
				if (DEBUG) System.out.print("Storing " + Fds + " = " + FBin.toDecimal(regs [Fd].clone()) + " to " + addr);
			}

			if (W == 1) {
				if (P == 0) {
					/* Post-indexed addressing */
					if (U == 1) addr += offset;
					else addr -= offset;
					addrBin = FBin.toBinI(addr);
				}

				/* Writeback to RN */
				pcu.regs [Rn] = addrBin;
				if (DEBUG) System.out.println(", Writeback: " + Rns + " = " + FBin.toDecimal(addrBin));
			}
			else if (DEBUG) System.out.println();
		}
		else throw new RuntimeException("Not implemented!");
	}
	
}
