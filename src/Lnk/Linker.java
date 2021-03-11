package Lnk;

import java.util.ArrayList;
import java.util.List;

import Exc.LNK_EXC;
import PreP.PreProcessor;
import Util.Logging.LogPoint.Type;
import Util.Logging.Message;

public class Linker {

	public static void linkProgram(List<String> asm) throws LNK_EXC {
		List<String> included = new ArrayList();
		
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
				
				asm.set(i, " ");
				i++;
				
				if (included.contains(incPath)) continue;
				else included.add(incPath);
				
				List<String> lines = PreProcessor.getFile(mappedPath);
				
				if (lines == null) {
					throw new LNK_EXC("Failed to locate include target %s", mappedPath);
				}
				else {
					boolean found = false;
					
					/* Search import */
					for (int a = 0; a < lines.size(); a++) {
						if (lines.get(a).trim().startsWith(".global " + label)) {
							/* Found position in artifact */
							found = true;
							
							int cnt = i;
							
							while (true) {
								/* Copy contents until EOF is reached, or until a new .global directive is seen */
								if (a >= lines.size() || (cnt > i && lines.get(a).contains(".global"))) break;
								
								asm.add(cnt++, lines.get(a++));
							}
							
							new Message("Resolved '" + label + "' to " + (cnt - a) + " lines from '" + mappedPath + "'", Type.INFO);
							break;
						}
					}
					
					if (!found) 
						new Message("Failed to locate '" + label + "' in '" + mappedPath + "'", Type.FAIL);
				}
			}
		}
		
		for (int i = 1; i < asm.size(); i++) {
			if (asm.get(i).trim().isEmpty() && asm.get(i - 1).trim().isEmpty()) {
				asm.remove(i);
				i--;
			}
		} 
	}
	
}
