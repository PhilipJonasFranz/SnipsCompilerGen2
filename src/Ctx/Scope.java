package Ctx;

import java.util.HashMap;

import Exc.CTX_EXCEPTION;
import Imm.AST.Statement.Declaration;

public class Scope {

	Scope parentScope;
	
	HashMap<String, Declaration> declarations = new HashMap();
	
	public Scope(Scope parentScope) {
		this.parentScope = parentScope;
	}
	
	public void addDeclaration(Declaration dec) throws CTX_EXCEPTION {
		this.checkDuplicate(dec);
		this.declarations.put(dec.fieldName, dec);
	}
	
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
	
	public Declaration getField(String name) {
		if (this.declarations.containsKey(name)) {
			return this.declarations.get(name);
		}
		else {
			if (this.parentScope != null) {
				return this.parentScope.getField(name);
			}
			else {
				return null;
			}
		}
	}
	
}
