package SEEn.Imm.Formula;

import Imm.AST.Expression.Boolean.Compare;
import SEEn.Imm.Term.DLTerm;
import SEEn.SEState;

public class FCmp extends FAbstr {

    public DLTerm left, right;

    public Compare.COMPARATOR operator;

    public FCmp(DLTerm left, DLTerm right, Compare.COMPARATOR operator) {
        this.left = left;
        this.right = right;
        this.operator = operator;
    }

    public boolean eval(SEState state) {
        return false;
    }

    public FAbstr clone() {
        return new FCmp(left.clone(), right.clone(), operator);
    }
}
