package SEEn.SMTSolver;

import SEEn.Imm.DLTerm.DLTerm;
import SEEn.SEState;

public class SMTSolver {

    private static final SMTSolver instance = new SMTSolver();

    public static SMTSolver getInstance() {
        return SMTSolver.instance;
    }

    public boolean prove(SEState state, DLTerm formula, DLTerm toVerify) {
        System.out.println("Proving: " + formula.toString() + " |- " + toVerify.toString());
        System.out.println("In State: " + state.toString());
        System.out.println();
        return true;
    }

}
