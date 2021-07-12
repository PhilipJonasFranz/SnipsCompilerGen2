package SEEn.Imm.DLTerm;

import SEEn.SEState;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class DLOr extends DLTerm {

    public List<DLTerm> operands;

    public DLOr(List<DLTerm> operands) {
        this.operands = operands;
    }

    public DLOr(DLTerm...operands) {
        this.operands = Arrays.asList(operands);
    }

    public boolean isEqual(DLTerm term) {
        return false;
    }

    public boolean eval(SEState state) {
        for (DLTerm formula : operands)
            if (formula.eval(state)) return true;
        return false;
    }

    public DLTerm clone() {
        return new DLOr(this.operands.stream().map(DLTerm::clone).collect(Collectors.toList()));
    }

    public String toString() {
        return "(" + this.operands.stream().map(DLTerm::toString).collect(Collectors.joining(" || ")) + ")";
    }

}
