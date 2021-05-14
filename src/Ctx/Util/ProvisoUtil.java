package Ctx.Util;

import java.util.ArrayList;
import java.util.List;

import Exc.SNIPS_EXC;
import Imm.AST.Statement.Declaration;
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
		else if (target instanceof FUNC f) {
			boolean map = true;
			
			/* Map to each predicate proviso */
			for (TYPE t : f.proviso) map &= map1To1Maybe(t, source);
			
			if (f.funcHead != null) {
				/* Map to each parameter type */
				for (Declaration d : f.funcHead.parameters)
					map &= map1To1Maybe(d.getRawType(), source);
				
				map &= map1To1Maybe(f.funcHead.getReturnTypeDirect(), source);
			}
			
			return map;
		}
		else if (target instanceof POINTER p) {
			/* Relay to targeted type */
			return map1To1Maybe(p.targetType, source);
		}
		else if (target instanceof PROVISO p) {
			if (p.isEqual(source)) {
				/* Successfully mapped proviso */
				p.setContext(source);
				return true;
			}
		}
		else if (target instanceof STRUCT s) {
			boolean map = true;
			
			/* Map to each struct proviso */
			for (TYPE t : s.proviso) map &= map1To1Maybe(t, source);
			
			return map;
		}
		else if (target instanceof INTERFACE i) {
			boolean map = true;
			
			/* Map to each struct proviso */
			for (TYPE t : i.proviso) map &= map1To1Maybe(t, source);
			
			return map;
		}
		else if (target instanceof ARRAY a) {
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
		}
		else if (target instanceof FUNC f) {
			/* Map to each predicate proviso */
			for (TYPE t : f.proviso) map1To1(t, source);
		}
		else if (target instanceof PROVISO p) {
			/* Successfully mapped proviso */
			p.setContext(source);	
		}
		else if (target instanceof POINTER p) {
			/* Relay to targeted type */
			map1To1(p.targetType, source);
		}
		else if (target instanceof STRUCT s) {
			/* Map to each struct proviso */
			for (TYPE t : s.proviso) map1To1(t, source);
		}
		else if (target instanceof INTERFACE i) {
			/* Map to each struct proviso */
			for (TYPE t : i.proviso) map1To1(t, source);
		}
		else if (target instanceof ARRAY a) {
			/* Relay to array target type */
			map1To1(a.elementType, source);
		}
		else throw new SNIPS_EXC(Const.CANNOT_MAP_TYPE_TO_PROVISO, source, target);
	}
	
	/**
	 * Maps a context to a single type. 
	 * N to 1 means that potentially any of the provided types may match,
	 * but its possible no one will match.
	 */
	public static void mapNTo1(TYPE target, List<TYPE> source) {
		/* Map the given source to the target, stop once a match was found */
		for (TYPE t : source) map1To1Maybe(target, t);
	}
	
	/**
	 * Map each type from the source to each type of the target. Possible that
	 * types do not match.
	 */
	public static void mapNToNMaybe(List<TYPE> target, List<TYPE> source) {
		for (TYPE s : source) for (TYPE t : target) map1To1Maybe(t, s);
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
	public static boolean mappingIsEqualProvisoFree(List<TYPE> map0, List<TYPE> map1) {
		boolean isEqual = true;
		
		for (int a = 0; a < map0.size(); a++) 
			isEqual &= map0.get(a).provisoFree().typeString().equals(map1.get(a).provisoFree().typeString());
		
		return isEqual;
	}
	
	/**
	 * Checks if two given mappings are equal. This means that they have to be of equal length, and 
	 * for every two types, the proviso-free type string has to be equal. The types are compared
	 * without calling proviso-free on them.
	 * @param map0 The first proviso map.
	 * @param map1 The second proviso map.
	 * @return True if the maps are equal, false if not.
	 */
	public static boolean mappingIsEqual(List<TYPE> map0, List<TYPE> map1) {
		boolean isEqual = true;
		for (int a = 0; a < map0.size(); a++) 
			isEqual &= map0.get(a).typeString().equals(map1.get(a).typeString());
		
		return isEqual;
	}
	
	/**
	 * Check if the given proviso mapping has no type that contains
	 * a capsuled proviso type.
	 */
	public static boolean isProvisoFreeMapping(List<TYPE> mapping) {
		return mapping.stream().noneMatch(TYPE::hasProviso);
	}

	/**
	 * For the given type, re-map all provisos of the interface to the provided provisos and
	 * apply the new provisos to the type. Return the resulting type.
	 */
	public static TYPE translate(TYPE t, List<TYPE> source, List<TYPE> target) {
		for (int k = 0; k < target.size(); k++) {
			PROVISO definedProviso = (PROVISO) source.get(k).clone();
			t = t.remapProvisoName(definedProviso.placeholderName, target.get(k).clone());
		}
		
		return t;
	}
	
	public static List<TYPE> mapToHead(List<TYPE> head, List<TYPE> context) {
		List<TYPE> headMapped = new ArrayList();
		for (int i = 0; i < head.size(); i++) {
			TYPE t0 = head.get(i).clone();
			ProvisoUtil.map1To1(t0, context.get(i).clone());
			headMapped.add(t0);
		}
		
		return headMapped;
	}
	
} 
