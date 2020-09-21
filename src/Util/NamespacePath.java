package Util;

import java.util.ArrayList;
import java.util.List;

public class NamespacePath {

	public enum PATH_TERMINATION {
		UNKNOWN, STRUCT, ENUM;
	}
	
			/* --- FIELDS --- */
	public List<String> path;
	
	public PATH_TERMINATION termination = PATH_TERMINATION.UNKNOWN;
	
	
			/* --- CONSTRUCTORS --- */
	public NamespacePath(List<String> path) {
		this.path = path;
	}
	
	public NamespacePath(List<String> path, PATH_TERMINATION termination) {
		this.path = path;
		this.termination = termination;
	}
	
	public NamespacePath(String path, PATH_TERMINATION termination) {
		this.path = new ArrayList();
		this.path.add(path);
		this.termination = termination;
	}
	
	public NamespacePath(String name) {
		this.path = new ArrayList();
		this.path.add(name);
	}
	
	public NamespacePath() {
		this.path = new ArrayList();
	}
	
	
			/* --- METHODS --- */
	public String getLast() {
		return this.path.get(this.path.size() - 1);
	}
	
	public String build() {
		String s = this.path.get(0);
		if (this.path.size() > 1) {
			for (int i = 1; i < this.path.size(); i++) {
				s += "." + this.path.get(i);
			}
		}
		return s;
	}
	
	public String buildPathOnly() {
		String s = "";
		for (int i = 0; i < this.path.size() - 1; i++) {
			s += this.path.get(i) + ".";
		}
		if (!s.isEmpty()) s = s.substring(0, s.length() - 1);
		return s;
	}
	
	public List<String> getPath() {
		return this.path;
	}
	
	public NamespacePath clone() {
		NamespacePath clone = new NamespacePath();
		clone.termination = this.termination;
		for (String s : this.path) clone.path.add(s);
		return clone;
	}
	
} 
