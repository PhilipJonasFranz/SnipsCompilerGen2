package Ctx.Util;

import Ctx.Util.CheckUtil.Callee;
import Exc.CTEX_EXC;
import Imm.AST.Function;
import Imm.AST.Expression.IDRef;
import Imm.AST.Typedef.StructTypedef;
import Imm.TYPE.PROVISO;
import Imm.TYPE.COMPOSIT.STRUCT;
import Res.Const;
import Util.NamespacePath;

/**
 * Contains utility to transform InlineCalls and FunctionCalls during
 * Context-Checking. These transformations include implicit to explicit
 * call path conversion.
 *
 */
public class CtxCallUtil {

	public static void transformNestedSuperCall(Callee c, Function currentFunction) throws CTEX_EXC {
		if (!c.getParams().isEmpty() && c.getParams().get(0) instanceof IDRef) {
			/* Nested call to super function */
			IDRef ref = (IDRef) c.getParams().get(0);
			if (ref.path.build().equals("super")) {
				Function f0 = currentFunction;
				
				if (!f0.parameters.get(0).path.build().equals("self")) 
					throw new CTEX_EXC(Const.CANNOT_INVOKE_SUPER_OUTSIDE_STRUCT);
				
				STRUCT struct = (STRUCT) f0.parameters.get(0).getType().getCoreType();
				StructTypedef def = struct.getTypedef().extension;
				
				if (def == null)
					throw new CTEX_EXC(Const.CANNOT_INVOKE_SUPER_NO_EXTENSION, struct);
				
				/* Disable settings to perform signature match here */
				boolean check = STRUCT.useProvisoFreeInCheck;
				boolean comp = PROVISO.COMPARE_NAMES;
				STRUCT.useProvisoFreeInCheck = false;
				PROVISO.COMPARE_NAMES = false;

				/* Search for function in thee super-chain */
				boolean found = false;
				while (def != null) {
					for (Function f : def.functions) {
						if (f.path.build().endsWith(c.getPath().build())) {
							c.setPath(f.path.clone());
							found = true;
							break;
						}
					}
					
					def = def.extension;
				}
				
				STRUCT.useProvisoFreeInCheck = check;
				PROVISO.COMPARE_NAMES = comp;
				
				if (!found) throw new CTEX_EXC(Const.UNDEFINED_FUNCTION_OR_PREDICATE_IN_SUPER, f0.path, struct);
				
				/* Switch to self-reference */
				ref.path = new NamespacePath("self");
			}
		}
	}
	
	public static Function transformSuperConstructorCall(Callee c, Function currentFunction) throws CTEX_EXC {
		Function func = null;
		
		/* Calls to super constructor, switch out with call to constructor */
		if (c.getPath().build().equals("super")) {
			
			Function f0 = currentFunction;
			
			/* We are in a constructor */
			if (f0.isConstructor()) {
				STRUCT struct = (STRUCT) f0.getReturnType();
				StructTypedef def = struct.getTypedef();
				
				if (def.extension == null) 
					throw new CTEX_EXC(Const.CANNOT_INVOKE_SUPER_NO_EXTENSION, struct);
				
				boolean found = false;
				
				/* Search for constructor of extension */
				for (Function f1 : def.extension.functions) 
					if (f1.isConstructor()) {
						/* Found static constructor, switch out 'super' with path to constructor */
						c.setPath(f1.path.clone());
						func = f1;
						found = true;
						break;
					}
				
				/* No super constructor was found */
				if (!found) throw new CTEX_EXC(Const.CANNOT_INVOKE_SUPER_NO_CONSTRUCTOR, def.extension.self);
			}
			else {
				/* Call to super function, written as 'super(...)'. */
				if (!f0.parameters.get(0).path.build().equals("self")) 
					throw new CTEX_EXC(Const.CANNOT_INVOKE_SUPER_OUTSIDE_STRUCT);
				
				STRUCT struct = (STRUCT) f0.parameters.get(0).getType().getCoreType();
				StructTypedef def = struct.getTypedef();
				
				if (def.extension == null) throw new CTEX_EXC(Const.CANNOT_INVOKE_SUPER_NO_EXTENSION, struct);
				if (f0.inheritLink == null) throw new CTEX_EXC(Const.UNDEFINED_FUNCTION_OR_PREDICATE_IN_SUPER, f0.path.getLast(), def.extension.self);
				
				/* Set new path, add self-reference, set nested call */
				c.setPath(f0.inheritLink.path.clone());
				c.getParams().add(0, new IDRef(f0.parameters.get(0).path.clone(), c.getCallee().getSource()));
				c.setNestedCall(true);
			}
		}
		
		return func;
	}
	
}
