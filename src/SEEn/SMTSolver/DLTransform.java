package SEEn.SMTSolver;

import Exc.SNIPS_EXC;
import SEEn.Imm.DLTerm.DLBind;
import SEEn.Imm.DLTerm.DLTerm;
import SEEn.Imm.DLTerm.DLVariable;
import SEEn.SEState;
import Tools.DLTermModifier;

public class DLTransform {

    private static DLTransform instance = new DLTransform();

    public static DLTransform getInstance() {
        return instance;
    }

    public DLTerm substitute(DLTerm target, DLTerm replace, DLTerm with) {
        target.replace(x -> x.isEqual(replace)? with.clone() : x);
        return target.isEqual(replace)? with.clone() : target;
    }

    public DLTerm resolveBindings(SEState state, DLTerm term, DLTerm result) {
        DLTermModifier mod = new DLTermModifier() {
            public DLTerm replace(DLTerm s) {
                if (s instanceof DLBind bind) {
                    if (bind.name.equals("result")) {
                        if (result == null)
                            throw new SNIPS_EXC("Cannot resolve binding '\\result', result is null!");
                        return result.clone();
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
