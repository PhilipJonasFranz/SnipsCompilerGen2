package SEEn.Imm.DLTerm;

import Imm.TYPE.PRIMITIVES.BOOL;
import SEEn.SEState;
import Tools.DLTermModifier;
import Tools.DLTermVisitor;

import java.util.ArrayList;
import java.util.List;

public class DLNot extends DLTerm {

    public DLTerm operand;

    public DLNot(DLTerm operand) {
        this.operand = operand;
    }

    public boolean isEqual(DLTerm term) {
        if (term instanceof DLNot not) {
            return this.operand.isEqual(not.operand);
        }
        return false;
    }

    public boolean eval(SEState state) {
        return !this.operand.eval(state);
    }

    public DLTerm clone() {
        return new DLNot(this.operand.clone());
    }

    public String toString() {
        return "!" + this.operand.toString();
    }

    public <T extends DLTerm> List<T> visit(DLTermVisitor<T> visitor) {
        List<T> result = new ArrayList<>();
        if (visitor.visit(this)) result.add((T) this);

        result.addAll(this.operand.visit(visitor));

        return result;
    }

    public <T extends DLTerm> void replace(DLTermModifier<T> visitor) {
        this.operand.replace(visitor);
        this.operand = visitor.replace(this.operand);
    }

    public DLTerm simplify() {
        this.operand = operand.simplify();

        if (operand instanceof DLNot not) return not.operand;
        else if (operand instanceof DLAnd and) {
            DLOr or = new DLOr();
            for (DLTerm term : and.operands) {
                or.operands.add(new DLNot(term));
            }
            this.operand = or.simplify();
        }
        else if (operand instanceof DLOr or) {
            DLAnd and = new DLAnd();
            for (DLTerm term : or.operands) {
                and.operands.add(new DLNot(term));
            }
            this.operand = and.simplify();
        }
        else if (this.operand instanceof DLAtom a && a.value instanceof BOOL b) {
            if (b.value) return new DLAtom(new BOOL("false"));
            else return new DLAtom(new BOOL("true"));
        }
        else if (this.operand instanceof DLCmp cmp) {
            cmp.operator = cmp.operator.negate();
            return cmp;
        }

        return this;
    }

}
