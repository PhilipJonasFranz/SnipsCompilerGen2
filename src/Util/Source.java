package Util;

public class Source {

	private int row, col;
	
	public Source(int row, int col) {
		this.row = row;
		this.col = col;
	}
	
	public String getSourceMarker() {
		return "line: " + this.row + ", column: " + this.col;
	}
	
	public Source clone() {
		return new Source(this.row, this.col);
	}
	
}
