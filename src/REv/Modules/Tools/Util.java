package REv.Modules.Tools;

import java.util.ArrayList;
import java.util.List;

import REv.CPU.ProcessorUnit;
import REv.Devices.Device;
import REv.Modules.RAsm.Assembler;
import Util.XMLParser.XMLNode;

public class Util {

			/* ---< METHODS >--- */
	public static int toDecimal(int [] num) {
    	int s = 0;
    	int c = num.length;
    	for (int i : num)s += i << --c;
    	return s;
    }
	
	public static int toDecimal2K(int [] num) {
		num = extend2K(num, 32);
		int isNeg = num [0];
		if (isNeg == 1)num = inv2K(num);
		return (isNeg == 0)? toDecimal(num) : -toDecimal(num);
    }
	
	public static int [] extend2K(int [] num, int width) {
		int [] copy = num.clone();
		num = new int [width];
		int c = copy.length - 1;
		for (int i = width - 1; i >= 0; i--) {
			if (c >= 0)num [i] = copy [c];
			else num [i] = copy [0];
			c--;
		}
		return num;
	}
	
	public static int [] toBinary(int num) {
		int isNegative = (num < 0)? 1 : 0;
    	num *= (isNegative == 1)? -1 : 1;
    	
    	int [] r = new int [32];
    	for (int i = 31; i >= 0 && num > 0; i--) {
    		if (num >= 1 << (i - 1)) {
    			r [32 - i] = 1;
    			num -= 1 << (i - 1);
    		}
    	}
    	
    	return (isNegative == 1)? inv2K(r) : r;
    }
    
    public static int [] inv2K(int [] num) {
    	for (int i = 0; i < 32; i++)num [i] = 1 - num [i];
    	return add(num, new int [] {0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1});
    }
    
    public static int [] add(int [] num0, int [] num1) {
    	int [] result = new int [32];
    	result [32 - 1] = num0 [32 - 1] ^ num1 [32 - 1];
    	int c = num0 [32 - 1] & num1 [32 - 1];

    	int r0;
    	for (int i = 32 - 2; i >= 0; i--) {
    		r0 = num0 [i] ^ num1 [i];
    		result [i] = r0 ^ c;
    		c = (num0 [i] & num1 [i]) | (r0 & c);
    	}
    	
    	return result;
    }
    
	public static ProcessorUnit buildEnvironmentFromXML(XMLNode head, List<String> asmIn, boolean silent) {
		/* Load devices */
		XMLNode devices = head.getNode("Devices");
		List<Device> deviceList = new ArrayList();
		for (XMLNode device : devices.children) {
			String [] s = device.value.split(",");
			
			/* Build static device */
			Device d = new Device(s [0].trim(), Integer.parseInt(s [2].trim()), Integer.parseInt(s [3].trim()), Integer.parseInt(s [4].trim()));
			deviceList.add(d);
		}
		
		int [] [] program = Assembler.assemble(asmIn, silent, false);
		if (program != null) {
			Device d = deviceList.get(0);
			for (int i = 0; i < program.length; i++) 
				d.internalMemory [i] = program [i];
		}
		
		ProcessorUnit pcu = new ProcessorUnit(deviceList.stream().toArray(Device []::new));
		return pcu;
	}
	
    public static void sleep() {
    	try {
			Thread.sleep(1);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
    }
    
} 
