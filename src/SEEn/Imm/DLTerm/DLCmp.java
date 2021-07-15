package SEEn.Imm.DLTerm;

import Imm.AST.Expression.Boolean.Compare.COMPARATOR;
import SEEn.SEState;
import Tools.DLTermModifier;
import Tools.DLTermVisitor;

import java.util.ArrayList;
import java.util.List;

public class DLCmp extends DLTerm {

    public DLTerm left, right;

    public COMPARATOR operator;

    public DLCmp(DLTerm left, DLTerm right, COMPARATOR operator) {
        this.left = left;
        this.right = right;
        this.operator = operator;
    }

    public boolean isEqual(DLTerm term) {
        if (term instanceof DLCmp cmp) {
            boolean equal = true;

            equal &= cmp.operator == this.operator;

            equal &= this.left.isEqual(cmp.left);
            equal &= this.right.isEqual(cmp.right);

            return equal;
        }
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

    public <T extends DLTerm> List<T> visit(DLTermVisitor<T> visitor) {
        List<T> result = new ArrayList<>();
        if (visitor.visit(this)) result.add((T) this);

        result.addAll(this.left.visit(visitor));
        result.addAll(this.right.visit(visitor));

        return result;
    }

    public <T extends DLTerm> void replace(DLTermModifier<T> visitor) {
        this.left.replace(visitor);
        this.left = visitor.replace(this.left);

        this.right.replace(visitor);
        this.right = visitor.replace(this.right);
    }

}
