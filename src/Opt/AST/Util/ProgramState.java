package Opt.AST.Util;

import java.util.HashMap;
import java.util.Map.Entry;
import java.util.Stack;

import Exc.SNIPS_EXC;
import Imm.AST.Expression.Expression;
import Imm.AST.Statement.Declaration;
import Snips.CompilerDriver;

public class ProgramState {
		
	public static class VarState {
		
		public Declaration declaration;
		
		private Expression currentValue;
		
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
			else
				state0.currentValue = null;
			
			return state0;
		}
		
		public Expression getCurrentValue() {
			return (this.currentValue != null)? this.currentValue.clone() : null;
		}
		
		public void setCurrentValue(Expression e) {
			this.currentValue = e.clone();
		}
		
		public void clearCurrentValue() {
			this.currentValue = null;
		}
		
	}
	
	public boolean isLoopedContext;
	
	private HashMap<Declaration, VarState> cState = new HashMap();
	
	public ProgramState parent;
	
	private HashMap<String, Stack<Boolean>> settings = new HashMap();
	
	public ProgramState(ProgramState parent, boolean isLoopedContext) {
		this.isLoopedContext = isLoopedContext;
		
		if (parent != null) {
			
			this.parent = parent;
			
			/* Copy state of parent */
			for (Entry<Declaration, VarState> entry : this.parent.cState.entrySet()) 
				this.cState.put(entry.getKey(), entry.getValue().clone());
			
			this.settings = this.parent.settings;
		}
	}
	
	/**
	 * Returns the VarState associated with the given declaration.
	 * May return null if the Declaration is not registered.
	 */
	public VarState get(Declaration origin) {
		return this.cState.get(origin);
	}
	
	/**
	 * Removes the VarState mapping associated with the given
	 * declaration from the cState map.
	 */
	public VarState remove(Declaration origin) {
		return this.cState.remove(origin);
	}
	
	public HashMap<Declaration, VarState> getCState() {
		return this.cState;
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
	public void pushSetting(String setting, boolean value) {
		if (!this.settings.containsKey(setting)) {
			Stack<Boolean> s = new Stack();
			this.settings.put(setting, s);
		}
		
		this.settings.get(setting).push(value);
	}
	
	/**
	 * Enable given setting in the settings-registry.
	 */
	public void popSetting(String setting) {
		if (!this.settings.containsKey(setting)) {
			Stack<Boolean> s = new Stack();
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
					
					if (pState.written) {
						/* 
						 * Value has changed in a child scopee of the parent. This
						 * means that it is no longer guaranteed that the current value
						 * will always be this expression. Hence we have to null the value.
						 */
						pState.currentValue = null;
					}
				}
			}
			
			this.parent.settings = this.settings;
		}
	}
	
	public boolean isInLoopedScope(Declaration dec) {
		if (this.cState.containsKey(dec)) {
			if (this.parent == null || !this.parent.cState.containsKey(dec)) return false;
			else return this.isLoopedContext || this.parent.isInLoopedScope(dec);
		}
		else return false;
	}
	
	public boolean isDeclarationScope(Declaration dec) {
		if (this.cState.containsKey(dec)) {
			return this.parent == null || !this.parent.cState.containsKey(dec);
		}
		else return false;
	}
	
	public void register(Declaration dec) {
		if (!this.cState.containsKey(dec)) 
			this.cState.put(dec, new VarState(dec));
	}
	
	public void setRead(Declaration dec) {
		if (this.cState.containsKey(dec)) 
			this.cState.get(dec).read = true;
	}
	
	public void setWrite(Declaration dec, boolean value) {
		if (this.cState.containsKey(dec)) 
			this.cState.get(dec).written = value;
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
	
	/**
	 * Returns wether given variable was either referenced, modified or read.
	 */
	public boolean getAll(Declaration dec) {
		if (this.cState.containsKey(dec)) 
			return this.cState.get(dec).referenced || this.cState.get(dec).written || this.cState.get(dec).read;
		return false;
	}
	
	public void print() {
		CompilerDriver.outs.println(" ------ State Dump ------ ");
		CompilerDriver.outs.println("Looped: " + this.isLoopedContext);
		for (Entry<Declaration, VarState> entry : this.cState.entrySet()) {
			VarState state = entry.getValue();
			
			String s = entry.getKey().path + " ";
			
			if (state.read) s += "read, ";
			if (state.referenced) s += "referenced, ";
			if (state.written) s += "write, ";
			
			if (state.currentValue != null) s += state.currentValue.codePrint();
			else s += "null";
			
			s += ", isLooped: " + this.isInLoopedScope(state.declaration);
			
			CompilerDriver.outs.println(s);
		}
		CompilerDriver.outs.println(" ------------------------ ");
	}
	
}
