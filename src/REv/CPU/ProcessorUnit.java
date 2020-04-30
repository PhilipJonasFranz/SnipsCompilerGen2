package REv.CPU;

import REv.Devices.Device;
import REv.Modules.Tools.Util;

/**
 * ARM Software Processor 32 bit - SWARMPC32
 */
public class ProcessorUnit {

	/* Debug */
	public int debug = 1, step = 0, cpuExit = 0;
	
	/* Bit Mode */
	public final int BIT = 1 << 5;
	
	/* Devices connected via databus */
	public int memSize = 0;
	public Device [] busDevices;
	public int [] [] [] memoryBlocks;
	
	/* Registers */
	public int [] [] regs  	= new int [1 << 4] [BIT];
	public int    [] cpsr  	= new int          [BIT];
	
	/* Data Streams */
	public int [] instr = new int [BIT];
	public int [] srcA, srcB, srcC, shiftOut, imm, result, readData, writeback;
	
	// Operands
	public int [] rN, rM, rD;
	public int [] rS 			= new int [4];
	
	/* Constants */
	public int [] r15        	= new int [] {1, 1, 1, 1};
	public int [] add0       	= toBinary(0);
	public int [] add1       	= toBinary(1);
	public int [] add4       	= toBinary(4);
	
	public int [] map0       	= toBinary(255);

	/* Control Flags */
	public int pcSrc, memToReg, memWrite, shift, regWrite, srcOvr;
	
	/* ALU Flags */
	public int aluSrc, immSrc, setCond, n0, n1, carry, accum;
	
	/* Memory Flags */
	public int enMem, bLink, wb, quantityBit, upDownBit, indexBit;
	
	public int [] resSrc		= new int [1];
	public int [] regSrc     	= new int [1 << 1];
	public int [] aluControl   	= new int [1 << 2];
	
	
	/* Main Processor */
	public ProcessorUnit(Device [] devices) {
		busDevices = devices;
		
		memoryBlocks = new int [busDevices.length] [] [];
		
		int c = 0;

		// Swap references of memory blocks, create reference in Device
		for (Device d : busDevices) {
			d.connect(this, memSize);
			memSize += d.memorySize;
			memoryBlocks [c] = d.internalMemory.clone();
			d.internalMemory = memoryBlocks [c];
			c++;
		}
		
		regs [11] = toBinary(busDevices [0].memorySize << 2);
		regs [13] = toBinary(busDevices [0].memorySize << 2);
		regs [14] = toBinary(-8);
	}
	
