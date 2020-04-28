package Ctx;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map.Entry;

import Exc.CTX_EXCEPTION;
import Imm.AST.Statement.Declaration;
import Util.NamespacePath;
import Util.Source;

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
	 * @throws CTX_EXCEPTION 
	 */
	public Declaration getField(NamespacePath path, Source source) throws CTX_EXCEPTION {
		if (this.declarations.containsKey(path)) {
			return this.declarations.get(path);
		}
		else {
			if (this.parentScope != null) {
				return this.parentScope.getField(path, source);
			}
			else {
				/* Path can be null, for example through a deref lhs: *(p + 2) -> No path available, just return 0 */
				if (path == null) return null;
				
				if (path.path.size() == 1) {
					List<Declaration> decs = new ArrayList();
					
					for (Entry<NamespacePath, Declaration> entry : this.declarations.entrySet()) {
						if (entry.getKey().getLast().equals(path.getLast())) {
							decs.add(entry.getValue());
						}
					}
					
					/* Return if there is only one result */
					if (decs.size() == 1) return decs.get(0);
					/* Multiple results, cannot determine correct one, return null */
					else if (decs.isEmpty()) {
						throw new CTX_EXCEPTION(source, "Unknown variable: " + path.build());
					}
					else {
						String s = "";
						for (Declaration d0 : decs) s += d0.path.build() + ", ";
						s = s.substring(0, s.length() - 2);
						throw new CTX_EXCEPTION(source, "Found multiple matches for field '" + path.build() + "': " + s + ". Make sure that the namespace path is explicit");
					}
				}
				else {
					throw new CTX_EXCEPTION(source, "Unknown variable: " + path.build());
				}
			}
		}
	}
	
}
