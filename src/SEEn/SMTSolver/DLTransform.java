package SEEn.SMTSolver;

import Exc.SNIPS_EXC;
import SEEn.Imm.DLTerm.DLBind;
import SEEn.Imm.DLTerm.DLTerm;
import SEEn.Imm.DLTerm.DLVariable;
import SEEn.SEState;
import Tools.DLTermModifier;

import java.util.HashMap;

public class DLTransform {

    private static DLTransform instance = new DLTransform();

    public static DLTransform getInstance() {
        return instance;
    }

    public DLTerm substitute(DLTerm target, DLTerm replace, DLTerm with) {
        DLTermModifier mod = x -> x.isEqual(replace)? with.clone() : x;

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

    public DLTerm inlineVariables(SEState state,DLTerm term) {
        DLTermModifier mod = s-> (s instanceof DLVariable var)? state.variables.get(var.name).clone() : s;
        term.replace(mod);
        return mod.replace(term);
    }

}
