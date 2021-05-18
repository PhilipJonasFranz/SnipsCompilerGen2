package Lnt.Rules;

import Imm.AST.Function;
import Imm.AST.Program;
import Imm.AST.Statement.Statement;
import Lnt.LRule;
import Util.Logging.LogPoint.Type;
import Util.Logging.Message;

import java.util.ArrayList;
import java.util.List;

public class MissingReturnLRule extends LRule {

	private List<Function> result;

	public void getResults(Program AST) {
		this.result = AST.visit(x -> {
			if (x instanceof Function f) {
				return !f.getReturnTypeDirect().isVoid() && !f.statementsWithoutReturn.isEmpty();
			}

			return false;
		});
	}

	public void reportResults() {
		for (Function f : this.result) {
			new Message("Function '" + f.signatureToString() + "' is missing a return statement, " + f.getSource().getSourceMarker(), Type.WARN);
			List<String> code = new ArrayList();

			for (Statement s : f.statementsWithoutReturn)
				if (!s.equals(f))
					code.add(s.codePrint(0).get(0) + " (" + s.getSource().getSourceMarker() + ")");

			this.printResultSourceCode(code);
		}
	}

}
