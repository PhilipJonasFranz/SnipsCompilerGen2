package SEEn.Imm.DLTerm;

import Imm.TYPE.TYPE;
import SEEn.SEState;
import Tools.DLTermModifier;
import Tools.DLTermVisitor;

import java.util.ArrayList;
import java.util.List;

public class DLAtom extends DLTerm {

    public TYPE value;

    public DLAtom(TYPE value) {
        this.value = value;
    }

    public boolean isEqual(DLTerm term) {
        return term instanceof DLAtom a && this.value.isEqual(a.value);
    }

    public boolean eval(SEState state) {
        return false;
    }

    public DLTerm clone() {
        return new DLAtom(this.value);
    }

    public String toString() {
        return this.value.value.toString();
    }

    public <T extends DLTerm> List<T> visit(DLTermVisitor<T> visitor) {
        List<T> result = new ArrayList<>();
        if (visitor.visit(this)) result.add((T) this);
        return result;
    }

    public <T extends DLTerm> void replace(DLTermModifier<T> visitor) {
        return;
    }

}
