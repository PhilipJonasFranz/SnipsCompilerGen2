package CGen.Util;

import java.util.List;

import Imm.TYPE.TYPE;

/**
 * Provides unique labels for branches.
 */
public class LabelUtil {

			/* ---< FIELDS >--- */
	/** Internal counter used to create labels */
	private static int c = 0;
	
	/** Literal Pool number */
	private static int p = 0;
	
	/** Unique ID number */
	private static int n = 0;
	
	/** Current function prefix */
	public static String funcPrefix = "";
	
	public static int funcUID = -1;
	
			/* ---< METHODS >--- */
	/**
	 * Returns a new unique label. 
	 */
	public static String getLabel() {
		return funcPrefix + ((funcPrefix.startsWith("__") || funcPrefix.equals("main") || 
							  funcPrefix.equals("resv") || funcPrefix.equals("free") || 
							  funcPrefix.equals("init") || funcPrefix.equals("hsize") || funcUID == -1)? "" : "@"  + funcUID)
				+ ".L" + c++;
	}
	
	/**
	 * Builds a postfix for labels that reflects the characteristics for
	 * this pattern. This pattern will contain the wordsizes of the given
	 * context. This will make it easy to determine if a context is suitable
	 * for another one, by comparing the word sizes of the types.
	 * @param context The context of the mapping
	 * @return A string that reflects this mapping.
	 */
	public static String getProvisoPostfix(List<TYPE> context) {
		String p = "_P_";
		for (TYPE t : context) p += t.wordsize() + "_";
		if (p.endsWith("_")) p = p.substring(0, p.length() - 1);
		return p;
	}
	
	/**
	 * Returns a unique number.
	 */
	public static int getUID() {
		return n++;
	}
	
	/**
	 * Resets the label generator. Note that all labels created after resetting are not
	 * guaranteed to be unique.
	 */
	public static void reset() {
		c = 0;
		p = 0;
		funcPrefix = "";
		funcUID = -1;
		n = 0;
	}
	
	/**
	 * Adds a prefix to given name which consists of a new unique label and _.
	 */
	public static String mapToAddressName(String name) {
		return getLabel() + "_" + name;
	}
	
	public static String literalPoolPrefix() {
		return ".POOL" + p++ + "_";
	}
	
	public static String getAnonLabel() {
		return "ANON" + LabelUtil.getUID();
	}
	
} 
