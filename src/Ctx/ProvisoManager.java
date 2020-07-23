package Ctx;

import java.util.ArrayList;
import java.util.List;

import CGen.LabelGen;
import Exc.CTX_EXC;
import Exc.SNIPS_EXC;
import Imm.AST.Statement.Declaration;
import Imm.TYPE.PROVISO;
import Imm.TYPE.TYPE;
import Imm.TYPE.COMPOSIT.ARRAY;
import Imm.TYPE.COMPOSIT.POINTER;
import Imm.TYPE.COMPOSIT.STRUCT;
import Imm.TYPE.PRIMITIVES.FUNC;
import Imm.TYPE.PRIMITIVES.PRIMITIVE;
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
	
	public void setContext(List<TYPE> context) throws CTX_EXC {
		if (context.size() != this.provisosTypes.size()) 
			throw new CTX_EXC(source, "Missmatching number of provided provisos, expected " + this.provisosTypes.size() + ", got " + context.size());
		
		for (int i = 0; i < this.provisosTypes.size(); i++) {
			TYPE pro = this.provisosTypes.get(i);
			if (!(pro instanceof PROVISO)) 
				throw new CTX_EXC(source, "Provided Type " + pro.typeString() + " is not a proviso type");
			
			PROVISO pro0 = (PROVISO) pro;
			pro0.setContext(context.get(i));
		}
	}
	
	public boolean isActiveContext(List<TYPE> context) {
		return this.mappingIsEqual(context, this.provisosTypes);
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
			
			if (map0.size() != map.size()) throw new SNIPS_EXC("Recieved proviso mapping length is not equal to expected length, expected " + map0.size() + ", but got " + map.size());
			
			if (this.mappingIsEqual(map0, map)) return true;
		}
		
		return false;
	}
	
	public boolean mappingIsEqual(List<TYPE> map0, List<TYPE> map1) {
		boolean isEqual = true;
		for (int a = 0; a < map0.size(); a++) 
			isEqual &= map0.get(a).isEqual(map1.get(a));
		return isEqual;
	}
	
	public String getPostfix(List<TYPE> map) {
		/* Search for postfix, if match is found return postfix */
		for (int i = 0; i < this.provisosCalls.size(); i++) {
			List<TYPE> map0 = this.provisosCalls.get(i).second.second;
			if (this.mappingIsEqual(map0, map)) 
				return this.provisosCalls.get(i).first;
		}
		
		return null;
	}
	
	public TYPE getMappingReturnType(List<TYPE> map) {
		for (int i = 0; i < this.provisosCalls.size(); i++) {
			List<TYPE> map0 = this.provisosCalls.get(i).second.second;
			
			boolean isEqual = true;
			for (int a = 0; a < map0.size(); a++) 
				isEqual &= map0.get(a).isEqual(map.get(a));
			
			if (isEqual) return this.provisosCalls.get(i).second.first;
		}
		
		throw new SNIPS_EXC("No mapping!");
	}
	
	public void addProvisoMapping(TYPE type, List<TYPE> context) {
		/* Proviso mapping already exists, just return */
		if (this.containsMapping(context)) return;
		else {
			/* Add proviso mapping, create new proviso postfix for mapping */
			String postfix = (context.isEmpty())? "" : LabelGen.getProvisoPostfix();
			this.provisosCalls.add(new Pair<String, Pair<TYPE, List<TYPE>>>(postfix, new Pair<TYPE, List<TYPE>>(type, context)));
		}
	}
	
	public static void setContext(List<TYPE> context, TYPE type, Source source) throws CTX_EXC {
		if (type instanceof PROVISO) {
			PROVISO p = (PROVISO) type;
			for (TYPE t : context) {
				if (p.isEqual(t)) {
					p.setContext(t);
					break;
				}
			}
		}
		else if (type instanceof FUNC) {
			FUNC f = (FUNC) type;
			if (f.funcHead != null) {
				for (Declaration d : f.funcHead.parameters) 
					d.setContext(context);
				
				setContext(context, f.funcHead.getReturnType(), source);
			}
		}
		else if (type instanceof ARRAY) {
			ARRAY arr = (ARRAY) type;
			
			if (arr.elementType instanceof STRUCT) {
				STRUCT s = (STRUCT) arr.elementType;
				
				setContext(context, s, source);
			}
			
			setContext(context, arr.elementType, source);
		}
		else if (type instanceof POINTER) {
			POINTER p = (POINTER) type;
			setContext(context, p.targetType, source);
		}
		else if (type instanceof STRUCT) {
			STRUCT s = (STRUCT) type;
			
			mapContextTo(s.proviso, context, source);
			
			/* Map initialized proviso types to typedef provisos */
			mapContextToStatic(s.typedef.proviso, s.proviso);
			
			/* Initialize capsuled proviso types */
			for (int i = 0; i < s.typedef.proviso.size(); i++) 
				s.typedef.proviso.set(i, setHiddenContext(s.typedef.proviso.get(i), source));
			
			/* Iterate over every field in the struct and apply the proviso */
			for (Declaration d : s.typedef.fields) {
				/* Prevent Recursion */
				if (!(d.getRawType() instanceof POINTER)) {
					
					if (d.getRawType() instanceof STRUCT) {
						STRUCT s0 = (STRUCT) d.getRawType();
						
						/* Map recieved context on the proviso types of the struct */
						ProvisoManager.mapContextTo(s0.proviso, context, source);
						
						/* Apply the previously initialized struct proviso to the fields */
						s0 = (STRUCT) setHiddenContext(s0, source);
					}
					
					setContext(s.typedef.proviso, d.getRawType(), source);
				}
				else {
					POINTER p = (POINTER) d.getRawType();
					
					/* Only Struct and Proviso needs proviso initialization */
					if (p.getCoreType() instanceof STRUCT) {
						STRUCT s1 = (STRUCT) p.getCoreType();
						
						// TODO ERROR HERE: PROVISOS OF STRUCT INIT TYPE ARE CHANGED WHEN CHECKING FIELDS
						
						/* Map recieved context on the proviso types of the struct */
						mapContextTo(s1.proviso, context, source);
						
						if (s1.typedef.path.build().equals(s.typedef.path.build())) 
							/* Struct is recursive, set loop reference and return */
							p.coreType = s1;
						else setContext(s1.proviso, s1, source);
					}
					else 
						/* Set the context of the target type, proviso can be capsuled inside of pointer */
						setContext(context, p.targetType, source);
				}
			}
		}
	}
	
	/**
	 * Maps the proviso types of the second argument to the first.
	 * @throws CTX_EXC 
	 */
	public static void mapContextTo(List<TYPE> target, List<TYPE> source, Source s) throws CTX_EXC {
		for (int i = 0; i < target.size(); i++) {
			/* Primitives may not have proviso types */
			if (target.get(i) instanceof PRIMITIVE) continue;
			
			setContext(source, target.get(i), s);
		}
	}
	
	/**
	 * Maps the proviso types of the second argument to the first, 1 to 1
	 */
	public static void mapContextToStatic(List<TYPE> target, List<TYPE> source) {
		for (int i = 0; i < target.size(); i++) {
			PROVISO p = (PROVISO) target.get(i);
			p.setContext(source.get(i));
		}
	}
	
	public static TYPE setHiddenContext(TYPE type, Source source) throws CTX_EXC {
		if (type instanceof PROVISO) {
			PROVISO p = (PROVISO) type;
			
			/* Initialize Capsuled Proviso type */
			if (p.hasContext()) 
				p.setContext(setHiddenContext(p.getContext(), source));
			
			return type;
		}
		else if (type instanceof ARRAY) {
			ARRAY arr = (ARRAY) type;
			arr.elementType = setHiddenContext(arr.elementType, source);
			return arr;
		}
		else if (type instanceof POINTER) {
			POINTER p = (POINTER) type;
			p.targetType = setHiddenContext(p.targetType, source);
			return p;
		}
		else if (type instanceof STRUCT) {
			STRUCT s = (STRUCT) type;
			
			if (s.proviso.size() != s.typedef.proviso.size()) 
				throw new CTX_EXC(source, "Expected " + s.typedef.proviso.size() + " provisos but got " + s.proviso.size());
			
			/* Map initialization proviso types to proviso head listing */
			mapContextToStatic(s.typedef.proviso, s.proviso);
			
			/* Propagate initialized proviso mapping on the fields */
			setContext(s.typedef.proviso, s, source);
			
			return s;
		}
		else return type;
	}
	
}
