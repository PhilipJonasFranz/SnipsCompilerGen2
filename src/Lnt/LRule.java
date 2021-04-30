package Lnt;

import Imm.AST.Program;

public abstract class LRule {

	public abstract void lint(Program AST);
	
	public abstract void report();
	
}
