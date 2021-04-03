package Opt.Util;

import java.util.HashMap;
import java.util.Map.Entry;

import Imm.AST.Expression.Expression;
import Imm.AST.Statement.Declaration;

public class ProgramContext {
		
	public static class VarState {
		
		public Declaration declaration;
		
		public Expression currentValue;
		
		public boolean 	written = false,		// Wether this variable was overwritten since declaration
						read = false,			// Wether this variable was read sincee declaration
						referenced = false;		// Wether an address reference was created to this declaration
		
		public VarState(Declaration declaration) {
			this.declaration = declaration;
			
			if (declaration.value != null) 
				this.currentValue = declaration.value.clone();
		}
		
		public VarState clone() {
			VarState state0 = new VarState(this.declaration);
			state0.written = this.written;
			state0.read = this.read;
			state0.referenced = this.referenced;
			
			if (this.currentValue != null) 
				state0.currentValue = this.currentValue.clone();
			
			return state0;
		}
		
	}
	
	public HashMap<Declaration, VarState> cState = new HashMap();
	
	public ProgramContext parent;
	
	public ProgramContext(ProgramContext parent) {
		if (parent != null) {
			this.parent = parent;
			
			/* Copy state of parent */
			for (Entry<Declaration, VarState> entry : this.parent.cState.entrySet()) 
				this.cState.put(entry.getKey(), entry.getValue().clone());
		}
	}
	
	public void transferContextChangeToParent() {
		if (this.parent != null) {
			for (Entry<Declaration, VarState> entry : this.cState.entrySet()) {
				Declaration dec = entry.getKey();
				VarState state = entry.getValue();
				
				/* 
				 * Parent also contains state information of this variable on a higher
				 * scope of the program. Transfer the state changes to the parent, but only relevant
				 * ones. We do not propagate the value changes of the variable. When a value change
				 * is capsuled, we cannot assume that the capsulation is always taken, for example
				 * if-statement. Instead we can tell the parent that there was a modification to
				 * this variable, and prevent further substitution after this statement.
				 */
				if (this.parent.cState.containsKey(dec)) {
					VarState pState = this.parent.cState.get(dec);
					
					/* Propagate changes to the variables state back up */
					pState.written |= state.written;
					pState.read |= state.read;
					pState.referenced |= state.referenced;
				}
			}
		}
	}
	
	public void add(Declaration dec) {
		if (!this.cState.containsKey(dec)) 
			this.cState.put(dec, new VarState(dec));
	}
	
	public void notifyRead(Declaration dec) {
		if (this.cState.containsKey(dec)) 
			this.cState.get(dec).read = true;
	}
	
	public void notifyWrite(Declaration dec) {
		if (this.cState.containsKey(dec)) 
			this.cState.get(dec).written = true;
	}
	
	public void notifyReference(Declaration dec) {
		if (this.cState.containsKey(dec)) 
			this.cState.get(dec).referenced = true;
	}
	
	public boolean getRead(Declaration dec) {
		if (this.cState.containsKey(dec)) 
			return this.cState.get(dec).read;
		return false;
	}
	
	public boolean getWrite(Declaration dec) {
		if (this.cState.containsKey(dec)) 
			return this.cState.get(dec).written;
		return false;
	}
	
	public boolean getReferenced(Declaration dec) {
		if (this.cState.containsKey(dec)) 
			return this.cState.get(dec).referenced;
		return false;
	}
	
}
