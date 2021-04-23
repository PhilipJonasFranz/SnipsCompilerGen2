package Res.Manager;

import Util.Logging.LogPoint.Type;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import Util.Logging.Message;

public class FileUtil {

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
	
	public static List<String> fileWalk(String path) {
		try (Stream<Path> walk = Files.walk(Paths.get(path))) {
			List<String> result = walk.filter(Files::isRegularFile)
				.map(x -> x.toString()).collect(Collectors.toList());
			return result;
		} catch (IOException e) {
			e.printStackTrace();
			return null;
		}
	}
	
	public static long computeHashSum(String path) {
		long sum = 0;
		
		List<String> ressource = RessourceManager.instance.getFile(path);
		
		if (ressource != null) {
			for (String s : ressource) {
				/* Exclude version number directive */
				if (!s.startsWith(".version")) 
					sum += s.hashCode();
				else sum = 0;
			}
		}
		else new Message("Failed to locate file '" + path + "', cannot compute hashsum.", Type.WARN);
		
		return sum;
	}
	
}
