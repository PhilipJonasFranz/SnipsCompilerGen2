package SEEn.Imm.Formula;

import SEEn.SEState;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class FAnd extends FAbstr {

    public List<FAbstr> operands;

    public FAnd(List<FAbstr> operands) {
        this.operands = operands;
    }

    public FAnd(FAbstr...operands) {
        this.operands = Arrays.asList(operands);
    }

    public boolean eval(SEState state) {
        for (FAbstr formula : operands)
            if (!formula.eval(state)) return false;
        return true;
    }

    public FAbstr clone() {
        return new FAnd(this.operands.stream().map(x -> x.clone()).collect(Collectors.toList()));
    }

}
