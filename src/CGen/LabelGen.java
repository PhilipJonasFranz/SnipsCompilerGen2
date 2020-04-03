package CGen;

public class LabelGen {

	private static int c = 0;
	
	public static String getLabel() {
		return ".L" + c++;
	}
	
	public static void reset() {
		c = 0;
	}
	
	public static String mapToAddressName(String name) {
		return getLabel() + "_" + name;
	}
	
}
