package Ctx;

import java.util.List;

import Exc.SNIPS_EXC;
import Imm.TYPE.PROVISO;
import Imm.TYPE.TYPE;
import Imm.TYPE.COMPOSIT.ARRAY;
import Imm.TYPE.COMPOSIT.POINTER;
import Imm.TYPE.COMPOSIT.STRUCT;
import Imm.TYPE.PRIMITIVES.FUNC;
import Imm.TYPE.PRIMITIVES.PRIMITIVE;

/**
 * Contains Utility to map proviso contexts to types.
 * 
 * None of these methods will get rid of proviso types, but will only assign
 * contexts to them. Ergo these methods are not destructive.
 */
public class ProvisoUtil {
	
	/**
	 * Attempts to map the source to the target. May not match.
	 */
	public static boolean map1To1Maybe(TYPE target, TYPE source) {
		if (target instanceof PRIMITIVE && !(target instanceof FUNC)) {
			/* No need to apply */
			return true;
		}
		else if (target instanceof FUNC) {
			FUNC f = (FUNC) target;

			boolean map = true;
			
			/* Map to each predicate proviso */
			for (int i = 0; i < f.proviso.size(); i++)
				map &= ProvisoUtil.map1To1Maybe(f.proviso.get(i), source);
			
			return map;
		}
		else if (target instanceof POINTER) {
			POINTER p = (POINTER) target;
			
			/* Relay to targeted type */
			return map1To1Maybe(p.targetType, source);
		}
		else if (target instanceof PROVISO) {
			PROVISO p = (PROVISO) target;
			
			if (p.isEqual(source)) {
				/* Successfully mapped proviso */
				p.setContext(source);
				return true;
			}
		}
		else if (target instanceof STRUCT) {
			STRUCT s = (STRUCT) target;
			
			boolean map = true;
			
			/* Map to each struct proviso */
			for (int i = 0; i < s.proviso.size(); i++)
				map &= ProvisoUtil.map1To1Maybe(s.proviso.get(i), source);
			
			return map;
		}
		else if (target instanceof ARRAY) {
			ARRAY a = (ARRAY) target;

			/* Relay to array target type */
			return map1To1Maybe(a.elementType, source);
		}
		
		System.out.println("Cannot map " + source.typeString() + " -> " + target.typeString());
		return false;
	}

	/**
	 * Attempts to map the source to the target. Throws an exception
	 * if the types do not match.
	 * @throws SNIPS_EXC if the types do not match
	 */
	public static void map1To1(TYPE target, TYPE source) throws SNIPS_EXC {
		if (target instanceof PROVISO) {
			PROVISO p = (PROVISO) target;
			
			/* Successfully mapped proviso */
			p.setContext(source);	
		}
		else throw new SNIPS_EXC("Cannot map " + source.typeString() + " -> " + target.typeString());
	}
	
	/**
	 * Maps a context to a single type. 
	 * N to 1 means that potentially any of the provided types may match,
	 * but its possible no one will match.
	 */
	public static void mapNTo1(TYPE target, List<TYPE> source) {
		/* Map the given source to the target, stop once a match was found */
		for (int i = 0; i < source.size(); i++) 
			if (map1To1Maybe(target, source.get(i))) break;
	}
	
	public static void mapNToNMaybe(List<TYPE> target, List<TYPE> source) {
		/* Map the given source to the target, stop once a match was found */
		for (int i = 0; i < source.size(); i++) 
			for (int a = 0; a < target.size(); a++)
				if (map1To1Maybe(target.get(i), source.get(i))) break;
	}
	
	/**
	 * Maps a context to a type list.
	 * N to N means that the n-th type in the lists must match
	 * the n-th type in the source.
	 * @throws SNIPS_EXC if any pair does not match.
	 */
	public static void mapNToN(List<TYPE> target, List<TYPE> source) throws SNIPS_EXC {
		/* Map the given source to the target, stop once a match was found */
		for (int i = 0; i < source.size(); i++) 
			map1To1(target.get(i), source.get(i));
	}
	
}
