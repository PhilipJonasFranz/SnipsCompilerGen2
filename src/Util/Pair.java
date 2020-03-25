package Util;

public class Pair<K, V> {

	public K t0;
	
	public V t1;
	
	public Pair(K k0, V v0) {
		this.t0 = k0;
		this.t1 = v0;
	}
	
	public K tpl_1() {
		return this.t0;
	}
	
	public V tpl_2() {
		return this.t1;
	}
	
}
