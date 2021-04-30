package Lnt.Rules;

import java.util.List;

import Imm.AST.Function;
import Imm.AST.Program;
import Imm.AST.SyntaxElement;
import Imm.AST.Statement.Declaration;
import Lnt.LRule;
import Snips.CompilerDriver;
import Util.Logging.LogPoint.Type;
import Util.Logging.Message;

public class VoidParamLRule extends LRule {

	private List<SyntaxElement> result;
	
	public void lint(Program AST) {
		List<SyntaxElement> result = AST.visit(x -> {
			if (x instanceof Function) {
				Function f = (Function) x;
				
				for (Declaration d : f.parameters) {
					if (d.getType().getCoreType().isVoid() && !d.path.build().equals("self")) 
						return true;
				}
			}
			
			return false;
		});
		
		this.result = result;
	}

	public void report() {
		for (SyntaxElement s : this.result) {
			new Message("Unchecked type VOID as parameter, " + s.getSource().getSourceMarker(), Type.WARN);
			
			String code = "    " + ((Function) s).signatureToString();
			CompilerDriver.outs.println(code);
			
			CompilerDriver.outs.print("    ^");
			for (int i = 0; i < code.length() - 5; i++)
				CompilerDriver.outs.print("~");
			
			CompilerDriver.outs.println();
		}
	}

}
