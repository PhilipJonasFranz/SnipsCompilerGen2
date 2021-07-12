package SEEn.Imm.DLTerm;

import SEEn.SEState;

public class DLNot extends DLTerm {

    public DLTerm operand;

    public DLNot(DLTerm operand) {
        this.operand = operand;
    }

    public boolean isEqual(DLTerm term) {
        return false;
    }

    public boolean eval(SEState state) {
        return !this.operand.eval(state);
    }

    public DLTerm clone() {
        return new DLNot(this.operand.clone());
    }

    public String toString() {
        return "!" + this.operand.toString();
    }

}
