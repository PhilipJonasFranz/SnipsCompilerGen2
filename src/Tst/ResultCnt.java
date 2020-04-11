package Tst;

public class ResultCnt {
		
	public int failed = 0, crashed = 0, timeout = 0;
	
	public ResultCnt() {
		
	}
	
	public ResultCnt(int f, int c, int t) {
		this.failed = f;
		this.crashed = c;
		this.timeout = t;
	}
	
	public void add(ResultCnt cnt) {
		this.failed += cnt.failed;
		this.crashed += cnt.crashed;
		this.timeout += cnt.timeout;
	}
	
}
