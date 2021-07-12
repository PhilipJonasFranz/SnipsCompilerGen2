package SEEn.Imm.Formula;

import SEEn.SEState;

public abstract class FAbstr {

    public abstract boolean eval(SEState state);

    public abstract FAbstr clone();

}
