package SEEn;

import Imm.AST.SyntaxElement;
import SEEn.Imm.Formula.FAbstr;

public class SEState {

    public SyntaxElement programCounter;

    public FAbstr pathCondition;

    public FAbstr postcondition;

    public SEState(SyntaxElement programCounter, FAbstr pathCondition, FAbstr postcondition) {
        this.programCounter = programCounter;
        this.pathCondition = pathCondition;
        this.postcondition = postcondition;
    }

    public SEState clone() {
        return new SEState(this.programCounter, this.pathCondition.clone(), this.postcondition.clone());
    }

}
