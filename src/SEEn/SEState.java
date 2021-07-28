package SEEn;

import Imm.AST.SyntaxElement;
import SEEn.Imm.DLTerm.*;
import Util.Pair;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SEState {

    /**
     * Variables that are currently active in the program with their respective DLTerm values.
     */
    public HashMap<String, DLTerm> variables = new HashMap<>();

    /**
     * The program element this SEState executed.
     */
    public SyntaxElement programCounter;

    /**
     * The path condition that contains conditions collected at control flow statements etc.
     */
    public DLTerm pathCondition;

    public DLTerm invariantCondition;

    /**
     * The precondtion of the current function, which is parsed from 'requires'-Annotations.
     */
    public DLAnd precondition = new DLAnd();

    /**
     * The postcondition of the current function, which is parsed from 'ensures'-Annotations.
     */
    public DLTerm postcondition;

    /**
     * Contains the possible return conditions of the current function. A DLTerm that represents
     * this value can be created using buildReturnConditionFromTerm().
     */
    public List<Pair<DLTerm, DLTerm>> returnCondition = new ArrayList<>();

    /**
     * The SEState this state forked from.
     */
    public SEState parent;

    /**
     * SEStates that forked from this state.
     */
    private List<SEState> forked = new ArrayList<>();

    /**
     * The depth of this state in the execution tree.
     */
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
        child.forked.clear();

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

        return "<" + ((this.programCounter == null)? "?" : this.programCounter.codePrintSingle()) + " : " + vars + " : [" + this.pathCondition.toString() + "] : [" + this.postcondition.toString() + "]>";
    }

    public void print() {
        for (int i = 0; i < this.depth; i++) System.out.print("    ");
        System.out.println(this);
    }

    public void printRec() {
        for (int i = 0; i < this.depth; i++) System.out.print("    ");
        System.out.println(this);
        for (SEState state0 : this.forked)
            state0.printRec();
    }

    public DLTerm buildReturnConditionFromTerm() {
        if (this.returnCondition.size() == 1) return this.returnCondition.get(0).getSecond().clone();
        else {
            DLTern tern = null;
            for (Pair<DLTerm, DLTerm> p : this.returnCondition) {
                if (tern == null) {
                    tern = new DLTern(p.first.clone(), p.second.clone(), null);
                }
                else if (tern.right == null) tern.right = p.second.clone();
                else tern = new DLTern(p.first.clone(), p.second.clone(), tern);
            }

            return tern;
        }
    }

}
