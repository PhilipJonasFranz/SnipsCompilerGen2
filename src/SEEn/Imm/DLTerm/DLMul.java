package SEEn.Imm.DLTerm;

import java.util.List;
import java.util.stream.Collectors;

public class DLMul extends DLNFold {

    public DLMul(List<DLTerm> operands) {
        super(operands);
    }

    public DLMul(DLTerm...operands) {
        super(operands);
    }

    public boolean isEqual(DLTerm term) {
        if (term instanceof DLMul add) {
            if (this.operands.size() != add.operands.size()) return false;

            boolean equal = true;

            for (int i = 0; i < this.operands.size(); i++)
                equal &= this.operands.get(i).isEqual(add.operands.get(i));

            return equal;
        }
        return false;
    }

    public DLTerm clone() {
        return new DLMul(this.operands.stream().map(DLTerm::clone).collect(Collectors.toList()));
    }

    public String toString() {
        return "(" + this.operands.stream().map(DLTerm::toString).collect(Collectors.joining(" * ")) + ")";
    }

    public DLTerm simplify() {
        if (this.operands.size() == 1) return this.operands.get(0);
        return this;
    }

}
