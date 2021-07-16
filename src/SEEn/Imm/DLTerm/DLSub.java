package SEEn.Imm.DLTerm;

import SEEn.SEState;
import Tools.DLTermModifier;
import Tools.DLTermVisitor;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class DLSub extends DLTerm {

    public List<DLTerm> operands = new ArrayList<>();

    public DLSub(List<DLTerm> operands) {
        this.operands.addAll(operands);
    }

    public DLSub(DLTerm...operands) {
        for (DLTerm op : operands)
            this.operands.add(op);
    }

    public boolean isEqual(DLTerm term) {
        if (term instanceof DLSub sub) {
            if (this.operands.size() != sub.operands.size()) return false;

            boolean equal = true;

            for (int i = 0; i < this.operands.size(); i++)
                equal &= this.operands.get(i).isEqual(sub.operands.get(i));

            return equal;
        }
        return false;
    }

    public boolean eval(SEState state) {
        for (DLTerm formula : operands)
            if (formula.eval(state)) return true;
        return false;
    }

    public DLTerm clone() {
        return new DLSub(this.operands.stream().map(DLTerm::clone).collect(Collectors.toList()));
    }

    public String toString() {
        return "(" + this.operands.stream().map(DLTerm::toString).collect(Collectors.joining(" - ")) + ")";
    }

    public DLTerm simplify() {
        return this;
    }

    public <T extends DLTerm> List<T> visit(DLTermVisitor<T> visitor) {
        List<T> result = new ArrayList<>();
        if (visitor.visit(this)) result.add((T) this);

        for (DLTerm op : this.operands)
            result.addAll(op.visit(visitor));

        return result;
    }

    public <T extends DLTerm> void replace(DLTermModifier<T> visitor) {
        for (int i = 0; i < this.operands.size(); i++) {
            this.operands.get(i).replace(visitor);
            this.operands.set(i, visitor.replace(this.operands.get(i)));
        }
    }

}
