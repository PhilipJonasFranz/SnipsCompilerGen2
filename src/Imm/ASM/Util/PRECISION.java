package Imm.ASM.Util;

public enum PRECISION {
	
	/* Single 32-Bit Integer */
	S32,
	
	/* Single 32-Bit Float */
	F32,
	
	/* Internal use, used to denote no precision postfix */
	__NONE;
	
	public String toString() {
		if (this == PRECISION.__NONE) return "";
		else return "." + this.name();
	}
	
}
