package SEEn.Imm.DLTerm;

import Ctx.Util.Callee;
import SEEn.SEState;
import Tools.DLTermModifier;
import Tools.DLTermVisitor;

import java.util.ArrayList;
import java.util.List;

public class DLCall extends DLTerm {

    public Callee callee;

    public DLCall(Callee callee) {
        this.callee = callee;
    }

    public boolean isEqual(DLTerm term) {
        return term instanceof DLCall call && this.callee.equals(call.callee);
    }

    public boolean eval(SEState state) {
        return false;
    }

    public DLTerm clone() {
        return new DLCall(this.callee);
    }

    public String toString() {
        return this.callee.getCallee().codePrintSingle();
    }

    public <T extends DLTerm> List<T> visit(DLTermVisitor<T> visitor) {
        List<T> result = new ArrayList<>();
        if (visitor.visit(this)) result.add((T) this);

        return result;
    }

    public <T extends DLTerm> void replace(DLTermModifier<T> visitor) {
        return;
    }

    public DLTerm simplify() {
        return this;
    }

}
