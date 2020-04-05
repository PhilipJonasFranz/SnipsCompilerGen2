package Par;

import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Stack;

import Exc.PARSE_EXCEPTION;
import Exc.SNIPS_EXCEPTION;
import Imm.AST.Function;
import Imm.AST.Program;
import Imm.AST.SyntaxElement;
import Imm.AST.Expression.Atom;
import Imm.AST.Expression.ElementSelect;
import Imm.AST.Expression.Expression;
import Imm.AST.Expression.IDRef;
import Imm.AST.Expression.InlineCall;
import Imm.AST.Expression.StructureInit;
import Imm.AST.Expression.Arith.Add;
import Imm.AST.Expression.Arith.Lsl;
import Imm.AST.Expression.Arith.Lsr;
import Imm.AST.Expression.Arith.Mul;
import Imm.AST.Expression.Arith.Sub;
import Imm.AST.Expression.Arith.UnaryMinus;
import Imm.AST.Expression.Boolean.And;
import Imm.AST.Expression.Boolean.Compare;
import Imm.AST.Expression.Boolean.Compare.COMPARATOR;
import Imm.AST.Expression.Boolean.Not;
import Imm.AST.Expression.Boolean.Or;
import Imm.AST.Expression.Boolean.Ternary;
import Imm.AST.Statement.Assignment;
import Imm.AST.Statement.BreakStatement;
import Imm.AST.Statement.CaseStatement;
import Imm.AST.Statement.ContinueStatement;
import Imm.AST.Statement.Declaration;
import Imm.AST.Statement.DefaultStatement;
import Imm.AST.Statement.DoWhileStatement;
import Imm.AST.Statement.ForStatement;
import Imm.AST.Statement.IfStatement;
import Imm.AST.Statement.ReturnStatement;
import Imm.AST.Statement.Statement;
import Imm.AST.Statement.SwitchStatement;
import Imm.AST.Statement.WhileStatement;
import Imm.TYPE.TYPE;
import Imm.TYPE.COMPOSIT.ARRAY;
import Imm.TYPE.COMPOSIT.STRUCT;
import Imm.TYPE.PRIMITIVES.BOOL;
import Imm.TYPE.PRIMITIVES.INT;
import Par.Token.TokenType;
import Par.Token.TokenType.TokenGroup;
import Snips.CompilerDriver;
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
		Token old = current;
		current = tokenStream.pop();
		return old;
	}
	
	public SyntaxElement parse() throws PARSE_EXCEPTION {
		return parseProgram();
	}
	
	protected Program parseProgram() throws PARSE_EXCEPTION {
		Source source = this.current.source;
		List<SyntaxElement> elements = new ArrayList();
		while (this.current.type != TokenType.EOF) {
			TYPE type = this.parseType();
			Token identifier = accept(TokenType.IDENTIFIER);
			
			if (current.type == TokenType.LPAREN) {
				elements.add(this.parseFunction(type, identifier));
			}
			else {
				elements.add(this.parseGlobalDeclaration(type, identifier));
			}
		}
		
		return new Program(elements, source);
	}
	
	protected Function parseFunction(TYPE returnType, Token identifier) throws PARSE_EXCEPTION {
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
		
		List<Statement> body = this.parseCompoundStatement(true);
		
		return new Function(returnType, identifier, parameters, body, identifier.source);
	}
	
	protected Declaration parseParameterDeclaration() throws PARSE_EXCEPTION {
		TYPE type = this.parseType();
		Token id = accept(TokenType.IDENTIFIER);
		return new Declaration(id, type, id.getSource());
	}
	
	protected Statement parseStatement() throws PARSE_EXCEPTION {
		if (current.type.group == TokenGroup.TYPE) {
			return this.parseDeclaration();
		}
		else if (current.type == TokenType.RETURN) {
			return this.parseReturn();
		}
		else if (current.type == TokenType.WHILE) {
			return this.parseWhile();
		}
		else if (current.type == TokenType.DO) {
			return this.parseDoWhile();
		}
		else if (current.type == TokenType.FOR) {
			return this.parseFor();
		}
		else if (current.type == TokenType.BREAK) {
			return this.parseBreak();
		}
		else if (current.type == TokenType.CONTINUE) {
			return this.parseContinue();
		}
		else if (current.type == TokenType.SWITCH) {
			return this.parseSwitch();
		}
		else if (current.type == TokenType.IDENTIFIER) {
			return this.parseAssignment(true);
		}
		else if (current.type == TokenType.IF) {
			return this.parseIf();
		}
		else throw new PARSE_EXCEPTION(current.source, current.type, 
			TokenType.TYPE, TokenType.RETURN, TokenType.WHILE, 
			TokenType.DO, TokenType.FOR, TokenType.BREAK, 
			TokenType.CONTINUE, TokenType.SWITCH, TokenType.IDENTIFIER, 
			TokenType.IF);
	}
	
	protected SwitchStatement parseSwitch() throws PARSE_EXCEPTION {
		Source source = accept(TokenType.SWITCH).getSource();
		accept(TokenType.LPAREN);
		Expression condition = this.parseExpression();
		accept(TokenType.RPAREN);
		accept(TokenType.LBRACE);
		
		List<CaseStatement> cases = new ArrayList();
		DefaultStatement defaultStatement = null;
		
		while (current.type != TokenType.RBRACE) {
			if (current.type == TokenType.CASE) {
				cases.add(this.parseCase());
			}
			else {
				defaultStatement = this.parseDefault();
				break;
			}
		}
		
		accept(TokenType.RBRACE);
		
		return new SwitchStatement(condition, cases, defaultStatement, source);
	}
	
	protected CaseStatement parseCase() throws PARSE_EXCEPTION {
		Source source = accept(TokenType.CASE).getSource();
		accept(TokenType.LPAREN);
		Expression condition = this.parseExpression();
		accept(TokenType.RPAREN);
		accept(TokenType.COLON);
		
		List<Statement> body = this.parseCompoundStatement(false);
		
		return new CaseStatement(condition, body, source);
	}
	
	protected DefaultStatement parseDefault() throws PARSE_EXCEPTION {
		Source source = accept(TokenType.DEFAULT).getSource();
		accept(TokenType.COLON);
		
		List<Statement> body = this.parseCompoundStatement(false);
		
		return new DefaultStatement(body, source);
	}
	
	protected BreakStatement parseBreak() throws PARSE_EXCEPTION {
		Source source = accept(TokenType.BREAK).getSource();
		accept(TokenType.SEMICOLON);
		return new BreakStatement(source);
	}
	
	protected ContinueStatement parseContinue() throws PARSE_EXCEPTION {
		Source source = accept(TokenType.CONTINUE).getSource();
		accept(TokenType.SEMICOLON);
		return new ContinueStatement(source);
	}
	
	protected IfStatement parseIf() throws PARSE_EXCEPTION {
		Source source = current.getSource();
		accept(TokenType.IF);
		accept(TokenType.LPAREN);
		Expression condition = this.parseExpression();
		accept(TokenType.RPAREN);
		
		List<Statement> body = this.parseCompoundStatement(false);
		
		IfStatement if0 = new IfStatement(condition, body, source);
		
		if (current.type == TokenType.ELSE) {
			Source elseSource = accept().getSource();
			if (current.type == TokenType.IF) {
				if0.elseStatement = (IfStatement) this.parseIf();
			}
			else {
				List<Statement> elseBody = this.parseCompoundStatement(false);
				if0.elseStatement = new IfStatement(elseBody, elseSource);
			}
		}
		
		return if0;
	}
	
	protected ForStatement parseFor() throws PARSE_EXCEPTION {
		Source source = accept(TokenType.FOR).getSource();
		accept(TokenType.LPAREN);
		
		/* Accepts semicolon */
		Declaration iterator = this.parseDeclaration();
		
		Expression condition = this.parseExpression();
		accept(TokenType.SEMICOLON);
		
		/* Dont accept semicolon */
		Assignment increment = this.parseAssignment(false);
		
		accept(TokenType.RPAREN);
		
		List<Statement> body = this.parseCompoundStatement(false);
		
		return new ForStatement(iterator, condition, increment, body, source);
	}
	
	protected WhileStatement parseWhile() throws PARSE_EXCEPTION {
		Source source = accept(TokenType.WHILE).getSource();
		accept(TokenType.LPAREN);
		Expression condition = this.parseExpression();
		accept(TokenType.RPAREN);
		List<Statement> body = this.parseCompoundStatement(false);
		return new WhileStatement(condition, body, source);
	}
	
	protected DoWhileStatement parseDoWhile() throws PARSE_EXCEPTION {
		Source source = accept(TokenType.DO).getSource();
		
		List<Statement> body = this.parseCompoundStatement(true);
		
		accept(TokenType.WHILE);
		
		accept(TokenType.LPAREN);
		Expression condition = this.parseExpression();
		accept(TokenType.RPAREN);
		
		accept(TokenType.SEMICOLON);
		
		return new DoWhileStatement(condition, body, source);
	}
	
	protected Assignment parseAssignment(boolean acceptSemicolon) throws PARSE_EXCEPTION {
		Expression target = this.parseElementSelect();
		accept(TokenType.LET);
		Expression value = this.parseExpression();
		if (acceptSemicolon) accept(TokenType.SEMICOLON);
		return new Assignment(target, value, target.getSource());
	}
	
	protected Declaration parseGlobalDeclaration(TYPE type, Token identifier) throws PARSE_EXCEPTION {
		Expression value = null;
		
		if (current.type == TokenType.LET) {
			accept();
			value = this.parseExpression();
		}
		
		accept(TokenType.SEMICOLON);
		return new Declaration(identifier, type, value, identifier.source);
	}
	
	protected Declaration parseDeclaration() throws PARSE_EXCEPTION {
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
	
	protected ReturnStatement parseReturn() throws PARSE_EXCEPTION {
		Token ret = accept(TokenType.RETURN);
		Expression expr = this.parseExpression();
		accept(TokenType.SEMICOLON);
		return new ReturnStatement(expr, ret.getSource());
	}
	
	protected Expression parseExpression() throws PARSE_EXCEPTION {
			return this.parseStructureInit();
	}
	
	protected Expression parseStructureInit() throws PARSE_EXCEPTION {
		if (current.type == TokenType.LBRACE) {
			Source source = accept().getSource();
			List<Expression> elements = new ArrayList();
			while (current.type != TokenType.RBRACE) {
				Expression expr = this.parseExpression();
				elements.add(expr);
				if (current.type == TokenType.COMMA) {
					accept();
				}
				else break;
			}
			
			accept(TokenType.RBRACE);
			return new StructureInit(elements, source);
		}
		else return this.parseTernary();
	}
	
	protected Expression parseTernary() throws PARSE_EXCEPTION {
		Source source = current.getSource();
		Expression condition = this.parseOr();
		if (current.type == TokenType.TERN) {
			accept();
			Expression left = this.parseExpression();
			accept(TokenType.COLON);
			Expression right = this.parseExpression();
			return new Ternary(condition, left, right, source);
		}
		else return condition;
	}
	
	protected Expression parseOr() throws PARSE_EXCEPTION {
		Expression left = this.parseAnd();
		while (current.type == TokenType.OR) {
			accept();
			left = new Or(left, this.parseAnd(), current.source);
		}
		return left;
	}
	
	protected Expression parseAnd() throws PARSE_EXCEPTION {
		Expression left = this.parseCompare();
		while (current.type == TokenType.AND) {
			accept();
			left = new And(left, this.parseCompare(), current.source);
		}
		return left;
	}
	
	protected Expression parseCompare() throws PARSE_EXCEPTION {
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
	
	protected Expression parseShift() throws PARSE_EXCEPTION {
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
	
	protected Expression parseAddSub() throws PARSE_EXCEPTION {
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
		
	protected Expression parseMulDiv() throws PARSE_EXCEPTION {
		Expression left = this.parseNot();
		while (current.type == TokenType.MUL || current.type == TokenType.DIV || current.type == TokenType.MOD) {
			if (current.type == TokenType.MUL) {
				accept();
				left = new Mul(left, this.parseNot(), current.source);
			}
			else if (current.type == TokenType.DIV) {
				Source source = accept().getSource();
				List<Expression> params = new ArrayList();
				params.add(left);
				params.add(this.parseNot());
				
				/* Create inline call to libary function, add div operator to referenced libaries */
				left = new InlineCall(new Token(TokenType.IDENTIFIER, source, "__op_div"), params, source);
				CompilerDriver.driver.referencedLibaries.add("lib/Operator/Div/__op_div.sn");
			}
			else {
				Source source = accept().getSource();
				List<Expression> params = new ArrayList();
				params.add(left);
				params.add(this.parseNot());
				
				/* Create inline call to libary function, add mod operator to referenced libaries */
				left = new InlineCall(new Token(TokenType.IDENTIFIER, source, "__op_mod"), params, source);
				CompilerDriver.driver.referencedLibaries.add("lib/Operator/Mod/__op_mod.sn");
			}
		}
		return left;
	}
	
	protected Expression parseNot() throws PARSE_EXCEPTION {
		Expression not = null;
		while (current.type == TokenType.NOT) {
			accept();
			not = new Not(this.parseNot(), current.source);
		}
		
		if (not == null) not = this.parseUnaryMinus();
		return not;
	}
	
	protected Expression parseUnaryMinus() throws PARSE_EXCEPTION {
		Expression not = null;
		while (current.type == TokenType.SUB) {
			accept();
			not = new UnaryMinus(this.parseUnaryMinus(), current.source);
		}
		
		if (not == null) not = this.parseElementSelect();
		return not;
	}
	
	protected Expression parseElementSelect() throws PARSE_EXCEPTION {
		Expression ref = this.parseAtom();
		
		if (current.type == TokenType.LBRACKET) {
			List<Expression> selection = new ArrayList();
			while (current.type == TokenType.LBRACKET) {
				accept();
				selection.add(this.parseExpression());
				accept(TokenType.RBRACKET);
			}
			
			return new ElementSelect(ref, selection, ref.getSource());
		}
		else return ref;
	}
	
	protected Expression parseAtom() throws PARSE_EXCEPTION {
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
		else if (current.type == TokenType.BOOLLIT) {
			Token token = accept();
			return new Atom(new BOOL(token.spelling), token, token.source);
		}
		else throw new PARSE_EXCEPTION(current.source, current.type, TokenType.LPAREN, TokenType.IDENTIFIER, TokenType.INTLIT);
	}

	protected TYPE parseType() throws PARSE_EXCEPTION {
		TYPE type = null;
		if (current.type.group == TokenGroup.TYPE) {
			if (current.type == TokenType.INT) {
				accept();
				type = new INT();
			}
			else if (current.type == TokenType.BOOL) {
				accept();
				type = new BOOL();
			}
		}
		
		Stack<Expression> dimensions = new Stack();
		while (current.type == TokenType.LBRACKET) {
			accept();
			Expression length = this.parseExpression();
			accept(TokenType.RBRACKET);
			dimensions.push(length);
		}
		
		while (!dimensions.isEmpty()) {
			type = new ARRAY(type, dimensions.pop());
		}
		
		return type;
	}
	
	protected List<Statement> parseCompoundStatement(boolean forceBraces) throws PARSE_EXCEPTION {
		List<Statement> body = new ArrayList();
		
		/* Compound Statement with braces */
		if (current.type == TokenType.LBRACE || forceBraces) {
			accept(TokenType.LBRACE);
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
