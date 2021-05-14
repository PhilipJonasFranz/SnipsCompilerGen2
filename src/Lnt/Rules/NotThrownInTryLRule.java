package Lnt.Rules;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import Imm.AST.Function;
import Imm.AST.Program;
import Imm.AST.SyntaxElement;
import Imm.TYPE.TYPE;
import Lnt.LRule;
import Util.Pair;
import Util.Logging.LogPoint.Type;
import Util.Logging.Message;

public class NotThrownInTryLRule extends LRule {

	private List<Pair<SyntaxElement, List<TYPE>>> result = new ArrayList();
	
	public void getResults(Program AST) {
		List<Function> result = AST.visit(x -> x instanceof Function);
		
		for (Function f : result) {
			List<TYPE> sCopy = new ArrayList<>(f.signalsTypes);
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

	public void reportResults() {
		for (Pair<SyntaxElement, List<TYPE>> p : this.result) {
			if (p.first instanceof Function f) {
				String types = p.second.stream().map(TYPE::toString).collect(Collectors.joining(", "));
				new Message("Function '" + f.path.build() + "' signals not thrown types: " + types + ", " + p.first.getSource().getSourceMarker(), Type.WARN);
				
				String code = "    " + ((Function) p.first).signatureToString();
				
				this.printResultSourceCode(Collections.singletonList(code));
			}
		}
	}

}
