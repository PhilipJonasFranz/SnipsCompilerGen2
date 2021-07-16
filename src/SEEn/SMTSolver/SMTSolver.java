package SEEn.SMTSolver;

import Imm.TYPE.PRIMITIVES.BOOL;
import SEEn.Imm.DLTerm.DLAnd;
import SEEn.Imm.DLTerm.DLAtom;
import SEEn.Imm.DLTerm.DLOr;
import SEEn.Imm.DLTerm.DLTerm;
import SEEn.SEState;
import Util.Logging.LogPoint;
import Util.Logging.Message;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class SMTSolver {

    private static final SMTSolver instance = new SMTSolver();

    public static SMTSolver getInstance() {
        return SMTSolver.instance;
    }

    public List<Message> solve(SEState state, DLTerm formula, DLTerm toVerify) {
        formula = formula.clone().simplify();
        toVerify = toVerify.clone();

        List<DLTerm> toProve = subs(state, formula, toVerify);

        /* Simplify the formulas as much as possible */
        toProve = toProve.stream().map(DLTerm::simplify).collect(Collectors.toList());

        List<DLTerm> duplicateFree = new ArrayList<>();
        for (DLTerm term : toProve) {
            if (duplicateFree.stream().noneMatch(x -> x.isEqual(term))) {
                duplicateFree.add(term);
            }
        }
        toProve = duplicateFree;

        /* Generate report which formulas could not be proven */
        List<Message> report = new ArrayList<>();

        toProve.forEach(x -> {
            if (!(x instanceof DLAtom atom && atom.value instanceof BOOL b && b.value)) {
                report.add(new Message("    -> " + x.toString(), LogPoint.Type.FAIL, true));
            }
        });

        if (!report.isEmpty()) {
            report.add(0, new Message("In the state:", LogPoint.Type.FAIL, true));

            report.add(1, new Message("    Condition " + formula.toString(), LogPoint.Type.FAIL, true));

            String vars = "";
            for (Map.Entry<String, DLTerm> var : state.variables.entrySet())
                vars += var.getKey() + " = " + var.getValue().toString() + ", ";
            if (vars.endsWith(", ")) vars = vars.substring(0, vars.length() - 2);
            else vars = "-";

            report.add(2, new Message("    Variables: " + vars, LogPoint.Type.FAIL, true));
            report.add(3, new Message("The formula(s) could not be proven:", LogPoint.Type.FAIL, true));
        }

        return report;
    }

    public List<DLTerm> subs(SEState state, DLTerm formula, DLTerm toVerify) {
        if (formula instanceof DLAnd and) return subsAnd(state, and, toVerify);
        else if (formula instanceof DLOr or) return subsOr(state, or, toVerify);
        else return List.of(toVerify.clone());
    }

    public List<DLTerm> subsAnd(SEState state, DLAnd and, DLTerm toVerify) {
        toVerify = toVerify.clone();
        DLTransform transform = DLTransform.getInstance();

        List<DLTerm> result = new ArrayList<>();

        for (DLTerm operand : and.operands)
            transform.substitute(toVerify, operand, new DLAtom(new BOOL("true")));

        for (DLTerm operand : and.operands)
            result.addAll(subs(state, operand, toVerify));

        if (result.isEmpty()) result.add(toVerify.clone());
        return result;
    }

    public List<DLTerm> subsOr(SEState state, DLOr or, DLTerm toVerify) {
        toVerify = toVerify.clone();
        DLTransform transform = DLTransform.getInstance();

        List<DLTerm> result = new ArrayList<>();

        for (DLTerm operand : or.operands) {
            DLTerm toVerify0 = toVerify.clone();
            transform.substitute(toVerify0, operand, new DLAtom(new BOOL("true")));
            result.addAll(subs(state, operand, toVerify0));
        }

        if (result.isEmpty()) result.add(toVerify.clone());
        return result;
    }

}
