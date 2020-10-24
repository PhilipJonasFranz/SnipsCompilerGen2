package CGen.Util;

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
	
			/* ---< METHODS >--- */
	/**
	 * Returns a new unique label. 
	 */
	public static String getLabel() {
		return funcPrefix + ".L" + c++;
	}
	
	public static String getProvisoPostfix() {
		return "_P" + c++;
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
