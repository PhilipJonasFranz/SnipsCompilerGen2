package Util;

import lombok.AllArgsConstructor;

@AllArgsConstructor
public class Source {

			/* --- FIELDS --- */
	private String sourceFile;
	
	private int row, col;
	
	
			/* --- METHODS --- */
	public String getSourceMarker() {
		return "line: " + this.row + ", column: " + this.col + " (" + this.sourceFile + ")";
	}
	
	public Source clone() {
		return new Source(this.sourceFile, this.row, this.col);
	}
	
}
