package Imm.AST.Statement;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import Ctx.ContextChecker;
import Exc.CTEX_EXC;
import Exc.OPT0_EXC;
import Imm.ASM.Util.Operands.RegOp.REG;
import Imm.AST.SyntaxElement;
import Imm.AST.Expression.Expression;
import Imm.TYPE.TYPE;
import Opt.AST.ASTOptimizer;
import Snips.CompilerDriver;
import Tools.ASTNodeVisitor;
import Util.Pair;
import Util.Source;
import Util.Util;

/**
 * This class represents a superclass for all AST-Nodes.
 */
public class DirectASMStatement extends Statement {

			/* ---< FIELDS >--- */
	public List<String> assembly;
	
	public List<Pair<Expression, REG>> dataIn;
	
	public List<Pair<Expression, REG>> dataOut;
	

			/* ---< CONSTRUCTORS >--- */
	public DirectASMStatement(List<String> assembly, List<Pair<Expression, REG>> dataIn, List<Pair<Expression, REG>> dataOut, Source source) {
		super(source);
		this.assembly = assembly;
		this.dataIn = dataIn;
		this.dataOut = dataOut;
	}
	
	
			/* ---< METHODS >--- */
	public void print(int d, boolean rec) {
		CompilerDriver.outs.println(Util.pad(d) + "Direct ASM");
		
		if (rec) {
			CompilerDriver.outs.println(Util.pad(d + this.printDepthStep) + "Data In:");
			
			for (Pair<Expression, REG> p : this.dataIn) {
				CompilerDriver.outs.println(Util.pad(d + this.printDepthStep) + p.second + " :");
				p.first.print(d + this.printDepthStep, rec);
			}
			
			CompilerDriver.outs.println(Util.pad(d + this.printDepthStep) + "Data Out:");
			
			for (Pair<Expression, REG> p : this.dataOut) {
				CompilerDriver.outs.println(Util.pad(d + this.printDepthStep) + p.second + " :");
				p.first.print(d + this.printDepthStep, rec);
			}
		}
	}

	public TYPE check(ContextChecker ctx) throws CTEX_EXC {
		ctx.pushTrace(this);
		
		TYPE t = ctx.checkDirectASMStatement(this);
		
		ctx.popTrace();
		return t;
	}
	
	public Statement opt(ASTOptimizer opt) throws OPT0_EXC {
		return opt.optDirectASMStatement(this);
	}
	
	public <T extends SyntaxElement> List<T> visit(ASTNodeVisitor<T> visitor) {
		List<T> result = new ArrayList();
		
		if (visitor.visit(this))
			result.add((T) this);
		
		return result;
	}
	
	public void setContext(List<TYPE> context) throws CTEX_EXC {
		for (Pair<Expression, REG> p : this.dataIn) 
			p.first.setContext(context);
		
		for (Pair<Expression, REG> p : this.dataOut) 
			p.first.setContext(context);
	}

	public Statement clone() {
		List<String> ac = new ArrayList();
		for (String s : this.assembly) ac.add(s);
		
		List<Pair<Expression, REG>> dataInC = new ArrayList();
		for (Pair<Expression, REG> p : this.dataIn) dataInC.add(new Pair<Expression, REG>(p.first.clone(), p.second));
		
		List<Pair<Expression, REG>> dataOutC = new ArrayList();
		for (Pair<Expression, REG> p : this.dataOut) dataOutC.add(new Pair<Expression, REG>(p.first.clone(), p.second));
		
		DirectASMStatement dasm = new DirectASMStatement(ac, dataInC, dataOutC, this.getSource().clone());
		dasm.copyDirectivesFrom(this);
		return dasm;
	}

	public List<String> codePrint(int d) {
		List<String> code = new ArrayList();
		
		String s = "asm";
		
		if (!this.dataIn.isEmpty()) 
			s += this.dataIn.stream().map(x -> x.first.codePrint() + " : " + x.second.toString().toLowerCase())
				.collect(Collectors.joining(", ", "(", ")"));
		
		s += " {";
		code.add(Util.pad(d) + s);
		
		for (int i = 0; i < this.assembly.size(); i++) {
			String assembly = this.assembly.get(i);
			code.add(Util.pad(d + this.printDepthStep) + assembly + ((i < this.assembly.size() - 1)? " :" : ""));
		}
		
		s = "}";
		
		if (!this.dataOut.isEmpty()) 
			s += this.dataOut.stream().map(x -> x.second.toString().toLowerCase() + " : " + x.first.codePrint())
					.collect(Collectors.joining(", ", "(", ")"));
		
		s += ";";
		code.add(Util.pad(d) + s);
		
		return code;
	}
	
} 
