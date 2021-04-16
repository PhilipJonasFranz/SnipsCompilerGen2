package Opt.Util;

import java.util.ArrayList;
import java.util.List;

import Imm.AST.Expression.IDRef;
import Imm.AST.Statement.Declaration;
import Imm.AST.Statement.ForStatement;
import Imm.AST.Statement.IfStatement;
import Imm.AST.Statement.Statement;
import Imm.AST.Statement.WhileStatement;
import Util.ASTDirective.DIRECTIVE;

/**
 * Contains methods that allow loop unrolling for 
 * 		- ForStatement
 * 		- WhileStatement
 * 
 * --- General strategy when unrolling loops ---
 * 
 * Create copy of body and increment operation and add it wrapped
 * in an if-statement guarded by the loop-condition. At the end,
 * the Statement is added so that a new unroll-operation can
 * take place. Example for forStatement:
 * 
 * 	int main() {
 *    int a = 15;
 *    int i = 5;
 *    for (i; i < 5; i = i + 1) {
 *      a = a + 1;
 *    }
 *    return a;
 *  }
 * 
 * will become
 * 
 *  int main() {
 *    int a = 15;
 *    int i = 5;
 *    if (i < 5) {
 *    	a = a + 1;
 *      i = i + 1;
 *      for (i; i < 5; i = i + 1) {
 *        a = a + 1;
 *      }
 *    }
 *    return a;
 *  }
 * 
 * The second if-statement can be simplified and potentially this will lead to
 * the if-statement being removed, the for-statement with it.
 */
public class UnrollStatementUtil {

	public static int MAX_UNROLL_DEPTH = 10;
	
	public static boolean unrollForStatement(ForStatement f, List<Statement> body) {
		
		/* Check if loop was marked to be unrolled */
		if (!f.hasDirective(DIRECTIVE.UNROLL)) return false;
		
		/* Abort if maximum unroll depth is exceeded */
		if (f.CURR_UNROLL_DEPTH < 0) return false;
		f.CURR_UNROLL_DEPTH--;
		
		/* 
		 * Make sure no declaration is created within the loop body. 
		 * Also check that no continue or break statement is in the
		 * body, since these statements cannot be copied out of the
		 * loop.
		 */
		if (Matchers.hasLoopUnrollBlockerStatements(f.body)) return false;
		
		List<Statement> result = new ArrayList();
		
		/* Extract Iterator Declaration, replace with IDRef */
		Declaration dec = null;
		if (f.iterator instanceof Declaration) {
			dec = (Declaration) f.iterator;
			result.add(dec);
			
			IDRef ref = new IDRef(dec.path.clone(), f.getSource());
			ref.origin = dec;
			f.iterator = ref;
		}
		
		
		List<Statement> bodyCopy = Makros.copyBody(f.body);
		bodyCopy.add(f.increment.clone());
		bodyCopy.add(f);
		IfStatement if0 = new IfStatement(f.condition.clone(), bodyCopy, f.getSource());
		result.add(if0);
		
		/* Replace the For-Statement in the body with the result */
		int index = body.indexOf(f);
		body.remove(index);
		body.addAll(index, result);
		
		return true;
	}
	
	public static boolean unrollWhileStatement(WhileStatement w, List<Statement> body) {
		
		/* Check if loop was marked to be unrolled */
		if (!w.hasDirective(DIRECTIVE.UNROLL)) return false;
		
		/* Abort if maximum unroll depth is exceeded */
		if (w.CURR_UNROLL_DEPTH < 0) return false;
		w.CURR_UNROLL_DEPTH--;
		
		/* 
		 * Make sure no declaration is created within the loop body. 
		 * Also check that no continue or break statement is in the
		 * body, since these statements cannot be copied out of the
		 * loop.
		 */
		if (Matchers.hasLoopUnrollBlockerStatements(w.body)) return false;
		
		List<Statement> result = new ArrayList();
		
		/*
		 * Create copy of body and increment operation and add it wrapped
		 * in an if-statement guarded by the loop-condition.
		 */
		List<Statement> bodyCopy = Makros.copyBody(w.body);
		bodyCopy.add(w);
		IfStatement if0 = new IfStatement(w.condition.clone(), bodyCopy, w.getSource());
		result.add(if0);
		
		/* Replace the While-Statement in the body with the result */
		int index = body.indexOf(w);
		body.remove(index);
		body.addAll(index, result);
		
		return true;
	}
	
}
