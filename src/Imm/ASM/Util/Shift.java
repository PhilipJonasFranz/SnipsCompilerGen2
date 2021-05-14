package Imm.ASM.Util;

import Imm.ASM.Util.Operands.Operand;

public class Shift {

			/* ---< NESTED >--- */
	public enum SHIFT {
		LSL, LSR, ASR, ROR
	}
	
	
			/* ---< FIELDS >--- */
	public SHIFT shift;
	
	public Operand distance;
	
	
			/* ---< CONSTRUCTORS >--- */
	public Shift(SHIFT shift, Operand distance) {
		this.shift = shift;
		this.distance = distance;
	}
	
	
			/* ---< METHODS >--- */
	public String getShiftPostfix() {
		return ", " + this.shift.toString().toLowerCase() + " " + this.distance.toString();
	}
	
	public Shift clone() {
		return new Shift(this.shift, this.distance.clone());
	}
	
} 
