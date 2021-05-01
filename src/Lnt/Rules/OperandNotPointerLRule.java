package Lnt.Rules;

import java.util.ArrayList;
import java.util.List;

import Ctx.Util.CheckUtil.Callee;
import Imm.AST.Program;
import Imm.AST.SyntaxElement;
import Imm.AST.Expression.Deref;
import Imm.AST.Expression.Expression;
import Imm.AST.Expression.InlineCall;
import Imm.AST.Statement.FunctionCall;
import Lnt.LRule;
import Util.Logging.LogPoint.Type;
import Util.Logging.Message;

public class OperandNotPointerLRule extends LRule {

	private List<SyntaxElement> result;
	
	public void getResults(Program AST) {
		result = AST.visit(x -> {
			/* Dereferencing a primitive can be a valid statement, but it can be unsafe. A pointer would be safer. */
			if (x instanceof Deref) {
				Deref deref = (Deref) x;
				return deref.expression.getType().isPrimitive();
			}
			else if (x instanceof FunctionCall || x instanceof InlineCall) {
				Callee c = (Callee) x;
				return c.isNestedCall() && c.isNestedDeref() && !c.getParams().get(0).getType().isPointer();
			}
			
			return false;
		});
	}

	public void reportResults() {
		for (SyntaxElement s : this.result) {
			if (s instanceof Deref) new Message("Deref operand is not a pointer, " + s.getSource().getSourceMarker(), Type.WARN);
			else new Message("Struct instance is not a pointer, but call does deref, " + s.getSource().getSourceMarker(), Type.WARN);
			
			List<String> code = new ArrayList();
			
			if (s instanceof Expression) code.add("    " + ((Expression) s).codePrint());
			else code.addAll(s.codePrint(4));
			
			this.printResultSourceCode(code);
		}
	}

}
