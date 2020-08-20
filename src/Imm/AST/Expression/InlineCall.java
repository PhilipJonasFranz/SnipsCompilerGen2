package Imm.AST.Expression;

import java.util.List;

import Ctx.ContextChecker;
import Ctx.ProvisoUtil;
import Exc.CTX_EXC;
import Imm.AST.Function;
import Imm.AST.SyntaxElement;
import Imm.AST.Statement.Declaration;
import Imm.TYPE.TYPE;
import Util.NamespacePath;
import Util.Source;

public class InlineCall extends Expression {

			/* --- FIELDS --- */
	public SyntaxElement watchpoint;
	
	public NamespacePath path;
	
	/** Reference to the AST node of the called function */
	public Function calledFunction;
	
	/** List of the provisos types this call is templated with */
	public List<TYPE> proviso;
	
	/** The Expressions used as parameters */
	public List<Expression> parameters;
	
	/** Anonymous target. Recieves value during ctx if call is calling a predicate that could not be linked. */
	public Declaration anonTarget;
	
	public boolean hasAutoProviso = false;
	
	
			/* --- CONSTRUCTORS --- */
	/**
	 * Default constructor.
	 * @param source See {@link #source}
	 */
	public InlineCall(NamespacePath path, List<TYPE> proviso, List<Expression> parameters, Source source) {
		super(source);
		this.path = path;
		this.proviso = proviso;
		this.parameters = parameters;
	}

	
			/* --- METHODS --- */
	public void print(int d, boolean rec) {
		System.out.print(this.pad(d) + ((this.anonTarget == null)? "" : "Anonymous ") + "Inline Call: " + this.path.build());
		if (this.calledFunction != null) {
			for (TYPE t : this.proviso) System.out.print(", " + t.typeString());
			System.out.println(" " + ((this.calledFunction != null)? this.calledFunction.toString().split("@") [1] : "?"));
		}
		else {
			System.out.println();
			if (anonTarget != null) anonTarget.print(d + this.printDepthStep, rec);
			else System.out.println(this.pad(d + this.printDepthStep) + "Target:?");
		}
		
		if (rec) for (Expression e : this.parameters) {
			e.print(d + this.printDepthStep, rec);
		}
	}

	public TYPE check(ContextChecker ctx) throws CTX_EXC {
		return ctx.checkInlineCall(this);
	}

	public void setContext(List<TYPE> context) throws CTX_EXC {
		/* If func head exists */
		if (this.anonTarget == null) {
			for (int i = 0; i < this.proviso.size(); i++) 
				ProvisoUtil.mapNTo1(this.proviso.get(i), context);
		}
		
		for (Expression e : this.parameters) {
			e.setContext(context);
		}
	}

} 
