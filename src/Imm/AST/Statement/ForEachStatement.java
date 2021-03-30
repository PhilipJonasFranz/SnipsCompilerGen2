package Imm.AST.Statement;

import java.util.List;

import CGen.Util.LabelUtil;
import Ctx.ContextChecker;
import Exc.CTEX_EXC;
import Imm.AST.Expression.ArraySelect;
import Imm.AST.Expression.Atom;
import Imm.AST.Expression.Expression;
import Imm.AST.Expression.IDRef;
import Imm.AsN.AsNNode.MODIFIER;
import Imm.TYPE.TYPE;
import Imm.TYPE.PRIMITIVES.INT;
import Snips.CompilerDriver;
import Util.NamespacePath;
import Util.Source;

/**
 * The for-Each loop iterates over a defined data set of fixed
 * size or with a given range. For each iteration, a new data-set
 * is loaded into the iterator variable, and the body is executed
 * with the current value of the iterator.
 * 
 * If enabled, the iterator is written back into the data source after
 * the each loop body execution. This can be enabled by using brackets
 * instead of parenthesis in the syntax.
 */
public class ForEachStatement extends CompoundStatement {

			/* ---< FIELDS >--- */
	/**
	 * Expression that selects the data from the data source.
	 */
	public Expression shadowRef;
	
	/** The declaration of the iterator. */
	public Declaration iterator;
	
	/**
	 * Internal counter with UID, keeps track of loop iterations.
	 */
	public Declaration counter;
	
	/** 
	 * Reference to the counter of the for-Each Statement 
	 */
	public IDRef counterRef;
	
	/**
	 * The expression that evaluates to the range this for-each
	 * loop should loop to.
	 */
	public Expression range;
	
	/** ArraySelect used when data is retrieved from an array */
	public ArraySelect select;
	
	/**
	 * If set to true, the value of the iterator is written back into
	 * the data source after the loop body.
	 */
	public boolean writeBackIterator = false;
	
	/**
	 * This assignment is set to a statement that represents
	 * the writeback of the iterator to the data target.
	 * 
	 * The assignment depends on the type of the selection the
	 * iterator data is retrieved in the first place. The assignment
	 * is generated automatically during CTX.
	 */
	public Assignment writeback;
	
	
			/* ---< CONSTRUCTORS >--- */
	public ForEachStatement(Declaration iterator, boolean writeBackIterator, Expression shadowRef, Expression range, List<Statement> body, Source source) {
		super(body, source);
		
		this.iterator = iterator;
		this.writeBackIterator = writeBackIterator;
		
		this.shadowRef = shadowRef;
		
		this.range = range;
		
		String name = "__it_cnt" + LabelUtil.getUID();
		
		/* Initialize internal Counter and reference to it */
		counter = new Declaration(new NamespacePath(name), new INT("0"), new Atom(new INT("0"), this.shadowRef.getSource()), MODIFIER.SHARED, this.shadowRef.getSource());
		counterRef = new IDRef(new NamespacePath(name), this.shadowRef.getSource());
	}
	
	
			/* ---< METHODS >--- */
	public void print(int d, boolean rec) {
		System.out.println(this.pad(d) + "ForEach");
		
		if (rec) {
			this.iterator.print(d + this.printDepthStep, rec);
			this.shadowRef.print(d + this.printDepthStep, rec);
			
			for (Statement s : this.body) 
				s.print(d + this.printDepthStep, rec);
		}
	}

	public TYPE check(ContextChecker ctx) throws CTEX_EXC {
		Source temp = CompilerDriver.lastSource;
		CompilerDriver.lastSource = this.getSource();
		
		TYPE t = ctx.checkForEachStatement(this);
		
		CompilerDriver.lastSource = temp;
		return t;
	}

	public Statement clone() {
		ForEachStatement f = new ForEachStatement(this.iterator.clone(), this.writeBackIterator, this.shadowRef.clone(), this.range.clone(), this.cloneBody(), this.getSource().clone());
		if (this.select != null)
			f.select = (ArraySelect) this.select.clone();
		
		return f;
	}
	
} 
