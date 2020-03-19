package Imm.AsN.Expression.Arith;

import CGen.RegSet;
import Imm.AST.Expression.Arith.Sub;
import Imm.AsN.Expression.AsNExpression;

public class AsNSub extends AsNExpression {

	public AsNSub() {
		
	}
	
	public static AsNSub cast(Sub s, RegSet r) {
		AsNSub sub = new AsNSub();
		
		sub.build();
		return sub;
	}
	
	protected void build() {
		
	}
	
}
