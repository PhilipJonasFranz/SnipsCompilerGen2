package Imm.AsN.Expression.Arith;

import CGen.RegSet;
import Exc.CGEN_EXCEPTION;
import Imm.AST.Expression.Arith.Sub;
import Imm.AsN.Expression.AsNExpression;

public class AsNSub extends AsNExpression {

	public AsNSub() {
		
	}
	
	public static AsNSub cast(Sub s, RegSet r) throws CGEN_EXCEPTION {
		AsNSub sub = new AsNSub();
	
		return sub;
	}
	
}
