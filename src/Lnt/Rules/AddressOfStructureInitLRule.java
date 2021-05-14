package Lnt.Rules;

import Imm.AST.Expression.AddressOf;
import Imm.AST.Expression.StructureInit;
import Imm.AST.Program;
import Lnt.LRule;
import Util.Logging.LogPoint.Type;
import Util.Logging.Message;

import java.util.Collections;
import java.util.List;

public class AddressOfStructureInitLRule extends LRule {

	private List<AddressOf> result;

	public void getResults(Program AST) {
		this.result = AST.visit(x -> {
			if (x instanceof AddressOf aof) {
				return aof.expression instanceof StructureInit;
			}

			return false;
		});
	}

	public void reportResults() {
		for (AddressOf s : this.result) {
			new Message("AddressOf captures address of soon to be overwritten memory, " + s.getSource().getSourceMarker(), Type.WARN);
			List<String> code = Collections.singletonList(s.codePrint());
			this.printResultSourceCode(code);
		}
	}

}
