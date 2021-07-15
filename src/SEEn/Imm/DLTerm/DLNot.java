package SEEn.Imm.DLTerm;

import SEEn.SEState;
import Tools.DLTermModifier;
import Tools.DLTermVisitor;

import java.util.ArrayList;
import java.util.List;

public class DLNot extends DLTerm {

    public DLTerm operand;

    public DLNot(DLTerm operand) {
        this.operand = operand;
    }

    public boolean isEqual(DLTerm term) {
        if (term instanceof DLNot not) {
            return this.operand.isEqual(not.operand);
        }
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

    public <T extends DLTerm> List<T> visit(DLTermVisitor<T> visitor) {
        List<T> result = new ArrayList<>();
        if (visitor.visit(this)) result.add((T) this);

        result.addAll(this.operand.visit(visitor));

        return result;
    }

    public <T extends DLTerm> void replace(DLTermModifier<T> visitor) {
        this.operand.replace(visitor);
        this.operand = visitor.replace(this.operand);
    }

}
