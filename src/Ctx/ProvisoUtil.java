package Ctx;

import java.util.List;

import Exc.SNIPS_EXC;
import Imm.TYPE.*;
import Imm.TYPE.COMPOSIT.*;
import Imm.TYPE.PRIMITIVES.*;
import Res.Const;

/**
 * Contains Utility to map proviso contexts to types.
 * 
 * None of these methods will get rid of proviso types, but will only assign
 * contexts to them. Ergo these methods are not destructive.
 * 
 * To get the proviso free version of a type with context, the 
 * {@link #provisoFree()} method can be used.
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
				map &= map1To1Maybe(f.proviso.get(i), source);

			if (f.funcHead != null) {
				/* Map to each parameter type */
				for (int i = 0; i < f.funcHead.parameters.size(); i++)
					map &= map1To1Maybe(f.funcHead.parameters.get(i).getType(), source);
			
				map &= map1To1Maybe(f.funcHead.getReturnTypeDirect(), source);
			}
			
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
				map &= map1To1Maybe(s.proviso.get(i), source);
			
			return map;
		}
		else if (target instanceof ARRAY) {
			ARRAY a = (ARRAY) target;

			/* Relay to array target type */
			return map1To1Maybe(a.elementType, source);
		}
		
		/* 
		 * When using this mapping method, its possible that f.E PROVISO<K, ...> is 
		 * attempted to be mapped to PROVISO<V>. This doesnt work of course, but this
		 * does not indicate anything wrong. Just return false, so the callee knows
		 * that this type was not matched.
		 */
		return false;
	}

	/**
	 * Attempts to map the source to the target. Throws an exception
	 * if the types do not match.
	 * @throws SNIPS_EXC if the types do not match
	 */
	public static void map1To1(TYPE target, TYPE source) throws SNIPS_EXC {
		if (target instanceof PRIMITIVE && !(target instanceof FUNC)) {
			/* No need to apply */
			return;
		}
		else if (target instanceof FUNC) {
			FUNC f = (FUNC) target;

			/* Map to each predicate proviso */
			for (int i = 0; i < f.proviso.size(); i++)
				map1To1(f.proviso.get(i), source);
		}
		else if (target instanceof PROVISO) {
			PROVISO p = (PROVISO) target;
			
			/* Successfully mapped proviso */
			p.setContext(source);	
		}
		else if (target instanceof POINTER) {
			POINTER p = (POINTER) target;
			
			/* Relay to targeted type */
			map1To1(p.targetType, source);
		}
		else if (target instanceof STRUCT) {
			STRUCT s = (STRUCT) target;
			
			/* Map to each struct proviso */
			for (int i = 0; i < s.proviso.size(); i++)
				map1To1(s.proviso.get(i), source);
		}
		else if (target instanceof ARRAY) {
			ARRAY a = (ARRAY) target;

			/* Relay to array target type */
			map1To1(a.elementType, source);
		}
		else throw new SNIPS_EXC(Const.CANNOT_MAP_TYPE_TO_PROVISO, source.typeString(), target.typeString());
	}
	
	/**
	 * Maps a context to a single type. 
	 * N to 1 means that potentially any of the provided types may match,
	 * but its possible no one will match.
	 */
	public static void mapNTo1(TYPE target, List<TYPE> source) {
		/* Map the given source to the target, stop once a match was found */
		for (int i = 0; i < source.size(); i++) 
			map1To1Maybe(target, source.get(i));
	}
	
	public static void mapNToNMaybe(List<TYPE> target, List<TYPE> source) {
		/* Map the given source to the target, stop once a match was found */
		for (int i = 0; i < source.size(); i++) 
			for (int a = 0; a < target.size(); a++)
				map1To1Maybe(target.get(i), source.get(i));
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
	

	/**
	 * Checks if two given mappings are equal. This means that they have to be of equal length, and 
	 * for every two types, the proviso-free type string has to be equal.
	 * @param map0 The first proviso map.
	 * @param map1 The second proviso map.
	 * @return True if the maps are equal, false if not.
	 */
	public static boolean mappingIsEqual(List<TYPE> map0, List<TYPE> map1) {
		boolean isEqual = true;
		for (int a = 0; a < map0.size(); a++) 
			isEqual &= map0.get(a).provisoFree().typeString().equals(map1.get(a).provisoFree().typeString());
		
		return isEqual;
	}
	
	
} 
