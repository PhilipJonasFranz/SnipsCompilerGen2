package Res.Manager;

import java.io.File;
import java.util.List;

import Snips.CompilerDriver;
import XMLParser.XMLParser.XMLNode;

public class RessourceManager {

	public static RessourceManager instance = new RessourceManager();
	
	private RessourceManager() {
		
	}
	
	public List<String> getFile(String filePath) {
		filePath = this.resolve(filePath);
		
		File file = new File(filePath);

		return FileUtil.readFile(file);
	}
	
	public String toASMPath(String path) {
		if (path.endsWith(".sn") || path.endsWith(".hn")) 
			path = path.substring(0, path.length() - 2) + "s";
		return path;
	}
	
	private String resolvePath(String filePath) {
		/* Check if filepath is an alias that is part of the system library */
		for (XMLNode c : CompilerDriver.sys_config.getNode("Library").getChildren()) {
			String [] v = c.getValue().split(":");
			
			/* Direct match, path is .hn or .sn patch with alias */
			String modPath = v [0];
			if (modPath.equals(filePath)) {
				return "release/" + v [1];
			}
			
			/* Check if the path is .s */
			modPath = this.toASMPath(v [0]);
			if (modPath.equals(filePath)) {
				return "release/" + this.toASMPath(v [1]);
			}
		}
		
		/* Simply check if path is valid */
		File file = new File(filePath);
		if (file.exists()) return filePath;
		
		/* Path relativ to main file */
		filePath = filePath.replace("\\", "/");
		String [] sp = filePath.split("/") ;
		String in = CompilerDriver.inputFile.getParent();
		return in + "/" + sp [sp.length - 1];
	}
	
	/**
	 * Resolves the given file path to a path from which the
	 * contents of the file can be loaded. The filepath can
	 * either be an alias defined in the system library, 
	 * a valid path by itself or a path relative to the main file.
	 */
	public String resolve(String filePath) {
		String resolved = this.resolvePath(filePath);
		resolved = resolved.replace("\\", "/");
		return resolved;
	}
	
	/**
	 * Check if the given filepath can be resolved to a ressource
	 * location that points to a file.
	 */
	public boolean ressourceExists(String filePath) {
		File file = new File(this.resolve(filePath));
		return file.exists();
	}
	
}
