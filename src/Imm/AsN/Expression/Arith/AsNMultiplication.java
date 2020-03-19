package Imm.AsN.Expression.Arith;

import CGen.RegSet;
import Imm.AST.Expression.Arith.Mul;
import Imm.AsN.Expression.AsNExpression;

public class AsNMultiplication extends AsNExpression {

	public AsNMultiplication() {
		
	}
	
	public static AsNMultiplication cast(Mul m, RegSet r) {
		AsNMultiplication mul = new AsNMultiplication();
		
		mul.build();
		return mul;
	}
	
	protected void build() {
		
	}
	
}
