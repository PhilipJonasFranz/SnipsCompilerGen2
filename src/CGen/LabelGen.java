package CGen;

/**
 * Provides unique labels for branches.
 */
public class LabelGen {

			/* --- FIELDS --- */
	/** Internal counter used to create labels */
	private static int c = 0;
	
	private static int sid = 1;
	
	public static String funcPrefix = "";
	
			/* --- METHODS --- */
	/**
	 * Returns a new unique label. 
	 */
	public static String getLabel() {
		return funcPrefix + ".L" + c++;
	}
	
	public static String getProvisoPostfix() {
		return "_P" + c++;
	}
	
	public static int getSID() {
		return sid++;
	}
	
	/**
	 * Resets the label generator. Note that all labels created after resetting are not
	 * guaranteed to be unique.
	 */
	public static void reset() {
		c = 0;
		i = 0;
		p = 0;
		sid = 1;
	}
	
	/**
	 * Adds a prefix to given name which consists of a new unique label and _.
	 */
	public static String mapToAddressName(String name) {
		return getLabel() + "_" + name;
	}
	
	private static int i = 0;
	
	public static String importLabelPrefix(String base) {
		return ".I" + i++ + "_" + base;
	}
	
	private static int p = 0;
	
	public static String literalPoolPrefix() {
		return ".POOL" + p++ + "_";
	}
	
}
