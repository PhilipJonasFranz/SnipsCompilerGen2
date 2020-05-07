package Util;

import java.util.ArrayList;
import java.util.List;

import lombok.Data;

@Data
public class NamespacePath {

	public enum PATH_TERMINATION {
		UNKNOWN, STRUCT, ENUM;
	}
	
			/* --- FIELDS --- */
	public List<String> path;
	
	public PATH_TERMINATION termination = PATH_TERMINATION.UNKNOWN;
	
	
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
	
	
			/* --- CONSTRUCTORS --- */
	public NamespacePath(String name) {
		this.path = new ArrayList();
		this.path.add(name);
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
	
}
