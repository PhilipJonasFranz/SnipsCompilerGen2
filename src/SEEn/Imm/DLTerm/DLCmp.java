package SEEn.Imm.DLTerm;

import Imm.AST.Expression.Boolean.Compare.COMPARATOR;
import SEEn.SEState;

public class DLCmp extends DLTerm {

    public DLTerm left, right;

    public COMPARATOR operator;

    public DLCmp(DLTerm left, DLTerm right, COMPARATOR operator) {
        this.left = left;
        this.right = right;
        this.operator = operator;
    }

    public boolean isEqual(DLTerm term) {
        return false;
    }

    public boolean eval(SEState state) {
        return false;
    }

    public DLTerm clone() {
        return new DLCmp(left.clone(), right.clone(), operator);
    }

    public String toString() {
        return "(" + this.left.toString() + " " + this.operator.toString() + " " + this.right.toString() + ")";
    }

}
