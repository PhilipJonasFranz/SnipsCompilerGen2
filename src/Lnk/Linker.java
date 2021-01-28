package Lnk;

import java.util.List;

import Exc.LNK_EXC;
import PreP.PreProcessor;
import Util.Logging.LogPoint.Type;
import Util.Logging.Message;

public class Linker {

	public static void linkProgram(List<String> asm) throws LNK_EXC {
		for (int i = 0; i < asm.size(); i++) {
			String line = asm.get(i);
			
			if (line.contains(".include")) {
				/* Found asm include directive */
				
				String incPath = line.trim().split(" ") [1];
				String [] sp = incPath.split("@");
				
				String filePath = sp [0];
				String label = sp [1];
				
				String mappedPath = PreProcessor.resolveToPath(filePath);
				mappedPath = mappedPath.substring(0, mappedPath.length() - 2) + "s";
				
				List<String> lines = PreProcessor.getFile(mappedPath);
				
				if (lines == null) {
					throw new LNK_EXC("Failed to locate include target %s", filePath);
				}
				else {
					boolean found = false;
					
					/* Search import */
					for (int a = 0; a < lines.size(); a++) {
						if (lines.get(a).trim().startsWith(label + ":")) {
							/* Found position in artifact */
							found = true;
							asm.remove(i);
							
							int cnt = i;
							
							while (true) {
								if (a >= lines.size() || lines.get(a).trim().equals("")) break;
								else asm.add(cnt++, lines.get(a++));
							}
							
							new Message("Resolved '" + label + "' to " + (cnt - a) + " lines in '" + mappedPath + "'", Type.INFO);
							break;
						}
					}
					
					if (!found) new Message("Failed to locate '" + label + "' in '" + mappedPath + "'", Type.FAIL);
				}
			}
		}
	}
	
}
