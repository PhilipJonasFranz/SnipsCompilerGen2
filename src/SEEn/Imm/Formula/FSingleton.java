package SEEn.Imm.Formula;

import SEEn.SEState;

public class FSingleton extends FAbstr {

    public boolean value;

    public FSingleton(boolean value) {
        this.value = value;
    }

    public boolean eval(SEState state) {
        return this.value;
    }

    public FAbstr clone() {
        return new FSingleton(this.value);
    }

}
