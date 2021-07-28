package SEEn;

import Exc.PARS_EXC;
import Exc.SNIPS_EXC;
import Imm.AST.Expression.Arith.Sub;
import Imm.AST.Expression.Arith.UnaryMinus;
import Imm.AST.Expression.Atom;
import Imm.AST.Expression.Boolean.Compare;
import Imm.AST.Expression.Expression;
import Imm.AST.Expression.IDRef;
import Imm.AST.Expression.InlineCall;
import Imm.AST.Function;
import Imm.AST.Lhs.SimpleLhsId;
import Imm.AST.Program;
import Imm.AST.Statement.*;
import Imm.AST.SyntaxElement;
import Imm.TYPE.PRIMITIVES.BOOL;
import SEEn.AnnotationPar.SEAnnotationParser;
import SEEn.Imm.DLTerm.*;
import SEEn.SMTSolver.DLTransform;
import SEEn.SMTSolver.SMTSolver;
import Util.Logging.LogPoint;
import Util.Logging.Message;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SEEngine {

    public Function currentFunction;

    public boolean failedVerificationInFunction = false;

    public boolean failedVerifications = false;

    public final List<Message> report = new ArrayList<>();

    public final HashMap<Function, SEState> functionMap = new HashMap<>();

    private final Program AST;

    public SEEngine(Program AST) {
        this.AST = AST;
    }

    public void interpret() throws PARS_EXC {
        this.interpretProgram(this.AST);

        if (failedVerifications)
            report.add(new Message("SNIPS_SEEN -> Verification failed!", LogPoint.Type.FAIL, true));
        else
            report.add(new Message("SNIPS_SEEN -> Verification successful, # of solves: " + SMTSolver.solves, LogPoint.Type.INFO, true));
    }

    public void interpretProgram(Program p) throws PARS_EXC {
        for (SyntaxElement s : p.programElements)
            this.interpretSyntaxElement(s);
    }

    public void interpretSyntaxElement(SyntaxElement s) throws PARS_EXC {
        this.interpretFunction((Function) s);
    }

    public void interpretFunction(Function f) throws PARS_EXC {
        SEState state = new SEState(this.AST, new DLAtom(new BOOL("true")), new DLAtom(new BOOL("true")));

        /* Register parameter variables */
        for (Declaration d : f.parameters)
            state.variables.put(d.path.build(), new DLVariable("_" + d.path.build()));

        state.programCounter = f;

        failedVerificationInFunction = false;
        this.currentFunction = f;

        /* Parse the annotations that were made for this function */
        new SEAnnotationParser(f, state).parseAnnotations();

        this.functionMap.put(f, state);

        interpretBody(state, f.body);

        if (!failedVerificationInFunction)
            report.add(new Message("SNIPS_SEEN -> Verified function '" + f.path.build() + "'", LogPoint.Type.INFO, true));
    }

    public List<SEState> interpretBody(SEState state, List<Statement> body) {
        List<SEState> result = List.of(state);

        for (Statement stmt : body) {
            List<SEState> result0 = new ArrayList<>();

            for (SEState state0 : result) {
                if (state0.programCounter == null)
                    state0.programCounter = stmt;

                result0.addAll(interpretStatement(state0, stmt));
            }

            result = result0;
        }

        return result;
    }

    public List<SEState> interpretStatement(SEState state, Statement s) {
        if (s instanceof Declaration d) return interpretDeclaration(state, d);
        else if (s instanceof Assignment a) return interpretAssignment(state, a);
        else if (s instanceof IfStatement if0) return interpretIfStmt(state, if0);
        else if (s instanceof ReturnStatement r) return interpretReturnStmt(state, r);

        throw new SNIPS_EXC("Cannot interpret statement '" + s.getClass().getSimpleName() + "'");
    }

    public List<SEState> interpretDeclaration(SEState state, Declaration d) {
        state = state.fork();
        state.programCounter = d;

        DLTerm value = null;

        if (d.value != null)
            value = this.interpretExpression(state, d.value);

        /* Register new variable */
        state.variables.put(d.path.build(), value);

        return List.of(state);
    }

    public List<SEState> interpretAssignment(SEState state, Assignment a) {
        state = state.fork();
        state.programCounter = a;

        DLTerm value = this.interpretExpression(state, a.value);

        if (a.lhsId instanceof SimpleLhsId id)
            state.variables.replace(id.origin.path.build(), value);
        else throw new SNIPS_EXC("Cannot update value for lhs type '" + a.lhsId.getClass().getSimpleName() + "'");

        return List.of(state);
    }

    public List<SEState> interpretIfStmt(SEState state, IfStatement if0) {
        List<SEState> result = new ArrayList<>();

        SEState copy;
        DLAnd ifBranchCondition = null;
        boolean hadElse = false;

        while (if0 != null) {
             copy = state.fork();
             copy.programCounter = if0;

             if (if0.condition != null) {
                DLTerm condition = interpretExpression(copy, if0.condition);
                copy.addToPathCondition(condition.clone());
                if (ifBranchCondition != null)
                    copy.addToPathCondition(ifBranchCondition.clone());

                result.addAll(interpretBody(copy, if0.body));

                if (ifBranchCondition == null) ifBranchCondition = new DLAnd(new DLNot(condition));
                else ifBranchCondition.operands.add(new DLNot(condition));
             }
             else {
                 if (ifBranchCondition != null) copy.addToPathCondition(ifBranchCondition.clone());

                 result.addAll(interpretBody(copy, if0.body));

                 hadElse = true;
             }

             if0 = if0.elseStatement;
        }

        if (!hadElse) {
            copy = state.fork();
            copy.programCounter = null;

            if (ifBranchCondition != null)
                copy.addToPathCondition(ifBranchCondition.clone());

            result.add(copy);
        }

        return result;
    }

    public List<SEState> interpretReturnStmt(SEState state, ReturnStatement r) {
        if (!(state.programCounter instanceof ReturnStatement)) {
            state = state.fork();
            state.programCounter = r;
        }

        DLTerm condition = state.pathCondition.clone();
        DLTerm toProve = state.postcondition.clone();

        DLTransform transform = DLTransform.getInstance();

        DLTerm result = null;
        if (r.value != null) result = interpretExpression(state, r.value);

        String cString = condition.simplify().toString();
        String pString = toProve.simplify().toString();

        condition = this.prepareTermInState(state, condition, result);
        toProve = this.prepareTermInState(state, toProve, result);

        /* Make sure the postcondition holds in the current state */
        boolean solved = SMTSolver.getInstance().solve(state, condition.clone(), toProve);
        if (!solved) report.addAll(generateReport(state, "Returned value does not fullfill postcondition", cString, pString));


        if (result != null) {
            /* Make sure the returned value matches the return value of the function */
            SEState fState = this.functionMap.get(this.currentFunction);
            if (!fState.returnCondition.isEmpty()) {
                DLTerm returnTerm = new DLCmp(fState.buildReturnConditionFromTerm(), result.clone(), Compare.COMPARATOR.EQUAL);

                returnTerm = this.prepareTermInState(state, returnTerm, result);

                cString = condition.simplify().toString();
                pString = returnTerm.simplify().toString();

                solved = SMTSolver.getInstance().solve(state, condition, returnTerm);
                if (!solved) report.addAll(generateReport(state, "Returned value does not match contract return value", cString, pString));
            }
        }


        /* No state returned here, marks leaf in execution tree */
        return new ArrayList();
    }

    public DLTerm interpretExpression(SEState state, Expression e) {
        if (e instanceof IDRef ref) return interpretIDRef(state, ref);
        else if (e instanceof Compare c) return interpretCompare(state, c);
        else if (e instanceof Sub s) return interpretSub(state, s);
        else if (e instanceof InlineCall i) return interpretInlineCall(state, i);
        else if (e instanceof Atom a) return interpretAtom(state, a);
        else if (e instanceof UnaryMinus m) return interpretUnaryMinus(state, m);
        throw new SNIPS_EXC("Cannot interpret expression '" + e.getClass().getSimpleName() + "'");
    }

    public DLTerm interpretCompare(SEState state, Compare c) {
        return new DLCmp(interpretExpression(state, c.operands.get(0)), interpretExpression(state, c.operands.get(1)), c.comparator);
    }

    public DLTerm interpretSub(SEState state, Sub s) {
        List<DLTerm> operands = new ArrayList<>();
        for (Expression e : s.operands)
            operands.add(interpretExpression(state, e));
        return new DLSub(operands);
    }

    public DLTerm interpretInlineCall(SEState state, InlineCall ic) {

        DLTransform transform = DLTransform.getInstance();

        SEState fState = this.functionMap.get(ic.calledFunction);

        /* Make sure we fullfill the preconditions of the called function */
        DLTerm contractCondition = fState.precondition.clone();

        String fString = contractCondition.toString();

        List<DLTerm> parameters = new ArrayList<>();

        /* Substitute the variables in the precondition with the values from the call */
        for (int i = 0; i < ic.calledFunction.parameters.size(); i++) {
            String varName = ic.calledFunction.parameters.get(i).path.build();

            DLTerm param = interpretExpression(state, ic.parameters.get(i));
            transform.substitute(contractCondition, new DLVariable(varName), param);
            parameters.add(param);
        }

        /* Prove that this function call fullfills the required clauses in the method contract */
        boolean solved = SMTSolver.getInstance().solve(state, state.pathCondition.clone(), contractCondition);
        if (!solved) report.addAll(generateReport(state, "Parameters do not match precondition", state.pathCondition.toString(), fString));

        /* Add the postcondition of the function contract to the current path condition */
        state.addToPathCondition(fState.postcondition.clone());

        DLCall call = new DLCall(ic, parameters);
        call.calledState = fState;
        return call;
    }

    public DLTerm interpretIDRef(SEState state, IDRef ref) {
        if (!state.variables.containsKey(ref.path.build())) throw new SNIPS_EXC("Unknown symbol: '" + ref.path.build() + "'");
        return state.variables.get(ref.path.build()).clone();
    }

    public DLTerm interpretUnaryMinus(SEState state, UnaryMinus m) {
        return new DLNot(this.interpretExpression(state, m.operand));
    }

    public DLTerm interpretAtom(SEState state, Atom a) {
        return new DLAtom(a.getType().clone());
    }

    public List<Message> generateReport(SEState state, String reason, String precondition, String formula) {
        /* Generate report which formulas could not be proven */
        List<Message> report = new ArrayList<>();

        if (!failedVerificationInFunction)
            report.add(new Message("Failed to verify function '" + this.currentFunction.path.build() + "'", LogPoint.Type.FAIL, true));

        report.add(new Message("SNIPS_SEEN -> Failed to verify formula", LogPoint.Type.FAIL, true));

        report.add(new Message("        -> " + formula.toString(), LogPoint.Type.FAIL, true));

        report.add(new Message("    at statement:    '" + state.programCounter.codePrintSingle() + "', " + state.programCounter.getSource().getSourceMarker(), LogPoint.Type.FAIL, true));

        report.add(new Message("    with condition:  " + precondition.toString(), LogPoint.Type.FAIL, true));

        String vars = "";
        for (Map.Entry<String, DLTerm> var : state.variables.entrySet())
            vars += var.getKey() + " = " + var.getValue().toString() + ", ";
        if (vars.endsWith(", ")) vars = vars.substring(0, vars.length() - 2);
        else vars = "-";

        report.add(new Message("    and variables:   " + vars, LogPoint.Type.FAIL, true));

        report.add(new Message("    reason:          " + reason, LogPoint.Type.FAIL, true));

        failedVerifications = true;
        failedVerificationInFunction = true;
        return report;
    }

    public DLTerm prepareTermInState(SEState state, DLTerm term, DLTerm result) {
        String s = term.    toString();

        DLTransform transform = DLTransform.getInstance();

        while (true) {
            /* Apply the values in the current state */
            term = transform.inlineVariables(state, term);

            term = transform.inlineCalls(state, this, term);

            /* Resolve the bindings to get a provable formula */
            term = transform.resolveBindings(state, term, (result == null)? null : result.clone());

            if (term.toString().equals(s)) break;
            else s = term.toString();
        }

        return term;
    }

}
