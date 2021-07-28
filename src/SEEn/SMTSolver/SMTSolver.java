package SEEn.SMTSolver;

import Exc.SNIPS_EXC;
import Imm.TYPE.PRIMITIVES.BOOL;
import SEEn.Imm.DLTerm.*;
import SEEn.SEState;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class SMTSolver {

    private static final SMTSolver instance = new SMTSolver();

    public static SMTSolver getInstance() {
        return SMTSolver.instance;
    }

    public boolean solve(SEState state, DLTerm formula, DLTerm toVerify) {
        DLTerm formula0 = formula.clone(), toVerify0 = toVerify.clone();

        formula = formula.clone().simplify();
        toVerify = toVerify.clone();

        List<DLTerm> toProve = getClauses(state, formula, toVerify);

        /* Simplify the formulas as much as possible */
        toProve = toProve.stream().map(DLTerm::simplify).collect(Collectors.toList());

        List<DLTerm> duplicateFree = new ArrayList<>();
        for (DLTerm term : toProve) {
            if (duplicateFree.stream().noneMatch(x -> x.isEqual(term))) {
                duplicateFree.add(term);
            }
        }
        toProve = duplicateFree;

        for (int i = 0; i < toProve.size(); i++) {
            if (proveClause(toProve.get(i).clone(), formula.clone())) {
                toProve.remove(i);
                i--;
            }
        }

        return toProve.isEmpty();
    }

    /**
     * Returns a list of clauses that need to be proven under the given formula.
     * By descending down the left formula, at each DLOr, a split occurrs, which generates an additional clause.
     */
    public List<DLTerm> getClauses(SEState state, DLTerm formula, DLTerm toVerify) {
        if (formula instanceof DLAnd and) return getClausesAndLeft(state, and, toVerify);
        else if (formula instanceof DLOr or) return getClausesOrLeft(state, or, toVerify);
        else return List.of(toVerify.clone());
    }

    public List<DLTerm> getClausesAndLeft(SEState state, DLAnd and, DLTerm toVerify) {
        toVerify = toVerify.clone();
        DLTransform transform = DLTransform.getInstance();

        List<DLTerm> result = new ArrayList<>();

        for (DLTerm operand : and.operands)
            transform.substitute(toVerify, operand, new DLAtom(new BOOL("true")));

        for (DLTerm operand : and.operands)
            result.addAll(getClauses(state, operand, toVerify));

        if (result.isEmpty()) result.add(toVerify.clone());
        return result;
    }

    public List<DLTerm> getClausesOrLeft(SEState state, DLOr or, DLTerm toVerify) {
        toVerify = toVerify.clone();
        DLTransform transform = DLTransform.getInstance();

        List<DLTerm> result = new ArrayList<>();

        for (DLTerm operand : or.operands) {
            DLTerm toVerify0 = toVerify.clone();
            transform.substitute(toVerify0, operand, new DLAtom(new BOOL("true")));
            result.addAll(getClauses(state, operand, toVerify0));
        }

        if (result.isEmpty()) result.add(toVerify.clone());
        return result;
    }

    /**
     * Attempt to prove a given clause with the given preconditions.
     */
    public boolean proveClause(DLTerm toProve, DLTerm condition) {
        if (toProve instanceof DLCmp cmp) return proveClauseCmp(cmp, condition);
        else if (toProve instanceof DLAtom atom && atom.value instanceof BOOL b) return b.value;
        else {
            new SNIPS_EXC("Cannot prove term '" + toProve.getClass().getSimpleName() + "': " + toProve.toString()).printStackTrace();
            return false;
        }
    }

    /**
     * Attempt to prove a comparison clause with the given preconditions.
     * The strategy is to solve for the variables contained in the comparison and then
     * search for a comparable term in the precondition.
     */
    public boolean proveClauseCmp(DLCmp cmp, DLTerm condition) {
        List<String> varNames = cmp.visit(x -> x instanceof DLVariable).stream().map(x -> ((DLVariable) x).name).collect(Collectors.toList());

        List<String> duplicateFree = new ArrayList<>();
        for (String name : varNames)
            if (!duplicateFree.contains(name))
                duplicateFree.add(name);
        varNames = duplicateFree;

        EQTransform transform = EQTransform.getInstance();

        for (String varName : varNames) {
            DLTerm transformed = new DLCmp(new DLVariable(varName), transform.transformToVar(cmp.left.clone(), cmp.right.clone(), varName), cmp.operator).simplify();
            if (!condition.visit(x -> x.isEqual(transformed)).isEmpty()) return true;
        }

        return false;
    }

}
