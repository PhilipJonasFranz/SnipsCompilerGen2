package Lnt.Rules;

import java.util.ArrayList;
import java.util.List;

import Imm.AST.Program;
import Imm.AST.Statement.DirectASMStatement;
import Lnt.LRule;
import Util.Logging.LogPoint.Type;
import Util.Logging.Message;

public class DirectASMNoOutputLRule extends LRule {

	private List<DirectASMStatement> result = new ArrayList();
	
	public void getResults(Program AST) {
		result = AST.visit(x -> {
			if (x instanceof DirectASMStatement asm) {
				return asm.dataOut.isEmpty();
			}
			
			return false;
		});
	}

	public void reportResults() {
		for (DirectASMStatement asm : this.result) {
			new Message("Direct ASM Operation has no explicit outputs, " + asm.getSource().getSourceMarker(), Type.WARN);
			List<String> code = asm.codePrint(4);
			this.printResultSourceCode(code);
		}
	}

}