	public void execute() {
		while (true) {
			// PC out of bounds
			int pcD = toDecimal(regs [15]);
			if (pcD >> 2 >= memSize || pcD < 0) {
				cpuExit = 1;
				break;
			};
			
			// Reset all control flags set in previous cycle
			resetFlags();
			
			// Fetch Instruction
			instr = readMem(regs [15]);
			
			// Check Predication
			if (checkCondition() == 1) {
				decodeInstruction();
				
				// Get operands rn, rd, rm
				if (srcOvr == 0) {
					rN = (regSrc [0] == 0)? new int [] {instr [19], instr [18], instr [17], instr [16]} : r15;
					rD =                    new int [] {instr [15], instr [14], instr [13], instr [12]};
					rM = (regSrc [1] == 0)? new int [] {instr  [3], instr  [2], instr  [1], instr  [0]} : rD;
				}
				
				int rNwb = toDecimal(rN);
				
				// Memory Access, Pre-Indexed
				if (enMem == 1 && indexBit == 0) {
					if (memToReg == 1)readData = readMem(regs [rNwb].clone());
					if (memWrite == 1)writeMem(regs [toDecimal(rN)].clone(), regs [toDecimal(rD)].clone());
				}
					
				// Get operands for alu
				srcA = regs [rNwb].clone();
				srcB = (aluSrc == 0)? regs [toDecimal(rM)].clone() : imm.clone();
				srcC = regs [toDecimal(rS)].clone();
				
				// Process Shift
				if (shift == 1)processShift();
				
				// Execute Alu
				if (resSrc [0] == 0)executeAlu();
				else result = mul(srcB, srcC, srcA);
				
				if (setCond == 1)updateNZCV();

				// Memory, Post-Indexed
				if (enMem == 1) {
					if (wb == 1)regs [rNwb] = result;
					if (indexBit == 1) {
						if (memToReg == 1)readData = readMem(result);
						if (memWrite == 1)writeMem(result, regs [toDecimal(rD)]);
					}
				}
				
				// Writeback
				writeback = (memToReg == 1)? readData : result;
				if (regWrite == 1) {
					int rDd = toDecimal(rD);
					regs [rDd] = writeback;
					if (rDd == 15)pcSrc = 1;
				}
				if (bLink == 1)regs [14] = add(add4, regs [15]);
				if (pcSrc == 1)regs [15] = writeback;
			}
			
			// Debug Timeout Loop
			if (debug == 1)Util.sleep();
			while (step == 0 && debug == 1)Util.sleep();
			if (debug == 1)step = 0;
			
			// Update PC
			if (pcSrc == 0)regs [15] = add(regs [15], add4);
		}
	}
	
	
	/* Control Unit */
	public int checkCondition() {
		if (instr [31] == 0) {
    		if (instr [30] == 0) {
    			if (instr [29] == 0) {
    				if (instr [28] == 0)return Z();
            		else return                1 - Z();
    			}
    			else {
    				if (instr [28] == 0)return C();
    				else return                1 - C();
    			}
    		}
    		else {
    			if (instr [29] == 0) {
    				if (instr [28] == 0)return N();
    				else return                1 - N();
    			}
    			else {
    				if (instr [28] == 0)return V();
    				else return                1 - V();
    			}
    		}
    	}
    	else {
    		if (instr [30] == 0) {
    			if (instr [29] == 0) {
    				if (instr [28] == 1)return (C() == 0 || Z() == 1)? 1 : 0;
    				else return                (C() == 1 && Z() == 0)? 1 : 0;
    			}
    			else {
    				if (instr [28] == 1)return (N() != V())? 1 : 0;
    				else return                (N() == V())? 1 : 0;
    			}
    		}
    		else {
    			if (instr [29] == 0) {
    				if (instr [28] == 0)return (Z() == 0 && (N() == V()))? 1 : 0;
    				else return                (Z() == 1 || (N() != V()))? 1 : 0;
    			}
    			else return                    1;
    		}
    	}
	}
	
	public void resetFlags() {
		pcSrc = 0;
		bLink = 0;
		memToReg = 0;
		memWrite = 0;
		regWrite = 0;
		regSrc [0] = 0;
		setCond = 0;
		shift = 0;
		srcOvr = 0;
		resSrc [0] = 0;
		enMem = 0;
		quantityBit = 0;
		wb = 0;
	}
	
