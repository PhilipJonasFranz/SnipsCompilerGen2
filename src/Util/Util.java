package Util;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import PreP.PreProcessor;
import Res.Const;
import Util.Logging.LogPoint.Type;
import Util.Logging.Message;

public class Util {

			/* ---< METHODS >--- */
	/** Reads the contents of given file and returns a list containing each line as a string. */
	public static List<String> readFile(File file) {
		try (Stream<String> s = Files.lines(Paths.get(file.getAbsolutePath()))) {
			return s.collect(Collectors.toList());
		} catch (Exception e) {
			return null;
		}
	}

	/** Writes in given file path, each string in a seperate file. */
	public static boolean writeInFile(List<String> content, String filePath) {
		File file = new File(filePath);
		try (FileWriter w = new FileWriter(file.getPath())) {
			for (String s : content) {
				w.write(s);
				w.write(System.getProperty("line.separator"));
			}
		} catch (IOException e) {
			e.printStackTrace();
			return false;
		}
		
		return true;
	}
	
	public static String formatNum(long num) {
		String [] sp = ("" + num).split("");
		String r = "";
		int c = 0;
		for (int i = sp.length - 1; i >= 0; i--) {
			c++;
			r = sp [i] + r;
			if (c % 3 == 0 && i > 0) r = "." + r;
		}
		return r;
	}
	
	/**
	 * Attempts to find the Constant field in Const.java that has given excMessage as value.
	 * If found, the name of the field is returned. Returns 'UNKNOWN_FIELD' otherwise. 
	 * @param excMessage The message that should be the value of the field.
	 * @return The field name or the unknown field string.
	 */
	public static String getExceptionFieldName(String excMessage) {
		Field [] declaredFields = Const.class.getDeclaredFields();
		
		for (Field field : declaredFields) {
		    if (java.lang.reflect.Modifier.isStatic(field.getModifiers())) {
		        try {
		        	if (field.get(null).toString().equals(excMessage)) 
						return field.getName();
				} catch (Exception e) {
					new Message("Could not find exception field name for message '" + excMessage + "'", Type.WARN);
				}
		    }
		}
		
		/* Field not found, or not externalized */
		return "UNKNOWN_FIELD";
	}
	
	public static String toASMPath(String path) {
		if (path.endsWith(".sn") || path.endsWith(".hn")) 
			path = path.substring(0, path.length() - 2) + "s";
		return path;
	}
	
	public static long computeHashSum(String path) {
		long sum = 0;
		
		String mappedPath = PreProcessor.resolveToPath(path);
		List<String> lines = Util.readFile(new File(mappedPath));
		
		if (lines != null) {
			for (String s : lines) 
				/* Exclude version number directive */
				if (!s.startsWith(".version"))
					sum += s.hashCode();
		}
		else new Message("Failed to locate file '" + path + "', cannot compute hashsum.", Type.WARN);
		
		return sum;
	}
    
} 
