package Util;

public class Source {

			/* --- FIELDS --- */
	private String sourceFile;
	
	public int row, col;
	
	
			/* --- CONSTRUCTORS --- */
	public Source(String sourceFile, int row, int col) {
		this.sourceFile = sourceFile;
		this.row = row;
		this.col = col;
	}
	
	
			/* --- METHODS --- */
	public String getSourceMarker() {
		return "line: " + this.row + ", column: " + this.col + " (" + this.sourceFile + ")";
	}
	
	public Source clone() {
		return new Source(this.sourceFile, this.row, this.col);
	}
	
}
