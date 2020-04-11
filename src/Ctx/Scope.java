package Ctx;

import java.util.HashMap;

import Exc.CTX_EXCEPTION;
import Imm.AST.Statement.Declaration;

/**
 * A scope capsules a reference to its parent scope and a 
 * hash map of declarations paired to their field names.
 */
public class Scope {

			/* --- FIELDS --- */
	/** Reference to the parent scope. Is null if this is the super scope. */
	Scope parentScope;
	
	HashMap<String, Declaration> declarations = new HashMap();
	
	
			/* --- CONSTRUCTORS --- */
	/** Create a new scope and set the parent scope. */
	public Scope(Scope parentScope) {
		this.parentScope = parentScope;
	}
	
	/** Add a new declaration to this scope. Checks for duplicates. */
	public void addDeclaration(Declaration dec) throws CTX_EXCEPTION {
		this.checkDuplicate(dec);
		this.declarations.put(dec.fieldName, dec);
	}
	
	/** 
	 * Check if this scope or any of the parent scopes contains a declaration with the identifier
	 * of the given declaration. Throws a CTX_EXCEPTION if this is the case.
	 */
	public void checkDuplicate(Declaration dec) throws CTX_EXCEPTION {
		if (this.declarations.containsKey(dec.fieldName)) {
			throw new CTX_EXCEPTION(dec.getSource(), "Duplicate field name: " + dec.fieldName);
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
	public Declaration getField(String name) {
		if (this.declarations.containsKey(name)) {
			return this.declarations.get(name);
		}
		else {
			if (this.parentScope != null) {
				return this.parentScope.getField(name);
			}
			else return null;
		}
	}
	
}
