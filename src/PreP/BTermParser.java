package PreP;

import Exc.PARS_EXC;
import Exc.SNIPS_EXC;
import Imm.AST.Expression.Atom;
import Imm.AST.Expression.Boolean.And;
import Imm.AST.Expression.Boolean.Or;
import Imm.AST.Expression.Expression;
import Imm.TYPE.PRIMITIVES.BOOL;
import Par.Scanner;
import Par.Token;
import Par.Token.TokenType;
import PreP.PreProcessor.LineObject;
import Util.Logging.LogPoint.Type;
import Util.Logging.Message;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Parses a micro-expression and evaluates it to a boolean
 * result. Used in conditions of directives.
 */
public class BTermParser {

				/* ---< FIELDS >--- */
		private List<Token> dequeue;
		
		private Token current;
		
		private LineObject expression;
	
		
				/* ---< CONSTRUCTORS >--- */
		public BTermParser(LineObject expression) {
			this.expression = expression;
			
			/* Remove #ifdef from start of line */
			List<String> sp = Arrays.stream(this.expression.line.split(" ")).collect(Collectors.toList());
			
			if (sp.get(0).trim().startsWith("#")) sp.remove(0);
			if (sp.get(0).trim().equals("ifdef")) sp.remove(0);

			this.expression.line = String.join(" ", sp).trim();
		}
		
	
				/* ---< METHODS >--- */
		public boolean evaluate() {
			
			this.dequeue = new Scanner(Collections.singletonList(this.expression), null).scan();
			this.current = this.dequeue.get(0);
			
			Expression expr;
			
			try {
				expr = this.parseExpression();
				
				if (!this.dequeue.isEmpty() && !(this.dequeue.get(0).type() == TokenType.EOF)) 
					throw new PARS_EXC(current.source(), current.type(), TokenType.LPAREN, TokenType.BOOLLIT, TokenType.AND, TokenType.OR);
			} catch (PARS_EXC e) {
				new Message("Failed to parse condition '" + this.expression.line.trim() + "', line " + this.expression.lineNumber + " (" + this.expression.fileName + ")", Type.FAIL);
				throw new SNIPS_EXC(e.getMessage());
			}
			
			return this.evaluateExpression(expr);
		}
		
				/* ---< EVALUATION >--- */
		private boolean evaluateExpression(Expression expr) {
			if (expr instanceof Or or) return this.evaluateOr(or);
			else if (expr instanceof And and) return this.evaluateAnd(and);
			else return this.evaluateAtom((Atom) expr);
		}
		
		private boolean evaluateOr(Or or) {
			boolean val = false;
			for (Expression op : or.operands)
				val |= this.evaluateExpression(op);
			return val;
		}
		
		private boolean evaluateAnd(And and) {
			boolean val = true;
			for (Expression op : and.operands)
				val &= this.evaluateExpression(op);
			return val;
		}
		
		private boolean evaluateAtom(Atom atom) {
			BOOL bool = (BOOL) atom.getType();
			return bool.value;
		}
		
				/* ---< PARSER >--- */
		private void accept(TokenType tokenType) throws PARS_EXC {
			if (current.type() == tokenType) {
				accept();
			} else throw new PARS_EXC(current.source(), current.type(), tokenType);
		}
		
		private Token accept() {
			Token old = current;
			
			if (!dequeue.isEmpty()) {
				dequeue.remove(0);
				current = dequeue.get(0);
			}
			
			return old;
		}
		
		private Expression parseExpression() throws PARS_EXC {
			return this.parseOr();
		}
		
		private Expression parseOr() throws PARS_EXC {
			Expression left = this.parseAnd();
			
			while (current.type() == TokenType.OR) {
				accept();
				left = new Or(left, this.parseExpression(), current.source());
			}
			
			return left;
		}
		
		private Expression parseAnd() throws PARS_EXC {
			Expression left = this.parseAtom();
			
			while (current.type() == TokenType.AND) {
				accept();
				left = new And(left, this.parseExpression(), current.source());
			}
			
			return left;
		}
		
		private Expression parseAtom() throws PARS_EXC {
			if (current.type() == TokenType.LPAREN) {
				accept();
				Expression e = this.parseExpression();
				accept(TokenType.RPAREN);
				return e;
			}
			else if (current.type() == TokenType.BOOLLIT) {
				Token token = accept();
				return new Atom(new BOOL(token.spelling()), token.source());
			}
			else throw new PARS_EXC(current.source(), current.type(), TokenType.LPAREN, TokenType.BOOLLIT);
		}

}
