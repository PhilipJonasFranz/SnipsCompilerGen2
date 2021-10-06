package SEEn.Imm.DLTerm;

import Imm.TYPE.PRIMITIVES.BOOL;
import SEEn.SEState;
import Tools.DLTermModifier;
import Tools.DLTermVisitor;

import java.util.ArrayList;
import java.util.List;

public class DLTern extends DLTerm {

    public DLTerm condition, left, right;

    public DLTern(DLTerm condition, DLTerm left, DLTerm right) {
        this.left = left;
        this.right = right;
        this.condition = condition;
    }

    public boolean isEqual(DLTerm term) {
        if (term instanceof DLTern tern) {
            return this.condition.isEqual(tern.condition) && this.left.isEqual(tern.left) && this.right.isEqual(tern.right);
        }
        return false;
    }

    public boolean eval(SEState state) {
        return false;
    }

    public DLTerm clone() {
        return new DLTern(this.condition.clone(), this.left.clone(), this.right.clone());
    }

    public String toString() {
        return "((" + this.condition.toString() + ")? " + this.left.toString() + " : " + this.right.toString() + ")";
    }

    public DLTerm simplify() {
        this.left = this.left.simplify();
        this.right = this.right.simplify();
        this.condition = this.condition.simplify();

        if (this.condition instanceof DLAtom atom && atom.value instanceof BOOL b && b.value) return this.left;

        return this;
    }

    public <T extends DLTerm> List<T> visit(DLTermVisitor<T> visitor) {
        List<T> result = new ArrayList<>();
        if (visitor.visit(this)) result.add((T) this);

        result.addAll(this.condition.visit(visitor));
        result.addAll(this.left.visit(visitor));
        result.addAll(this.right.visit(visitor));

        return result;
    }

    public <T extends DLTerm> void replace(DLTermModifier<T> visitor) {
        this.condition.replace(visitor);
        this.condition = visitor.replace(this.condition);

        this.left.replace(visitor);
        this.left = visitor.replace(this.left);

        this.right.replace(visitor);
        this.right = visitor.replace(this.right);
    }

}
