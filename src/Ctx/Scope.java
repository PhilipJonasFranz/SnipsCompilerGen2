package Ctx;

import Exc.CTEX_EXC;
import Imm.AST.Statement.Declaration;
import Res.Const;
import Snips.CompilerDriver;
import Util.Logging.LogPoint;
import Util.Logging.Message;
import Util.NamespacePath;
import Util.Pair;
import Util.Source;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map.Entry;
import java.util.stream.Collectors;

/**
 * A scope capsules a reference to its parent scope and a 
 * hash map of declarations paired to their field names.
 */
public class Scope {

			/* ---< FIELDS >--- */
	/** 
	 * Reference to the parent scope. Is null if this is the super scope. 
	 */
	private Scope parentScope;
	
	/** 
	 * Set to true if scope is part of a loop 
	 */
	boolean isLoopedScope = false;
	
	/** 
	 * Stores all the declarations made in this scope. 
	 */
	HashMap<String, Pair<Declaration, NamespacePath>> declarations = new HashMap();
	
	
			/* ---< CONSTRUCTORS >--- */
	/** 
	 * Create a new scope and set the parent scope. 
	 */
	public Scope(Scope parentScope) {
		this.parentScope = parentScope;
	}
	
	/**
	 * Create a new scope and set parent scope. Also tag this scope
	 * as a looped scope. Looped means that it is for examplee the 
	 * scope created for the body of a for-loop.
	 */
	public Scope(Scope parentScope, boolean isLoopedScope) {
		this.parentScope = parentScope;
		this.isLoopedScope = isLoopedScope;
	}
	
	/** 
	 * Print out the current scope and all parent scopes and the stored declarations 
	 */
	public void print(int d) {
		if (d != 0) CompilerDriver.outs.println("--- SCOPE ---");
		else CompilerDriver.outs.println("--- TOP SCOPE ---");
		
		for (Entry<String, Pair<Declaration, NamespacePath>> dec : this.declarations.entrySet()) 
			dec.getValue().first.print(d, true);

		if (this.parentScope != null) this.parentScope.print(d + 4);
	}
	
	/** 
	 * Add a new declaration to this scope. Checks for duplicates if checkDups is true. 
	 */
	public void addDeclaration(Declaration dec, boolean checkDups) throws CTEX_EXC {
		if (checkDups) this.checkDuplicate(dec);
		this.declarations.put(dec.path.build(), new Pair<>(dec, dec.path));
	}
	
	/** Add a new declaration to this scope. Checks for duplicates. */
	public Message addDeclaration(Declaration dec) throws CTEX_EXC {
		Message m = this.checkDuplicate(dec);
		this.declarations.put(dec.path.build(), new Pair<>(dec, dec.path));
		return m;
	}
	
	/** 
	 * Check if this scope or any of the parent scopes contains a declaration with the identifier
	 * of the given declaration. Throws a CTX_EXCEPTION if this is the case.
	 */
	public Message checkDuplicate(Declaration dec) throws CTEX_EXC {
		if (this.declarations.containsKey(dec.path.build())) {
			throw new CTEX_EXC(dec, Const.DUPLICATE_FIELD_NAME, dec.path);
		}
		else {
			if (this.parentScope != null) {
				Declaration dec0;
				if ((dec0 = this.parentScope.checkDuplicateRec(dec)) != null) {
					if (!CompilerDriver.disableWarnings) {
						return new Message(String.format(Const.VARIABLE_SHADOWED_BY, dec0.path, dec0.getSource().getSourceMarker(), dec.path.build(), dec.getSource().getSourceMarker()), LogPoint.Type.WARN, true);
					}
				}
			}
		}
		
		return null;
	}
	
	/**
	 * Internal recursive function to check for duplicate declaration names. 
	 * Checks through the parent scopes.
	 */
	private Declaration checkDuplicateRec(Declaration dec) {
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
	 */
	public Declaration getField(NamespacePath path, Source source) throws CTEX_EXC {
		if (path != null && this.declarations.containsKey(path.build())) {
			return this.declarations.get(path.build()).first;
		}
		else {
			if (this.parentScope != null) {
				return this.parentScope.getField(path, source);
			}
			else {
				/* 
				 * Path can be null, for example through a deref lhs: 
				 * *(p + 2) -> No path available, just return null 
				 */
				if (path == null) return null;
				
				if (path.path.size() == 1) {
					List<Declaration> decs = new ArrayList();
					
					for (Entry<String, Pair<Declaration, NamespacePath>> entry : this.declarations.entrySet()) 
						if (entry.getValue().second.getLast().equals(path.getLast())) 
							decs.add(entry.getValue().first);
					
					/* Return if there is only one result */
					if (decs.size() == 1) return decs.get(0);
					/* Multiple results, cannot determine correct one, return null */
					else if (decs.isEmpty()) {
						throw new CTEX_EXC(source, Const.UNKNOWN_VARIABLE, path);
					}
					else {
						String s = decs.stream().map(x -> x.path.build()).collect(Collectors.joining(", "));
						throw new CTEX_EXC(source, Const.MULTIPLE_MATCHES_FOR_X, "field", path, s);
					}
				}
				else throw new CTEX_EXC(source, Const.UNKNOWN_VARIABLE, path);
			}
		}
	}
	
	/**
	 * Same as {@link #getField(NamespacePath, Source)}, but returns null instead of
	 * throwing exception.
	 */
	public Declaration getFieldNull(NamespacePath path) {
		if (path != null && this.declarations.containsKey(path.build())) {
			return this.declarations.get(path.build()).first;
		}
		else {
			if (this.parentScope != null) {
				return this.parentScope.getFieldNull(path);
			}
			else {
				/* 
				 * Path can be null, for example through a deref lhs: 
				 * *(p + 2) -> No path available, just return 0 
				 */
				if (path == null) return null;
				
				if (path.path.size() == 1) {
					List<Declaration> decs = new ArrayList();
					
					for (Entry<String, Pair<Declaration, NamespacePath>> entry : this.declarations.entrySet()) 
						if (entry.getValue().second.getLast().equals(path.getLast())) 
							decs.add(entry.getValue().first);
					
					/* Return if there is only one result */
					if (decs.size() == 1) return decs.get(0);
				}
			}
		}
		
		return null;
	}

	/**
	 * Returns all currently active declarations in this scope.
	 */
	public List<Declaration> getDeclarationsInScope() {
		List<Declaration> immutableListDecs = this.declarations.values().stream().map(x -> x.first).toList();
		List<Declaration> mutableListDecs = new ArrayList<>(immutableListDecs);
		return mutableListDecs;
	}

	/**
	 * Returns all currently active declarations, in this and all parent scopes.
	 */
	public List<Declaration> getAllDeclarations() {
		if (this.parentScope == null) return this.getDeclarationsInScope();
		else {
			List<Declaration> fromParent = this.parentScope.getAllDeclarations();
			fromParent.addAll(this.getDeclarationsInScope());
			return fromParent;
		}
	}

	/**
	 * Returns all currently active declarations, in this and all parent scopes.
	 * Excludes declarations in the root-scope, or global variables.
	 */
	public List<Declaration> getAllDeclarationsExceptGlobal() {
		if (this.parentScope == null) return new ArrayList();
		else {
			List<Declaration> fromParent = this.parentScope.getAllDeclarations();
			fromParent.addAll(this.getDeclarationsInScope());
			return fromParent;
		}
	}
	
} 
