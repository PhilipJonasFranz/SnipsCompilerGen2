package Imm.AST.Typedef;

import java.util.ArrayList;
import java.util.List;

import Ctx.ContextChecker;
import Exc.CTEX_EXC;
import Exc.OPT0_EXC;
import Exc.SNIPS_EXC;
import Imm.AST.SyntaxElement;
import Imm.TYPE.TYPE;
import Imm.TYPE.PRIMITIVES.ENUM;
import Imm.TYPE.PRIMITIVES.VOID;
import Opt.AST.ASTOptimizer;
import Snips.CompilerDriver;
import Tools.ASTNodeVisitor;
import Util.NamespacePath;
import Util.Source;
import Util.Util;

/**
 * This class represents a superclass for all AST-Nodes.
 */
public class EnumTypedef extends SyntaxElement {

			/* ---< FIELDS >--- */
	public NamespacePath path;
	
	public List<String> enums;
	
	public ENUM enumType;
	
	public List<ENUM> enumFields = new ArrayList();
	
	
			/* ---< CONSTRUCTORS >--- */
	/**
	 * Default constructor.
	 * @param source See {@link #source}
	 */
	public EnumTypedef(NamespacePath path, List<String> enums, Source source) {
		super(source);
		this.path = path;
		this.enums = enums;
		
		int mappingNum = -1;
		
		/* Undefined mapping has value -1 */
		this.enumType = new ENUM(this, path.getLast() + ":UNDEFINED", mappingNum++);
		
		/* Initialize Enum Fields with mapping numbers, first is 0 */
		for (int i = 0; i < this.enums.size(); i++) 
			this.enumFields.add(new ENUM(this, enums.get(i), mappingNum++));
	}
	
	public ENUM getEnumField(String value, Source source) {
		for (ENUM t : this.enumFields) 
			if (t.fieldName.equals(value)) return t;
		
		throw new SNIPS_EXC("The enum " + this.path + " does not contain the field " + value + ", " + source.getSourceMarker());
	}
	
	
			/* ---< METHODS >--- */
	public void print(int d, boolean rec) {
		CompilerDriver.outs.println(Util.pad(d) + "Enum Typedef <" + this.path + ">");
		
		if (rec) {
			for (String e : this.enums) 
				CompilerDriver.outs.println(Util.pad(d + this.printDepthStep) + e);
		}
	}

	public TYPE check(ContextChecker ctx) throws CTEX_EXC {
		return new VOID();
	}
	
	public SyntaxElement opt(ASTOptimizer opt) throws OPT0_EXC {
		return opt.optEnumTypedef(this);
	}
	
	public <T extends SyntaxElement> List<T> visit(ASTNodeVisitor<T> visitor) {
		List<T> result = new ArrayList();
		
		if (visitor.visit(this))
			result.add((T) this);
		
		return result;
	}

	public void setContext(List<TYPE> context) throws CTEX_EXC {
		return;
	}

	public List<String> codePrint(int d) {
		List<String> code = new ArrayList();
		code.add(Util.pad(d) + "enum " + this.path + " {");
		for (String field : this.enums) {
			code.add(Util.pad(d + this.printDepthStep) + field + ", ");
		}
		
		code.add(Util.pad(d) + "}");
		return code;
	}

	public SyntaxElement clone() {
		return this;
	}

} 
