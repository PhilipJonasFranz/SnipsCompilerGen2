package SEEn.Imm.DLTerm;

import Imm.TYPE.PRIMITIVES.BOOL;
import SEEn.SEState;
import Tools.DLTermModifier;
import Tools.DLTermVisitor;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class DLAnd extends DLTerm {

    public List<DLTerm> operands = new ArrayList<>();

    public DLAnd(List<DLTerm> operands) {
        for (DLTerm op : operands) {
            if (op instanceof DLAtom atom)
                if (atom.value instanceof BOOL b && b.value)
                    continue;

            this.operands.add(op);
        }
    }

    public DLAnd(DLTerm...operands) {
        for (DLTerm op : operands) {
            if (op instanceof DLAtom atom)
                if (atom.value instanceof BOOL b && b.value)
                    continue;

            this.operands.add(op);
        }
    }

    public boolean isEqual(DLTerm term) {
        if (term instanceof DLAnd and) {
            if (this.operands.size() != and.operands.size()) return false;

            boolean equal = true;

            for (int i = 0; i < this.operands.size(); i++)
                equal &= this.operands.get(i).isEqual(and.operands.get(i));

            return equal;
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

    public <T extends DLTerm> List<T> visit(DLTermVisitor<T> visitor) {
        List<T> result = new ArrayList<>();
        if (visitor.visit(this)) result.add((T) this);

        for (DLTerm op : this.operands)
            result.addAll(op.visit(visitor));

        return result;
    }

    public <T extends DLTerm> void replace(DLTermModifier<T> visitor) {
        for (int i = 0; i < this.operands.size(); i++) {
            this.operands.get(i).replace(visitor);
            this.operands.set(i, visitor.replace(this.operands.get(i)));
        }
    }

    public DLTerm simplify() {
        for (int i = 0; i < this.operands.size(); i++) {
            DLTerm op = this.operands.get(i);
            op = op.simplify();

            if (op instanceof DLAtom a && a.value instanceof BOOL b) {
                if (b.value) {
                    this.operands.remove(i);
                    i--;
                }
                else {
                    return new DLAtom(new BOOL("false"));
                }
            }
        }

        if (this.operands.isEmpty()) return new DLAtom(new BOOL("true"));

        return this;
    }

}
