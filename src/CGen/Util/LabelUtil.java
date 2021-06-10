package CGen.Util;

import Imm.ASM.Structural.Label.ASMLabel;
import Imm.TYPE.TYPE;

import java.util.HashMap;
import java.util.List;

/**
 * Provides unique labels for branches.
 */
public class LabelUtil {

			/* ---< FIELDS >--- */
	/** Internal counter used to create labels */
	public static HashMap<String, Integer> c = new HashMap();
	
	/** Unique ID number */
	private static int n = 0;
	
	/** Current function prefix */
	public static String funcPrefix = "";
	
	public static int funcUID = -1;
	
	public static String currentContext = null;
	
			/* ---< METHODS >--- */
	/**
	 * Returns a new unique label. 
	 */
	public static String getLabelString() {
		String prov = "";
		if (currentContext != null)
			prov = currentContext;
		
		if (!c.containsKey(funcPrefix)) c.put(funcPrefix, 0);
		
		int c0 = c.get(funcPrefix);
		c.replace(funcPrefix, c0 + 1);
		
		return funcPrefix + prov + ((funcPrefix.startsWith("__") || funcPrefix.equals("main") || 
							  funcPrefix.equals("resv") || funcPrefix.equals("free") || 
							  funcPrefix.equals("init") || funcPrefix.equals("hsize") || funcUID == -1)? "" : "_"  + funcUID)
				+ ".L" + c0;
	}

	/**
	 * Creates a new, unique label using getLabelString().
	 * @return The newly created label.
	 */
	public static ASMLabel getLabel() {
		return new ASMLabel(LabelUtil.getLabelString());
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
		if (context.isEmpty()) return "";
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
		poolLabels.clear();
		funcPrefix = "";
		funcUID = -1;
		n = 0;
	}
	
	private static HashMap<String, Integer> poolLabels = new HashMap();
	
	public static String literalPoolPrefix(String fileName) {
		int p = 0;
		
		if (poolLabels.containsKey(fileName))
			p = poolLabels.get(fileName);
		else
			poolLabels.put(fileName, p);
		
		String label = ".P" + Math.abs((fileName + p++).hashCode()) + "_";
		poolLabels.replace(fileName, p);
		
		return label;
	}
	
	public static String getAnonLabel() {
		return "ANON" + LabelUtil.getUID();
	}
	
} 