	public void decodeInstruction() {
		// Branch and Exchange
		if (instr [27] == 0 && instr [26] == 0 && instr [25] == 0 && instr [24] == 1 && instr [23] == 0 && instr [22] == 0 && instr [21] == 1 && instr [20] == 0 && instr [19] == 1 && instr [18] == 1 && instr [17] == 1 && instr [16] == 1 && instr [15] == 1 && instr [14] == 1 && instr [13] == 1 && instr [12] == 1 && instr [11] == 1 && instr [10] == 1 && instr [9] == 1 && instr [8] == 1) {
			pcSrc = 1;
			regSrc [1] = 0;
			aluSrc = 0;
			aluControl = new int [] {1, 1, 0, 1};
		}
		// 101 - Branch
		else if (instr [27] == 1 && instr [26] == 0 && instr [25] == 1) {
			pcSrc = 1;
			regSrc [0] = 1;
			aluSrc = 1;
			immSrc = 1;
			imm = new int [] {0, 0, 0, 0, 0, 0, 0, 0, instr [23], instr [22], instr [21], instr [20], instr [19], instr [18], instr [17], instr [16], instr [15], instr [14], instr [13], instr [12], instr [11], instr [10], instr [9], instr [8], instr [7], instr [6], instr [5], instr [4], instr [3], instr [2], instr [1], instr [0]};
			aluControl = new int [] {1, 1, 0, 1};
			bLink = instr [24];
		}
		// Single Data Swap (SWP)
		else if (instr [27] == 0 && instr [26] == 0 && instr [25] == 0 && instr [24] == 1 && instr [23] == 0 && instr [21] == 0 && instr [11] == 0 && instr [10] == 0 && instr [9] == 0 && instr [8] == 0 && instr [7] == 1 && instr [6] == 0 && instr [5] == 0 && instr [4] == 1) {
			enMem = 1;
			aluSrc = 1; // Force Immediate
			regWrite = 1;
			memToReg = 1;
			memWrite = 1;
			immSrc = 1;
			imm = add0; // Force 0 immediate 
			aluControl = new int [] {0, 1, 0, 0};
			indexBit = 0;
			quantityBit = instr [22];
		}
		// mul, mla
		else if (instr [27] == 0 && instr [26] == 0 && instr [25] == 0 && instr [24] == 0 && instr [27] == 0 && instr [26] == 0 && instr [7] == 1 && instr [6] == 0 && instr [5] == 0 && instr [4] == 1) {
			srcOvr = 1;
			rD = new int [] {instr [19], instr [18], instr [17], instr [16]};
			rN = new int [] {instr [15], instr [14], instr [13], instr [12]};
			rS = new int [] {instr [11], instr [10], instr [ 9], instr [ 8]};
			rM = new int [] {instr [ 3], instr [ 2], instr [ 1], instr [ 0]};
			
			setCond = instr [20];
			accum = instr [21];
			
			regWrite = 1;
			resSrc [0] = 1;
			aluSrc = 0;
		}
		// 00 - Data Processing
		else if (instr [27] == 0 && instr [26] == 0) {
			regWrite = 1;
			aluControl = new int [] {instr [24], instr [23], instr [22], instr [21]};
			setCond = instr [20];
			
			if (instr [25] == 1) { // Immediate
				immSrc = 1;
				aluSrc = 1;
				imm = new int [] {0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, instr [7], instr [6], instr [5], instr [4], instr [3], instr [2], instr [1], instr [0]};
			}
			else { // Register
				immSrc = 0;
				aluSrc = 0;
				regSrc [1] = 0;
				shift = 1;
			}
		}
		// 01 - Memory
		else if (instr [27] == 0 && instr [26] == 1) {
			enMem = 1;
			aluSrc = 0;
			regWrite = instr [20];
			if (regWrite == 1)memToReg = 1;
			memWrite = 1 - regWrite;
			immSrc = 1 - instr [25];
			wb = instr [21];
			aluControl = (instr [23] == 0)? new int [] {0, 0, 1, 0} : new int [] {0, 1, 0, 0};
			indexBit = instr [24];
			
			if (immSrc == 1) {
				aluSrc = 1;
				imm = new int [] {0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, instr [11], instr [10], instr [9], instr [8], instr [7], instr [6], instr [5], instr [4], instr [3], instr [2], instr [1], instr [0]};
			}
			else shift = 1;
		}
	}
	
	
	/* Memory */
    public void writeMem(int [] addr, int [] din) {
    	int address = toDecimal(addr) >> 2;
		for (int i = 0; i < memoryBlocks.length; i++) {
			if (memoryBlocks [i].length > address) {
    			memoryBlocks [i] [address] = din;
    			return;
    		}
    		address -= memoryBlocks [i].length;
    	}
    }

    public int [] readMem(int [] addr) {
    	int address = toDecimal(addr), acc = 0;
    	int off = (address % 4) << 3;
    	address = address >> 2;
    	for (int i = 0; i < memoryBlocks.length; i++) {
    		if (acc + memoryBlocks [i].length > address) {
    			if (quantityBit == 0)return memoryBlocks [i] [address - acc].clone();
    			else {
    				int [] readData = memoryBlocks [i] [address - acc].clone();
    				if (off > 0) {
    					for (int a = BIT - 1; a >= 0; a--) {
    						if (a - off >= 0)readData [a] = readData [a - off];
    						else readData [a] = 0;
    					}
    				}
    				return and(readData, map0);
    			}
    		}
    		acc += memoryBlocks [i].length;
    	}
    	return add0;
    }
  
    
    /* ALU Control */
    public void executeAlu() {
    	if (aluControl [0] == 0) {
    		if (aluControl [1] == 0) {
    			if (aluControl [2] == 0) {
    				if (aluControl [3] == 0)result = and(srcA, srcB); // a & b
            		else result =                    eor(srcA, srcB); // a ^ b
    			}
    			else {
    				if (aluControl [3] == 0)result = sub(srcA, srcB); // a - b
    				else result =                    sub(srcB, srcA); // b - a
    			}
    		}
    		else {
    			if (aluControl [2] == 0) {
    				if (aluControl [3] == 0)result = add(srcA, srcB); // a + b
    				else result =                    adc(srcA, srcB); // a + b + c
    			}
    			else {
    				if (aluControl [3] == 0)result = sbc(srcA, srcB); // a - b + c - 1
    				else result =                    sbc(srcB, srcA); // b - a + c - 1
    			}
    		}
    	}
    	else {
    		if (aluControl [1] == 0) {
    			if (aluControl [2] == 0) {
    				if (aluControl [3] == 0)result = tst(srcA, srcB); // a & b: flag
    				else result =                    teq(srcA, srcB); // a ^ b: flag
    			}
    			else {
    				if (aluControl [3] == 0)result = cmp(srcA, srcB); // a - b: flag
    				else result =                    cmn(srcA, srcB); // a + b: flag
    			}
    		}
    		else {
    			if (aluControl [2] == 0) {
    				if (aluControl [3] == 0)result = orr(srcA, srcB); // a | b
    				else result =                    mov(srcA, srcB); // b
    			}
    			else {
    				if (aluControl [3] == 0)result = bic(srcA, srcB); // 1 - (a & b)
    				else result =                    mvn(srcA, srcB); // 1 - b
    			}
    		}
    	}
    }
    
