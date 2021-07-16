package SEEn.Imm.DLTerm;

import Imm.AST.Expression.Boolean.Compare.COMPARATOR;
import Imm.TYPE.PRIMITIVES.BOOL;
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

    public DLTerm simplify() {
        if (this.operator == COMPARATOR.EQUAL && this.left.isEqual(this.right)) return new DLAtom(new BOOL("true"));
        else if (this.left instanceof DLAtom a0 && this.right instanceof DLAtom a1) {
            int l = a0.value.toInt(), r = a1.value.toInt();

            boolean result = false;

            if (this.operator == COMPARATOR.EQUAL) result = l == r;
            else if (this.operator == COMPARATOR.NOT_EQUAL) result = l != r;
            else if (this.operator == COMPARATOR.GREATER_SAME) result = l >= r;
            else if (this.operator == COMPARATOR.LESS_SAME) result = l <= r;
            else if (this.operator == COMPARATOR.GREATER_THAN) result = l > r;
            else if (this.operator == COMPARATOR.LESS_THAN) result = l < r;

            return new DLAtom(new BOOL(result + ""));
        }

        return this;
    }

}
