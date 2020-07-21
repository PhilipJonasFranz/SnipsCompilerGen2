package Ctx;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map.Entry;

import Exc.CTX_EXC;
import Imm.AST.Statement.Declaration;
import Snips.CompilerDriver;
import Util.NamespacePath;
import Util.Pair;
import Util.Source;
import Util.Logging.Message;

/**
 * A scope capsules a reference to its parent scope and a 
 * hash map of declarations paired to their field names.
 */
public class Scope {

			/* --- FIELDS --- */
	/** Reference to the parent scope. Is null if this is the super scope. */
	Scope parentScope;
	
	/** Stores all the declarations made in this scope. */
	HashMap<String, Pair<Declaration, NamespacePath>> declarations = new HashMap();
	
	
			/* --- CONSTRUCTORS --- */
	/** Create a new scope and set the parent scope. */
	public Scope(Scope parentScope) {
		this.parentScope = parentScope;
	}
	
	/** Print out the current scope and all parent scopes and the stored declarations */
	public void print(int d) {
		if (d != 0) System.out.println("--- SCOPE ---");
		else System.out.println("--- TOP SCOPE ---");
		for (Entry<String, Pair<Declaration, NamespacePath>> dec : this.declarations.entrySet()) {
			dec.getValue().first.print(d, true);
		}
		if (this.parentScope != null) this.parentScope.print(d + 4);
	}
	
	/** Add a new declaration to this scope. Checks for duplicates if checkDups is true. */
	public void addDeclaration(Declaration dec, boolean checkDups) throws CTX_EXC {
		if (checkDups) this.checkDuplicate(dec);
		this.declarations.put(dec.path.build(), new Pair<Declaration, NamespacePath>(dec, dec.path));
	}
	
	/** Add a new declaration to this scope. Checks for duplicates. */
	public Message addDeclaration(Declaration dec) throws CTX_EXC {
		Message m = this.checkDuplicate(dec);
		this.declarations.put(dec.path.build(), new Pair<Declaration, NamespacePath>(dec, dec.path));
		return m;
	}
	
	/** 
	 * Check if this scope or any of the parent scopes contains a declaration with the identifier
	 * of the given declaration. Throws a CTX_EXCEPTION if this is the case.
	 */
	public Message checkDuplicate(Declaration dec) throws CTX_EXC {
		if (this.declarations.containsKey(dec.path.build())) {
			throw new CTX_EXC(dec.getSource(), "Duplicate field name: " + dec.path.build());
		}
		else {
			if (this.parentScope != null) {
				Declaration dec0 = null;
				if ((dec0 = this.parentScope.checkDuplicateRec(dec)) != null) {
					if (!CompilerDriver.disableWarnings) {
						return new Message("Variable '" + dec0.path.build() + "' at " + dec0.getSource().getSourceMarker() + " shadowed by '" + dec.path.build() + "' at " + dec.getSource().getSourceMarker(), Message.Type.WARN, true);
					}
				}
			}
		}
		
		return null;
	}
	
	private Declaration checkDuplicateRec(Declaration dec) throws CTX_EXC {
		if (this.declarations.containsKey(dec.path.build())) {
			return this.declarations.get(dec.path.build()).first;
		}
		else {
			if (this.parentScope != null) {
				return this.parentScope.checkDuplicateRec(dec);
			}
			else return null;
		}
	}
	
	/** 
	 * Returns the declaration with given field name from this scope or any of the parent scopes. 
	 * Returns null if the field is not found.
	 * @throws CTX_EXC 
	 */
	public Declaration getField(NamespacePath path, Source source) throws CTX_EXC {
		if (path != null && this.declarations.containsKey(path.build())) {
			return this.declarations.get(path.build()).first;
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
					
					for (Entry<String, Pair<Declaration, NamespacePath>> entry : this.declarations.entrySet()) {
						if (entry.getValue().second.getLast().equals(path.getLast())) {
							decs.add(entry.getValue().first);
						}
					}
					
					/* Return if there is only one result */
					if (decs.size() == 1) return decs.get(0);
					/* Multiple results, cannot determine correct one, return null */
					else if (decs.isEmpty()) {
						throw new CTX_EXC(source, "Unknown variable: " + path.build());
					}
					else {
						String s = "";
						for (Declaration d0 : decs) s += d0.path.build() + ", ";
						s = s.substring(0, s.length() - 2);
						throw new CTX_EXC(source, "Multiple matches for field '" + path.build() + "': " + s + ". Ensure namespace path is explicit and correct");
					}
				}
				else {
					throw new CTX_EXC(source, "Unknown variable: " + path.build());
				}
			}
		}
	}
	
	/**
	 * Same as {@link #getField(NamespacePath, Source)}, but returns null instead of
	 * throwing exception.
	 */
	public Declaration getFieldNull(NamespacePath path, Source source) {
		if (path != null && this.declarations.containsKey(path.build())) {
			return this.declarations.get(path.build()).first;
		}
		else {
			if (this.parentScope != null) {
				return this.parentScope.getFieldNull(path, source);
			}
			else {
				/* Path can be null, for example through a deref lhs: *(p + 2) -> No path available, just return 0 */
				if (path == null) return null;
				
				if (path.path.size() == 1) {
					List<Declaration> decs = new ArrayList();
					
					for (Entry<String, Pair<Declaration, NamespacePath>> entry : this.declarations.entrySet()) {
						if (entry.getValue().second.getLast().equals(path.getLast())) {
							decs.add(entry.getValue().first);
						}
					}
					
					/* Return if there is only one result */
					if (decs.size() == 1) return decs.get(0);
				}
			}
		}
		
		return null;
	}
	
}
