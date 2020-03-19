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
import Imm.AST.Expression.Arith.Add;
import Imm.AST.Expression.Arith.Div;
import Imm.AST.Expression.Arith.Mul;
import Imm.AST.Expression.Arith.Sub;
import Imm.AST.Statement.Declaration;
import Imm.AST.Statement.Return;
import Imm.AST.Statement.Statement;
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
	
	public SyntaxElement parse() {
		try {
			return parseProgram();
		} catch (PARSE_EXCEPTION e) {
			e.printStackTrace();
			return null;
		}
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
		accept(TokenType.LBRACE);
		
		List<Statement> statements = new ArrayList();
		while (current.type != TokenType.RBRACE) {
			statements.add(this.parseStatement());
		}
		accept(TokenType.RBRACE);
		
		return new Function(TYPE.fromToken(type), id, parameters, statements, type.source);
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
		else throw new PARSE_EXCEPTION(current.source, current.type, TokenType.TYPE);
	}
	
	public Declaration parseDeclaration() throws PARSE_EXCEPTION {
		Token type = accept(TokenGroup.TYPE);
		Token id = accept(TokenType.IDENTIFIER);
		
		Expression value = null;
		if (current.type == TokenType.LET) {
			accept();
			value = this.parseExpression();
		}
		
		accept(TokenType.SEMICOLON);
		return new Declaration(id, TYPE.fromToken(type), value, type.getSource());
	}
	
	public Return parseReturn() throws PARSE_EXCEPTION {
		Token ret = accept(TokenType.RETURN);
		Expression expr = this.parseExpression();
		accept(TokenType.SEMICOLON);
		return new Return(expr, ret.getSource());
	}
	
	public Expression parseExpression() throws PARSE_EXCEPTION {
			return this.parseMulDiv();
	}
		
	public Expression parseMulDiv() throws PARSE_EXCEPTION {
		Expression left = this.parseAddSub();
		while (current.type == TokenType.MUL || current.type == TokenType.DIV) {
			if (current.type == TokenType.MUL) {
				accept();
				left = new Mul(left, this.parseAddSub(), current.source);
			}
			else {
				accept();
				left = new Div(left, this.parseAddSub(), current.source);
			}
		}
		return left;
	}
	
	public Expression parseAddSub() throws PARSE_EXCEPTION {
		Expression left = this.parseAtom();
		while (current.type == TokenType.ADD || current.type == TokenType.SUB) {
			if (current.type == TokenType.ADD) {
				accept();
				left = new Add(left, this.parseAtom(), current.source);
			}
			else {
				accept();
				left = new Sub(left, this.parseAtom(), current.source);
			}
		}
		return left;
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
			return new IDRef(id, id.source);
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
	
}
