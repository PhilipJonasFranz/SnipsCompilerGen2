package Util;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class NamespacePath {

			/* ---< NESTED >--- */
	/**
	 * Signals where this namespace path was parsed. For example, 
	 * with the code:<br>
	 * <br>
	 * <code>Namespace::StructName<Provisos...>::(Params);</code><br>
	 * <br>
	 * The path termination would be set to struct. With the code:<br>
	 * <br>
	 * <code>Namespace::EnumName.Value;</code><br>
	 * <br>
	 * The path termination would be set to Enum. In any other case, the 
	 * termination is set to unknown.
	 */
	public enum PATH_TERMINATION {
		UNKNOWN, INTERFACE, STRUCT, ENUM;
	}
	
	
			/* ---< FIELDS >--- */
	/** 
	 * The parsed namespace path parts. For example, with the namespace path:<br>
	 * <br>
	 * <code>Name1::Name2::Target</code><br>
	 * <br>
	 * The resulting list would be:<br>
	 * <br>
	 * <code>[Name1, Name2, Target]</code>
	 */
	public List<String> path = new ArrayList();
	
	/** The termination for this namespace path. See {@link #PATH_TERMINATION} for more information. */
	public PATH_TERMINATION termination = PATH_TERMINATION.UNKNOWN;
	
	
			/* ---< CONSTRUCTORS >--- */
	public NamespacePath(List<String> path) {
		this.path = path;
	}
	
	public NamespacePath(List<String> path, PATH_TERMINATION termination) {
		this.path = path;
		this.termination = termination;
	}
	
	public NamespacePath(String path, PATH_TERMINATION termination) {
		this.path.add(path);
		this.termination = termination;
	}
	
	public NamespacePath(String name) {
		this.path.add(name);
	}
	
	public NamespacePath() {
		
	}
	
	
			/* ---< METHODS >--- */
	public String getLast() {
		return this.path.get(this.path.size() - 1);
	}
	
	/**
	 * Builds the namespace path into a string of the form<br>
	 * <br>
	 * <code>Name1.Name2.Target</code>
	 */
	public String build() {
		return this.path.stream().collect(Collectors.joining("."));
	}
	
	/**
	 * Builds the path like {@link #build()}, but ignores
	 * the last path part.
	 */
	public String buildPathOnly() {
		String s = this.path.stream().collect(Collectors.joining("."));
		
		if (!this.path.isEmpty()) {
			s = s.substring(0, s.length() - this.path.get(this.path.size() - 1).length());
			if (s.endsWith(".")) s = s.substring(0, s.length() - 1);
		}
		
		return s;
	}
	
	/** Returns the path in form of a list of strings. */
	public List<String> getPath() {
		return this.path;
	}
	
	public NamespacePath clone() {
		NamespacePath clone = new NamespacePath();
		clone.termination = this.termination;
		for (String s : this.path) clone.path.add(s);
		return clone;
	}
	
	/**
	 * Relays to {@link #build()}.
	 */
	public String toString() {
		return this.build();
	}
	
	public boolean equals(Object object) {
		return object instanceof NamespacePath && this.toString().equals(object.toString());
	}
	
} 
