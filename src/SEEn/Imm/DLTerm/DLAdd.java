package SEEn.Imm.DLTerm;

import Imm.TYPE.PRIMITIVES.INT;

import java.util.List;
import java.util.stream.Collectors;

public class DLAdd extends DLNFold {

    public DLAdd(List<DLTerm> operands) {
        super(operands);
    }

    public DLAdd(DLTerm...operands) {
        super(operands);
    }

    public boolean isEqual(DLTerm term) {
        if (term instanceof DLAdd add) {
            if (this.operands.size() != add.operands.size()) return false;

            boolean equal = true;

            for (int i = 0; i < this.operands.size(); i++)
                equal &= this.operands.get(i).isEqual(add.operands.get(i));

            return equal;
        }
        return false;
    }

    public DLTerm clone() {
        return new DLAdd(this.operands.stream().map(DLTerm::clone).collect(Collectors.toList()));
    }

    public String toString() {
        return "(" + this.operands.stream().map(DLTerm::toString).collect(Collectors.joining(" + ")) + ")";
    }

    public DLTerm simplify() {
        int res = 0;

        for (int i = 0; i < this.operands.size(); i++) {
            DLTerm op = this.operands.get(i);
            op = op.simplify();

            if (op instanceof DLAtom a && a.value instanceof INT int0) {
                if (int0.hasInt()) {
                    res += int0.toInt();

                    this.operands.remove(i);
                    i--;
                    continue;
                }
            }

            this.operands.set(i, op);
        }

        if (res != 0 || this.operands.isEmpty()) this.operands.add(0, new DLAtom(new INT("" + res)));

        if (this.operands.size() == 1) return this.operands.get(0);
        return this;
    }

}
