package Util;

public class Source {

			/* ---< FIELDS >--- */
	/**
	 * The filename where this source was created. Does
	 * not include the full path to the file.
	 */
	public String sourceFile;
	
	/**
	 * The row and the column of this source.
	 */
	public int row, col;
	
	
			/* ---< CONSTRUCTORS >--- */
	public Source(String sourceFile, int row, int col) {
		this.sourceFile = sourceFile;
		this.row = row;
		this.col = col;
	}
	
	
			/* ---< METHODS >--- */
	public String getSourceMarker() {
		return "line: " + this.row + ", column: " + this.col + " (" + this.sourceFile + ")";
	}
	
	public String getSourceMarkerWithoutFile() {
		return "line: " + this.row + ", column: " + this.col;
	}
	
	public Source clone() {
		return new Source(this.sourceFile, this.row, this.col);
	}
	
} 
