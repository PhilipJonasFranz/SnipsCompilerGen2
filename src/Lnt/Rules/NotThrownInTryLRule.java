package Lnt.Rules;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import Imm.AST.Function;
import Imm.AST.Program;
import Imm.AST.SyntaxElement;
import Imm.TYPE.TYPE;
import Lnt.LRule;
import Snips.CompilerDriver;
import Util.Pair;
import Util.Logging.LogPoint.Type;
import Util.Logging.Message;

public class NotThrownInTryLRule extends LRule {

	private List<Pair<SyntaxElement, List<TYPE>>> result = new ArrayList();
	
	public void lint(Program AST) {
		List<Function> result = AST.visit(x -> {
			return x instanceof Function;
		});
		
		for (Function f : result) {
			List<TYPE> sCopy = f.signalsTypes.stream().collect(Collectors.toList());
			for (TYPE t : f.actualSignals) {
				for (int i = 0; i < sCopy.size(); i++) {
					if (sCopy.get(i).isEqual(t)) {
						sCopy.remove(i);
						break;
					}
				}
			}
			
			if (!sCopy.isEmpty()) 
				this.result.add(new Pair<SyntaxElement, List<TYPE>>(f, sCopy));
		}
	}

	public void report() {
		for (Pair<SyntaxElement, List<TYPE>> p : this.result) {
			if (p.first instanceof Function) {
				Function f = (Function) p.first;
				
				String types = p.second.stream().map(x -> x.toString()).collect(Collectors.joining(", "));
				new Message("Function '" + f.path.build() + "' signals not thrown types: " + types + ", " + p.first.getSource().getSourceMarker(), Type.WARN);
				
				String code = "    " + ((Function) p.first).signatureToString();
				CompilerDriver.outs.println(code);
				
				CompilerDriver.outs.print("    ^");
				for (int i = 0; i < code.length() - 5; i++)
					CompilerDriver.outs.print("~");
				
				CompilerDriver.outs.println();
			}
		}
	}

}
