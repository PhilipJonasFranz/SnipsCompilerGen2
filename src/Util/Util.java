package Util;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map.Entry;

import Imm.AST.Program;
import Imm.AST.SyntaxElement;
import Imm.AST.Expression.Expression;
import Imm.AsN.AsNNode;
import Res.Const;
import Res.Manager.FileUtil;
import Snips.CompilerDriver;
import Util.Logging.LogPoint;
import Util.Logging.LogPoint.Type;
import Util.Logging.Message;
import XMLParser.XMLParser.XMLNode;

public class Util {

			/* ---< METHODS >--- */
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
	
	public static void printOPT0Graph(List<Integer> complexity) {
		new Message("SNIPS_OPT0 -> Program Complexity History: ", LogPoint.Type.INFO);
		
		List<Double> inter = new ArrayList();
		
		double stepSize = (double) complexity.size() / 100;
		
		double max = 0;
		double step = 0;
		for (int i = 0; i < 100; i++) {
			double v = (double) complexity.get((int) step);
			if (v > max) max = v;
			inter.add(v);
			step += stepSize;
		}
		
		if (max > 20) {
			for (int i = 0; i < inter.size(); i++)
				inter.set(i, inter.get(i) / max * 20);
			max = 20;
		}
		
		for (int i = (int) max; i >= 0; i--) {
			CompilerDriver.outs.print("|");
			
			for (int a = 0; a < 100; a++) {
				if (inter.get(a) > i) CompilerDriver.outs.print("\u2588");
				else CompilerDriver.outs.print(" ");
			}
			
			CompilerDriver.outs.println();
		}
		
		CompilerDriver.outs.print(" ");
		for (int i = 0; i < 100; i++) {
			CompilerDriver.outs.print("-");
		}
		CompilerDriver.outs.println();
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
	
	/**
	 * Converts a camel-case formatted string into a lowercase, space-seperated string:
	 * Example: AbstractCompoundStatement -> abstract compound statement
	 */
	public static String revCamelCase(String s) {
		String [] sp = s.split("");
		String out = "";
		for (int i = 0; i < sp.length; i++) {
			String s0 = sp [i];
			
			boolean shortened = i + 1 < sp.length && Character.isUpperCase(sp [i + 1].charAt(0));
			
			if (Character.isUpperCase(s0.charAt(0)) && !shortened) out += " ";
			out += s0.toLowerCase();
		}
		
		return out.trim();
	}
	
	public static void buildStackTrace(String initialSource) {
		String last = initialSource;
		
		while (!CompilerDriver.stackTrace.isEmpty()) {
			SyntaxElement s = CompilerDriver.stackTrace.pop();
			
			String loc = s.getSource().getSourceMarkerWithoutFile() + " ";
			if (last == null || !s.getSource().sourceFile.equals(last)) {
				loc = s.getSource().getSourceMarker() + " ";
				last = s.getSource().sourceFile;
			}
			
			String trace = "  at " + Util.revCamelCase(s.getClass().getSimpleName()) + ", " + loc;
			
			if (s instanceof Expression) {
				Expression e = (Expression) s;
				trace += "[" + e.codePrint() + "]";
			}
			else {
				List<String> code = s.codePrint(0);
				if (code != null && !code.isEmpty()) {
					String line = code.get(0);
					
					if (line.endsWith(";") || line.endsWith("{")) 
						line = line.substring(0, line.length() - 1);
					
					trace += "[" + line.trim() + "]";
				}
			}
			
			CompilerDriver.log.add(new Message(trace, LogPoint.Type.FAIL));
		}
	}
	
	public static void flushAsNNodeMetrics() {
		HashMap<String, Pair<Integer, Pair<Integer, Integer>>> map = AsNNode.metricsMap;
		XMLNode node = CompilerDriver.metrics_config;
		
		for (Entry<String, Pair<Integer, Pair<Integer, Integer>>> entry : map.entrySet()) {
			XMLNode child = node.getNode(entry.getKey());
			if (child == null) {
				child = new XMLNode(entry.getKey());
				node.getChildren().add(child);
			}
			
			double ratioI = ((double) entry.getValue().second.first) / entry.getValue().first;
			double ratioC = ((double) entry.getValue().second.second) / entry.getValue().first;
			child.setValue("" + (int) ratioI + " " + (int) ratioC);
		}
		
		List<String> out = node.asString();
		FileUtil.writeInFile(out, "release/metric-inf.xml");
	}
	
} 
