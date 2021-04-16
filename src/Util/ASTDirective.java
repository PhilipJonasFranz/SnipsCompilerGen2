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
		 */
		INLINE;
		
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
		return this.properties.containsKey(key);
	}
	
	public String getProperty(String key) {
		return this.properties.get(key);
	}
	
} 
