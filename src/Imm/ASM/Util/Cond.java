package Imm.ASM.Util;

import Imm.AST.Expression.Boolean.Compare.COMPARATOR;

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
		
		public static COND toCondition(COMPARATOR c) {
			if (c == COMPARATOR.EQUAL) return COND.EQ;
			else if (c == COMPARATOR.NOT_EQUAL) return COND.NE;
			else if (c == COMPARATOR.GREATER_SAME) return COND.GE;
			else if (c == COMPARATOR.GREATER_THAN) return COND.GT;
			else if (c == COMPARATOR.LESS_SAME) return COND.LE;
			else if (c == COMPARATOR.LESS_THAN) return COND.LT;
			return null;
		}
		
		public COND negate() {
			if (this == COND.EQ) return COND.NE;
			if (this == COND.NE) return COND.EQ;
			if (this == COND.GE) return COND.LT;
			if (this == COND.GT) return COND.LE;
			if (this == COND.LE) return COND.GT;
			if (this == COND.LT) return COND.GE;
			return null;
		}
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
