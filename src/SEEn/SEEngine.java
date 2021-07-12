package SEEn;

import Exc.PARS_EXC;
import Imm.AST.Function;
import Imm.AST.Program;
import Imm.AST.Statement.Statement;
import Imm.AST.SyntaxElement;
import SEEn.AnnotationPar.SEAnnotationParser;
import SEEn.Imm.Formula.FSingleton;

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
        SEState state = new SEState(this.AST, new FSingleton(true), new FSingleton(true));

        /* Parse the annotations that were made for this function */
        new SEAnnotationParser(f, state).parseAnnotations();

        for (Statement stmt : f.body)
            interpretStatement(state, stmt);
    }

    public void interpretStatement(SEState state, Statement s) {

    }

}
