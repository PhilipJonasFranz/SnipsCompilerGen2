package Imm.ASM.Util;

public class Cond {

			/* ---< NESTED >--- */
	/**
	 * Condition mnemonics for the assembly instructions.
	 */
	public enum COND {
		EQ, NE, CS, CC,
		MI, PL, VS, VC,
		HI, LS, GE, LT,
		GT, LE, AL, NO;
	}
	
	
			/* ---< FIELDS >--- */
	public COND cond;
	
	
			/* ---< CONSTRUCTORS >--- */
	public Cond(COND cond) {
		this.cond = cond;
	}
	
	
			/* ---< METHODS >--- */
	public String getCondPostfix() {
		return this.cond.toString().toLowerCase();
	}
	
} 
