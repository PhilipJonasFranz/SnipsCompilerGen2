package Opt.Util;

import java.util.HashMap;
import java.util.Map.Entry;
import java.util.Stack;

import Exc.SNIPS_EXC;
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
	
	private HashMap<String, Stack<Boolean>> settings = new HashMap();
	
	public ProgramContext(ProgramContext parent) {
		if (parent != null) {
			this.parent = parent;
			
			/* Copy state of parent */
			for (Entry<Declaration, VarState> entry : this.parent.cState.entrySet()) 
				this.cState.put(entry.getKey(), entry.getValue().clone());
			
			for (Entry<String, Stack<Boolean>> entry : this.parent.settings.entrySet()) 
				this.settings.put(entry.getKey(), (Stack<Boolean>) entry.getValue().clone());
		}
	}
	
	/**
	 * Returns wether the given setting is enabled or disabled.
	 */
	public boolean getSetting(String setting) {
		if (!this.settings.containsKey(setting)) return true;
		return this.settings.get(setting).peek();
	}
	
	/**
	 * Disable the given setting in the settings-registry.
	 */
	public void disableSetting(String setting) {
		if (!this.settings.containsKey(setting)) {
			Stack<Boolean> s = new Stack();
			s.push(true);
			
			this.settings.put(setting, s);
		}
		
		this.settings.get(setting).push(false);
	}
	
	/**
	 * Enable given setting in the settings-registry.
	 */
	public void enableSetting(String setting) {
		if (!this.settings.containsKey(setting)) {
			Stack<Boolean> s = new Stack();
			s.push(false);
			
			this.settings.put(setting, s);
		}
	
		this.settings.get(setting).pop();
	}
	
	public void transferContextChangeToParent() {
		if (this.parent != null) {
			
			/* Make sure settings registry is in consistent state. */
			for (Entry<String, Stack<Boolean>> entry : this.settings.entrySet()) {
				if (entry.getValue().size() > 1)
					throw new SNIPS_EXC("Setting-Registry is in inconsistent state! Setting: " + entry.getKey());
			}
			
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
					
					if (pState.currentValue != null && !pState.currentValue.equals(state.currentValue)) {
						/* 
						 * Value has changed in a child scopee of the parent. This
						 * means that it is no longer guaranteed that the current value
						 * will always be this expression. Hence we have to null the value.
						 */
						pState.currentValue = null;
					}
				}
			}
		}
	}
	
	public void register(Declaration dec) {
		if (!this.cState.containsKey(dec)) 
			this.cState.put(dec, new VarState(dec));
	}
	
	public void setRead(Declaration dec) {
		if (this.cState.containsKey(dec)) 
			this.cState.get(dec).read = true;
	}
	
	public void setWrite(Declaration dec) {
		if (this.cState.containsKey(dec)) 
			this.cState.get(dec).written = true;
	}
	
	public void setReferenced(Declaration dec) {
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
