package Util;

/** A pair of two different data types. */
public class Pair<K, V> {

			/* --- FIELDS --- */
	/** The first component. */
	public K first;
	
	/* The second component */
	public V second;
	
			
			/* --- CONSTRUCTORS --- */
	public Pair(K first, V second) {
		this.first = first;
		this.second = second;
	}
	
	
			/* --- METHODS --- */
	public K getFirst() {
		return this.first;
	}
	
	public void setFirst(K k) {
		this.first = k;
	}
	
	public V getSecond() {
		return this.second;
	}
	
	public void setSecond(V v) {
		this.second = v;
	}
	

	
} 
