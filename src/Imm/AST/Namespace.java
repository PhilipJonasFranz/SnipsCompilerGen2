package Imm.AST;

import java.util.ArrayList;
import java.util.List;

import Ctx.ContextChecker;
import Exc.CTEX_EXC;
import Exc.OPT0_EXC;
import Imm.TYPE.TYPE;
import Imm.TYPE.PRIMITIVES.VOID;
import Opt.ASTOptimizer;
import Snips.CompilerDriver;
import Tools.ASTNodeVisitor;
import Util.NamespacePath;
import Util.Source;
import Util.Util;

/**
 * The namespace node is used to capsule multiple program
 * elements within a single namespace.
 * 
 * All namespace are flattened by the namespace processor
 * before context-checking, so the pipeline does not need
 * support for the namespace node.
 */
public class Namespace extends SyntaxElement {

			/* ---< FIELDS >--- */
	/**
	 * The relative namespace path of this namespace. If for example
	 * this namespace 'B' lies in another namespace 'A', this field will
	 * be set to a namespace path representing 'B'. The full namespace
	 * path for the contained ressources is constructed in the namespace-processor.
	 */
	public NamespacePath path;
	
	/**
	 * Program elements contained within this namespace.
	 */
	public List<SyntaxElement> programElements;
	
	
			/* ---< CONSTRUCTORS >--- */
	/**
	 * Default constructor.
	 * @param source See {@link #source}
	 */
	public Namespace(NamespacePath path, List<SyntaxElement> programElements, Source source) {
		super(source);
		this.path = path;
		this.programElements = programElements;
	}
	
	
			/* ---< METHODS >--- */
	public void print(int d, boolean rec) {
		CompilerDriver.outs.println(Util.pad(d) + "Namespace: " + this.path.build());
		if (rec) for (SyntaxElement e : this.programElements) {
			e.print(d + this.printDepthStep, rec);
		}
	}

	public TYPE check(ContextChecker ctx) throws CTEX_EXC {
		/* This function should not be called since namespaces are flattened */
		return new VOID();
	}
	
	public Namespace opt(ASTOptimizer opt) throws OPT0_EXC {
		return this;
	}
	
	public <T extends SyntaxElement> List<T> visit(ASTNodeVisitor<T> visitor) {
		List<T> result = new ArrayList();
		
		if (visitor.visit(this))
			result.add((T) this);
		
		for (SyntaxElement s : this.programElements) {
			if (visitor.visit(s))
				result.add((T) s);
		}
		
		return result;
	}

	public void setContext(List<TYPE> setContext) {
		return;
	}
	
	public List<String> codePrint(int d) {
		List<String> code = new ArrayList();
		
		String s = "namespace " + this.path.build() + "{";
		code.add(Util.pad(d) + s);
		
		for (SyntaxElement s0 : this.programElements) {
			code.addAll(s0.codePrint(d + this.printDepthStep));
		}
		
		code.add(Util.pad(d) + "}");
		return code;
	}

	public Namespace clone() {
		List<SyntaxElement> copy = new ArrayList();
		for (SyntaxElement s : this.programElements) copy.add(s.clone());
		return new Namespace(this.path.clone(), copy, this.getSource().clone());
	}

} 
