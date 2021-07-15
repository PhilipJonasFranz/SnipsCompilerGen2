package SEEn;

import Exc.PARS_EXC;
import Exc.SNIPS_EXC;
import Imm.AST.Expression.Arith.Sub;
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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class SEEngine {

    private final HashMap<Function, SEState> functionMap = new HashMap<>();

    private final Program AST;

    public SEEngine(Program AST) {
        this.AST = AST;
    }

    public void interpret() throws PARS_EXC {
        this.interpretProgram(this.AST);
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
            state.variables.put(d.path.build(), new DLVariable(d.path.build()));

        state.programCounter = f;

        /* Parse the annotations that were made for this function */
        new SEAnnotationParser(f, state).parseAnnotations();

        this.functionMap.put(f, state);

        interpretBody(state, f.body);

        state.printRec();
        System.out.println();
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

        DLTerm formula = state.getPathCondition().clone();
        DLTerm toProve = state.getPostCondition().clone();

        DLTransform transform = DLTransform.getInstance();

        DLTerm result = null;
        if (r.value != null) result = interpretExpression(state, r.value);

        /* Resolve the bindings to get a provable formula */
        formula = transform.resolveBindings(state, formula, result);
        toProve = transform.resolveBindings(state, toProve, result);

        /* Apply the values in the current state */
        formula = transform.inlineVariables(state, formula);
        toProve = transform.inlineVariables(state, toProve);

        /* Make sure the postcondition holds in the current state */
        SMTSolver.getInstance().prove(state, formula, toProve);

        /* No state returned here, marks leaf in execution tree */
        return new ArrayList();
    }

    public DLTerm interpretExpression(SEState state, Expression e) {
        if (e instanceof IDRef ref) return interpretIDRef(state, ref);
        else if (e instanceof Compare c) return interpretCompare(state, c);
        else if (e instanceof Sub s) return interpretSub(state, s);
        else if (e instanceof InlineCall i) return interpretInlineCall(state, i);
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

    public DLTerm interpretInlineCall(SEState state, InlineCall i) {

        SEState fState = this.functionMap.get(i.calledFunction);

        /* Make sure we fullfill the preconditions of the called function */
        DLTerm contractCondition = fState.getPrecondition();
        SMTSolver.getInstance().prove(state, state.getPathCondition(), contractCondition);

        /* Add the postcondition of the function contract to the current path condition */
        state.addToPathCondition(fState.getPostCondition());

        return new DLCall(i);
    }

    public DLTerm interpretIDRef(SEState state, IDRef ref) {
        if (!state.variables.containsKey(ref.path.build())) throw new SNIPS_EXC("Unknown symbol: '" + ref.path.build() + "'");
        return state.variables.get(ref.path.build()).clone();
    }

}
