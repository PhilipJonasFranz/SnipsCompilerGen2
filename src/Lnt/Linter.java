package Lnt;

import java.util.ArrayList;
import java.util.List;

import Imm.AST.Program;
import Lnt.Rules.DanglingPointerLRule;
import Lnt.Rules.NotThrownInTryLRule;
import Lnt.Rules.VoidParamLRule;

public class Linter {

	List<LRule> rules = new ArrayList();
	
	Program AST;
	
	public Linter(Program AST) {
		this.AST = AST;
		
		rules.add(new DanglingPointerLRule());
		rules.add(new VoidParamLRule());
		rules.add(new NotThrownInTryLRule());
	}
	
	public void lint() {
		this.rules.stream().forEach(x -> x.lint(this.AST));
	}
	
	public void report() {
		this.rules.stream().forEach(LRule::report);
	}
	
}
