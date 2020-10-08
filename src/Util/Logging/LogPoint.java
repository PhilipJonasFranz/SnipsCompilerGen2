package Util.Logging;

public class LogPoint {

	public class ColorCodes {
		
		public static final String ANSI_RESET  = "\u001B[0m";
		public static final String ANSI_BLACK  = "\u001B[30m";
		public static final String ANSI_RED    = "\u001B[31m";
		public static final String ANSI_GREEN  = "\u001B[32m";
		public static final String ANSI_YELLOW = "\u001B[33m";
		public static final String ANSI_BLUE   = "\u001B[34m";
		public static final String ANSI_PURPLE = "\u001B[35m";
		public static final String ANSI_CYAN   = "\u001B[36m";
		public static final String ANSI_WHITE  = "\u001B[37m";
		
	}
	
	public static enum Type {
		INFO, WARN, FAIL;
	}
	
	public static String getEscapeCodeFor(Type type) {
		if (type == Type.INFO)
			return ColorCodes.ANSI_GREEN;
		else if (type == Type.WARN)
			return ColorCodes.ANSI_YELLOW;
		else 
			return ColorCodes.ANSI_RED;
	}
	
	public static String typeToString(Type type) {
		String s = type.toString().toLowerCase();
		s = s.substring(0, 1).toUpperCase() + s.substring(1);
		return s;
	}
	
}
