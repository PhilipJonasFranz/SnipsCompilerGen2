package Util;

import lombok.AllArgsConstructor;
import lombok.Data;

/** A pair of two different data types. */
@Data
@AllArgsConstructor
public class Pair<K, V> {

			/* --- FIELDS --- */
	/** The first component. */
	public K first;
	
	/* The second component */
	public V second;
	
}
