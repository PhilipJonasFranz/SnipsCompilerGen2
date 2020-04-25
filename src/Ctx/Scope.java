package Ctx;

import java.util.HashMap;
import java.util.Map.Entry;

import Exc.CTX_EXCEPTION;
import Imm.AST.Statement.Declaration;
import Util.NamespacePath;

/**
 * A scope capsules a reference to its parent scope and a 
 * hash map of declarations paired to their field names.
 */
public class Scope {

			/* --- FIELDS --- */
	/** Reference to the parent scope. Is null if this is the super scope. */
	Scope parentScope;
	
	HashMap<NamespacePath, Declaration> declarations = new HashMap();
	
	
			/* --- CONSTRUCTORS --- */
	/** Create a new scope and set the parent scope. */
	public Scope(Scope parentScope) {
		this.parentScope = parentScope;
	}
	
	public void print(int d) {
		if (d != 0) System.out.println("--- SCOPE ---");
		else System.out.println("--- TOP SCOPE ---");
		for (Entry<NamespacePath, Declaration> dec : this.declarations.entrySet()) {
			dec.getValue().print(d, true);
		}
		if (this.parentScope != null) this.parentScope.print(d + 4);
	}
	
	/** Add a new declaration to this scope. Checks for duplicates if checkDups is true. */
	public void addDeclaration(Declaration dec, boolean checkDups) throws CTX_EXCEPTION {
		if (checkDups) this.checkDuplicate(dec);
		this.declarations.put(dec.path, dec);
	}
	
	/** Add a new declaration to this scope. Checks for duplicates. */
	public void addDeclaration(Declaration dec) throws CTX_EXCEPTION {
		this.checkDuplicate(dec);
		this.declarations.put(dec.path, dec);
	}
	
	/** 
	 * Check if this scope or any of the parent scopes contains a declaration with the identifier
	 * of the given declaration. Throws a CTX_EXCEPTION if this is the case.
	 */
	public void checkDuplicate(Declaration dec) throws CTX_EXCEPTION {
		if (this.declarations.containsKey(dec.path)) {
			throw new CTX_EXCEPTION(dec.getSource(), "Duplicate field name: " + dec.path.build());
		}
		else {
			if (this.parentScope != null) {
				this.parentScope.checkDuplicate(dec);
			}
		}
	}
	
	/** 
	 * Returns the declaration with given field name from this scope or any of the parent scopes. 
	 * Returns null if the field is not found.
	 */
	public Declaration getField(NamespacePath path) {
		if (this.declarations.containsKey(path)) {
			return this.declarations.get(path);
		}
		else {
			if (this.parentScope != null) {
				return this.parentScope.getField(path);
			}
			else return null;
		}
	}
	
}