    /**
     * Updates the NCZV Flags in the CPSR.
     * @param n0 The MSB of the first operand
     * @param n1 The MSB of the second operand
     * @param result The Result of the Arithmetic operation
     * @param carry The Carry out bit of the operation
     */
    public void updateNZCV() {
    	cpsr [31] = (result [0] == 1)? 1 : 0;                          // Negative
    	cpsr [30] = (toDecimal(result) == 0)? 1 : 0;                   // Zero
    	cpsr [29] = carry;                                             // Carry
    	cpsr [28] = ((1 - (n0 ^ n1) == 1 && result [0] != n0))? 1 : 0; // Overflow
    }
    
    public int N() { return cpsr [31]; } // Returns the Negative Flag 
    public int Z() { return cpsr [30]; } // Returns the Zero Flag
    public int C() { return cpsr [29]; } // Returns the Carry Flag
    public int V() { return cpsr [28]; } // Returns the Overflow Flag
    
    
    /* Arithmetic Operations */
    public int [] and(int [] num0, int [] num1) {
    	int [] result = new int [BIT];
    	for (int i = 0; i < BIT; i++)result [i] = num0 [i] & num1 [i];
    	return result;
    }
    
    public int [] eor(int [] num0, int [] num1) {
    	int [] result = new int [BIT];
    	for (int i = 0; i < BIT; i++)result [i] = num0 [i] ^ num1 [i];
    	return result;
    }
    
    public int [] orr(int [] num0, int [] num1) {
    	int [] result = new int [BIT];
    	for (int i = 0; i < BIT; i++)result [i] = num0 [i] | num1 [i];
    	return result;
    }
    
    public int [] bic(int [] num0, int [] num1) {
    	int [] result = new int [BIT];
    	for (int i = 0; i < BIT; i++)result [i] = 1 - (num0 [i] & num1 [i]);
    	return result;
    }
    
    public int [] sub(int [] num0, int [] num1) {
    	int [] result = add(num0, inv2K(num1));
    	n0 = num0 [0];
    	n1 = num1 [0];
    	return result;
    }
    
    public int [] add(int [] num0, int [] num1) {
    	int [] result = new int [BIT];
    	result [BIT - 1] = num0 [BIT - 1] ^ num1 [BIT - 1];
    	int c = num0 [BIT - 1] & num1 [BIT - 1];

    	int r0;
    	for (int i = BIT - 2; i >= 0; i--) {
    		r0 = num0 [i] ^ num1 [i];
    		result [i] = r0 ^ c;
    		c = (num0 [i] & num1 [i]) | (r0 & c);
    	}
    	
    	carry = c;
    	n0 = num0 [0];
    	n1 = num1 [0];
    	return result;
    }
    
    public int [] sbc(int [] num0, int [] num1) {
    	int [] result = sub(num0, num1);
    	result = add(result, (C() == 1)? add1 : add0);
    	result = sub(result, add1);
    	return result;
    }
    
