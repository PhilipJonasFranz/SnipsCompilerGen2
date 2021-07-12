package SEEn.Imm.DLTerm;

import SEEn.SEState;

public abstract class DLTerm {

    public abstract boolean isEqual(DLTerm term);

    public abstract boolean eval(SEState state);

    public abstract DLTerm clone();

    public abstract String toString();

}
