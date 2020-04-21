package Par;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Stack;

import Exc.CTX_EXCEPTION;
import Exc.PARSE_EXCEPTION;
import Exc.SNIPS_EXCEPTION;
import Imm.AST.Function;
import Imm.AST.Program;
import Imm.AST.SyntaxElement;
import Imm.AST.Directive.CompileDirective;
import Imm.AST.Directive.CompileDirective.COMP_DIR;
import Imm.AST.Directive.Directive;
import Imm.AST.Directive.IncludeDirective;
import Imm.AST.Expression.AddressOf;
import Imm.AST.Expression.ArrayInit;
import Imm.AST.Expression.ArraySelect;
import Imm.AST.Expression.Atom;
import Imm.AST.Expression.Deref;
import Imm.AST.Expression.Expression;
import Imm.AST.Expression.IDRef;
import Imm.AST.Expression.IDRefWriteback;
import Imm.AST.Expression.IDRefWriteback.ID_WRITEBACK;
import Imm.AST.Expression.InlineCall;
import Imm.AST.Expression.SizeOfExpression;
import Imm.AST.Expression.SizeOfType;
import Imm.AST.Expression.StructSelect;
import Imm.AST.Expression.StructureInit;
import Imm.AST.Expression.TypeCast;
import Imm.AST.Expression.Arith.Add;
import Imm.AST.Expression.Arith.BitAnd;
import Imm.AST.Expression.Arith.BitNot;
import Imm.AST.Expression.Arith.BitOr;
import Imm.AST.Expression.Arith.BitXor;
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
import Imm.AST.Lhs.ArraySelectLhsId;
import Imm.AST.Lhs.LhsId;
import Imm.AST.Lhs.PointerLhsId;
import Imm.AST.Lhs.SimpleLhsId;
import Imm.AST.Lhs.StructSelectLhsId;
import Imm.AST.Statement.AssignWriteback;
import Imm.AST.Statement.Assignment;
import Imm.AST.Statement.Assignment.ASSIGN_ARITH;
import Imm.AST.Statement.BreakStatement;
import Imm.AST.Statement.CaseStatement;
import Imm.AST.Statement.Comment;
import Imm.AST.Statement.ContinueStatement;
import Imm.AST.Statement.Declaration;
import Imm.AST.Statement.DefaultStatement;
import Imm.AST.Statement.DoWhileStatement;
import Imm.AST.Statement.ForStatement;
import Imm.AST.Statement.FunctionCall;
import Imm.AST.Statement.IfStatement;
import Imm.AST.Statement.ReturnStatement;
import Imm.AST.Statement.Statement;
import Imm.AST.Statement.StructTypedef;
import Imm.AST.Statement.SwitchStatement;
import Imm.AST.Statement.WhileStatement;
import Imm.TYPE.PROVISO;
import Imm.TYPE.TYPE;
import Imm.TYPE.COMPOSIT.ARRAY;
import Imm.TYPE.COMPOSIT.POINTER;
import Imm.TYPE.COMPOSIT.STRUCT;
import Imm.TYPE.PRIMITIVES.BOOL;
import Imm.TYPE.PRIMITIVES.INT;
import Par.Token.TokenType;
import Par.Token.TokenType.TokenGroup;
import Snips.CompilerDriver;
import Util.Pair;
import Util.Source;
import Util.Logging.Message;

public class Parser {

	protected List<Token> tokenStream;
	
	Token current;
	
	HashMap<String, STRUCT> structs = new HashMap();
	
	List<Pair<Token, StructTypedef>> structIds = new ArrayList();
	
	List<String> activeProvisos = new ArrayList();
	
	public Parser(List tokens) throws SNIPS_EXCEPTION {
		if (tokens == null) throw new SNIPS_EXCEPTION("SNIPS_PARSE -> Tokens are null!");
		tokenStream = tokens;
		
		current = tokenStream.get(0);
		tokenStream.remove(0);
	}
	
	/**
	 * Accept a token based on its type.
	 * @param tokenType The type the token should match.
	 * @return The accepted Token.
	 * @throws PARSE_EXCEPTION Thrown then the token does not have the given type.
	 */
	protected Token accept(TokenType tokenType) throws PARSE_EXCEPTION {
		/* Convert tokens dynamically based on the currently active provisos */
		if (this.activeProvisos.contains(current.spelling)) {
			current.type = TokenType.PROVISO;
		}
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
		/* Convert tokens dynamically based on the currently active provisos */
		if (this.activeProvisos.contains(current.spelling)) {
			current.type = TokenType.PROVISO;
		}
		if (current.type().group == group)return accept();
		else throw new PARSE_EXCEPTION(current.source, current.type());
	}
	
