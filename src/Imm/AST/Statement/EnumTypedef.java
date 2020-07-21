package Imm.AST.Statement;

import java.util.ArrayList;
import java.util.List;

import Ctx.ContextChecker;
import Exc.CTX_EXC;
import Exc.SNIPS_EXC;
import Imm.AST.SyntaxElement;
import Imm.TYPE.TYPE;
import Imm.TYPE.PRIMITIVES.ENUM;
import Imm.TYPE.PRIMITIVES.VOID;
import Util.NamespacePath;
import Util.Source;

/**
 * This class represents a superclass for all AST-Nodes.
 */
public class EnumTypedef extends SyntaxElement {

			/* --- FIELDS --- */
	public NamespacePath path;
	
	public List<String> enums;
	
	public ENUM enumType;
	
	public List<ENUM> enumFields = new ArrayList();
	
	
			/* --- CONSTRUCTORS --- */
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
		for (int i = 0; i < this.enums.size(); i++) {
			this.enumFields.add(new ENUM(this, enums.get(i), mappingNum++));
		}
	}
	
	public ENUM getEnumField(String value, Source source) {
		for (ENUM t : this.enumFields) 
			if (t.fieldName.equals(value)) return t;
		
		throw new SNIPS_EXC("The enum " + this.path.build() + " does not contain the field " + value + ", " + source.getSourceMarker());
	}
	
	
			/* --- METHODS --- */
	public void print(int d, boolean rec) {
		System.out.println(this.pad(d) + "Enum Typedef <" + this.path.build() + ">");
		if (rec) {
			for (String e : this.enums) 
				System.out.println(this.pad(d + this.printDepthStep) + e);
		}
	}

	public TYPE check(ContextChecker ctx) throws CTX_EXC {
		return new VOID();
	}

	public void setContext(List<TYPE> context) throws CTX_EXC {
		return;
	}

	public void releaseContext() {
		return;
	}
	
}
