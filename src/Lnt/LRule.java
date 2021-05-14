package Lnt;

import java.util.List;

import Imm.AST.Program;
import Snips.CompilerDriver;

public abstract class LRule {

	public abstract void getResults(Program AST);
	
	public abstract void reportResults();
	
	public void printResultSourceCode(List<String> code) {
		code.forEach(CompilerDriver.outs::println);
		
		CompilerDriver.outs.print("    ^");
		for (int i = 0; i < code.get(code.size() - 1).length() - 5; i++)
			CompilerDriver.outs.print("~");
		
		CompilerDriver.outs.println();
	}
	
}
