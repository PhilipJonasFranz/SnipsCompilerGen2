package Lnt.Rules;

import java.util.ArrayList;
import java.util.List;

import Ctx.Util.Callee;
import Imm.AST.Program;
import Imm.AST.SyntaxElement;
import Imm.AST.Expression.Expression;
import Imm.AST.Expression.InlineCall;
import Imm.AST.Statement.FunctionCall;
import Lnt.LRule;
import Util.Logging.LogPoint.Type;
import Util.Logging.Message;

public class DanglingPointerLRule extends LRule {

	private List<SyntaxElement> result;
	
	private boolean danglingPointer(Expression e) {
		return !e.visit(x -> {
			if (x instanceof InlineCall ic) {
				return ic.getType().isPointer();
			}
			return false;
		}).isEmpty();
	}
	
	public void getResults(Program AST) {
		List<SyntaxElement> result = new ArrayList();
		
		List<SyntaxElement> calls = AST.visit(x -> x instanceof FunctionCall || x instanceof InlineCall);
		
		calls.forEach(x -> {
			if (x instanceof FunctionCall c) {
				if (c.getType().isPointer())
					result.add(x);
			}
		});
		
		for (SyntaxElement call : calls) {
			Callee c = (Callee) call;
			for (Expression e : c.getParams()) {
				if (this.danglingPointer(e))
					result.add(e);
			}
		}
		
		this.result = result;
	}

	public void reportResults() {
		for (SyntaxElement s : this.result) {
			if (s instanceof FunctionCall fc) {
				new Message("Potential dangling pointer, " + fc.getSource().getSourceMarker(), Type.WARN);
			}
			else new Message("Potential dangling pointer in call, " + s.getSource().getSourceMarker(), Type.WARN);
			
			List<String> code = new ArrayList();
			
			if (s instanceof Expression) code.add("    " + ((Expression) s).codePrint());
			else code.addAll(s.codePrint(4));
			
			this.printResultSourceCode(code);
		}
	}

}
