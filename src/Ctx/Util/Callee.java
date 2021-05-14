package Ctx.Util;

import java.util.List;

import Imm.AST.Function;
import Imm.AST.SyntaxElement;
import Imm.AST.Expression.Expression;
import Imm.AST.Statement.Declaration;
import Imm.TYPE.TYPE;
import Util.NamespacePath;

/**
 * Interface to unify FunctionCall and InlineCall behind one interface
 * and be able to check them together in a single method.
 */
public interface Callee {
	
	boolean isNestedCall();
	
	boolean isNestedDeref();
	
	boolean hasAutoProviso();
	
	TYPE getType();
	
	List<Expression> getParams();
	
	NamespacePath getPath();
	
	SyntaxElement getCallee();
	
	Expression getBaseRef();
	
	List<TYPE> getProviso();
	
	void setNestedCall(boolean b);
	
	void setAutoProviso(boolean b);
	
	void setProviso(List<TYPE> proviso);
	
	void setAnonTarget(Declaration d);
	
	void setType(TYPE t);
	
	void setPath(NamespacePath path);
	
	void setCalledFunction(Function f);
	
	void setWatchpoint(SyntaxElement w);
	
}
