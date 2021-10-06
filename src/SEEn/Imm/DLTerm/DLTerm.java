package SEEn.Imm.DLTerm;

import SEEn.SEState;
import Tools.DLTermModifier;
import Tools.DLTermVisitor;

import java.util.List;

public abstract class DLTerm {

    public abstract boolean isEqual(DLTerm term);

    public boolean weakerOrEqual(DLTerm term) {
        return this.isEqual(term);
    }

    public abstract boolean eval(SEState state);

    public abstract DLTerm clone();

    public abstract String toString();

    public abstract DLTerm simplify();

    public abstract <T extends DLTerm> List<T> visit(DLTermVisitor<T> visitor);

    public abstract <T extends DLTerm> void replace(DLTermModifier<T> visitor);

}