	/**
	 * Accept a token without any checks.
	 * @return The accepted token.
	 */
	protected Token accept() {
		/* Convert tokens dynamically based on the currently active provisos */
		if (this.activeProvisos.contains(current.spelling)) {
			current.type = TokenType.PROVISO;
		}
		//System.out.println(current.type.toString() + " " + current.spelling);
		Token old = current;
		current = tokenStream.get(0);
		tokenStream.remove(0);
		return old;
	}
	
	public SyntaxElement parse() throws PARSE_EXCEPTION {
		return parseProgram();
	}
	
	protected Program parseProgram() throws PARSE_EXCEPTION {
		Source source = this.current.source;
		List<SyntaxElement> elements = new ArrayList();
		
		boolean incEnd = false;
		List<Directive> include = new ArrayList();
		List<Directive> directives = new ArrayList();
		
		while (this.current.type != TokenType.EOF) {
			this.activeProvisos.clear();
			if (current.type == TokenType.COMMENT) {
				elements.add(this.parseComment());
			}
			else if (current.type == TokenType.DIRECTIVE) {
				Directive dir = this.parseDirective();
				
				if (incEnd) {
					new Message("All include statements should be at the head of the file", Message.Type.WARN);
				}
				
				if (dir instanceof IncludeDirective) {
					include.add(dir);
				}
				else {
					incEnd = true;
					directives.add(dir);
				}
			}
			else {
				incEnd = true;
				if (current.type == TokenType.STRUCT) {
					elements.add(this.parseStructTypedef());
				}
				else {
					TYPE type = this.parseType();
					
					Token identifier = accept(TokenType.IDENTIFIER);
					
					SyntaxElement element = null;
					if (current.type == TokenType.LPAREN || current.type == TokenType.CMPLT) {
						element = this.parseFunction(type, identifier);
					}
					else {
						element = this.parseGlobalDeclaration(type, identifier);
					}
					
					elements.add(element);
					if (!directives.isEmpty()) {
						element.directives.addAll(directives);
						directives.clear();
					}
				}
			}
		}
		
		Program program = new Program(elements, source);
		program.directives.addAll(include);
		
		return program;
	}
	
	public Comment parseComment() throws PARSE_EXCEPTION {
		Token comment = accept(TokenType.COMMENT);
		return new Comment(comment, comment.getSource());
	}
	
	public StructTypedef parseStructTypedef() throws PARSE_EXCEPTION {
		Source source = accept(TokenType.STRUCT).getSource();
		Token id = accept(TokenType.STRUCTID);
		
		List<TYPE> proviso = this.parseProviso();
		
		StructTypedef def = new StructTypedef(id.spelling, proviso, new ArrayList(), source);
		this.structIds.add(new Pair<Token, StructTypedef>(id, def));
		
		accept(TokenType.LBRACE);
		
		while (current.type != TokenType.RBRACE) {
			def.fields.add(this.parseDeclaration());
		}
		accept(TokenType.RBRACE);
		
		return def;
	}
	
	public Directive parseDirective() throws PARSE_EXCEPTION {
		Source source = accept(TokenType.DIRECTIVE).getSource();
		if (current.type == TokenType.INCLUDE) {
			accept();
			accept(TokenType.CMPLT);
			String path = "";
			while (current.type != TokenType.CMPGT) {
				path += accept().spelling;
			}
			accept();
			return new IncludeDirective(path, source);
		}
		else if (current.type == TokenType.IDENTIFIER) {
			COMP_DIR dir;
			String s = accept(TokenType.IDENTIFIER).spelling.toLowerCase();
			if (s.equals("operator")) dir = COMP_DIR.OPERATOR;
			else if (s.equals("libary")) dir = COMP_DIR.LIBARY;
			else if (s.equals("unroll")) dir = COMP_DIR.UNROLL;
			else throw new PARSE_EXCEPTION(source, TokenType.IDENTIFIER);
			
			return new CompileDirective(dir, source);
		}
		else throw new PARSE_EXCEPTION(source, TokenType.INCLUDE, TokenType.IDENTIFIER);
	}
	
