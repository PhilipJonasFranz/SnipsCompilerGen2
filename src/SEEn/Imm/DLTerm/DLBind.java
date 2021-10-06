package SEEn.Imm.DLTerm;

import SEEn.SEState;
import Tools.DLTermModifier;
import Tools.DLTermVisitor;

import java.util.ArrayList;
import java.util.List;

public class DLBind extends DLTerm {

    public String name, id;

    public DLBind(String name) {
        this.name = name;
    }

    public DLBind(String name, String id) {
        this.name = name;
        this.id = id;
    }

    public boolean isEqual(DLTerm term) {
        if (term instanceof DLBind b && b.name.equals(this.name)) {
            if (this.id == null) return b.id == null;
            else if (b.id == null) return false;
            else return this.id.equals(b.id);
        }
        return false;
    }

    public boolean eval(SEState state) {
        if (name.equals("result")) {

        }
        else if (name.equals("old")) {

        }

        return false;
    }

    public DLTerm clone() {
        return new DLBind(this.name, this.id);
    }

    public String toString() {
        String s = "\\" + this.name;
        if (this.id != null)
            s += "(" + this.id + ")";
        return s;
    }

    public <T extends DLTerm> List<T> visit(DLTermVisitor<T> visitor) {
        List<T> result = new ArrayList<>();
        if (visitor.visit(this)) result.add((T) this);

        return result;
    }

    public <T extends DLTerm> void replace(DLTermModifier<T> visitor) {
        return;
    }

    public DLTerm simplify() {
        return this;
    }

}
