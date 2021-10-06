package SEEn.Imm.DLTerm;

import Imm.TYPE.PRIMITIVES.BOOL;

import java.util.List;
import java.util.stream.Collectors;

public class DLAnd extends DLNFold {

    public DLAnd(List<DLTerm> operands) {
        super(operands);
    }

    public DLAnd(DLTerm...operands) {
        super(operands);
    }

    public boolean isEqual(DLTerm term) {
        if (term instanceof DLAnd and) {
            if (this.operands.size() != and.operands.size()) return false;

            boolean equal = true;

            for (int i = 0; i < this.operands.size(); i++)
                equal &= this.operands.get(i).isEqual(and.operands.get(i));

            return equal;
        }
        return false;
    }

    public DLTerm clone() {
        return new DLAnd(this.operands.stream().map(DLTerm::clone).collect(Collectors.toList()));
    }

    public String toString() {
        return "(" + this.operands.stream().map(DLTerm::toString).collect(Collectors.joining(" && ")) + ")";
    }

    public DLTerm simplify() {
        for (int i = 0; i < this.operands.size(); i++) {
            DLTerm op = this.operands.get(i);
            op = op.simplify();

            if (op instanceof DLAtom a && a.value instanceof BOOL b) {
                if (b.value) {
                    this.operands.remove(i);
                    i--;
                    continue;
                }
                else {
                    return new DLAtom(new BOOL("false"));
                }
            }

            this.operands.set(i, op);
        }

        if (this.operands.size() == 1) return this.operands.get(0);
        if (this.operands.isEmpty()) return new DLAtom(new BOOL("true"));

        return this;
    }

}
