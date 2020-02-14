package Util;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class Util {

	public static List<String> readFile(File file) {
		try (Stream<String> s = Files.lines(Paths.get(file.getAbsolutePath()))) {
			return s.collect(Collectors.toList());
		} catch (IOException e) {
			return null;
		}
	}

	public static void writeInFile(List<String> content, String filePath) {
		try (FileWriter w = new FileWriter(filePath)) {
			for (String s : content) {
				w.write(s);
				w.write(System.getProperty("line.separator"));
			}
		} catch (IOException e) {
		
		}
	}
    
}
