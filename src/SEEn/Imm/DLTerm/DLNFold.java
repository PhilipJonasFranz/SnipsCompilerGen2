package SEEn.Imm.DLTerm;

import SEEn.SEState;
import Tools.DLTermModifier;
import Tools.DLTermVisitor;

import java.util.ArrayList;
import java.util.List;

public abstract class DLNFold extends DLTerm {

    public List<DLTerm> operands = new ArrayList<>();

    public DLNFold(List<DLTerm> operands) {
        this.operands.addAll(operands);
    }

    public DLNFold(DLTerm...operands) {
        for (DLTerm op : operands)
            this.operands.add(op);
    }

    public boolean eval(SEState state) {
        for (DLTerm formula : operands)
            if (formula.eval(state)) return true;
        return false;
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
