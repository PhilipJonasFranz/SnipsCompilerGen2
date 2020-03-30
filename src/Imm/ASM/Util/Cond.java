package Imm.ASM.Util;

public class Cond {

			/* --- NESTED --- */
	public enum COND {
		EQ, NE, CS, CC,
		MI, PL, VS, VC,
		HI, LS, GE, LT,
		GT, LE, AL
	}
	
	
			/* --- FIELDS --- */
	public COND cond;
	
	
			/* --- CONSTRUCTORS --- */
	public Cond(COND cond) {
		this.cond = cond;
	}
	
	
			/* --- METHODS --- */
	public String getCondPostfix() {
		return this.cond.toString().toLowerCase();
	}
	
}
