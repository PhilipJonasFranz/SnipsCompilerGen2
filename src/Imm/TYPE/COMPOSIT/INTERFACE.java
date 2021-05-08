package Imm.TYPE.COMPOSIT;

import java.util.List;
import java.util.stream.Collectors;

import Ctx.Util.ProvisoUtil;
import Imm.AST.Typedef.InterfaceTypedef;
import Imm.TYPE.PROVISO;
import Imm.TYPE.TYPE;
import Snips.CompilerDriver;

public class INTERFACE extends COMPOSIT {

	private InterfaceTypedef typedef;
	
	public List<TYPE> proviso;
	
	public INTERFACE(InterfaceTypedef typedef, List<TYPE> proviso) {
		this.typedef = typedef;
		this.proviso = proviso;
		
		/* 
		 * Register a new interface mapping if all of the 
		 * provided provisos have non-proviso types set. 
		 */
		if (ProvisoUtil.isProvisoFreeMapping(this.proviso)) 
			this.typedef.registerMapping(this.proviso);
	}
	
	public boolean isEqual(TYPE type) {
		if (type.getCoreType().isVoid()) return true;
		if (type.isInterface()) {
			INTERFACE intf = (INTERFACE) type;
			
			if (intf.getTypedef().equals(this.typedef)) {
				for (int i = 0; i < this.proviso.size(); i++) 
					if (!this.proviso.get(i).isEqual(intf.proviso.get(i)))
						return false;
				
				return true;
			}
		}
		
		return false;
	}
	
	public InterfaceTypedef getTypedef() {
		return this.typedef;
	}
	
	public String typeString() {
		String s = this.typedef.path.build();
		
		if (!this.proviso.isEmpty()) 
			s += this.proviso.stream().map(TYPE::toString).collect(Collectors.joining(",", "<", ">"));
		
		if (CompilerDriver.printObjectIDs) s += " " + this.toString().split("@") [1];
		return s;
	}
	
	public String getProvisoString() {
		String s = "";
		
		if (!this.proviso.isEmpty()) 
			s += this.proviso.stream().map(TYPE::toString).collect(Collectors.joining(", ", " {", "}"));
		
		return s;
	}

	public TYPE getCoreType() {
		/* Struct acts as its own type, so its is own core type. */
		return this;
	}
	
	public TYPE getContainedType() {
		return this;
	}

	public INTERFACE clone() {
		List<TYPE> prov0 = this.proviso.stream().map(x -> x.clone()).collect(Collectors.toList());
		return new INTERFACE(this.typedef, prov0);
	}

	public TYPE provisoFree() {
		INTERFACE s = (INTERFACE) this.clone();
		
		for (int i = 0; i < s.proviso.size(); i++) 
			s.proviso.set(i, s.proviso.get(i).provisoFree());
		
		return s;
	}

	public TYPE remapProvisoName(String name, TYPE newType) {
		for (int i = 0; i < this.proviso.size(); i++) 
			this.proviso.set(i, this.proviso.get(i).remapProvisoName(name, newType));
		
		return this;
	}

	public TYPE mappable(TYPE mapType, String searchedProviso) {
		if (mapType.isInterface()) {
			INTERFACE s = (INTERFACE) mapType;
			if (s.getTypedef().equals(this.getTypedef())) {
				for (int i = 0; i < this.proviso.size(); i++) {
					PROVISO prov = (PROVISO) this.proviso.get(i);
					if (prov.placeholderName.equals(searchedProviso)) {
						return s.proviso.get(i);
					}
						
				}
			}
		}
		
		return null;
	}

	public boolean hasProviso() {
		return this.proviso.stream().filter(x -> x.hasProviso()).count() > 0;
	}
	
	public String codeString() {
		String s = this.getTypedef().path.build();
		
		if (!this.proviso.isEmpty()) 
			s += this.proviso.stream().map(TYPE::codeString).collect(Collectors.joining(", ", "<", ">"));
		
		return s;
	}
	
} 
