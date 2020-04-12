package Util;

import lombok.AllArgsConstructor;
import lombok.Data;

/** A pair of two different data types. */
@Data
@AllArgsConstructor
public class Pair<K, V> {

			/* --- FIELDS --- */
	/** The first component. */
	private K first;
	
	/* The second component */
	private V second;
	
}
