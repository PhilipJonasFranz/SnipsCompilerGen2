package Par;

import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;

import Exc.PARSE_EXCEPTION;
import Exc.SNIPS_EXCEPTION;
import Imm.AST.Function;
import Imm.AST.Program;
import Imm.AST.SyntaxElement;
import Imm.AST.Expression.Atom;
import Imm.AST.Expression.Expression;
import Imm.AST.Expression.IDRef;
import Imm.AST.Expression.InlineCall;
import Imm.AST.Expression.Arith.Add;
import Imm.AST.Expression.Arith.Div;
import Imm.AST.Expression.Arith.Lsl;
import Imm.AST.Expression.Arith.Lsr;
import Imm.AST.Expression.Arith.Mul;
import Imm.AST.Expression.Arith.Sub;
import Imm.AST.Expression.Boolean.And;
import Imm.AST.Expression.Boolean.Compare;
import Imm.AST.Expression.Boolean.Compare.COMPARATOR;
import Imm.AST.Expression.Boolean.Not;
import Imm.AST.Expression.Boolean.Or;
import Imm.AST.Statement.Assignment;
import Imm.AST.Statement.Declaration;
import Imm.AST.Statement.IfStatement;
import Imm.AST.Statement.Return;
import Imm.AST.Statement.Statement;
import Imm.AST.Statement.WhileStatement;
import Imm.TYPE.TYPE;
import Imm.TYPE.COMPOSIT.STRUCT;
import Imm.TYPE.PRIMITIVES.INT;
import Par.Token.TokenType;
import Par.Token.TokenType.TokenGroup;
import Util.Source;

public class Parser {

	protected Deque<Token> tokenStream;
	
	Token current;
	
	HashMap<String, STRUCT> structs = new HashMap();
	
	public Parser(Deque tokens) throws SNIPS_EXCEPTION {
		if (tokens == null) throw new SNIPS_EXCEPTION("SNIPS_PARSE -> Tokens are null!");
		tokenStream = tokens;
		current = tokenStream.pop();
	}
	
	/**
	 * Accept a token based on its type.
	 * @param tokenType The type the token should match.
	 * @return The accepted Token.
	 * @throws PARSE_EXCEPTION Thrown then the token does not have the given type.
	 */
	protected Token accept(TokenType tokenType) throws PARSE_EXCEPTION {
		if (current.type() == tokenType)return accept();
		else throw new PARSE_EXCEPTION(current.source, current.type(), tokenType);
	}
	
	/**
	 * Accept a token based on its token group.
	 * @param group The group the token should match.
	 * @return The accepted Token.
	 * @throws PARSE_EXCEPTION Thrown when the token does not have the given token group.
	 */
	protected Token accept(TokenGroup group) throws PARSE_EXCEPTION {
		if (current.type().group == group)return accept();
		else throw new PARSE_EXCEPTION(current.source, current.type());
	}
	
	/**
	 * Accept a token without any checks.
	 * @return The accepted token.
	 */
	protected Token accept() {
		//System.out.println(current.spelling + " " + current.type.toString());
		Token old = current;
		current = tokenStream.pop();
		return old;
	}
	
	public SyntaxElement parse() throws PARSE_EXCEPTION {
		return parseProgram();
	}
	
	public Program parseProgram() throws PARSE_EXCEPTION {
		Source source = this.current.source;
		List<SyntaxElement> elements = new ArrayList();
		while (this.current.type != TokenType.EOF) {
			if (this.current.type().group == TokenGroup.TYPE) {
				elements.add(this.parseFunction());
			}
		}
		
		return new Program(elements, source);
	}
	
	public Function parseFunction() throws PARSE_EXCEPTION {
		Token type = accept();
		Token id = accept(TokenType.IDENTIFIER);
		accept(TokenType.LPAREN);
		
		List<Declaration> parameters = new ArrayList();
		while (current.type != TokenType.RPAREN) {
			parameters.add(this.parseParameterDeclaration());
			if (current.type == TokenType.COMMA) {
				accept();
			}
			else {
				break;
			}
		}
		
		accept(TokenType.RPAREN);
		
		List<Statement> body = this.parseCompoundStatement();
		
		return new Function(TYPE.fromToken(type), id, parameters, body, type.source);
	}
	
