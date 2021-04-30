package Lnt.Rules;

import java.util.Arrays;
import java.util.List;

import Imm.AST.Program;
import Imm.AST.Expression.InlineCall;
import Imm.AST.Expression.TypeCast;
import Lnt.LRule;
import Util.Logging.LogPoint.Type;
import Util.Logging.Message;

public class ImplicitAnonymousTypeLRule extends LRule {

	private List<TypeCast> result;
	
	public void getResults(Program AST) {
		result = AST.visit(x -> {
			/* Dereferencing a primitive can be a valid statement, but it can be unsafe. A pointer would be safer. */
			if (x instanceof TypeCast) {
				TypeCast tc = (TypeCast) x;
				if (tc.expression instanceof InlineCall) {
					InlineCall ic = (InlineCall) tc.expression;
					return ic.calledFunction == null;
				}
			}
			
			return false;
		});
	}

	public void reportResults() {
		for (TypeCast s : this.result) {
			new Message("Using implicit anonymous type '" + s.getType().codeString() + "', " + s.getSource().getSourceMarker(), Type.WARN);
			String code = "    " + s.codePrint();
			this.printResultSourceCode(Arrays.asList(code));
		}
	}

}
