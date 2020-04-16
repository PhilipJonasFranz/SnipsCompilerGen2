package Ctx;

import java.util.ArrayList;
import java.util.List;

import CGen.LabelGen;
import Exc.CTX_EXCEPTION;
import Imm.TYPE.PROVISO;
import Imm.TYPE.TYPE;
import Util.Pair;
import Util.Source;

public class ProvisoManager {
	
			/* --- FIELDS --- */
	/** List of the provisos types this function is templated with */
	public List<TYPE> provisosTypes;
	
	/** A list that contains the combinations of types this function was templated with */
	public List<Pair<String, Pair<TYPE, List<TYPE>>>> provisosCalls = new ArrayList();
	
	private Source source;
	
	public ProvisoManager(Source source, List<TYPE> provisoTypes) {
		this.provisosTypes = provisoTypes;
		this.source = source;
	}
	
	public void printContexts() {
		System.out.println("Mappings: ");
		for (int i = 0; i < this.provisosCalls.size(); i++) {
			System.out.print(this.provisosCalls.get(i).second.first.typeString() + " : ");
			List<TYPE> map = this.provisosCalls.get(i).second.second;
			for (TYPE t : map) System.out.print(t.typeString() + ", ");
			System.out.println();
		}
	}
	
	public void setContext(List<TYPE> context) throws CTX_EXCEPTION {
		for (int i = 0; i < this.provisosTypes.size(); i++) {
			TYPE pro = this.provisosTypes.get(i);
			if (!(pro instanceof PROVISO)) {
				throw new CTX_EXCEPTION(source, "Provided Type " + pro.typeString() + " is not a proviso type.");
			}
			
			PROVISO pro0 = (PROVISO) pro;
			//System.out.println("Applied " + context.get(i).typeString() + " to proviso " + pro0.typeString());
			pro0.setContext(context.get(i));
			//System.out.println("New proviso: " + pro0.typeString());
		}
	}
	
	public void releaseContext() {
		for (int i = 0; i < this.provisosTypes.size(); i++) {
			PROVISO pro0 = (PROVISO) this.provisosTypes.get(i);
			pro0.releaseContext();
		}
	}
	
	public boolean containsMapping(List<TYPE> map) {
		for (int i = 0; i < this.provisosCalls.size(); i++) {
			List<TYPE> map0 = this.provisosCalls.get(i).second.second;
			
			if (this.mappingIsEqual(map0, map)) {
				return true;
			}
		}
		
		return false;
	}
	
	public boolean mappingIsEqual(List<TYPE> map0, List<TYPE> map1) {
		boolean isEqual = true;
		for (int a = 0; a < map0.size(); a++) {
			isEqual &= map0.get(a).isEqual(map1.get(a));
		}
		return isEqual;
	}
	
	public String getPostfix(List<TYPE> map) {
		for (int i = 0; i < this.provisosCalls.size(); i++) {
			List<TYPE> map0 = this.provisosCalls.get(i).second.second;
			if (this.mappingIsEqual(map0, map)) {
				return this.provisosCalls.get(i).first;
			}
		}
		
		return null;
	}
	
	public TYPE getMappingReturnType(List<TYPE> map) {
		for (int i = 0; i < this.provisosCalls.size(); i++) {
			List<TYPE> map0 = this.provisosCalls.get(i).second.second;
			
			boolean isEqual = true;
			for (int a = 0; a < map0.size(); a++) {
				isEqual &= map0.get(a).isEqual(map.get(a));
			}
			
			if (isEqual) return this.provisosCalls.get(i).second.first;
		}
		
		System.out.println("No mapping!");
		return null;
	}
	
	public void addProvisoMapping(TYPE type, List<TYPE> context) {
		if (this.containsMapping(context)) {
			return;
		}
		else {
			this.provisosCalls.add(new Pair<String, Pair<TYPE, List<TYPE>>>(LabelGen.getProvisoPostfix(), new Pair<TYPE, List<TYPE>>(type, context)));
		}
	}
	
}
