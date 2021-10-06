package SEEn.Imm.DLTerm;

import SEEn.SEState;
import Tools.DLTermModifier;
import Tools.DLTermVisitor;

import java.util.ArrayList;
import java.util.List;

public class DLSum extends DLTerm {

    public DLTerm iterator;

    public DLTerm condition;

    public DLTerm operand;

    public DLSum(DLTerm iterator, DLTerm condition, DLTerm operand) {
        this.iterator = iterator;
        this.condition = condition;
        this.operand = operand;
    }

    public boolean isEqual(DLTerm term) {
        if (term instanceof DLSum sum) {
            return this.iterator.isEqual(sum.iterator) && this.condition.isEqual(sum.condition) && this.operand.isEqual(sum.operand);
        }
        return false;
    }

    public boolean eval(SEState state) {
        return false;
    }

    public DLTerm clone() {
        return new DLSum(this.iterator.clone(), this.condition.clone(), this.operand.clone());
    }

    public String toString() {
        return "\\sum(" + this.iterator.toString() + "; " + this.condition.toString() + "; " + this.operand.toString() + ")";
    }

    public DLTerm simplify() {
        this.condition = this.condition.simplify();
        this.operand = this.operand.simplify();

        return this;
    }

    public <T extends DLTerm> List<T> visit(DLTermVisitor<T> visitor) {
        List<T> result = new ArrayList<>();
        if (visitor.visit(this)) result.add((T) this);

        result.addAll(this.iterator.visit(visitor));
        result.addAll(this.condition.visit(visitor));
        result.addAll(this.operand.visit(visitor));

        return result;
    }

    public <T extends DLTerm> void replace(DLTermModifier<T> visitor) {
        this.iterator.replace(visitor);
        this.iterator = (DLVariable) visitor.replace(this.iterator);

        this.condition.replace(visitor);
        this.condition = visitor.replace(this.condition);

        this.operand.replace(visitor);
        this.operand = visitor.replace(this.operand);
    }

}
