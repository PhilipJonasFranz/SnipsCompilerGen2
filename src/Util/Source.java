package Util;

import lombok.AllArgsConstructor;

@AllArgsConstructor
public class Source {

			/* --- FIELDS --- */
	private int row, col;
	
	
			/* --- METHODS --- */
	public String getSourceMarker() {
		return "line: " + this.row + ", column: " + this.col;
	}
	
	public Source clone() {
		return new Source(this.row, this.col);
	}
	
}
