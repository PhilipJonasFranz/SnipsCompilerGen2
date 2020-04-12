package CGen;

/**
 * Provides unique labels for branches.
 */
public class LabelGen {

			/* --- FIELDS --- */
	/** Internal counter used to create labels */
	private static int c = 0;
	
	
			/* --- METHODS --- */
	/**
	 * Returns a new unique label. 
	 */
	public static String getLabel() {
		return ".L" + c++;
	}
	
	/**
	 * Resets the label generator. Note that all labels created after resetting are not
	 * guaranteed to be unique.
	 */
	public static void reset() {
		c = 0;
	}
	
	/**
	 * Adds a prefix to given name which consists of a new unique label and _.
	 */
	public static String mapToAddressName(String name) {
		return getLabel() + "_" + name;
	}
	
}
