package Util;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import Imm.AST.Program;
import PreP.PreProcessor;
import Res.Const;
import Snips.CompilerDriver;
import Util.Logging.LogPoint;
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
			for (String s : lines) {
				/* Exclude version number directive */
				if (!s.startsWith(".version")) 
					sum += s.hashCode();
				else sum = 0;
			}
		}
		else new Message("Failed to locate file '" + path + "', cannot compute hashsum.", Type.WARN);
		
		return sum;
	}
	
	public static void plot(List<Double> history) {
		String f = "  ";
		
		int [] map = new int [100];
		for (double d : history) {
			if (d < 0) continue;
			map [(int) d]++;
		}
		
		int m = 0;
		for (int i : map) if (i > m) m = i;
		
		f = ("" + m).replaceAll(".", " ");
		
		for (int i = m; i >= 0; i--) {
			
			if (i % 5 == 0) {
				String num = "" + i;
				for (int k = 0; k < f.length() - num.length(); k++) CompilerDriver.outs.print(" ");
				CompilerDriver.outs.print(num + "|");
			}
			else CompilerDriver.outs.print(f + "|");
			for (int a = 0; a < 100; a++) {
				if (map [a] > i) CompilerDriver.outs.print("\u2588");
				else CompilerDriver.outs.print(" ");
			}
			CompilerDriver.outs.println();
		}
		
		for (int i = 0; i < 100; i++) {
			if (i > f.length()) CompilerDriver.outs.print("-");
			else CompilerDriver.outs.print(" ");
		}
		CompilerDriver.outs.println();
		
		CompilerDriver.outs.print(" ");
		String s = f;
		for (int i = 0; i <= 100; i += 10) {
			if (i % 10 == 0) {
				s += "" + i;
				while (s.length() < i + 10) s += " ";
			}
		}
		
		CompilerDriver.outs.println(s + "\n");
	}
	
	public static void printStats(CompilerDriver driver) {
		double [] rate = {0, 0};
		CompilerDriver.compressions0.stream().forEach(x -> rate [0] += x / CompilerDriver.compressions0.size());
		CompilerDriver.compressions1.stream().forEach(x -> rate [1] += x / CompilerDriver.compressions1.size());
		
		double r0 = rate [0];
		r0 = Math.round(r0 * 100.0) / 100.0;
		
		double r1 = rate [1];
		r1 = Math.round(r1 * 100.0) / 100.0;
		
		if (CompilerDriver.useASTOptimizer) {
			new Message("SNIPS_OPT0 -> Compression Statistics: ", LogPoint.Type.INFO);
			
			/* Plot compression statistics */		
			CompilerDriver.outs.println();
			
			plot(CompilerDriver.compressions0);
			
			double l = ((double) CompilerDriver.opt0_loops) / CompilerDriver.opt0_exc;
			l = Math.round(l * 100.0) / 100.0;
			
			new Message("SNIPS_OPT0 -> Average compression rate: " + r0 + "%, min: " + CompilerDriver.c_min0 + "%, max: " + CompilerDriver.c_max0 + "%", LogPoint.Type.INFO);
			new Message("SNIPS_OPT0 -> Average optimization cycles: " + l, LogPoint.Type.INFO);
		}
		
		if (CompilerDriver.useASMOptimizer) {
			new Message("SNIPS_OPT1 -> Compression Statistics: ", LogPoint.Type.INFO);
			
			/* Plot compression statistics */		
			CompilerDriver.outs.println();
			
			plot(CompilerDriver.compressions1);
			
			new Message("SNIPS_OPT1 -> Average compression rate: " + r1 + "%, min: " + CompilerDriver.c_min1 + "%, max: " + CompilerDriver.c_max1 + "%", LogPoint.Type.INFO);
		}
		
		new Message("SNIPS_OPT1 -> Relative frequency of instructions: ", LogPoint.Type.INFO);
		
		List<Pair<Integer, String>> rmap = new ArrayList();
		for (Entry<String, Integer> e : CompilerDriver.ins_p.entrySet()) {
			if (rmap.isEmpty()) {
				rmap.add(new Pair<Integer, String>(e.getValue(), e.getKey()));
			}
			else {
				boolean added = false;
				for (int i = 0; i < rmap.size(); i++) {
					if (e.getValue() > rmap.get(i).first) {
						rmap.add(i, new Pair<Integer, String>(e.getValue(), e.getKey()));
						added = true;
						break;
					}
				}
				
				if (!added) rmap.add(new Pair<Integer, String>(e.getValue(), e.getKey()));
			}
		}
		
		String f = "  ";
		
		if (!rmap.isEmpty()) {
			CompilerDriver.outs.println();
			
			double stretch = 1.0;
			
			if (rmap.get(0).first > 75) {
				stretch = 75.0 / rmap.get(0).first;
			}
			
			for (int i = 0; i < rmap.size(); i++) {
				CompilerDriver.outs.print(f + "|");
				for (int a = 0; a < (int) ((double) rmap.get(i).first * stretch); a++) {
					CompilerDriver.outs.print("\u2588");
				}
				
				String n = rmap.get(i).second.split("\\.") [rmap.get(i).second.split("\\.").length - 1];
				
				CompilerDriver.outs.println(" : " + n + " (" + rmap.get(i).first + ")");
			}
			
			CompilerDriver.outs.println();
		}
		
		new Message("SNIPS_OPT1 -> Total Instructions generated: " + Util.formatNum(CompilerDriver.instructionsGenerated), LogPoint.Type.INFO);
	}
	
	public static void removeDuplicates(List<Program> ASTs) {
		if (ASTs.size() > 1) for (int i = 0; i < ASTs.size(); i++) {
			for (int a = i + 1; a < ASTs.size(); a++) {
				if (ASTs.get(i).fileName.equals(ASTs.get(a).fileName)) {
					ASTs.remove(a);
					a--;
				}
			}
		}
	}
	
	/** 
	 * Create a padding of spaces with the given length.
	 * For example w=3 -> '   '.
	 */
	public static String pad(int w) {
		String pad = "";
		for (int i = 0; i < w; i++) pad += " ";
		return pad;
	}
	
	/**
	 * Iterative Fibonacci Sequence Implementation.
	 * Returns Integer.MAX_VALUE if the value is too large to be stored in 
	 * an integer.
	 */
	public static int fib(int n) {
		int n1 = 1;
		int n2 = 1;
		
		while (n >= 3) {
			int temp = n1 + n2;
			n1 = n2;
			n2 = temp;
			n--;
		}
		
		if (n2 < 0) return Integer.MAX_VALUE;
		return n2;
	}
	
} 
