package Imm.ASM.Util;

public class Cond {

	public enum COND {
		EQ,
		NE,
		CS,
		CC,
		MI,
		PL,
		VS,
		VC,
		HI,
		LS,
		GE,
		LT,
		GT,
		LE,
		AL
	}
	
	public COND cond;
	
	public Cond(COND cond) {
		this.cond = cond;
	}
	
	public String getCondPostfix() {
		return this.cond.toString().toLowerCase();
	}
	
}
