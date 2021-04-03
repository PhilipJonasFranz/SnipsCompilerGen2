package Imm.AST.Statement;

import java.util.ArrayList;
import java.util.List;

import Ctx.ContextChecker;
import Exc.CTEX_EXC;
import Exc.OPT0_EXC;
import Imm.ASM.Util.Operands.RegOp.REG;
import Imm.AST.Expression.Expression;
import Imm.TYPE.TYPE;
import Opt.ASTOptimizer;
import Snips.CompilerDriver;
import Util.Pair;
import Util.Source;

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
		System.out.println(this.pad(d) + "Direct ASM");
		
		if (rec) {
			System.out.println(this.pad(d + this.printDepthStep) + "Data In:");
			
			for (Pair<Expression, REG> p : this.dataIn) {
				System.out.println(this.pad(d + this.printDepthStep) + p.second + " :");
				p.first.print(d + this.printDepthStep, rec);
			}
			
			System.out.println(this.pad(d + this.printDepthStep) + "Data Out:");
			
			for (Pair<Expression, REG> p : this.dataOut) {
				System.out.println(this.pad(d + this.printDepthStep) + p.second + " :");
				p.first.print(d + this.printDepthStep, rec);
			}
		}
	}

	public TYPE check(ContextChecker ctx) throws CTEX_EXC {
		Source temp = CompilerDriver.lastSource;
		CompilerDriver.lastSource = this.getSource();
		
		TYPE t = ctx.checkDirectASMStatement(this);
		
		CompilerDriver.lastSource = temp;
		return t;
	}
	
	public Statement opt(ASTOptimizer opt) throws OPT0_EXC {
		return opt.optDirectASMStatement(this);
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
		for (Pair<Expression, REG> p : this.dataIn) dataInC.add(new Pair<Expression, REG>(p.first, p.second));
		
		List<Pair<Expression, REG>> dataOutC = new ArrayList();
		for (Pair<Expression, REG> p : this.dataOut) dataOutC.add(new Pair<Expression, REG>(p.first, p.second));
		
		return new DirectASMStatement(ac, dataInC, dataOutC, this.getSource().clone());
	}

} 
