package Util;

import java.util.HashMap;
import java.util.Map.Entry;

public class ASTDirective {

			/* ---< NESTED >--- */
	public enum DIRECTIVE {
		
		/**
		 * If set, the AST-Optimizer will attempt to unroll the loop.
		 * 
		 * Properties:
		 * 		- depth = n 		: The maximum depth the loop should be unrolled.
		 */
		UNROLL,
		
		/**
		 * If set, the AST-Optimizer will attempt to inline this function
		 * into where it is used. This may lead to code-duplication. It
		 * is recommended to use the ALWAYS optimization strategy, since
		 * otherwise changes may be discarded.
		 * 
		 * Properties:
		 * 		- depth = n 		: The maximum depth the function should 
		 * 								be inlined, only relevant for recursion.
		 */
		INLINE,
		
		/**
		 * If set the AST-Optimizer will skip a function annotated with this
		 * directive. If a function is manipulating memory directly or uses
		 * direct stack accesses, it can be safer to exclude it from optimizing.
		 */
		UNSAFE,
		
		/**
		 * If set to a function, the optimization strategy will be overwritten
		 * for the scope of this function.
		 * 
		 * Properties:
		 * 		- always			: Sets the strategy 'ALWAYS' for the target function.
		 * 		- on_improvement	: Sets the strategy 'ON_IMPROVEMENT' for the target function.
		 */
		STRATEGY,
		
		/**
		 * If set to a function, this function is considered not state changing. This
		 * means that the function ensures that it only depends on input variables
		 * and does not change the programs state by calling other functions, modifying
		 * global variables or the heap etc.
		 */
		PREDICATE;
		
	}
	
	
			/* ---< FIELDS >--- */
	public DIRECTIVE type;
	
	public HashMap<String, String> properties;
	
	
			/* ---< CONSTRUCTORS >--- */
	public ASTDirective(DIRECTIVE type, HashMap<String, String> properties) {
		this.type = type;
		this.properties = properties;
	}
	
	public ASTDirective(DIRECTIVE type) {
		this.type = type;
	}
	
	
			/* ---< METHODS >--- */
	public ASTDirective clone() {
		ASTDirective annotation = new ASTDirective(this.type);
		for (Entry<String, String> entry : this.properties.entrySet()) {
			annotation.properties.put(entry.getKey(), entry.getValue());
		}
		return annotation;
	}
	
	public DIRECTIVE type() {
		return this.type;
	}
	
	
	public boolean hasProperty(String key) {
		return this.properties.containsKey(key.toLowerCase());
	}
	
	public String getProperty(String key) {
		return this.properties.get(key.toLowerCase());
	}
	
} 
