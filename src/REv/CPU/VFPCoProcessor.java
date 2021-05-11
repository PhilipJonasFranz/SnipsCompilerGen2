package REv.CPU;

import Util.FBinConverter;

public class VFPCoProcessor {

	/* 32 * F32 Registers */
	int [] [] regs = new int [32] [32];
	
	
	public void doInstruction(ProcessorUnit pcu, int [] instr) {
		/* Data-Processing Operation */
		if (instr [27] == 1 && instr [26] == 1 && instr [25] == 1 && instr [24] == 0) {
			
			/* Decode Instruction */
			int [] Fd0 = new int [] {instr [15], instr [14], instr [13], instr [12]};
			int [] Fn0 = new int [] {instr [19], instr [18], instr [17], instr [16]};
			int [] Fm0 = new int [] {instr [4], instr [2], instr [1], instr [0]};
			
			int D = instr [22];
			int N = instr [7];
			int M = instr [5];
			
			/* Operand register numbers */
			int Fd = FBinConverter.toDecimal(new int [] {Fd0 [0], Fd0 [1], Fd0 [2], Fd0 [3], D});
			int Fn = FBinConverter.toDecimal(new int [] {Fn0 [0], Fn0 [1], Fn0 [2], Fn0 [3], N});
			int Fm = FBinConverter.toDecimal(new int [] {Fm0 [0], Fm0 [1], Fm0 [2], Fm0 [3], M});
			
			/* Opcode */
			int p = instr [23];
			int q = instr [21];
			int r = instr [20];
			int s = instr [6];
			
			/* Float values decoded from regs */
			float vFd = FBinConverter.fromFBin(regs [Fd].clone());
			float vFn = FBinConverter.fromFBin(regs [Fn].clone());
			float vFm = FBinConverter.fromFBin(regs [Fm].clone());
			
			/* Output, will be written to Fd */
			float out = 0;
			
			/* FMACS Fd = Fd + (Fn * Fm) */
			if (p == 0 && q == 0 && r == 0 && s == 0) {
				out = vFd + (vFn * vFm);
			}
			/* FNMACS Fd = Fd - (Fn * Fm) */
			else if (p == 0 && q == 0 && r == 0 && s == 1) {
				out = vFd - (vFn * vFm);
			}
			/* FMSCS Fd = -Fd + (Fn * Fm) */
			else if (p == 0 && q == 0 && r == 1 && s == 0) {
				out = -vFd + (vFn * vFm);
			}
			/* FNMSCS Fd = -Fd - (Fn * Fm) */
			else if (p == 0 && q == 0 && r == 1 && s == 1) {
				out = -vFd - (vFn * vFm);
			}
			/* FMULS Fd = Fn * Fm */
			else if (p == 0 && q == 1 && r == 0 && s == 0) {
				out = vFn * vFm;
			}
			/* FNMULS Fd = -(Fn * Fm) */
			else if (p == 0 && q == 1 && r == 0 && s == 1) {
				out = -(vFn * vFm);
			}
			/* FADDS Fd = Fn + Fm */
			else if (p == 0 && q == 1 && r == 1 && s == 0) {
				out = vFn + vFm;
			}
			/* FSUBS Fd = Fn - Fm */
			else if (p == 0 && q == 1 && r == 1 && s == 1) {
				out = vFn - vFm;
			}
			/* FDIVS Fd = Fn / Fm */
			else if (p == 1 && q == 0 && r == 0 && s == 0) {
				out = vFn / vFm;
			}
			/* Extension Instructions */
			else if (p == 1 && q == 1 && r == 1 && s == 1) {
				/* FCPYS Fd = Fm */
				if (Fn0 [0] == 0 && Fn0 [1] == 0 && Fn0 [2] == 0 && Fn0 [3] == 0 && N == 0) {
					out = vFm;
				}
				/* FABSS Fd = Fm */
				else if (Fn0 [0] == 0 && Fn0 [1] == 0 && Fn0 [2] == 0 && Fn0 [3] == 0 && N == 1) {
					out = (vFm < 0)? -vFm : vFm;
				}
				/* FNEGS Fd = -Fm */
				else if (Fn0 [0] == 0 && Fn0 [1] == 0 && Fn0 [2] == 0 && Fn0 [3] == 1 && N == 0) {
					out = -vFm;
				}
				/* FSQRTS Fd = sqrt(Fm) */
				else if (Fn0 [0] == 0 && Fn0 [1] == 0 && Fn0 [2] == 0 && Fn0 [3] == 1 && N == 1) {
					out = (float) Math.sqrt(vFm);
				}
				/* FCMPS Compare Fd with Fm */
				else if (Fn0 [0] == 0 && Fn0 [1] == 1 && Fn0 [2] == 0 && Fn0 [3] == 0 && N == 0) {
					// TODO
				}
				/* FCMPES Compare Fd with Fm */
				else if (Fn0 [0] == 0 && Fn0 [1] == 1 && Fn0 [2] == 0 && Fn0 [3] == 0 && N == 1) {
					// TODO
				}
				/* FCMPZS Compare Fd with Fm */
				else if (Fn0 [0] == 0 && Fn0 [1] == 1 && Fn0 [2] == 0 && Fn0 [3] == 1 && N == 0) {
					// TODO
				}
				/* FCMPEZS Compare Fd with Fm */
				else if (Fn0 [0] == 0 && Fn0 [1] == 1 && Fn0 [2] == 0 && Fn0 [3] == 1 && N == 1) {
					// TODO
				}
				/* FSITOS Signed integer -> floating-point conversions */
				else if (Fn0 [0] == 1 && Fn0 [1] == 0 && Fn0 [2] == 0 && Fn0 [3] == 0 && N == 1) {
					out = FBinConverter.toDecimal(regs [Fm].clone());
				}
			}
			
			/* FTOSIS Floating-point -> signed integer conversions */
			if (Fn0 [0] == 1 && Fn0 [1] == 1 && Fn0 [2] == 0 && Fn0 [3] == 1 && N == 0) {
				/* Special case, output format not encoded in IEEE 754 */
				regs [Fd] = FBinConverter.toBinI((int) vFm);
			}
			else regs [Fd] = FBinConverter.toFBin(out);
		}
	}
	
}
