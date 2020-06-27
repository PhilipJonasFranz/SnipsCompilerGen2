package Imm.AST.Statement;

import java.util.List;

import Ctx.ContextChecker;
import Exc.CTX_EXCEPTION;
import Imm.ASM.Util.Operands.RegOperand.REGISTER;
import Imm.AST.Expression.Expression;
import Imm.TYPE.TYPE;
import Util.Pair;
import Util.Source;

/**
 * This class represents a superclass for all AST-Nodes.
 */
public class DirectASMStatement extends Statement {

			/* --- FIELDS --- */
	public List<String> assembly;
	
	public List<Pair<Expression, REGISTER>> dataIn;
	
	public List<Pair<Expression, REGISTER>> dataOut;
	

			/* --- CONSTRUCTORS --- */
	public DirectASMStatement(List<String> assembly, List<Pair<Expression, REGISTER>> dataIn, List<Pair<Expression, REGISTER>> dataOut, Source source) {
		super(source);
		this.assembly = assembly;
		this.dataIn = dataIn;
		this.dataOut = dataOut;
	}
	
	
			/* --- METHODS --- */
	public void print(int d, boolean rec) {
		System.out.println(this.pad(d) + "Direct ASM");
		if (rec) {
			System.out.println(this.pad(d + this.printDepthStep) + "Data In:");
			
			for (Pair<Expression, REGISTER> p : this.dataIn) {
				System.out.println(this.pad(d + this.printDepthStep) + p.second + " :");
				p.first.print(d + this.printDepthStep, rec);
			}
			
			System.out.println(this.pad(d + this.printDepthStep) + "Data Out:");
			
			for (Pair<Expression, REGISTER> p : this.dataOut) {
				System.out.println(this.pad(d + this.printDepthStep) + p.second + " :");
				p.first.print(d + this.printDepthStep, rec);
			}
		}
	}

	public TYPE check(ContextChecker ctx) throws CTX_EXCEPTION {
		return ctx.checkDirectASMStatement(this);
	}
	
	public void setContext(List<TYPE> context) throws CTX_EXCEPTION {
		for (Pair<Expression, REGISTER> p : this.dataIn) {
			p.first.setContext(context);
		}
		
		for (Pair<Expression, REGISTER> p : this.dataOut) {
			p.first.setContext(context);
		}
	}

	public void releaseContext() {
		for (Pair<Expression, REGISTER> p : this.dataIn) {
			p.first.releaseContext();
		}
		
		for (Pair<Expression, REGISTER> p : this.dataOut) {
			p.first.releaseContext();
		}
	}
	
}
