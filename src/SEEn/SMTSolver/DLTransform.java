package SEEn.SMTSolver;

import Exc.SNIPS_EXC;
import Imm.AST.Expression.InlineCall;
import SEEn.Imm.DLTerm.DLBind;
import SEEn.Imm.DLTerm.DLCall;
import SEEn.Imm.DLTerm.DLTerm;
import SEEn.Imm.DLTerm.DLVariable;
import SEEn.SEEngine;
import SEEn.SEState;
import Tools.DLTermModifier;

import java.util.HashMap;

public class DLTransform {

    private static DLTransform instance = new DLTransform();

    public static DLTransform getInstance() {
        return instance;
    }

    /**
     * Substitute all occurrences of the replace term in the target term with the replacement term.
     */
    public DLTerm substitute(DLTerm target, DLTerm replace, DLTerm replacement) {
        DLTermModifier mod = x -> replace.weakerOrEqual(x)? replacement.clone() : x;

        target.replace(mod);
        return mod.replace(target);
    }

    public DLTerm resolveBindings(SEState state, DLTerm term, DLTerm result) {
        HashMap<String, DLTerm> oldVarCache = new HashMap<>();

        DLTermModifier mod = new DLTermModifier() {
            public DLTerm replace(DLTerm s) {
                if (s instanceof DLBind bind) {
                    /*
                     * Resolve the \result binding. This is done by substituting
                     * the passed result DLTerm.
                     */
                    if (bind.name.equals("result")) {
                        if (result == null)
                            throw new SNIPS_EXC("Cannot resolve binding '\\result', result is null!");
                        return result.clone();
                    }
                    /*
                     * Resolve \old([varname]) bindings. This is done by searching
                     * the first occurrence in the SE-Tree and substituting the
                     * value found there.
                     */
                    else if (bind.name.equals("old")) {
                        String id = bind.id;

                        if (!oldVarCache.containsKey(id)) {
                            if (!state.variables.containsKey(id))
                                throw new SNIPS_EXC("Unknown variable: '" + id + "'");

                            DLTerm value = state.variables.get(id);
                            SEState state0 = state;
                            while (state0 != null) {
                                if (state0.variables.containsKey(id)) break;
                                else {
                                    value = state.variables.get(id);
                                    state0 = state0.parent;
                                }
                            }

                            oldVarCache.put(id, value);
                        }

                        return oldVarCache.get(id).clone();
                    }
                }
                return s;
            }
        };

        term.replace(mod);
        return mod.replace(term);
    }

    public DLTerm inlineVariables(SEState state, DLTerm term) {
        DLTermModifier mod = s -> {
            if (s instanceof DLVariable var) {
                if (var.name.startsWith("_")) {
                    return s;
                }
                else if (state.variables.containsKey(var.name)) return state.variables.get(var.name).clone();
            }
            return s;
        };

        term.replace(mod);
        return mod.replace(term);
    }

    public DLTerm inlineCalls(SEState state, SEEngine eng, DLTerm term) {
        DLTermModifier mod = s-> {
            if (s instanceof DLCall call) {
                InlineCall ic = (InlineCall) call.callee.getCallee();
                DLTerm returnCondition = eng.functionMap.get(ic.calledFunction).buildReturnConditionFromTerm();

                /* Substitute the variables in the precondition with the values from the call */
                for (int i = 0; i < ic.calledFunction.parameters.size(); i++) {
                    String varName = ic.calledFunction.parameters.get(i).path.build();

                    this.substitute(returnCondition, new DLVariable(varName), call.parameters.get(i).clone());
                    this.substitute(returnCondition, new DLBind("old", varName), call.parameters.get(i).clone());
                }

                return returnCondition;
            }
            else return s;
        };

        term.replace(mod);
        return mod.replace(term);
    }

}
