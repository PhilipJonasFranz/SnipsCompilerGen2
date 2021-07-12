package SEEn.Imm.DLTerm;

import SEEn.SEState;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class DLAnd extends DLTerm {

    public List<DLTerm> operands = new ArrayList<>();

    public DLAnd(List<DLTerm> operands) {
        this.operands = operands;
    }

    public DLAnd(DLTerm...operands) {
        this.operands.addAll(Arrays.asList(operands));
    }

    public boolean isEqual(DLTerm term) {
        if (term instanceof DLAnd and) {
            if (this.operands.size() == and.operands.size()) {
                for (int i = 0; i < this.operands.size(); i++) {
                    if (!this.operands.get(i).isEqual(and.operands.get(i))) return false;
                }

                return true;
            }
        }

        return false;
    }

    public boolean eval(SEState state) {
        for (DLTerm term : operands)
            if (!term.eval(state)) return false;
        return true;
    }

    public DLTerm clone() {
        return new DLAnd(this.operands.stream().map(DLTerm::clone).collect(Collectors.toList()));
    }

    public String toString() {
        return "(" + this.operands.stream().map(DLTerm::toString).collect(Collectors.joining(" && ")) + ")";
    }

}
