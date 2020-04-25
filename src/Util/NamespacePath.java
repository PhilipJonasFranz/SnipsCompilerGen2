package Util;

import java.util.ArrayList;
import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class NamespacePath {

			/* --- FIELDS --- */
	public List<String> path;
	
	
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