	protected Function parseFunction(TYPE returnType, Token identifier) throws PARSE_EXCEPTION {
		List<TYPE> proviso = this.parseProviso();
		
		for (TYPE t : proviso) {
			PROVISO p = (PROVISO) t;
			this.activeProvisos.add(p.placeholderName);
		}
		
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
		
		return new Function(returnType, identifier, proviso, parameters, body, identifier.source);
	}
	
	protected Declaration parseParameterDeclaration() throws PARSE_EXCEPTION {
		TYPE type = this.parseType();
		Token id = accept(TokenType.IDENTIFIER);
		return new Declaration(id, type, id.getSource());
	}
	
	protected Statement parseStatement() throws PARSE_EXCEPTION {
		if (current.type == TokenType.COMMENT) {
			return this.parseComment();
		}
		else if (current.type.group == TokenGroup.TYPE) {
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
		else if (current.type == TokenType.IDENTIFIER && 
				(this.tokenStream.get(0).type == TokenType.LPAREN || this.tokenStream.get(0).type == TokenType.CMPLT)) {
			return this.parseFunctionCall();
		}
		else if (current.type == TokenType.IDENTIFIER || current.type == TokenType.MUL) {
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
	
	protected FunctionCall parseFunctionCall() throws PARSE_EXCEPTION {
		Token identifier = accept(TokenType.IDENTIFIER);
		
		List<TYPE> provisos = this.parseProviso();
		
		accept(TokenType.LPAREN);
		
		List<Expression> params = new ArrayList();
		while (current.type != TokenType.RPAREN) {
			params.add(this.parseExpression());
			if (current.type == TokenType.COMMA) {
				accept();
			}
			else break;
		}
		accept(TokenType.RPAREN);
		accept(TokenType.SEMICOLON);
		
		if (identifier.spelling.equals("free")) {
			CompilerDriver.driver.referencedLibaries.add("lib/mem/free.sn");
		}
		
		return new FunctionCall(identifier, provisos, params, identifier.getSource());
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
		Statement increment = this.parseAssignment(false);
		
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
	
	protected Statement parseAssignment(boolean acceptSemicolon) throws PARSE_EXCEPTION {
		if (tokenStream.get(0).type == TokenType.INCR || tokenStream.get(0).type == TokenType.DECR) {
			Token id = accept();
			ID_WRITEBACK idWb = (current.type == TokenType.INCR)? ID_WRITEBACK.INCR : ID_WRITEBACK.DECR;
			
			accept();
			if (acceptSemicolon) accept(TokenType.SEMICOLON);
			
			return new AssignWriteback(new IDRefWriteback(idWb, new IDRef(id, id.getSource()), id.getSource()), id.getSource());
		}
		else {
			LhsId target = this.parseLhsIdentifer();
			ASSIGN_ARITH arith = this.parseAssignOperator();
			Expression value = this.parseExpression();
			if (acceptSemicolon) accept(TokenType.SEMICOLON);
			return new Assignment(arith, target, value, target.getSource());
		}
	}
	
	protected ASSIGN_ARITH parseAssignOperator() throws PARSE_EXCEPTION {
		if (current.type == TokenType.LET) {
			accept();
			return ASSIGN_ARITH.NONE;
		}
		else if (current.type == TokenType.ADD) {
			accept();
			accept(TokenType.LET);
			return ASSIGN_ARITH.ADD_ASSIGN;
		}
		else if (current.type == TokenType.SUB) {
			accept();
			accept(TokenType.LET);
			return ASSIGN_ARITH.SUB_ASSIGN;
		}
		else if (current.type == TokenType.MUL) {
			accept();
			accept(TokenType.LET);
			return ASSIGN_ARITH.MUL_ASSIGN;
		}
		else if (current.type == TokenType.DIV) {
			accept();
			accept(TokenType.LET);
			return ASSIGN_ARITH.DIV_ASSIGN;
		}
		else if (current.type == TokenType.ADDROF) {
			accept();
			accept(TokenType.LET);
			return ASSIGN_ARITH.AND_ASSIGN;
		}
		else if (current.type == TokenType.BITOR) {
			accept();
			accept(TokenType.LET);
			return ASSIGN_ARITH.ORR_ASSIGN;
		}
		else if (current.type == TokenType.XOR) {
			accept();
			accept(TokenType.LET);
			return ASSIGN_ARITH.XOR_ASSIGN;
		}
		else if (current.type == TokenType.CMPLT && this.tokenStream.get(0).type == TokenType.CMPLT) {
			accept();
			accept();
			accept(TokenType.LET);
			return ASSIGN_ARITH.LSL_ASSIGN;
		}
		else if (current.type == TokenType.CMPGT && this.tokenStream.get(0).type == TokenType.CMPGT) {
			accept();
			accept();
			accept(TokenType.LET);
			return ASSIGN_ARITH.LSR_ASSIGN;
		}
		else throw new PARSE_EXCEPTION(current.source, current.type, TokenType.LET);
	}
	
	protected LhsId parseLhsIdentifer() throws PARSE_EXCEPTION {
		if (current.type == TokenType.MUL) {
			Source source = current.getSource();
			return new PointerLhsId(this.parseDeref(), source);
		}
		else {
			Expression target = this.parseStructSelect();
			if (target instanceof ArraySelect) {
				return new ArraySelectLhsId((ArraySelect) target, target.getSource());
			}
			else if (target instanceof IDRef) {
				return new SimpleLhsId((IDRef) target, target.getSource());
			}
			else if (target instanceof StructSelect) {
				return new StructSelectLhsId((StructSelect) target, target.getSource());
			}
			else {
				throw new PARSE_EXCEPTION(current.source, current.type, TokenType.IDENTIFIER);
			}
		}
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
		
		if (current.type == TokenType.SEMICOLON) {
			/* Void return */
			accept();
			return new ReturnStatement(null, ret.getSource());
		}
		else {
			/* Value return */
			Expression expr = this.parseExpression();
			accept(TokenType.SEMICOLON);
			return new ReturnStatement(expr, ret.getSource());
		}
	}
	
	protected Expression parseExpression() throws PARSE_EXCEPTION {
			return this.parseStructureInit();
	}
	
	protected Expression parseStructureInit() throws PARSE_EXCEPTION {
		if (current.type == TokenType.STRUCTID) {
			Token id = accept();
			StructTypedef def = this.getStructTypedef(id);
			
			List<TYPE> proviso = this.parseProviso();
			
			accept(TokenType.COLON);
			accept(TokenType.COLON);
			
			accept(TokenType.LPAREN);
			
			List<Expression> elements = new ArrayList();
			while (current.type != TokenType.RBRACE) {
				Expression expr = this.parseExpression();
				elements.add(expr);
				if (current.type == TokenType.COMMA) {
					accept();
				}
				else break;
			}
			
			accept(TokenType.RPAREN);
			return new StructureInit(def, proviso, elements, id.getSource());
		}
		else return this.parseArrayInit();
	}
	
	protected Expression parseArrayInit() throws PARSE_EXCEPTION {
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
			return new ArrayInit(elements, source);
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
		Expression left = this.parseBitOr();
		while (current.type == TokenType.AND) {
			accept();
			left = new And(left, this.parseBitOr(), current.source);
		}
		return left;
	}
	
	protected Expression parseBitOr() throws PARSE_EXCEPTION {
		Expression left = this.parseBitXor();
		while (current.type == TokenType.BITOR) {
			accept();
			left = new BitOr(left, this.parseBitXor(), current.source);
		}
		return left;
	}
	
	protected Expression parseBitXor() throws PARSE_EXCEPTION {
		Expression left = this.parseBitAnd();
		while (current.type == TokenType.XOR) {
			accept();
			left = new BitXor(left, this.parseBitAnd(), current.source);
		}
		return left;
	}
	
	protected Expression parseBitAnd() throws PARSE_EXCEPTION {
		Expression left = this.parseCompare();
		while (current.type == TokenType.ADDROF) {
			accept();
			left = new BitAnd(left, this.parseCompare(), current.source);
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
		while ((current.type == TokenType.CMPLT && this.tokenStream.get(0).type == TokenType.CMPLT) || 
			   (current.type == TokenType.CMPGT && this.tokenStream.get(0).type == TokenType.CMPGT)) {
			if (current.type == TokenType.CMPLT) {
				accept();
				accept();
				left = new Lsl(left, this.parseAddSub(), current.source);
			}
			else {
				accept();
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
		Expression left = this.parseSizeOf();
		while (current.type == TokenType.MUL || current.type == TokenType.DIV || current.type == TokenType.MOD) {
			if (current.type == TokenType.MUL) {
				accept();
				left = new Mul(left, this.parseSizeOf(), current.source);
			}
			else if (current.type == TokenType.DIV) {
				Source source = accept().getSource();
				List<Expression> params = new ArrayList();
				params.add(left);
				params.add(this.parseSizeOf());
				
				/* Create inline call to libary function, add div operator to referenced libaries */
				left = new InlineCall(new Token(TokenType.IDENTIFIER, source, "__op_div"), new ArrayList(), params, source);
				CompilerDriver.driver.referencedLibaries.add("lib/op/__op_div.sn");
			}
			else {
				Source source = accept().getSource();
				List<Expression> params = new ArrayList();
				params.add(left);
				params.add(this.parseSizeOf());
				
				/* Create inline call to libary function, add mod operator to referenced libaries */
				left = new InlineCall(new Token(TokenType.IDENTIFIER, source, "__op_mod"), new ArrayList(), params, source);
				CompilerDriver.driver.referencedLibaries.add("lib/op/__op_mod.sn");
			}
		}
		return left;
	}
	
	protected Expression parseSizeOf() throws PARSE_EXCEPTION {
		Expression sof = null;
		while (current.type == TokenType.SIZEOF) {
			Source source = accept().getSource();
			accept(TokenType.LPAREN);
			
			/* Size of Type */
			if (current.type.group == TokenGroup.TYPE) {
				TYPE type = this.parseType();
				sof = new SizeOfType(type, source);
			}
			/* Size of Expression */
			else sof = new SizeOfExpression(this.parseExpression(), source);
			
			accept(TokenType.RPAREN);
		}
		
		if (sof == null) sof = this.parseAddressOf();
		return sof;
	}
	
	protected Expression parseAddressOf() throws PARSE_EXCEPTION {
		Expression addr = null;
		while (current.type == TokenType.ADDROF) {
			Source source = accept().getSource();
			addr = new AddressOf(this.parseDeref(), source);
		}
		
		if (addr == null) addr = this.parseDeref();
		return addr;
	}
	
	protected Expression parseDeref() throws PARSE_EXCEPTION {
		Expression addr = null;
		if (current.type == TokenType.MUL) {
			Source source = accept().getSource();
			addr = new Deref(this.parseDeref(), source);
		}
		
		if (addr == null) addr = this.parseTypeCast();
		return addr;
	}
	
	protected Expression parseTypeCast() throws PARSE_EXCEPTION {
		Expression cast = null;
		
		/* Convert next token */
		if (this.activeProvisos.contains(this.tokenStream.get(0).spelling)) {
			this.tokenStream.get(0).type = TokenType.PROVISO;
		}
		
		while (current.type == TokenType.LPAREN && this.tokenStream.get(0).type.group == TokenGroup.TYPE) {
			Source source = accept().getSource();
			TYPE castType = this.parseType();
			accept(TokenType.RPAREN);
			
			Expression cast0 = this.parseNot();
			
			cast = new TypeCast(cast0, castType, source);
		}
		
		if (cast == null) cast = this.parseNot();
		return cast;
	}
	
	protected Expression parseNot() throws PARSE_EXCEPTION {
		Expression not = null;
		while (current.type == TokenType.NEG || current.type == TokenType.NOT) {
			if (current.type == TokenType.NEG) {
				accept();
				not = new Not(this.parseNot(), current.source);
			}
			else {
				accept(TokenType.NOT);
				not = new BitNot(this.parseNot(), current.source);
			}
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
		
		if (not == null) not = this.parseStructSelect();
		return not;
	}
	
	protected Expression parseStructSelect() throws PARSE_EXCEPTION {
		Expression ref = this.parseArraySelect();
		
		if (current.type == TokenType.DOT) {
			accept();
			ref = new StructSelect(ref, this.parseStructSelect(), false, ref.getSource());
		}
		else if (current.type == TokenType.UNION_ACCESS) {
			accept();
			ref = new StructSelect(ref, this.parseStructSelect(), true, ref.getSource());
		}
		
		return ref;
	}
	
	protected Expression parseArraySelect() throws PARSE_EXCEPTION {
		Expression ref = this.parseIncrDecr();
		
		if (current.type == TokenType.LBRACKET) {
			List<Expression> selection = new ArrayList();
			while (current.type == TokenType.LBRACKET) {
				accept();
				selection.add(this.parseExpression());
				accept(TokenType.RBRACKET);
			}
			
			return new ArraySelect(ref, selection, ref.getSource());
		}
		else return ref;
	}
	
	protected Expression parseIncrDecr() throws PARSE_EXCEPTION {
		Expression ref = this.parseAtom();
		
		while (current.type == TokenType.INCR || current.type == TokenType.DECR) {
			Source source = current.getSource();
			if (current.type == TokenType.INCR) {
				accept();
				ref = new IDRefWriteback(ID_WRITEBACK.INCR, ref, source);
			}
			else {
				accept();
				ref = new IDRefWriteback(ID_WRITEBACK.DECR, ref, source);
			}
		}
		
		return ref;
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
			
			/* Convert next token */
			if (this.activeProvisos.contains(this.tokenStream.get(0).spelling)) {
				this.tokenStream.get(0).type = TokenType.PROVISO;
			}
			
			if (current.type == TokenType.LPAREN || (current.type == TokenType.CMPLT && tokenStream.get(0).type.group == TokenGroup.TYPE)) {
				/* Inline Call */
				List<TYPE> proviso = this.parseProviso();
				
				accept(TokenType.LPAREN);
				
				List<Expression> parameters = new ArrayList();
				while (current.type != TokenType.RPAREN) {
					parameters.add(this.parseExpression());
					if (current.type != TokenType.COMMA) break;
					accept(TokenType.COMMA);
				}
				accept(TokenType.RPAREN);
				
				if (id.spelling.equals("resv")) {
					CompilerDriver.heap_referenced = true;
					CompilerDriver.driver.referencedLibaries.add("lib/mem/resv.sn");
				}
				return new InlineCall(id, proviso, parameters, id.getSource());
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
	
	public StructTypedef getStructTypedef(Token token) {
		for (Pair<Token, StructTypedef> p : this.structIds) {
			if (p.getFirst().spelling.equals(token.spelling)) {
				return p.getSecond();
			}
		}
		return null;
	}

	protected TYPE parseType() throws PARSE_EXCEPTION {
		TYPE type = null;
		Token token = null;
		
		if (current.type == TokenType.IDENTIFIER) token = accept();
		else token = accept(TokenGroup.TYPE);
		
		StructTypedef def = this.getStructTypedef(token);
		
		if (def != null) {
			type = def.struct.clone();
			
			List<TYPE> proviso = this.parseProviso();
			
			if (type instanceof STRUCT) {
				STRUCT s = (STRUCT) type;
				s.proviso = proviso;
			}
			else {
				if (!proviso.isEmpty()) {
					/* Throw a 'Parser Context Exception', since we know that this would be an error in CTX anyway */
					try {
						throw new CTX_EXCEPTION(token.getSource(), "Cannot apply proviso to a non struct type");
					} catch (Exception e) {}
					throw new PARSE_EXCEPTION(current.source, TokenType.CMPLT, TokenType.MUL, TokenType.LBRACKET);
				}
			}
		}
		else {
			type = TYPE.fromToken(token);
			
			if (type instanceof PROVISO && !this.activeProvisos.contains(token.spelling)) {
				this.activeProvisos.add(token.spelling);
			}
		}
		
		while (true) {
			Token c0 = current;
			while (current.type == TokenType.MUL) {
				accept();
				type = new POINTER(type);
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
			
			if (current.equals(c0)) break;
		}
		
		return type;
	}
	
	protected List<TYPE> parseProviso() throws PARSE_EXCEPTION {
		List<TYPE> pro = new ArrayList();
		
		if (current.type == TokenType.CMPLT) {
			
			/* Set type of all identifiers to proviso until a CMPGT */
			for (int i = 0; i < this.tokenStream.size(); i++) {
				if (this.tokenStream.get(i).type == TokenType.CMPGT || 
						(this.tokenStream.get(i).type != TokenType.COMMA &&
						this.tokenStream.get(i).type != TokenType.IDENTIFIER &&
						this.tokenStream.get(i).type != TokenType.CMPGT)) break;
				
				if (this.tokenStream.get(i).type == TokenType.IDENTIFIER) {
					this.tokenStream.get(i).type = TokenType.PROVISO;
				}
			}
			
			accept();
			
			while (current.type != TokenType.CMPGT) {
				TYPE type = this.parseType();
				pro.add(type);
				if (current.type == TokenType.COMMA) {
					accept();
				}
				else break;
			}
			
			accept(TokenType.CMPGT);
		}
		
		return pro;
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
