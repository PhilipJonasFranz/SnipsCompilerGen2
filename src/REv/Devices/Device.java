package REv.Devices;

import REv.CPU.ProcessorUnit;

public class Device {

	/* Memory */
	public int memorySize;
	public int [] [] internalMemory;
	
	/* Memory Read/Write enable */
	public int enRead, enWrite;
	
	/* Bus Connection */
	public int addressStart = 0;
	public ProcessorUnit pcu;
	
	/* Display Name */
	public String name;
	
	
	public Device(String name, int memorySize, int enRead, int enWrite) {
		this.name = name;
		this.memorySize = memorySize;
		this.enRead = enRead;
		this.enWrite = enWrite;
		
		this.internalMemory = new int [memorySize] [32];
	}
	
	public void connect(ProcessorUnit pcu, int start) {
		this.pcu = pcu;
		this.addressStart = start;
	}
	
}