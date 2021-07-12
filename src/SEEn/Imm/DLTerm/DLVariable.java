package SEEn.Imm.DLTerm;

import SEEn.SEState;

public class DLVariable extends DLTerm {

    public String name;

    public DLVariable(String name) {
        this.name = name;
    }

    public boolean isEqual() {
        return false;
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

}
