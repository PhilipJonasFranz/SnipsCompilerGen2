package SEEn.Imm.DLTerm;

import Imm.TYPE.TYPE;
import SEEn.SEState;

public class DLAtom extends DLTerm {

    public TYPE value;

    public DLAtom(TYPE value) {
        this.value = value;
    }

    public boolean isEqual(DLTerm term) {
        return false;
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

}
