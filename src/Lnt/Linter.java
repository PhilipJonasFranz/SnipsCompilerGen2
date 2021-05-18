package Lnt;

import Imm.AST.Program;
import Lnt.Rules.*;

import java.util.ArrayList;
import java.util.List;

/**
 * The Linter is used to statically analyze the AST and check
 * for potential issues with the code. These issues include 
 * potential memory leaks, bad coding conventions, unsafe operations
 * etc. The results are printed as warnings with their respective
 * source code and location.
 */
public class Linter {

	List<LRule> rules = new ArrayList();
	
	Program AST;
	
	public Linter(Program AST) {
		this.AST = AST;
		
		/* Register rules */
		rules.add(new DanglingPointerLRule());
		rules.add(new VoidParamLRule());
		rules.add(new NotThrownInTryLRule());
		rules.add(new OperandNotPointerLRule());
		rules.add(new ImplicitAnonymousTypeLRule());
		rules.add(new ModifierViolationLRule());
		rules.add(new DirectASMNoOutputLRule());
		rules.add(new AddressOfStructureInitLRule());
		rules.add(new MissingReturnLRule());
		
	}
	
	public void lint() {
		this.rules.forEach(x -> x.getResults(this.AST));
	}
	
	public void report() {
		this.rules.forEach(LRule::reportResults);
	}
	
}
