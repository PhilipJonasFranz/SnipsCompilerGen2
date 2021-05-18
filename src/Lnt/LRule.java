package Lnt;

import Imm.AST.Program;
import Snips.CompilerDriver;

import java.util.List;

public abstract class LRule {

	public abstract void getResults(Program AST);
	
	public abstract void reportResults();
	
	public void printResultSourceCode(List<String> code) {
		for (String s : code) {
			CompilerDriver.outs.println(s);

			boolean print = false;
			for (int i = 0; i < s.length() - 5; i++) {
				if (s.charAt(i + 5) == ' ' && !print) {
					CompilerDriver.outs.print(" ");
				}
				else {
					if (!print) CompilerDriver.outs.print("    ^");
					print = true;
					CompilerDriver.outs.print("~");
				}
			}

			CompilerDriver.outs.println();
		}
	}
	
}
