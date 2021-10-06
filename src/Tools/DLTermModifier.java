package Tools;

import SEEn.Imm.DLTerm.DLTerm;

public interface DLTermModifier<T extends DLTerm> {

	DLTerm replace(DLTerm s);
	
}
