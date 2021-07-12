package SEEn;

import Exc.PARS_EXC;
import Exc.SNIPS_EXC;
import Imm.AST.Expression.Boolean.Compare;
import Imm.AST.Expression.Expression;
import Imm.AST.Expression.IDRef;
import Imm.AST.Function;
import Imm.AST.Lhs.SimpleLhsId;
import Imm.AST.Program;
import Imm.AST.Statement.*;
import Imm.AST.SyntaxElement;
import Imm.TYPE.PRIMITIVES.BOOL;
import SEEn.AnnotationPar.SEAnnotationParser;
import SEEn.Imm.DLTerm.*;

import java.util.ArrayList;
import java.util.List;

public class SEEngine {

    private Program AST;

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
        if (s instanceof Function f) {
            this.interpretFunction(f);
        }
    }

    public void interpretFunction(Function f) throws PARS_EXC {
        SEState state = new SEState(this.AST, new DLAtom(new BOOL("true")), new DLAtom(new BOOL("true")));

        /* Register parameter variables */
        for (Declaration d : f.parameters)
            state.variables.put(d.path.build(), new DLVariable(d.path.build()));

        state.programCounter = f;

        /* Parse the annotations that were made for this function */
        new SEAnnotationParser(f, state).parseAnnotations();

        state.print();

        interpretBody(state, f.body);
    }

    public List<SEState> interpretBody(SEState state, List<Statement> body) {
        List<SEState> result = List.of(state);

        for (Statement stmt : body) {
            List<SEState> result0 = new ArrayList<>();

            for (SEState state0 : result)
                result0.addAll(interpretStatement(state, stmt));

            result = result0;
        }

        return result;
    }

    public List<SEState> interpretStatement(SEState state, Statement s) {
        List<SEState> result = new ArrayList<>();

        if (s instanceof Declaration d) result = interpretDeclaration(state, d);
        else if (s instanceof Assignment a) result = interpretAssignment(state, a);
        else if (s instanceof IfStatement if0) result = interpretIfStmt(state, if0);
        else if (s instanceof ReturnStatement r) result = interpretReturnStmt(state, r);

        return result;
    }

    public List<SEState> interpretDeclaration(SEState state, Declaration d) {
        state = state.fork();
        state.programCounter = d;

        DLTerm value = null;

        if (d.value != null)
            value = this.interpretExpression(state, d.value);

        /* Register new variable */
        state.variables.put(d.path.build(), value);

        state.print();

        return List.of(state);
    }

    public List<SEState> interpretAssignment(SEState state, Assignment a) {
        state = state.fork();
        state.programCounter = a;

        DLTerm value = this.interpretExpression(state, a.value);

        if (a.lhsId instanceof SimpleLhsId id)
            state.variables.replace(id.origin.path.build(), value);
        else throw new SNIPS_EXC("Cannot update value for lhs type '" + a.lhsId.getClass().getSimpleName() + "'");

        state.print();

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

                 copy.print();

                interpretBody(copy, if0.body);

                if (ifBranchCondition == null) ifBranchCondition = new DLAnd(new DLNot(condition));
                else ifBranchCondition.operands.add(new DLNot(condition));
             }
             else {
                 if (ifBranchCondition != null) copy.addToPathCondition(ifBranchCondition.clone());

                 copy.print();

                 interpretBody(copy, if0.body);

                 hadElse = true;
             }

             result.add(copy);
             if0 = if0.elseStatement;
        }

        if (!hadElse) {
            copy = state.fork();

            if (ifBranchCondition != null) copy.addToPathCondition(ifBranchCondition.clone());

            copy.print();

            interpretBody(copy, new ArrayList<>());
        }

        return result;
    }

    public List<SEState> interpretReturnStmt(SEState state, ReturnStatement r) {
        state = state.fork();
        state.programCounter = r;
        state.print();

        // TODO: Check postconditions with return value here
        return new ArrayList();
    }

    public DLTerm interpretExpression(SEState state, Expression e) {
        if (e instanceof IDRef ref) return interpretIDRef(state, ref);
        else if (e instanceof Compare c) return interpretCompare(state, c);
        throw new SNIPS_EXC("Cannot interpret expression '" + e.getClass().getSimpleName() + "'");
    }

    public DLTerm interpretCompare(SEState state, Compare c) {
        return new DLCmp(interpretExpression(state, c.operands.get(0)), interpretExpression(state, c.operands.get(1)), c.comparator);
    }

    public DLTerm interpretIDRef(SEState state, IDRef ref) {
        if (!state.variables.containsKey(ref.path.build())) throw new SNIPS_EXC("Unknown symbol: '" + ref.path.build() + "'");
        return state.variables.get(ref.path.build()).clone();
    }

}
