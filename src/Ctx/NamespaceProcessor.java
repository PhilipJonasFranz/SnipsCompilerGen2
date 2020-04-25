package Ctx;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import Exc.SNIPS_EXCEPTION;
import Imm.AST.Function;
import Imm.AST.Namespace;
import Imm.AST.Program;
import Imm.AST.SyntaxElement;
import Imm.AST.Statement.Declaration;
import Imm.AST.Statement.StructTypedef;
import lombok.NoArgsConstructor;

/**
 * The namespace processor is responsible to merge namespaces with the same name,
 * and flatten their syntax elements into the syntax elements of the program.
 */
@NoArgsConstructor
public class NamespaceProcessor {
	
	public void process(Program p) {
		List<Namespace> namespaces = new ArrayList();
		
		for (SyntaxElement s : p.programElements) {
			if (s instanceof Namespace) namespaces.add((Namespace) s);
		}
		
		/* Merge and integrate namespaces */
		merge(namespaces);
		
		List<String> names, containedNames = new ArrayList();
		names = namespaces.stream().map(x -> x.path.build()).collect(Collectors.toList());
		
		/* Remove duplicates */
		for (int i = 0; i < p.programElements.size(); i++) {
			if (p.programElements.get(i) instanceof Namespace) {
				Namespace n = (Namespace) p.programElements.get(i);
				/* Check if namespace is still in list, or already added */
				if (!names.contains(n.path.build()) || containedNames.contains(n.path.build())) {
					p.programElements.remove(i);
					i--;
				}
				else containedNames.add(n.path.build());
			}
		}
		
		/* Flatten namespaces */
		for (int i = 0; i < p.programElements.size(); i++) {
			if (p.programElements.get(i) instanceof Namespace) {
				Namespace n = (Namespace) p.programElements.get(i);
				p.programElements.remove(i);
				List<SyntaxElement> target = new ArrayList();
				flatten(target, n);
				p.programElements.addAll(i, target);
				i--;
			}
		}
	}
	
	protected void flatten(List<SyntaxElement> target, Namespace name) {
		for (SyntaxElement s : name.programElements) {
			if (s instanceof Namespace) {
				Namespace n = (Namespace) s;
				
				/* Append current namespace path to namespace, and flatten into target */
				n.path.path.addAll(0, name.path.path);
				flatten(target, n);
			}
			else {
				/* Global declaration */
				if (s instanceof Declaration) {
					Declaration dec = (Declaration) s;
					
					/* Append current namespace path */
					dec.path.path.addAll(0, name.path.path);
				}
				else if (s instanceof StructTypedef) {
					/* Full path is already present */
				}
				else if (s instanceof Function) {
					Function f = (Function) s;
					
					f.path.path.addAll(0, name.path.path);
				}
				else throw new SNIPS_EXCEPTION("Cannot flatten " + s.getClass().getName());
				
				target.add(s);
			}
		}
	}
	
	protected void merge(List<Namespace> namespaces) {
		for (int i = 0; i < namespaces.size(); i++) {
			merge(namespaces.get(i));
			
			for (int a = i + 1; a < namespaces.size(); a++) {
				if (namespaces.get(i).path.getPath().get(0).equals(namespaces.get(a).path.getPath().get(0))) {
					/* Namespace pops into this namespace, add all elements */
					if (namespaces.get(a).path.path.size() == 1) {
						namespaces.get(i).programElements.addAll(namespaces.get(a).programElements);
					}
					/* Namespace will capsule in this namespace, cut from path and add namespace */
					else {
						namespaces.get(a).path.path.remove(0);
						namespaces.get(i).programElements.add(namespaces.get(a));
					}
					
					namespaces.remove(a);
				}
			}
		}
	}
	
	protected void merge(Namespace n) {
		List<Namespace> name = getNamespaces(n);
		merge(name);
	}
	
	public List<Namespace> getNamespaces(Namespace n) {
		List<Namespace> name = new ArrayList();
		for (SyntaxElement s : n.programElements) {
			if (s instanceof Namespace) name.add((Namespace) s);
		}
		return name;
	}
	
}
