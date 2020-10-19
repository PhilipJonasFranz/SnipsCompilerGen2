package Imm.TYPE.COMPOSIT;

import java.util.ArrayList;
import java.util.List;

import Ctx.Util.ProvisoUtil;
import Imm.AST.Statement.InterfaceTypedef;
import Imm.TYPE.PROVISO;
import Imm.TYPE.TYPE;
import Imm.TYPE.PRIMITIVES.VOID;
import Snips.CompilerDriver;

public class INTERFACE extends COMPOSIT {

	private InterfaceTypedef typedef;
	
	public List<TYPE> proviso;
	
	public INTERFACE(InterfaceTypedef typedef, List<TYPE> proviso) {
		super(null);
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
		if (type.getCoreType() instanceof VOID) return true;
		if (type instanceof INTERFACE) {
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
		
		if (!this.proviso.isEmpty()) {
			s += "<";
			for (TYPE t : this.proviso) {
				s += t.typeString() + ",";
			}
			s = s.substring(0, s.length() - 1);
			s += ">";
		}
		
		if (CompilerDriver.printObjectIDs) s += " " + this.toString().split("@") [1];
		return s;
	}
	
	public String getProvisoString() {
		String s = "";
		if (!this.proviso.isEmpty()) {
			s += " {";
			for (TYPE t : this.proviso) s += t.typeString() + ", ";
			s = s.substring(0, s.length() - 2);
			s += "}";
		}
		return s;
	}

	public void setValue(String value) {
		return;
	}

	public String sourceCodeRepresentation() {
		return null;
	}

	public int wordsize() {
		return 1;
	}

	public TYPE getCoreType() {
		/* Struct acts as its own type, so its is own core type. */
		return this;
	}

	public INTERFACE clone() {
		List<TYPE> prov0 = new ArrayList();
		
		for (TYPE t : this.proviso) 
			prov0.add(t.clone());
		
		return new INTERFACE(this.typedef, prov0);
	}

	public TYPE provisoFree() {
		INTERFACE s = (INTERFACE) this.clone();
		for (int i = 0; i < s.proviso.size(); i++) s.proviso.set(i, s.proviso.get(i).provisoFree());
		return s;
	}

	public TYPE remapProvisoName(String name, TYPE newType) {
		for (int i = 0; i < this.proviso.size(); i++) 
			this.proviso.set(i, this.proviso.get(i).remapProvisoName(name, newType));
		
		return this;
	}

	public TYPE mappable(TYPE mapType, String searchedProviso) {
		if (mapType instanceof INTERFACE) {
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
		for (TYPE t : this.proviso)
			if (t.hasProviso())
				return true;
		return false;
	}
	
} 
