package SEEn.SMTSolver;

import Exc.SNIPS_EXC;
import SEEn.Imm.DLTerm.*;
import Tools.DLTermVisitor;

public class EQTransform {

    private static EQTransform instance = new EQTransform();

    private EQTransform() {

    }

    public static EQTransform getInstance() {
        return instance;
    }

    public DLTerm transformToVar(DLTerm left, DLTerm right, String varName) {
        left = left.clone();
        right = right.clone();

        if (left.visit(x -> x instanceof DLVariable var && var.name.equals(varName)).isEmpty()) {
            DLTerm tmp = left;
            left = right;
            right = tmp;
        }

        return this.transformToVarImpl(left, right, varName);
    }

    private DLTerm transformToVarImpl(DLTerm left, DLTerm right, String varName) {
        DLTermVisitor<DLTerm> containsVar = s -> s instanceof DLVariable var && var.name.equals(varName);

        if (left instanceof DLVariable var && var.name.equals(varName)) return right;
        else {
            if (left instanceof DLNFold lfold) {
                DLNFold fold = null;

                if (left instanceof DLAdd)      { fold = new DLSub(right); }
                else if (left instanceof DLSub) { fold = new DLAdd(right); }
                else if (left instanceof DLMul) { fold = new DLDiv(right); }
                else if (left instanceof DLDiv) { fold = new DLMul(right); }

                right = fold;

                DLTerm occurrence = null;

                for (DLTerm operand : lfold.operands) {
                    if (containsVar.visit(operand)) {
                        /* Current operand is the searched variable */
                        continue;
                    }
                    else if (operand.visit(containsVar).isEmpty()) {
                        /* Part of the equation does not contain our variable, we can just add it to the other side */
                        fold.operands.add(operand);
                    }
                    else {
                        if (occurrence != null)
                            throw new SNIPS_EXC("Found multiple occurrences of '" + varName + "' in equation '" + left.toString() + " = " + right.toString() + "'");

                        /* We have to keep transforming this subtree till we reach the searched variable */
                        occurrence = operand;
                    }
                }

                if (occurrence != null)
                    right = transformToVar(occurrence, right, varName);
            }
            else throw new SNIPS_EXC("Cannot transform to variable: " + left.getClass().getSimpleName());

            return right;
        }
    }

}