	public Declaration parseParameterDeclaration() throws PARSE_EXCEPTION {
		Token type = accept(TokenGroup.TYPE);
		Token id = accept(TokenType.IDENTIFIER);
		return new Declaration(id, TYPE.fromToken(type), type.getSource());
	}
	
	public Statement parseStatement() throws PARSE_EXCEPTION {
		if (current.type.group == TokenGroup.TYPE) {
			return this.parseDeclaration();
		}
		else if (current.type == TokenType.RETURN) {
			return this.parseReturn();
		}
		else if (current.type == TokenType.WHILE) {
			return this.parseWhile();
		}
		else if (current.type == TokenType.IDENTIFIER) {
			return this.parseAssignment();
		}
		else if (current.type == TokenType.IF) {
			return this.parseIf();
		}
		else throw new PARSE_EXCEPTION(current.source, current.type, TokenType.TYPE, TokenType.WHILE, TokenType.RETURN, TokenType.IDENTIFIER, TokenType.IF);
	}
	
	public Statement parseIf() throws PARSE_EXCEPTION {
		Source source = current.getSource();
		accept(TokenType.IF);
		accept(TokenType.LPAREN);
		Expression condition = this.parseExpression();
		accept(TokenType.RPAREN);
		
		List<Statement> body = this.parseCompoundStatement();
		
		IfStatement if0 = new IfStatement(condition, body, source);
		
		if (current.type == TokenType.ELSE) {
			Source elseSource = accept().getSource();
			if (current.type == TokenType.IF) {
				if0.elseStatement = (IfStatement) this.parseIf();
			}
			else {
				List<Statement> elseBody = this.parseCompoundStatement();
				if0.elseStatement = new IfStatement(elseBody, elseSource);
			}
		}
		
		return if0;
	}
	
	public WhileStatement parseWhile() throws PARSE_EXCEPTION {
		Source source = accept(TokenType.WHILE).getSource();
		accept(TokenType.LPAREN);
		Expression condition = this.parseExpression();
		accept(TokenType.RPAREN);
		List<Statement> body = this.parseCompoundStatement();
		return new WhileStatement(condition, body, source);
	}
	
	public Assignment parseAssignment() throws PARSE_EXCEPTION {
		Token id = accept(TokenType.IDENTIFIER);
		accept(TokenType.LET);
		Expression value = this.parseExpression();
		accept(TokenType.SEMICOLON);
		return new Assignment(id, value, id.source);
	}
	
	public Declaration parseDeclaration() throws PARSE_EXCEPTION {
		Source source = current.getSource();
		TYPE type = this.parseType();
		Token id = accept(TokenType.IDENTIFIER);
		
		Expression value = null;
		if (current.type == TokenType.LET) {
			accept();
			value = this.parseExpression();
		}
		
		accept(TokenType.SEMICOLON);
		return new Declaration(id, type, value, source);
	}
	
	public Return parseReturn() throws PARSE_EXCEPTION {
		Token ret = accept(TokenType.RETURN);
		Expression expr = this.parseExpression();
		accept(TokenType.SEMICOLON);
		return new Return(expr, ret.getSource());
	}
	
	public Expression parseExpression() throws PARSE_EXCEPTION {
			return this.parseOr();
	}
	
	public Expression parseOr() throws PARSE_EXCEPTION {
		Expression left = this.parseAnd();
		while (current.type == TokenType.OR) {
			accept();
			left = new Or(left, this.parseAnd(), current.source);
		}
		return left;
	}
	
	public Expression parseAnd() throws PARSE_EXCEPTION {
		Expression left = this.parseCompare();
		while (current.type == TokenType.AND) {
			accept();
			left = new And(left, this.parseCompare(), current.source);
		}
		return left;
	}
	
