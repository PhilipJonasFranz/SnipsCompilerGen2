package Lnt.Rules;

import java.util.ArrayList;
import java.util.List;

import Imm.AST.Program;
import Imm.AST.SyntaxElement;
import Imm.AST.Expression.Expression;
import Lnt.LRule;
import Util.Logging.LogPoint.Type;
import Util.Logging.Message;

public class ModifierViolationLRule extends LRule {

	private List<SyntaxElement> result;
	
	public void getResults(Program AST) {
		result = AST.visit(x -> x.modifierViolated);
	}

	public void reportResults() {
		for (SyntaxElement s : this.result) {
			new Message("Modifier violation, " + s.getSource().getSourceMarker(), Type.WARN);
			
			List<String> code = new ArrayList();
			
			if (s instanceof Expression) code.add("    " + ((Expression) s).codePrint());
			else code.addAll(s.codePrint(4));
			
			this.printResultSourceCode(code);
		}
	}

}
