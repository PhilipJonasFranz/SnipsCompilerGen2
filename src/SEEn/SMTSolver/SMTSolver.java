package SEEn.SMTSolver;

import Exc.SNIPS_EXC;
import Imm.TYPE.PRIMITIVES.BOOL;
import SEEn.Imm.DLTerm.*;
import SEEn.SEState;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class SMTSolver {

    public static int solves = 0;

    private static final SMTSolver instance = new SMTSolver();

    public static SMTSolver getInstance() {
        return SMTSolver.instance;
    }


    /* ------------------------------------------------------------------------------------------ */
    /* ------------------------------------- SMT SOLVER HEAD ------------------------------------ */
    /* ------------------------------------------------------------------------------------------ */

    /**
     * Attempt to prove that the toVerify term is always true under the given condition.
     * Returns true if this is the case. Passed terms are not modified.
     */
    public boolean solve(SEState state, DLTerm condition, DLTerm toVerify) {
        solves++;

        condition = condition.clone().simplify();
        toVerify = toVerify.clone();

        /* Generate a set of clauses that need to be proven */
        List<DLTerm> toProve = generateClauses(state, condition, toVerify);

        /* Simplify the formulas as much as possible */
        toProve = toProve.stream().map(DLTerm::simplify).collect(Collectors.toList());

        /* Filter duplicates */
        List<DLTerm> duplicateFree = new ArrayList<>();
        for (DLTerm term : toProve) {
            if (duplicateFree.stream().noneMatch(x -> x.isEqual(term))) {
                duplicateFree.add(term);
            }
        }
        toProve = duplicateFree;

        /* Attemt to prove clauses */
        DLTerm finalCondition = condition;
        toProve = toProve.stream().filter(x -> !proveClause(x.clone(), finalCondition.clone())).collect(Collectors.toList());

        /* Proof is successful if all claues were proven */
        return toProve.isEmpty();
    }


    /* ------------------------------------------------------------------------------------------ */
    /* ------------------------------------ CLAUSE GENERATOR ------------------------------------ */
    /* ------------------------------------------------------------------------------------------ */

    /**
     * Returns a list of clauses that need to be proven under the given formula.
     * By descending down the left formula, at each DLOr, a split occurrs, which generates an additional clause.
     */
    public List<DLTerm> generateClauses(SEState state, DLTerm condition, DLTerm toVerify) {
        if (condition instanceof DLAnd and) return generateClausesAndL(state, and, toVerify);
        else if (condition instanceof DLOr or) return generateClausesOrL(state, or, toVerify);
        else return List.of(toVerify.clone());
    }

    public List<DLTerm> generateClausesAndL(SEState state, DLAnd condition, DLTerm toVerify) {
        toVerify = toVerify.clone();
        DLTransform transform = DLTransform.getInstance();

        List<DLTerm> result = new ArrayList<>();

        for (DLTerm operand : condition.operands)
            transform.substitute(toVerify, operand, new DLAtom(new BOOL("true")));

        for (DLTerm operand : condition.operands)
            result.addAll(generateClauses(state, operand, toVerify));

        if (result.isEmpty()) result.add(toVerify.clone());
        return result;
    }

    public List<DLTerm> generateClausesOrL(SEState state, DLOr condition, DLTerm toVerify) {
        toVerify = toVerify.clone();
        DLTransform transform = DLTransform.getInstance();

        List<DLTerm> result = new ArrayList<>();

        for (DLTerm operand : condition.operands) {
            DLTerm toVerify0 = toVerify.clone();
            transform.substitute(toVerify0, operand, new DLAtom(new BOOL("true")));
            result.addAll(generateClauses(state, operand, toVerify0));
        }

        if (result.isEmpty()) result.add(toVerify.clone());
        return result;
    }


    /* ------------------------------------------------------------------------------------------ */
    /* -------------------------------------- CLAUSE PROVER ------------------------------------- */
    /* ------------------------------------------------------------------------------------------ */

    /**
     * Attempt to prove a given clause with the given preconditions.
     */
    public boolean proveClause(DLTerm toProve, DLTerm condition) {
        if (toProve instanceof DLCmp cmp) return proveClauseCmp(cmp, condition);
        else if (toProve instanceof DLAtom atom && atom.value instanceof BOOL b) return b.value;
        else if (toProve instanceof DLAnd and) return proveClauseAnd(and, condition);
        else if (toProve instanceof DLOr or) return proveClauseOr(or, condition);
        else {
            new SNIPS_EXC("Cannot prove term '" + toProve.getClass().getSimpleName() + "': " + toProve.toString()).printStackTrace();
            return false;
        }
    }

    public boolean proveClauseAnd(DLAnd toProve, DLTerm condition) {
        for (DLTerm operand : toProve.operands)
            if (!this.proveClause(operand, condition))
                return false;
        return true;
    }

    public boolean proveClauseOr(DLOr toProve, DLTerm condition) {
        for (DLTerm operand : toProve.operands)
            if (this.proveClause(operand, condition))
                return true;
        return false;
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
            try {
                DLTerm transformed = new DLCmp(new DLVariable(varName), transform.transformToVar(cmp.left.clone(), cmp.right.clone(), varName), cmp.operator).simplify();
                if (!condition.visit(x -> x.weakerOrEqual(transformed)).isEmpty()) return true;
            } catch (SNIPS_EXC e) {
                // Cannot transform every term
            }
        }

        return false;
    }

}