	public Expression parseCompare() throws PARSE_EXCEPTION {
		Expression left = this.parseShift();
		if (current.type.group == TokenGroup.COMPARE) {
			Source source = current.getSource();
			if (current.type == TokenType.CMPEQ) {
				accept();
				return new Compare(left, this.parseShift(), COMPARATOR.EQUAL, source);
			}
			else if (current.type == TokenType.CMPNE) {
				accept();
				return new Compare(left, this.parseShift(), COMPARATOR.NOT_EQUAL, source);
			}
			else if (current.type == TokenType.CMPGE) {
				accept();
				return new Compare(left, this.parseShift(), COMPARATOR.GREATER_SAME, source);
			}
			else if (current.type == TokenType.CMPGT) {
				accept();
				return new Compare(left, this.parseShift(), COMPARATOR.GREATER_THAN, source);
			}
			else if (current.type == TokenType.CMPLE) {
				accept();
				return new Compare(left, this.parseShift(), COMPARATOR.LESS_SAME, source);
			}
			else if (current.type == TokenType.CMPLT) {
				accept();
				return new Compare(left, this.parseShift(), COMPARATOR.LESS_THAN, source);
			}
		}
		
		return left;
	}
	
	public Expression parseShift() throws PARSE_EXCEPTION {
		Expression left = this.parseAddSub();
		while (current.type == TokenType.LSL || current.type == TokenType.LSR) {
			if (current.type == TokenType.LSL) {
				accept();
				left = new Lsl(left, this.parseAddSub(), current.source);
			}
			else {
				accept();
				left = new Lsr(left, this.parseAddSub(), current.source);
			}
		}
		return left;
	}
	
	public Expression parseAddSub() throws PARSE_EXCEPTION {
		Expression left = this.parseMulDiv();
		while (current.type == TokenType.ADD || current.type == TokenType.SUB) {
			if (current.type == TokenType.ADD) {
				accept();
				left = new Add(left, this.parseMulDiv(), current.source);
			}
			else {
				accept();
				left = new Sub(left, this.parseMulDiv(), current.source);
			}
		}
		return left;
	}
		
	public Expression parseMulDiv() throws PARSE_EXCEPTION {
		Expression left = this.parseNot();
		while (current.type == TokenType.MUL || current.type == TokenType.DIV) {
			if (current.type == TokenType.MUL) {
				accept();
				left = new Mul(left, this.parseNot(), current.source);
			}
			else {
				accept();
				left = new Div(left, this.parseNot(), current.source);
			}
		}
		return left;
	}
	
	public Expression parseNot() throws PARSE_EXCEPTION {
		Expression not = null;
		while (current.type == TokenType.NOT) {
			accept();
			not = new Not(this.parseNot(), current.source);
		}
		
		if (not == null) not = this.parseAtom();
		return not;
	}
	
	public Expression parseAtom() throws PARSE_EXCEPTION {
		if (current.type == TokenType.LPAREN) {
			accept();
			Expression expression = this.parseExpression();
			accept(TokenType.RPAREN);
			return expression;
		}
		else if (current.type == TokenType.IDENTIFIER) {
			Token id = accept();
			
			if (current.type == TokenType.LPAREN) {
				/* Inline Call */
				accept();
				
				List<Expression> parameters = new ArrayList();
				while (current.type != TokenType.RPAREN) {
					parameters.add(this.parseExpression());
					if (current.type != TokenType.COMMA) break;
					accept(TokenType.COMMA);
				}
				accept(TokenType.RPAREN);
				
				return new InlineCall(id, parameters, id.getSource());
			}
			else {
				/* Identifier Reference */
				return new IDRef(id, id.source);
			}
		}
		else if (current.type == TokenType.INTLIT) {
			Token token = accept();
			return new Atom(new INT(token.spelling), token, token.source);
		}
		else throw new PARSE_EXCEPTION(current.source, current.type, TokenType.LPAREN, TokenType.IDENTIFIER, TokenType.INTLIT);
	}

	public TYPE parseType() {
		TYPE type = null;
		while (current.type.group == TokenGroup.TYPE) {
			if (current.type == TokenType.INT) {
				accept();
				type = new INT();
			}
		}
		
		return type;
	}
	
	protected List<Statement> parseCompoundStatement() throws PARSE_EXCEPTION {
		List<Statement> body = new ArrayList();
		
		/* Compound Statement with braces */
		if (current.type == TokenType.LBRACE) {
			accept();
			while (current.type != TokenType.RBRACE) {
				body.add(this.parseStatement());
			}
			accept(TokenType.RBRACE);
		}
		/* Without braces, one statement only */
		else {
			body.add(this.parseStatement());
		}
		
		return body;
	}
	
}
