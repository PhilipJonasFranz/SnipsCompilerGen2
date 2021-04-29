package Ctx.Util;

import java.util.List;

import Imm.AST.Function;
import Imm.AST.SyntaxElement;
import Imm.AST.Expression.Expression;
import Imm.AST.Statement.Declaration;
import Imm.TYPE.TYPE;
import Util.NamespacePath;

public class CheckUtil {

	/**
	 * Interface to unify FunctionCall and InlineCall behind one interface
	 * and be able to check them together in a single method.
	 */
	public interface Callee {
		
		public boolean isNestedCall();
		
		public boolean isNestedDeref();
		
		public boolean hasAutoProviso();
		
		public TYPE getType();
		
		public List<Expression> getParams();
		
		public NamespacePath getPath();
		
		public SyntaxElement getCallee();
		
		public Expression getBaseRef();
		
		public List<TYPE> getProviso();
		
		public void setNestedCall(boolean b);
		
		public void setAutoProviso(boolean b);
		
		public void setProviso(List<TYPE> proviso);
		
		public void setAnonTarget(Declaration d);
		
		public void setType(TYPE t);
		
		public void setPath(NamespacePath path);
		
		public void setCalledFunction(Function f);
		
		public void setWatchpoint(SyntaxElement w);
		
	}
	
}
