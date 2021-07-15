package Tools;

import SEEn.Imm.DLTerm.DLTerm;

public interface DLTermVisitor<T extends DLTerm> {

	boolean visit(DLTerm s);

}
