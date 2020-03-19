package CGen;

public class LabelGen {

	private static int c = 0;
	
	public static String getLabel() {
		return "L" + c++;
	}
	
}
