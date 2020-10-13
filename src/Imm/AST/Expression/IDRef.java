package Imm.AST.Expression;

import java.util.List;

import Ctx.ProvisoUtil;
import Exc.CTX_EXC;
import Imm.AST.Statement.Declaration;
import Imm.TYPE.TYPE;
import Util.NamespacePath;
import Util.Source;

/**
 * This class represents a superclass for all Expressions.
 */
public class IDRef extends Expression {

			/* --- FIELDS --- */
	public NamespacePath path;
	
	/* Set during context checking */
	public Declaration origin;
	
	public boolean lastUsage = false;
	
	
			/* --- CONSTRUCTORS --- */
	/**
	 * Default constructor.
	 * @param source See {@link #source}
	 */
	public IDRef(NamespacePath path, Source source) {
		super(source);
		this.path = path;
	}

	
			/* --- METHODS --- */
	public void print(int d, boolean rec) {
		System.out.println(this.pad(d) + "IDRef: " + this.path.build() + "<" + ((this.getType() != null)? this.getType().typeString() : "?") + ">");
	}
	
	public void setContext(List<TYPE> context) throws CTX_EXC {
		if (this.origin != null) 
			this.setType(this.origin.getType().clone());
		
		ProvisoUtil.mapNTo1(this.getType(), context);
	}

} 
