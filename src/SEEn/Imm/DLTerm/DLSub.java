package SEEn.Imm.DLTerm;

import Imm.TYPE.PRIMITIVES.INT;

import java.util.List;
import java.util.stream.Collectors;

public class DLSub extends DLNFold {

    public DLSub(List<DLTerm> operands) {
        super(operands);
    }

    public DLSub(DLTerm...operands) {
        super(operands);
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

    public DLTerm clone() {
        return new DLSub(this.operands.stream().map(DLTerm::clone).collect(Collectors.toList()));
    }

    public String toString() {
        return "(" + this.operands.stream().map(DLTerm::toString).collect(Collectors.joining(" - ")) + ")";
    }

    public DLTerm simplify() {
        int res = 0;
        boolean hadAtom = false;

        for (int i = 0; i < this.operands.size(); i++) {
            DLTerm op = this.operands.get(i);
            op = op.simplify();

            if (op instanceof DLAtom a && a.value instanceof INT int0) {
                if (int0.hasInt()) {
                    if (hadAtom) res -= int0.toInt();
                    else {
                        hadAtom = true;
                        res = int0.toInt();
                    }

                    this.operands.remove(i);
                    i--;
                    continue;
                }
            }

            this.operands.set(i, op);
        }

        if (hadAtom) this.operands.add(0, new DLAtom(new INT("" + res)));

        if (this.operands.size() == 1) return this.operands.get(0);
        return this;
    }

}
