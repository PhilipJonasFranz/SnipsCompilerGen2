package Tst;

public class ResultCnt {
		
			/* --- FIELDS --- */
	public int failed = 0, crashed = 0, timeout = 0;
	
	
			/* --- CONSTRUCTORS -- */
	public ResultCnt() {
		
	}
	
	public ResultCnt(int failed, int crashed, int timeout) {
		this.failed = failed;
		this.crashed = crashed;
		this.timeout = timeout;
	}
	
	
			/* --- METHODS --- */
	public void add(ResultCnt cnt) {
		this.failed += cnt.failed;
		this.crashed += cnt.crashed;
		this.timeout += cnt.timeout;
	}
	
	public int getFailed() {
		return this.failed;
	}
	
	public int getCrashed() {
		return this.crashed;
	}
	
	public int getTimeout() {
		return this.timeout;
	}
	
} 