    public int [] adc(int [] num0, int [] num1) {
    	int [] result = add(num1, (carry == 1)? add1 : add0);
    	result [BIT - 1] = num0 [BIT - 1] ^ num1 [BIT - 1];
    	int c = num0 [BIT - 1] & num1 [BIT - 1];

    	int r0;
    	for (int i = BIT - 2; i >= 0; i--) {
    		r0 = num0 [i] ^ num1 [i];
    		result [i] = r0 ^ c;
    		c = (num0 [i] & num1 [i]) | (r0 & c);
    	}
    	
    	carry = c;
    	n0 = num0 [0];
    	n1 = num1 [0];
    	return result;
    }
    
    public int [] tst(int [] num0, int [] num1) {
    	setCond = 1;
    	regWrite = 0;
    	return sub(num0, num1);
    }
    
    public int [] teq(int [] num0, int [] num1) {
    	setCond = 1;
    	regWrite = 0;
    	return eor(num0, num1);
    }
    
    public int [] cmp(int [] num0, int [] num1) {
    	setCond = 1;
    	regWrite = 0;
    	return sub(num0, num1);
    }
    
    public int [] cmn(int [] num0, int [] num1) {
    	setCond = 1;
    	regWrite = 0;
    	return add(num0, num1);
    }
    
    public int [] mov(int [] num0, int [] num1) {
    	return num1;
    }
    
    public int [] mvn(int [] num0, int [] num1) {
    	return this.inv2K(this.add(num1.clone(), this.add1));
    }
    
    public int [] mul(int [] num0, int [] num1, int [] num2) {
    	int [] num = num0.clone(), r = new int [BIT];
    	for (int i = BIT - 1; i >= 0; i--) {
    		if (num1 [i] == 1)r = add(r, num);
    		for (int a = 0; a < BIT; a++) {
    			if (a + 1 < BIT)num [a] = num [a + 1];
    			else num [a] = 0;
    		}
    	}
    	
    	return (accum == 1)? add(r, num2) : r;
    }
    
    
	/* Shifts */
	public void processShift() {
		int amount = 0;
		if (instr [4] == 0) { 
			// Shift by immediate
			amount = toDecimal(new int [] {instr [11], instr [10], instr [9], instr [8], instr [7]});
		}
		else { 
			// Shift by shift register
			amount = toDecimal(regs [toDecimal(new int [] {instr [11], instr [10], instr [9], instr [8]})]);
		}
		
		// Get shift operand
		if (instr [6] == 0) {
			if (instr [5] == 0)	lsl(amount);
			else                lsr(amount);
		}
		else {
			if (instr [5] == 0)	asr(amount);
			else                ror(amount);
		}
	}
	
	public void lsl(int amount) {
		for (int i = 0; i < BIT; i++) {
			if (i + amount < BIT)srcB [i] = srcB [i + amount];
			else srcB [i] = 0;
		}
	}
	
	public void lsr(int amount) {
		for (int i = BIT - 1; i >= 0; i--) {
			if (i - amount >= 0)srcB [i] = srcB [i - amount];
			else srcB [i] = 0;
		}
	}
	
	public void asr(int amount) {
		for (int i = BIT - 1; i >= 0; i--) {
			if (i - amount >= 0)srcB [i] = srcB [i - amount];
			else srcB [i] = srcB [0];
		}
	}
	
	public void ror(int amount) {
		int [] r = new int [BIT];
		for (int i = BIT - 1; i >= 0; i--) {
			int in = i - amount;
			if (in < 0)in += BIT;
			r [i] = srcB [in];
		}
		srcB = r;
	}
    
    
    /* Misc */
    public int toDecimal(int [] num) {
    	int s = 0;
    	int c = num.length;
    	for (int i : num)s += i << --c;
    	return s;
    }
    
    
    /* Conversions */
    public int [] toBinary(int num) {
    	int isNegative = (num < 0)? 1 : 0;
    	num *= (isNegative == 1)? -1 : 1;
    	
    	int [] r = new int [BIT];
    	for (int i = BIT - 1; i >= 0 && num > 0; i--) {
    		if (num >= 1 << (i - 1)) {
    			r [BIT - i] = 1;
    			num -= 1 << (i - 1);
    		}
    	}
    	
    	return (isNegative == 1)? inv2K(r) : r;
    }
    
    public int [] inv2K(int [] num) {
    	for (int i = 0; i < BIT; i++)num [i] = 1 - num [i];
    	return add(num, add1);
    }
    
}