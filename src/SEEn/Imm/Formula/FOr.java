package SEEn.Imm.Formula;

import SEEn.SEState;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class FOr extends FAbstr {

    public List<FAbstr> operands;

    public FOr(List<FAbstr> operands) {
        this.operands = operands;
    }

    public FOr(FAbstr...operands) {
        this.operands = Arrays.asList(operands);
    }

    public boolean eval(SEState state) {
        for (FAbstr formula : operands)
            if (formula.eval(state)) return true;
        return false;
    }

    public FAbstr clone() {
        return new FOr(this.operands.stream().map(x -> x.clone()).collect(Collectors.toList()));
    }

}
