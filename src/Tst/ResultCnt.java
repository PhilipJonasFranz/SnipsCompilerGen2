package Tst;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ResultCnt {
		
			/* --- FIELDS --- */
	public int failed = 0, crashed = 0, timeout = 0;
	
	
			/* --- METHODS --- */
	public void add(ResultCnt cnt) {
		this.failed += cnt.failed;
		this.crashed += cnt.crashed;
		this.timeout += cnt.timeout;
	}
	
}
