package Lnt;

import java.util.ArrayList;
import java.util.List;

import Imm.AST.Program;
import Lnt.Rules.DanglingPointerLRule;
import Lnt.Rules.ImplicitAnonymousTypeLRule;
import Lnt.Rules.NotThrownInTryLRule;
import Lnt.Rules.OperandNotPointerLRule;
import Lnt.Rules.VoidParamLRule;

public class Linter {

	List<LRule> rules = new ArrayList();
	
	Program AST;
	
	public Linter(Program AST) {
		this.AST = AST;
		
		rules.add(new DanglingPointerLRule());
		rules.add(new VoidParamLRule());
		rules.add(new NotThrownInTryLRule());
		rules.add(new OperandNotPointerLRule());
		rules.add(new ImplicitAnonymousTypeLRule());
	}
	
	public void lint() {
		this.rules.stream().forEach(x -> x.getResults(this.AST));
	}
	
	public void report() {
		this.rules.stream().forEach(LRule::reportResults);
	}
	
}
