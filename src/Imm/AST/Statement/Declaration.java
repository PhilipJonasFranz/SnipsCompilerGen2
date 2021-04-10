package Imm.AST.Statement;

import java.util.ArrayList;
import java.util.List;

import Ctx.ContextChecker;
import Ctx.Util.ProvisoUtil;
import Exc.CTEX_EXC;
import Exc.OPT0_EXC;
import Imm.AST.SyntaxElement;
import Imm.AST.Expression.Expression;
import Imm.AsN.AsNNode.MODIFIER;
import Imm.TYPE.PROVISO;
import Imm.TYPE.TYPE;
import Opt.ASTOptimizer;
import Snips.CompilerDriver;
import Tools.ASTNodeVisitor;
import Util.NamespacePath;
import Util.Source;
import Util.Util;

/**
 * This class represents a superclass for all AST-Nodes.
 */
public class Declaration extends Statement {

	private static int SERIAL_CNT = 0;
	
			/* ---< FIELDS >--- */
	public long SERIAL = SERIAL_CNT++;
	
	/** 
	 * The visibility modifer of this declaration. Can only be applied to global declarations. 
	 */
	public MODIFIER modifier;
	
	/** 
	 * The namespace path of this declaration. 
	 */
	public NamespacePath path;
	
	/** 
	 * The effective type of this declaration. 
	 */
	private TYPE type;
	
	/** 
	 * The initial value of this declaration. 
	 */
	public Expression value;
	
	/** 
	 * The last statement that accessed this Declaration. 
	 */
	public Statement last = null;
	
	
			/* ---< CONSTRUCTORS >--- */
	/**
	 * Default constructor.
	 * @param source See {@link #source}
	 */
	public Declaration(NamespacePath path, TYPE type, MODIFIER modifier, Source source) {
		super(source);
		this.path = path;
		this.type = type;
		this.modifier = modifier;
	}
	
	public Declaration(NamespacePath path, TYPE type, Expression value, MODIFIER modifier, Source source) {
		super(source);
		this.path = path;
		this.type = type;
		this.value = value;
		this.modifier = modifier;
	}
	
	
			/* ---< METHODS >--- */
	public void print(int d, boolean rec) {
		try {
			CompilerDriver.outs.println(Util.pad(d) + "Declaration <" + this.type.typeString() + "> " + this.path.build());
		} catch (Exception e) {
			CompilerDriver.outs.println(Util.pad(d) + "Declaration <?> " + this.path.build());
		}
		
		if (rec && this.value != null) 
			this.value.print(d + this.printDepthStep, rec);
	}

	public TYPE check(ContextChecker ctx) throws CTEX_EXC {
		Source temp = CompilerDriver.lastSource;
		CompilerDriver.lastSource = this.getSource();
		
		TYPE t = ctx.checkDeclaration(this);
		
		CompilerDriver.lastSource = temp;
		return t;
	}
	
	public Statement opt(ASTOptimizer opt) throws OPT0_EXC {
		return opt.optDeclaration(this);
	}
	
	public <T extends SyntaxElement> List<T> visit(ASTNodeVisitor<T> visitor) {
		List<T> result = new ArrayList();
		
		if (visitor.visit(this))
			result.add((T) this);
		
		if (this.value != null)
			result.addAll(this.value.visit(visitor));
		
		return result;
	}

	public void setContext(List<TYPE> context) throws CTEX_EXC {
		/* Apply to declaration type */
		ProvisoUtil.mapNTo1(this.type, context);
		
		/* Apply to value */
		if (this.value != null) 
			this.value.setContext(context);
	}

	/** 
	 * Return the current context, or the actual type.
	 */
	public TYPE getType() {
		if (this.type instanceof PROVISO) {
			PROVISO p = (PROVISO) this.type;
			if (p.hasContext()) return p.getContext();
			else return p;
		}
		else return this.type;
	}
	
	public TYPE getRawType() {
		return this.type;
	}
	
	public void setType(TYPE type) {
		this.type = type;
	}
	
	public Declaration clone() {
		Declaration clone = new Declaration(this.path, this.type.clone(), this.modifier, this.getSource());
		
		if (this.value != null) clone.value = this.value.clone();
		
		clone.SERIAL = this.SERIAL;
		return clone;
	}

	public List<String> codePrint(int d) {
		List<String> code = new ArrayList();
		String s = this.type.codeString() + " " + this.path.build();
		if (this.value != null)
			s += " = " + this.value.codePrint() + ";";
		code.add(Util.pad(d) + s);
		return code;
	}
	
} 
