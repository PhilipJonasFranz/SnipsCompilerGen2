package Imm.AsN.Expression.Arith;

import CGen.RegSet;
import Imm.AST.Expression.Arith.Sub;
import Imm.AsN.Expression.AsNExpression;

public class AsNSubtraction extends AsNExpression {

	public AsNSubtraction() {
		
	}
	
	public static AsNSubtraction cast(Sub s, RegSet r) {
		AsNSubtraction sub = new AsNSubtraction();
		
		sub.build();
		return sub;
	}
	
	protected void build() {
		
	}
	
}
