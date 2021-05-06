package Imm.AST.Expression;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import Ctx.ContextChecker;
import Exc.CTEX_EXC;
import Exc.OPT0_EXC;
import Imm.AST.Function;
import Imm.AST.SyntaxElement;
import Imm.TYPE.TYPE;
import Opt.AST.ASTOptimizer;
import Snips.CompilerDriver;
import Tools.ASTNodeVisitor;
import Util.Source;
import Util.Util;

/**
 * Describes an operation that is applied to two or more operands.
 */
public class OperatorExpression extends Expression {	
	
			/* ---< FIELDS >--- */
	public Expression actualExpression;
	
	/** The operator */
	public String operator;
	
	public Function calledFunction;
	
	public List<TYPE> provisoTypes = new ArrayList();
	
	
			/* ---< CONSTRUCTORS >--- */
	/**
	 * Default constructor.
	 * @param source See {@link #source}
	 */
	public OperatorExpression(Expression actualExpression, String operator, Source source) {
		super(source);
		
		this.actualExpression = actualExpression;
		
		this.operator = operator;
	}
	
	
			/* ---< METHODS >--- */
	public void print(int d, boolean rec) {
		CompilerDriver.outs.print(Util.pad(d) + "Operator< " + this.operator + " >");
		
		if (this.calledFunction != null)
			CompilerDriver.outs.print(" (" + this.calledFunction.signatureToString() + ")");
		
		CompilerDriver.outs.println();
		
		if (rec) {
			this.actualExpression.print(d + this.printDepthStep, rec);
		}
	}
	
	public TYPE check(ContextChecker ctx) throws CTEX_EXC {
		ctx.pushTrace(this);
		
		TYPE t = ctx.checkOperatorExpression(this);
		
		ctx.popTrace();
		return t;
	}
	
	public <T extends SyntaxElement> List<T> visit(ASTNodeVisitor<T> visitor) {
		List<T> result = new ArrayList();
		
		if (visitor.visit(this))
			result.add((T) this);
		
		result.addAll(this.actualExpression.visit(visitor));
		
		return result;
	}
	
	public void setContext(List<TYPE> context) throws CTEX_EXC {
		this.actualExpression.setContext(context);
	}

	public Expression opt(ASTOptimizer s) throws OPT0_EXC {
		return s.optOperatorExpression(this);
	}

	public Expression clone() {
		OperatorExpression op = new OperatorExpression(this.actualExpression.clone(), this.operator, this.getSource().clone());
		
		if (this.getType() != null)
			op.setType(this.getType().clone());
		
		op.calledFunction = this.calledFunction;
		
		if (this.provisoTypes != null) {
			op.provisoTypes = this.provisoTypes.stream().map(x -> x.clone()).collect(Collectors.toList());
		}
		
		return op;
	}

	public String codePrint() {
		return this.actualExpression.codePrint();
	}
	
	public List<Expression> extractOperands() {
		List<Expression> operands = null;
		
		if (this.actualExpression instanceof NFoldExpression) {
			NFoldExpression nfold = (NFoldExpression) this.actualExpression;
			operands = nfold.operands;
		}
		else if (this.actualExpression instanceof UnaryExpression) {
			UnaryExpression unary = (UnaryExpression) this.actualExpression;
			operands = Arrays.asList(unary.operand);
		}
		else if (this.actualExpression instanceof IDRefWriteback) {
			IDRefWriteback idwb = (IDRefWriteback) this.actualExpression;
			operands = Arrays.asList(idwb.getShadowRef());
		}
		
		return operands;
	}
	
} 
