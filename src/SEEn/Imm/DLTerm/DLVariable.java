package SEEn.Imm.DLTerm;

import SEEn.SEState;
import Tools.DLTermModifier;
import Tools.DLTermVisitor;

import java.util.ArrayList;
import java.util.List;

public class DLVariable extends DLTerm {

    public String name;

    public DLVariable(String name) {
        this.name = name;
    }

    public boolean isEqual(DLTerm term) {
        return term instanceof DLVariable v && v.name.equals(this.name);
    }

    public boolean eval(SEState state) {
        return false;
    }

    public DLTerm clone() {
        return new DLVariable(this.name);
    }

    public String toString() {
        return this.name;
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
