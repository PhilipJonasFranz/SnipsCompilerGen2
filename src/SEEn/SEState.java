package SEEn;

import Imm.AST.SyntaxElement;
import SEEn.Imm.DLTerm.DLAnd;
import SEEn.Imm.DLTerm.DLTerm;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SEState {

    public HashMap<String, DLTerm> variables = new HashMap<>();

    public SyntaxElement programCounter;

    public DLTerm pathCondition;

    public DLTerm postcondition;

    public SEState parent;

    public List<SEState> forked = new ArrayList<>();

    public int depth = 0;

    public SEState(SyntaxElement programCounter, DLTerm pathCondition, DLTerm postcondition) {
        this.programCounter = programCounter;
        this.pathCondition = pathCondition;
        this.postcondition = postcondition;
    }

    public SEState clone() {
        SEState state = new SEState(this.programCounter, this.pathCondition.clone(), this.postcondition.clone());

        for (Map.Entry<String, DLTerm> entry : this.variables.entrySet())
            state.variables.put(entry.getKey(), entry.getValue().clone());

        state.programCounter = this.programCounter;
        state.forked = new ArrayList<>();
        state.forked.addAll(this.forked);

        state.depth = this.depth;

        return state;
    }

    public SEState fork() {
        SEState child = this.clone();
        child.parent = this;
        child.depth = this.depth + 1;
        this.forked.add(child);
        return child;
    }

    public void addToPathCondition(DLTerm formula) {
        if (this.pathCondition instanceof DLAnd and) and.operands.add(formula);
        else this.pathCondition = new DLAnd(this.pathCondition, formula);
    }

    public void addToPostCondition(DLTerm formula) {
        if (this.postcondition instanceof DLAnd and) and.operands.add(formula);
        else this.postcondition = new DLAnd(this.postcondition, formula);
    }

    public String toString() {
        String vars = "";
        for (Map.Entry<String, DLTerm> var : this.variables.entrySet())
            vars += var.getKey() + " = " + var.getValue().toString() + ", ";
        if (vars.endsWith(", ")) vars = vars.substring(0, vars.length() - 2);
        else vars = "-";

        return "<" + this.programCounter.codePrint(0).get(0) + " : " + vars + " : [" + this.pathCondition.toString() + "] : [" + this.postcondition.toString() + "]>";
    }

    public void print() {
        for (int i = 0; i < this.depth; i++) System.out.print("    ");
        System.out.println(this.toString());
    }

    public void printRec(int d) {
        for (int i = 0; i < d; i++) System.out.print("    ");
        System.out.println(this.toString());
    }

}
