package Par;

import java.util.ArrayList;
import java.util.List;
import java.util.Stack;

import CGen.LabelGen;
import Exc.CTX_EXCEPTION;
import Exc.PARSE_EXCEPTION;
import Exc.SNIPS_EXCEPTION;
import Imm.AST.Function;
import Imm.AST.Namespace;
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
import Imm.AST.Expression.FunctionRef;
import Imm.AST.Expression.IDRef;
import Imm.AST.Expression.IDRefWriteback;
import Imm.AST.Expression.InlineCall;
import Imm.AST.Expression.RegisterAtom;
import Imm.AST.Expression.SizeOfExpression;
import Imm.AST.Expression.SizeOfType;
import Imm.AST.Expression.StructSelect;
import Imm.AST.Expression.StructSelectWriteback;
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
import Imm.AST.Statement.AssignWriteback.WRITEBACK;
import Imm.AST.Statement.Assignment;
import Imm.AST.Statement.Assignment.ASSIGN_ARITH;
import Imm.AST.Statement.BreakStatement;
import Imm.AST.Statement.CaseStatement;
import Imm.AST.Statement.Comment;
import Imm.AST.Statement.ContinueStatement;
import Imm.AST.Statement.Declaration;
import Imm.AST.Statement.DefaultStatement;
import Imm.AST.Statement.DoWhileStatement;
import Imm.AST.Statement.EnumTypedef;
import Imm.AST.Statement.ForStatement;
import Imm.AST.Statement.FunctionCall;
import Imm.AST.Statement.IfStatement;
import Imm.AST.Statement.ReturnStatement;
import Imm.AST.Statement.SignalStatement;
import Imm.AST.Statement.Statement;
import Imm.AST.Statement.StructTypedef;
import Imm.AST.Statement.SwitchStatement;
import Imm.AST.Statement.TryStatement;
import Imm.AST.Statement.WatchStatement;
import Imm.AST.Statement.WhileStatement;
import Imm.AsN.AsNNode.MODIFIER;
import Imm.TYPE.NULL;
import Imm.TYPE.PROVISO;
import Imm.TYPE.TYPE;
import Imm.TYPE.COMPOSIT.ARRAY;
import Imm.TYPE.COMPOSIT.POINTER;
import Imm.TYPE.COMPOSIT.STRUCT;
import Imm.TYPE.PRIMITIVES.BOOL;
import Imm.TYPE.PRIMITIVES.CHAR;
import Imm.TYPE.PRIMITIVES.FUNC;
import Imm.TYPE.PRIMITIVES.INT;
import Par.Token.TokenType;
import Par.Token.TokenType.TokenGroup;
import Snips.CompilerDriver;
import Util.NamespacePath;
import Util.NamespacePath.PATH_TERMINATION;
import Util.Pair;
import Util.Source;
import Util.Logging.Message;
import Util.Logging.ProgressMessage;

/**
 * Parses an AST out of the token stream. The process is similar to this symbolic representation:
 * 
 * 	          --> www.reddit.com/r/ProgrammerHumor/comments/h83mqx/parser_be_like/ <--
 * 
 */
public class Parser {

	protected List<Token> tokenStream;
	
	protected Token current;
	
	/* Active Struct Ids */
	List<Pair<NamespacePath, Function>> functions = new ArrayList();
	
	/* Active Struct Ids */
	List<Pair<NamespacePath, StructTypedef>> structIds = new ArrayList();
	
	/* Active Enum Ids */
	List<Pair<NamespacePath, EnumTypedef>> enumIds = new ArrayList();
	
	/* All active Provisos, like T, V */
	List<String> activeProvisos = new ArrayList();
	
	/* All types that need to be cloned after parsing is complete */
	List<Pair<TYPE, List<TYPE>>> toClone = new ArrayList();
	
	/* The currently open namespaces */
	Stack<NamespacePath> namespaces = new Stack();
	
	Stack<List<Declaration>> scopes = new Stack();
	
	List<Message> buffered = new ArrayList();
	
	protected ProgressMessage progress;
	
	public int done = 0, toGo;
	
	public Parser(List tokens, ProgressMessage progress) throws SNIPS_EXCEPTION {
		if (tokens == null) {
			this.progress.abort();
			throw new SNIPS_EXCEPTION("SNIPS_PARSE -> Tokens are null!");
		}
		tokenStream = tokens;
		
		this.progress = progress;
		
		this.toGo = tokens.size();
		
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
		
		if (current.type() == tokenType) return accept();
		else {
			this.progress.abort();
			throw new PARSE_EXCEPTION(current.source, current.type(), tokenType);
		}
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
		else {
			this.progress.abort();
			throw new PARSE_EXCEPTION(current.source, current.type());
		}
	}
	
	public int p = 0;
	
	/**
	 * Accept a token without any checks.
	 * @return The accepted token.
	 */
	protected Token accept() {
		/* Convert tokens dynamically based on the currently active provisos */
		if (this.activeProvisos.contains(current.spelling)) {
			current.type = TokenType.PROVISO;
		}
		
		//System.out.println("\t" + current.type.toString() + " " + current.spelling);
		
		if (this.progress != null) {
			done++;
			this.progress.incProgress((double) done / this.toGo);
		}
		
		Token old = current;
		if (!tokenStream.isEmpty()) {
			current = tokenStream.get(0);
			tokenStream.remove(0);
		}
		
		return old;
	}
	
	public SyntaxElement parse() throws PARSE_EXCEPTION {
		Program p = parseProgram();
		
		/* Clone all struct type instances that were parsed from the SSOT */
		for (Pair<TYPE, List<TYPE>> p0 : this.toClone) {
			if (p0.first instanceof STRUCT) {
				STRUCT s = (STRUCT) p0.first;
				
				s.typedef = s.typedef.clone();
			}
		}
		
		return p;
	}
	
	protected Program parseProgram() throws PARSE_EXCEPTION {
		this.scopes.push(new ArrayList());
		Source source = this.current.source;
		List<SyntaxElement> elements = new ArrayList();
		
		boolean incEnd = false;
		List<Directive> include = new ArrayList();
		List<Directive> directives = new ArrayList();
		
		while (this.current.type != TokenType.EOF) {
			this.activeProvisos.clear();
			if (current.type == TokenType.DIRECTIVE) {
				Directive dir = this.parseDirective();
				
				if (incEnd) {
					buffered.add(new Message("All include statements should be at the head of the file", Message.Type.WARN, true));
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
				elements.add(this.parseProgramElement());
			}
		}
		
		accept(TokenType.EOF);
		
		Program program = new Program(elements, source);
		program.directives.addAll(include);
		
		for (Message m : buffered) m.flush();
		
		this.scopes.pop();
		return program;
	}
	
	public Namespace parseNamespace() throws PARSE_EXCEPTION {
		Source source = accept(TokenType.NAMESPACE).getSource();
		
		NamespacePath path = this.parseNamespacePath();
		
		this.namespaces.push(path);
		
		accept(TokenType.LBRACE);
		
		List<SyntaxElement> elements = new ArrayList();
		
		while (current.type != TokenType.RBRACE) {
			elements.add(this.parseProgramElement());
		}
		
		this.namespaces.pop();
		
		accept(TokenType.RBRACE);
		
		return new Namespace(path, elements, source);
	}
	
	public SyntaxElement parseProgramElement() throws PARSE_EXCEPTION {
		if (current.type == TokenType.COMMENT) {
			return this.parseComment();
		}
		else if (current.type == TokenType.STRUCT || (current.type.group == TokenGroup.MODIFIER && this.tokenStream.get(0).type == TokenType.STRUCT)) {
			return this.parseStructTypedef();
		}
		else if (current.type == TokenType.ENUM || (current.type.group == TokenGroup.MODIFIER && this.tokenStream.get(0).type == TokenType.ENUM)) {
			return this.parseEnumTypedef();
		}
		else if (current.type == TokenType.NAMESPACE) {
			return this.parseNamespace();
		}
		else {
			MODIFIER mod = this.parseModifier();
			
			TYPE type = this.parseType();
			
			Token identifier = accept(TokenType.IDENTIFIER);
			
			SyntaxElement element = null;
			if (current.type == TokenType.LPAREN || current.type == TokenType.CMPLT) {
				element = this.parseFunction(type, identifier, mod);
			}
			else {
				Declaration d = this.parseGlobalDeclaration(type, identifier, mod);
				this.scopes.peek().add(d);
				element = d;
			}
			
			return element;
		}
	}
	
	public Comment parseComment() throws PARSE_EXCEPTION {
		Token comment = accept(TokenType.COMMENT);
		return new Comment(comment, comment.getSource());
	}
	
	public StructTypedef parseStructTypedef() throws PARSE_EXCEPTION {
		
		MODIFIER mod = this.parseModifier();
		
		Source source = accept(TokenType.STRUCT).getSource();
		Token id = accept(TokenType.STRUCTID);
		
		List<TYPE> proviso = this.parseProviso();
		NamespacePath path = this.buildPath(id.spelling);
		
		/* Extend Struct */
		StructTypedef ext = null;
		List<Declaration> extendDecs = new ArrayList();
		if (current.type == TokenType.COLON) {
			accept();
			NamespacePath ext0 = this.parseNamespacePath();
			ext = this.getStructTypedef(ext0, source);
			
			for (Declaration d : ext.fields) extendDecs.add(d.clone());
		}
		
		StructTypedef def = new StructTypedef(path, LabelGen.getSID(), proviso, new ArrayList(), ext, mod, source);
		this.structIds.add(new Pair<NamespacePath, StructTypedef>(path, def));
		
		def.fields.addAll(extendDecs);
		
		accept(TokenType.LBRACE);
		
		while (current.type != TokenType.RBRACE) {
			def.fields.add(this.parseDeclaration(MODIFIER.SHARED));
		}
		accept(TokenType.RBRACE);
		
		return def;
	}
	
	public EnumTypedef parseEnumTypedef() throws PARSE_EXCEPTION {
		Source source = accept(TokenType.ENUM).getSource();
		Token id = accept(TokenType.ENUMID);
		
		NamespacePath path = this.buildPath(id.spelling);
		
		accept(TokenType.LBRACE);
		
		List<String> enums = new ArrayList();
		
		while (current.type != TokenType.RBRACE) {
			String e = accept(TokenType.IDENTIFIER).spelling;
			enums.add(e);
			
			if (current.type == TokenType.COMMA) {
				accept();
			}
			else {
				accept(TokenType.SEMICOLON);
				break;
			}
		}
		
		for (int i = 0; i < tokenStream.size(); i++) {
			if (enums.contains(tokenStream.get(i).spelling)) {
				tokenStream.get(i).type = TokenType.ENUMLIT;
			}
		}
		
		accept(TokenType.RBRACE);
		
		EnumTypedef def = new EnumTypedef(path, enums, source);
		enumIds.add(new Pair<NamespacePath, EnumTypedef>(path, def));
		
		return def;
	}
	
	public NamespacePath buildPath(String id) {
		NamespacePath path = new NamespacePath(new ArrayList());
		for (int i = 0; i < this.namespaces.size(); i++) {
			path.path.addAll(this.namespaces.get(i).path);
		}
		path.path.add(id);
		return path;
	}
	
	public NamespacePath buildPath() {
		NamespacePath path = new NamespacePath(new ArrayList());
		for (int i = 0; i < this.namespaces.size(); i++) {
			path.path.addAll(this.namespaces.get(i).path);
		}
		return path;
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
			else {
				this.progress.abort();
				throw new PARSE_EXCEPTION(source, TokenType.IDENTIFIER);
			}
			
			return new CompileDirective(dir, source);
		}
		else {
			this.progress.abort();
			throw new PARSE_EXCEPTION(source, TokenType.INCLUDE, TokenType.IDENTIFIER);
		}
	}
	
	protected Function parseFunction(TYPE returnType, Token identifier, MODIFIER mod) throws PARSE_EXCEPTION {
		this.scopes.push(new ArrayList());
		
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
		
		boolean signals = false;
		List<TYPE> signalsTypes = new ArrayList();
		if (current.type == TokenType.SIGNALS) {
			signals = true;
			accept();
			while (current.type != TokenType.LBRACE) {
				signalsTypes.add(this.parseType());
				if (current.type == TokenType.COMMA) {
					accept();
				}
			}
		}
		
		List<Statement> body = this.parseCompoundStatement(true);
		
		NamespacePath path = this.buildPath(identifier.spelling);
		Function f = new Function(returnType, path, proviso, parameters, signals, signalsTypes, body, mod, identifier.source);
		this.functions.add(new Pair<NamespacePath, Function>(path, f));
		
		this.scopes.pop();
		return f;
	}
	
	protected Declaration parseParameterDeclaration() throws PARSE_EXCEPTION {
		if (current.type == TokenType.FUNC) {
			Source source = accept().getSource();
			
			List<TYPE> proviso = new ArrayList();
			List<TYPE> types = new ArrayList();
			
			TYPE ret = null;
			
			boolean anonymous = false;
			
			/* Non-anonymous */
			if (current.type != TokenType.IDENTIFIER) {
				if (current.type == TokenType.LPAREN || current.type == TokenType.CMPLT) {
					if (current.type == TokenType.CMPLT) {
						proviso = this.parseProviso();
					}
					
					/* Params in braces */
					accept(TokenType.LPAREN);
					
					while (current.type != TokenType.RPAREN) {
						types.add(this.parseType());
						
						if (current.type == TokenType.COMMA) {
							accept();
						}
						else {
							break;
						}
					}
					
					accept(TokenType.RPAREN);
				}
				else if (current.type != TokenType.UNION_ACCESS) {
					/* Only one param, no braces */
					types.add(this.parseType());
				}
				
				accept(TokenType.UNION_ACCESS);
				
				ret = this.parseType();
			}
			else {
				anonymous = true;
			}
			
			Token id = accept(TokenType.IDENTIFIER);
			List<String> path = new ArrayList();
			path.add(id.spelling);
			
			if (anonymous) {
				/* Anonymous call, pass null function */
				Declaration d = new Declaration(new NamespacePath(id.spelling), new FUNC(null, proviso), null, source);
				this.scopes.peek().add(d);
				return d;
			}
			else {
				List<Declaration> params = new ArrayList();
				int c = 0;
				for (TYPE t : types) {
					params.add(new Declaration(new NamespacePath("param" + c++), t, null, source));
				}
				
				/* Wrap parsed function head in function object, wrap created head in declaration */
				Function lambda = new Function(ret, new NamespacePath(path, PATH_TERMINATION.UNKNOWN), new ArrayList(), params, false, new ArrayList(), new ArrayList(), MODIFIER.SHARED, source);
				lambda.isLambdaHead = true;
				
				Declaration d = new Declaration(new NamespacePath(id.spelling), new FUNC(lambda, proviso), null, source);
				this.scopes.peek().add(d);
				return d;
			}
		}
		else {
			TYPE type = this.parseType();
			Token id = accept(TokenType.IDENTIFIER);
			Declaration d = new Declaration(new NamespacePath(id.spelling), type, MODIFIER.SHARED, id.getSource());
			this.scopes.peek().add(d);
			return d;
		}
	}
	
	protected Statement parseStatement() throws PARSE_EXCEPTION {
		/* Convert next token */
		if (this.activeProvisos.contains(current.spelling)) {
			current.type = TokenType.PROVISO;
		}
		
		Token modT = (current.type.group == TokenGroup.MODIFIER)? current : null;
		MODIFIER mod = this.parseModifier();
		
		boolean functionCheck = current.type == TokenType.NAMESPACE_IDENTIFIER || current.type == TokenType.IDENTIFIER;
		for (int i = 0; i < this.tokenStream.size(); i += 3) {
			if (tokenStream.get(i).type == TokenType.LPAREN || tokenStream.get(i).type == TokenType.CMPLT) {
				if (tokenStream.get(i + 1).type == TokenType.CMPLE) functionCheck = false;
				break;
			}
			functionCheck &= tokenStream.get(i).type == TokenType.COLON;
			functionCheck &= tokenStream.get(i + 1).type == TokenType.COLON;
			
			Token t = tokenStream.get(i + 2);
			if (t.type == TokenType.NAMESPACE_IDENTIFIER) {
				continue;
			}
			else if (t.type == TokenType.STRUCTID) {
				/* Found Struct ID, Must be a structure init */
				functionCheck = false;
				break;
			}
			else if (t.type != TokenType.IDENTIFIER) {
				functionCheck = false;
				break;
			}
		}
		
		boolean decCheck = current.type.group == TokenGroup.TYPE || current.type == TokenType.NAMESPACE_IDENTIFIER || current.type == TokenType.ENUMID;
		for (int i = 0; i < this.tokenStream.size(); i += 3) {
			if (tokenStream.get(i).type == TokenType.IDENTIFIER || 
				tokenStream.get(i).type == TokenType.ENUMID || 
				tokenStream.get(i).type == TokenType.LBRACKET || 
				tokenStream.get(i).type == TokenType.MUL ||
				tokenStream.get(i).type == TokenType.CMPLT) {
				break;
			}
			
			decCheck &= tokenStream.get(i).type == TokenType.COLON;
			decCheck &= tokenStream.get(i + 1).type == TokenType.COLON;
			
			Token t = tokenStream.get(i + 2);
			if (t.type == TokenType.NAMESPACE_IDENTIFIER) {
				continue;
			}
			else if (t.type == TokenType.STRUCTID) {
				break;
			}
			else if (t.type == TokenType.ENUMID) {
				break;
			}
			else {
				decCheck = false;
				break;
			}
		}
		
		if (decCheck) {
			return this.parseDeclaration(mod);
		}
		else {
			if (modT != null) {
				throw new PARSE_EXCEPTION(modT.source, modT.type, 
						TokenType.TYPE, TokenType.RETURN, TokenType.WHILE, 
						TokenType.DO, TokenType.FOR, TokenType.BREAK, 
						TokenType.CONTINUE, TokenType.SWITCH, TokenType.IDENTIFIER, 
						TokenType.IF);
			}
			
			else if (current.type == TokenType.COMMENT) {
				return this.parseComment();
			}
			else if (functionCheck) {
				return this.parseFunctionCall();
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
			else if (current.type == TokenType.IDENTIFIER || current.type == TokenType.MUL || current.type == TokenType.NAMESPACE_IDENTIFIER) {
				return this.parseAssignment(true);
			}
			else if (current.type == TokenType.IF) {
				return this.parseIf();
			}
			else if (current.type == TokenType.TRY) {
				return this.parseTry();
			}
			else if (current.type == TokenType.SIGNAL) {
				return this.parseSignal();
			}
			else {
				this.progress.abort();
				throw new PARSE_EXCEPTION(current.source, current.type, 
				TokenType.TYPE, TokenType.RETURN, TokenType.WHILE, 
				TokenType.DO, TokenType.FOR, TokenType.BREAK, 
				TokenType.CONTINUE, TokenType.SWITCH, TokenType.IDENTIFIER, 
				TokenType.IF);
			}
		}
	}
	
	protected FunctionCall parseFunctionCall() throws PARSE_EXCEPTION {
		Source source = current.getSource();
		
		NamespacePath path = this.parseNamespacePath();
		
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
		
		if (path.getLast().equals("free")) {
			CompilerDriver.driver.referencedLibaries.add("lib/mem/free.sn");
		}
		
		return new FunctionCall(path, provisos, params, source);
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
	
	protected TryStatement parseTry() throws PARSE_EXCEPTION {
		Source source = current.getSource();
		accept(TokenType.TRY);
		
		List<Statement> body = this.parseCompoundStatement(true);
		
		List<WatchStatement> watchpoints = new ArrayList();
		while (current.type == TokenType.WATCH) {
			watchpoints.add(this.parseWatch());
		}
		
		return new TryStatement(body, watchpoints, source);
	}
	
	protected WatchStatement parseWatch() throws PARSE_EXCEPTION {
		this.scopes.push(new ArrayList());
		
		Source source = current.getSource();
		accept(TokenType.WATCH);
		
		accept(TokenType.LPAREN);
		
		Declaration watched = this.parseParameterDeclaration();
		this.scopes.peek().add(watched);
		
		accept(TokenType.RPAREN);
		
		List<Statement> body = this.parseCompoundStatement(false);
		
		this.scopes.pop();
		return new WatchStatement(body, watched, source);
	}
	
	protected SignalStatement parseSignal() throws PARSE_EXCEPTION {
		Source source = current.getSource();
		
		accept(TokenType.SIGNAL);
		
		Expression ex0 = this.parseExpression();
		
		accept(TokenType.SEMICOLON);
		
		/* Signal Statement will transform in resv and assign statement */
		//CompilerDriver.heap_referenced = true;
		//CompilerDriver.driver.referencedLibaries.add("lib/mem/resv.sn");
		
		return new SignalStatement(ex0, source);
	}
	
	protected ForStatement parseFor() throws PARSE_EXCEPTION {
		this.scopes.push(new ArrayList());
		
		Source source = accept(TokenType.FOR).getSource();
		accept(TokenType.LPAREN);
		
		/* Accepts semicolon */
		Declaration iterator = this.parseDeclaration(MODIFIER.SHARED);
		this.scopes.peek().add(iterator);
		
		Expression condition = this.parseExpression();
		accept(TokenType.SEMICOLON);
		
		/* Dont accept semicolon */
		Statement increment = this.parseAssignment(false);
		
		accept(TokenType.RPAREN);
		
		List<Statement> body = this.parseCompoundStatement(false);
		
		this.scopes.pop();
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
		/* Check if tokens ahead are a struct select */
		boolean structSelectCheck = current.type == TokenType.IDENTIFIER;
		for (int i = 1; i < this.tokenStream.size(); i += 2) {
			if ((tokenStream.get(i - 1).type == TokenType.INCR || tokenStream.get(i - 1).type == TokenType.DECR) && i > 1) break;
			else if (tokenStream.get(i - 1).type == TokenType.DOT || tokenStream.get(i - 1).type == TokenType.UNION_ACCESS) {
				structSelectCheck &= tokenStream.get(i).type == TokenType.IDENTIFIER;
			}
			else {
				structSelectCheck = false;
				break;
			}
		}
		
		boolean increment = current.type == TokenType.IDENTIFIER || current.type == TokenType.NAMESPACE_IDENTIFIER;
		for (int i = 0; i < this.tokenStream.size() - 3; i += 3) {
			if (tokenStream.get(i).type == TokenType.INCR || tokenStream.get(i).type == TokenType.DECR) break;
			
			increment &= tokenStream.get(i).type == TokenType.COLON;
			increment &= tokenStream.get(i + 1).type == TokenType.COLON;
			
			increment &= tokenStream.get(i + 2).type == TokenType.IDENTIFIER || tokenStream.get(i + 2).type == TokenType.NAMESPACE_IDENTIFIER;
		}
		
		Source source = current.getSource();
		
		if (increment || structSelectCheck) {
			
			if (structSelectCheck) {
				Expression select = this.parseStructSelect();
				
				WRITEBACK idWb = (current.type == TokenType.INCR)? WRITEBACK.INCR : WRITEBACK.DECR;
				accept();
				if (acceptSemicolon) accept(TokenType.SEMICOLON);
			
				return new AssignWriteback(new StructSelectWriteback(idWb, select, source), source);
			}
			else {
				NamespacePath path = this.parseNamespacePath();
				
				WRITEBACK idWb = (current.type == TokenType.INCR)? WRITEBACK.INCR : WRITEBACK.DECR;
				
				accept();
				if (acceptSemicolon) accept(TokenType.SEMICOLON);
				
				return new AssignWriteback(new IDRefWriteback(idWb, new IDRef(path, source), source), source);
			}
		}
		else {
			LhsId target = this.parseLhsIdentifer();
			
			/*
			 * Special case where it is impossible to determine that this is in fact a INCR/DECR writeback
			 * operation, needs to be handeled like this. This occurs for example in an assignment:
			 * 
			 * x [0].value++;
			 * 
			 * because of the array select the lookahead cant determine what lies ahead. This is handeled by
			 * extracting the struct select from the target lhs and reformatting into an AssignWriteback
			 * with a StructSelectWriteback as payload.
			 */
			if (current.type == TokenType.INCR || current.type == TokenType.DECR) {
				StructSelectLhsId lhs = (StructSelectLhsId) target;
				
				WRITEBACK idWb = (current.type == TokenType.INCR)? WRITEBACK.INCR : WRITEBACK.DECR;
				accept();
				if (acceptSemicolon) accept(TokenType.SEMICOLON);
			
				return new AssignWriteback(new StructSelectWriteback(idWb, lhs.select, source), source);
			}
			
			/*
			 * Normal behaviour.
			 */
			ASSIGN_ARITH arith = this.parseAssignOperator();
			Expression value = this.parseExpression();
			if (acceptSemicolon) accept(TokenType.SEMICOLON);
			return new Assignment(arith, target, value, target.getSource());
		}
	}
	
	protected ASSIGN_ARITH parseAssignOperator() throws PARSE_EXCEPTION {
		/* None */
		if (current.type == TokenType.LET) {
			accept();
			return ASSIGN_ARITH.NONE;
		}
		/* Arithmetic Operation */
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
			CompilerDriver.driver.referencedLibaries.add("lib/op/__op_div.sn");
			return ASSIGN_ARITH.DIV_ASSIGN;
		}
		else if (current.type == TokenType.MOD) {
			accept();
			accept(TokenType.LET);
			CompilerDriver.driver.referencedLibaries.add("lib/op/__op_mod.sn");
			return ASSIGN_ARITH.MOD_ASSIGN;
		}
		/* Bitwise Operation */
		else if (current.type == TokenType.ADDROF) {
			accept();
			accept(TokenType.LET);
			return ASSIGN_ARITH.BIT_AND_ASSIGN;
		}
		else if (current.type == TokenType.BITOR) {
			accept();
			accept(TokenType.LET);
			return ASSIGN_ARITH.BIT_ORR_ASSIGN;
		}
		else if (current.type == TokenType.XOR) {
			accept();
			accept(TokenType.LET);
			return ASSIGN_ARITH.BIT_XOR_ASSIGN;
		}
		/* Boolean logic */
		else if (current.type == TokenType.AND) {
			accept();
			accept(TokenType.LET);
			return ASSIGN_ARITH.AND_ASSIGN;
		}
		else if (current.type == TokenType.OR) {
			accept();
			accept(TokenType.LET);
			return ASSIGN_ARITH.ORR_ASSIGN;
		}
		/* Shifts */
		else if (current.type == TokenType.CMPLT && this.tokenStream.get(0).type == TokenType.CMPLE) {
			accept();
			accept();
			return ASSIGN_ARITH.LSL_ASSIGN;
		}
		else if (current.type == TokenType.CMPGT && this.tokenStream.get(0).type == TokenType.CMPGE) {
			accept();
			accept();
			return ASSIGN_ARITH.LSR_ASSIGN;
		}
		else if (current.type == TokenType.IDENTIFIER) {
			this.progress.abort();
			throw new SNIPS_EXCEPTION("Got '" + current.spelling + "', check for misspelled types, " + current.getSource().getSourceMarker());
		}
		else {
			this.progress.abort();
			throw new PARSE_EXCEPTION(current.source, current.type, TokenType.LET);
		}
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
				this.progress.abort();
				throw new PARSE_EXCEPTION(current.source, current.type, TokenType.IDENTIFIER);
			}
		}
	}
	
	protected Declaration parseGlobalDeclaration(TYPE type, Token identifier, MODIFIER mod) throws PARSE_EXCEPTION {
		Expression value = null;
		
		if (current.type == TokenType.LET) {
			accept();
			value = this.parseExpression();
		}
		
		accept(TokenType.SEMICOLON);
		Declaration d = new Declaration(new NamespacePath(identifier.spelling), type, value, mod, identifier.source);
		this.scopes.peek().add(d);
		return d;
	}
	
	protected Declaration parseDeclaration(MODIFIER mod) throws PARSE_EXCEPTION {
		Source source = current.getSource();
		TYPE type = this.parseType();
		
		Token id = accept(TokenType.IDENTIFIER);
		
		Expression value = null;
		if (current.type == TokenType.LET) {
			accept();
			value = this.parseExpression();
		}
		
		accept(TokenType.SEMICOLON);
		Declaration d = new Declaration(new NamespacePath(id.spelling), type, value, mod, source);
		this.scopes.peek().add(d);
		return d;
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
		boolean structInitCheck = current.type == TokenType.IDENTIFIER || current.type == TokenType.NAMESPACE_IDENTIFIER;
		for (int i = 0; i < this.tokenStream.size(); i += 3) {
			structInitCheck &= tokenStream.get(i).type == TokenType.COLON;
			structInitCheck &= tokenStream.get(i + 1).type == TokenType.COLON;
			if (tokenStream.get(i + 2).type == TokenType.LPAREN) {
				break;
			}
			else {
				Token t = tokenStream.get(i + 2);
				
				if (t.type == TokenType.STRUCTID) {
					/* Found Struct ID, Must be a structure init */
					break;
				}
				else if (t.type != TokenType.IDENTIFIER && t.type != TokenType.NAMESPACE_IDENTIFIER && t.type != TokenType.STRUCTID) {
					structInitCheck = false;
					break;
				}
			}
		}
		
		if (current.type == TokenType.STRUCTID || structInitCheck) {
			Source source = current.getSource();
			
			TYPE type = this.parseType();
			
			if (!(type instanceof STRUCT)) {
				/* Something is definetly wrong at this point */
				this.progress.abort();
				throw new SNIPS_EXCEPTION(new CTX_EXCEPTION(source, "Expected STRUCT type, got " + type.typeString()).getMessage());
			}
			
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
			return new StructureInit((STRUCT) type, elements, source);
		}
		else return this.parseArrayInit();
	}
	
	protected Expression parseArrayInit() throws PARSE_EXCEPTION {
		if (current.type == TokenType.LBRACE || current.type == TokenType.LBRACKET) {
			boolean dontCare = current.type == TokenType.LBRACKET;
			
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
			
			if (dontCare) accept(TokenType.RBRACKET);
			else accept(TokenType.RBRACE);
			
			return new ArrayInit(elements, dontCare, source);
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
				left = new InlineCall(new NamespacePath("__op_div"), new ArrayList(), params, source);
				CompilerDriver.driver.referencedLibaries.add("lib/op/__op_div.sn");
			}
			else {
				Source source = accept().getSource();
				List<Expression> params = new ArrayList();
				params.add(left);
				params.add(this.parseSizeOf());
				
				/* Create inline call to libary function, add mod operator to referenced libaries */
				left = new InlineCall(new NamespacePath("__op_mod"), new ArrayList(), params, source);
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
			
			/* Convert next token */
			if (this.activeProvisos.contains(current.spelling)) {
				current.type = TokenType.PROVISO;
			}
			
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
			
			if (current.type == TokenType.LPAREN) {
				accept();
				Expression e = this.parseExpression();
				accept(TokenType.RPAREN);
				addr = new Deref(e, source);
			}
			else addr = new Deref(this.parseDeref(), source);
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
		
		while (this.castCheck()) {
			Source source = accept().getSource();
			TYPE castType = this.parseType();
			accept(TokenType.RPAREN);
			
			Expression cast0 = this.parseNot();
			
			cast = new TypeCast(cast0, castType, source);
		}
		
		if (cast == null) cast = this.parseNot();
		return cast;
	}
	
	public boolean castCheck() {
		boolean castCheck = current.type == TokenType.LPAREN;
		if (!castCheck) return false;
		
		for (int i = 2; i < tokenStream.size(); i += 3) {
			/* 
			 * First type token, from here only allowed token are RPAREN and all other type related
			 * tokens like [, ], *. If a colon is seen, the current structure cannot be a cast.
			 */
			if (tokenStream.get(i - 2).type.group == TokenGroup.TYPE) {
				for (int a = i - 1; a < tokenStream.size(); a++) {
					if (tokenStream.get(a).type == TokenType.RPAREN) return true;
					else if (tokenStream.get(a).type == TokenType.COLON) {
						return false;
					}
				}
				return false;
			}
			else {
				castCheck &= tokenStream.get(i - 2).type == TokenType.NAMESPACE_IDENTIFIER || tokenStream.get(i - 2).type == TokenType.IDENTIFIER; 
				castCheck &= tokenStream.get(i - 1).type == TokenType.COLON;
				castCheck &= tokenStream.get(i).type == TokenType.COLON;
				
				if (!castCheck) break;
			}
		}
		return castCheck;
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
		
		if (not == null) not = this.parseIncrDecr();
		return not;
	}
	
	protected Expression parseIncrDecr() throws PARSE_EXCEPTION {
		Expression ref = this.parseStructSelect();
		
		while (current.type == TokenType.INCR || current.type == TokenType.DECR) {
			Source source = current.getSource();
			if (current.type == TokenType.INCR) {
				accept();
				if (ref instanceof IDRef)
					ref = new IDRefWriteback(WRITEBACK.INCR, ref, source);
				else ref = new StructSelectWriteback(WRITEBACK.INCR, ref, source);
			}
			else {
				accept();
				if (ref instanceof IDRef)
					ref = new IDRefWriteback(WRITEBACK.DECR, ref, source);
				else ref = new StructSelectWriteback(WRITEBACK.DECR, ref, source);
			}
		}
		
		return ref;
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
		Expression ref = this.parseAtom();
		
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
	
	protected Expression parseAtom() throws PARSE_EXCEPTION {
		if (current.type == TokenType.LPAREN) {
			accept();
			Expression expression = this.parseExpression();
			accept(TokenType.RPAREN);
			return expression;
		}
		else if (current.type == TokenType.NULL) {
			Token id = accept();
			CompilerDriver.null_referenced = true;
			return new Atom(new NULL(), id, id.getSource());
		}
		else if (current.type == TokenType.IDENTIFIER || current.type == TokenType.ENUMID || current.type == TokenType.NAMESPACE_IDENTIFIER) {
			Source source = current.getSource();
			
			NamespacePath path = this.parseNamespacePath();
			
			if (current.type == TokenType.COLON && tokenStream.get(0).type == TokenType.COLON) {
				this.progress.abort();
				throw new SNIPS_EXCEPTION("Unknown namespace '" + path.build() + "', " + source.getSourceMarker());
			}
			
			/* Convert next token */
			if (this.activeProvisos.contains(this.tokenStream.get(0).spelling)) {
				this.tokenStream.get(0).type = TokenType.PROVISO;
			}
			
			if (current.type == TokenType.LPAREN || (current.type == TokenType.CMPLT && tokenStream.get(0).type.group == TokenGroup.TYPE)) {
				List<TYPE> proviso = this.parseProviso();
				
				/* Predicate with proviso */
				if (current.type != TokenType.LPAREN) {
					/* Predicate without proviso */
					return new FunctionRef(proviso, path, source);
				}
				else {
					/* Inline Call */
					accept(TokenType.LPAREN);
					
					List<Expression> parameters = new ArrayList();
					while (current.type != TokenType.RPAREN) {
						parameters.add(this.parseExpression());
						if (current.type != TokenType.COMMA) break;
						accept(TokenType.COMMA);
					}
					accept(TokenType.RPAREN);
					
					if (path.getPath().get(0).equals("resv")) {
						CompilerDriver.heap_referenced = true;
						CompilerDriver.driver.referencedLibaries.add("lib/mem/resv.sn");
					}
					else if (path.getPath().get(0).equals("hsize")) {
						CompilerDriver.driver.referencedLibaries.add("lib/mem/hsize.sn");
					}
					
					return new InlineCall(path, proviso, parameters, source);
				}
			}
			else if (current.type == TokenType.DOT && (tokenStream.get(0).type == TokenType.ENUMLIT || path.termination == PATH_TERMINATION.ENUM)) {
				accept(TokenType.DOT);
				
				if (current.type != TokenType.ENUMLIT) {
					this.progress.abort();
					throw new SNIPS_EXCEPTION("The expression '" + current.spelling + "' is not a known field of the enum " + path.build() + ", " + source.getSourceMarker());
				}
				
				/* Actual enum field value */
				Token value = accept(TokenType.ENUMLIT);
				
				/* Get enum type mapped to this value */
				EnumTypedef def = this.getEnumTypedef(path, source);
				
				if (def == null) {
					this.progress.abort();
					throw new SNIPS_EXCEPTION("Unknown enum type: " + path.build() + ", " + source.getSourceMarker());
				}
				
				return new Atom(def.getEnumField(value.spelling, source), value, source);
			}
			else {
				/* Find the function that may match this path and act as a predicate */
				Function lambda = this.findFunction(path);
				
				/* 
				 * Check if a variable exists that matches the path, if yes, this variable
				 * reference is treated with a higher priority.
				 */
				for (int i = this.scopes.size() - 1; i >= 0; i--) {
					List<Declaration> scope = this.scopes.get(i);
					for (int a = 0; a < scope.size(); a++) {
						if (scope.get(a).path.build().equals(path.build())) {
							/* Path referres to a variable, set lambda to null */
							lambda = null;
						}
					}
				}
				
				if (lambda != null) {
					/* Predicate without proviso */
					return new FunctionRef(new ArrayList(), lambda, source);
				}
				else {
					/* Identifier Reference */
					return new IDRef(path, source);
				}
			}
		}
		else if (current.type == TokenType.DIRECTIVE) {
			/* Direct Reg Targeting */
			Source source = accept().getSource();
			Token reg = accept(TokenType.IDENTIFIER);
			return new RegisterAtom(reg, source);
		}
		else if (current.type == TokenType.INTLIT) {
			Token token = accept();
			return new Atom(new INT(token.spelling), token, token.source);
		}
		else if (current.type == TokenType.CHARLIT) {
			Token token = accept();
			return new Atom(new CHAR(token.spelling), token, token.source);
		}
		else if (current.type == TokenType.STRINGLIT) {
			Token token = accept();
			List<Expression> charAtoms = new ArrayList();
			String [] sp = token.spelling.split("");
			for (int i = 0; i < sp.length; i++) {
				charAtoms.add(new Atom(new CHAR(sp [i]), new Token(TokenType.CHARLIT, token.source, sp [i]), token.source));
			}
			charAtoms.add(new Atom(new CHAR(null), new Token(TokenType.CHARLIT, token.source, null), token.source));
			return new ArrayInit(charAtoms, false, token.getSource());
		}
		else if (current.type == TokenType.BOOLLIT) {
			Token token = accept();
			return new Atom(new BOOL(token.spelling), token, token.source);
		}
		else {
			/* 
			 * Set current to anchor curr0.
			 * If the anchor is null, the parser has not been at this location in the 
			 * previous iteration. If the anchor is not null and equals the current token,
			 * the parser has been at this location with the current token, meaning its stuck.
			 * In this case throw a exception. In the last case, set the anchor to the current
			 * and try again.
			 */
			if (curr0 == null) {
				curr0 = current;
				return this.parseExpression();
			}
			else {
				if (curr0 == current) {
					this.progress.abort();
					throw new PARSE_EXCEPTION(current.source, current.type, TokenType.LPAREN, TokenType.IDENTIFIER, TokenType.INTLIT);
				}
				else {
					curr0 = current;
					return this.parseExpression();
				}
			}
		}
	}
	
	public Function findFunction(NamespacePath path) {
		Function lambda = null;
		
		for (Pair<NamespacePath, Function> p : this.functions) {
			if (p.first.build().equals(path.build())) {
				return p.second;
			}
		}
		
		if (lambda == null) {
			if (path.path.size() == 1) {
				List<Function> f0 = new ArrayList();
				
				for (Pair<NamespacePath, Function> p : this.functions) {
					if (p.first.getLast().equals(path.getLast())) {
						f0.add(p.second);
					}
				}
				
				/* Return if there is only one result */
				if (f0.size() == 1) return f0.get(0);
			}
		}
		
		return null;
	}
	
	/* Anchor used for expression parsing. */
	private Token curr0 = null;
	
	public StructTypedef getStructTypedef(NamespacePath path, Source source) {
		for (Pair<NamespacePath, StructTypedef> p : this.structIds) {
			if (p.getFirst().build().equals(path.build())) {
				return p.getSecond();
			}
		}
		
		List<StructTypedef> defs = new ArrayList();
		for (Pair<NamespacePath, StructTypedef> p : this.structIds) {
			if (p.getFirst().getLast().equals(path.getLast())) {
				defs.add(p.getSecond());
			}
		}
		
		if (defs.isEmpty()) return null;
		else if (defs.size() == 1 && path.path.size() == 1) {
			return defs.get(0);
		}
		else {
			String s = "";
			for (StructTypedef def : defs) s += def.path.build() + ", ";
			s = s.substring(0, s.length() - 2);
			this.progress.abort();
			throw new SNIPS_EXCEPTION("Multiple matches for struct type '" + path.build() + "': " + s + ". Ensure namespace path is explicit and correct, " + source.getSourceMarker());
		}
	}
	
	public EnumTypedef getEnumTypedef(NamespacePath path, Source source) {
		for (Pair<NamespacePath, EnumTypedef> p : this.enumIds) {
			if (p.getFirst().build().equals(path.build())) {
				return p.getSecond();
			}
		}
		
		List<EnumTypedef> defs = new ArrayList();
		for (Pair<NamespacePath, EnumTypedef> p : this.enumIds) {
			if (p.getFirst().getLast().equals(path.getLast())) {
				defs.add(p.getSecond());
			}
		}
		
		if (defs.isEmpty()) return null;
		else if (defs.size() == 1 && path.path.size() == 1) {
			return defs.get(0);
		}
		else {
			String s = "";
			for (EnumTypedef def : defs) s += def.path.build() + ", ";
			s = s.substring(0, s.length() - 2);
			this.progress.abort();
			throw new SNIPS_EXCEPTION("Multiple matches for enum type '" + path.build() + "': " + s + ". Ensure namespace path is explicit and correct, " + source.getSourceMarker());
		}
	}
	
	public boolean containsStructTypedef(String token) {
		for (Pair<NamespacePath, StructTypedef> p : this.structIds) {
			if (p.getFirst().getLast().equals(token)) {
				return true;
			}
		}
		return false;
	}
	
	public boolean containsEnumTypedef(String token) {
		for (Pair<NamespacePath, EnumTypedef> p : this.enumIds) {
			if (p.getFirst().getLast().equals(token)) {
				return true;
			}
		}
		return false;
	}

	protected TYPE parseType() throws PARSE_EXCEPTION {
		TYPE type = null;
		Token token = null;
		
		if (current.type == TokenType.FUNC) token = accept();
		else if (current.type == TokenType.IDENTIFIER) token = accept();
		else if (current.type == TokenType.NAMESPACE_IDENTIFIER) token = accept();
		else token = accept(TokenGroup.TYPE);
		
		StructTypedef def = null;
		EnumTypedef enu = null;
		NamespacePath path = null;
		
		if (this.containsStructTypedef(token.spelling) || this.containsEnumTypedef(token.spelling) || 
				(current.type == TokenType.COLON && 
				tokenStream.get(0).type == TokenType.COLON && 
				tokenStream.get(1).type != TokenType.LPAREN)) {
			
			path = this.parseNamespacePath(token);
			
			/* Search with relative path */
			def = this.getStructTypedef(path, token.getSource());
			
			/* Nothing found, attempt to convert to current absolut path and try again */
			if (def == null) {
				path.path.addAll(0, this.buildPath().path);
				def = this.getStructTypedef(path, token.getSource());
			}
			
			/* Search with relative path */
			enu = this.getEnumTypedef(path, token.getSource());
			
			/* Nothing found, attempt to convert to current absolut path and try again */
			if (enu == null) {
				path.path.addAll(0, this.buildPath().path);
				enu = this.getEnumTypedef(path, token.getSource());
			}
			
			/* Nothing found, error */
			if (enu == null && def == null) {
				this.progress.abort();
				throw new SNIPS_EXCEPTION("Unknown struct or enum type '" + path.build() + "', " + token.getSource().getSourceMarker());
			}
		}
		
		if (def != null) {
			/* 
			 * Create a reference to the SSOT, since the SSOT may be still in parsing. Create 
			 * copy at the end of parsing when SSOT is definetley finished. Add type to list toClone
			 * for to indicate that this type needs cloning after parsing ends.
			 */
			type = def.struct;
		
			List<TYPE> proviso = this.parseProviso();
			
			STRUCT s = def.struct.clone();
			s.typedef = def;
			s.proviso = proviso;
		
			type = s;
			
			this.toClone.add(new Pair<TYPE, List<TYPE>>(type, proviso));
		}
		else if (enu != null) {
			/* Enum def was found, set reference to default enum field */
			type = enu.enumType;
		}
		else {
			type = TYPE.fromToken(token, buffered);
			
			if (type instanceof PROVISO && !this.activeProvisos.contains(token.spelling)) 
				this.activeProvisos.add(token.spelling);
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
			
			while (!dimensions.isEmpty()) 
				type = new ARRAY(type, dimensions.pop());
			
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
				if (current.type == TokenType.COMMA) 
					accept();
				else break;
			}
			
			accept(TokenType.CMPGT);
		}
		
		return pro;
	}
	
	protected NamespacePath parseNamespacePath() throws PARSE_EXCEPTION {
		Token token;
		
		if (current.type == TokenType.STRUCTID) {
			return new NamespacePath(accept().spelling, PATH_TERMINATION.STRUCT);
		}
		else if (current.type == TokenType.ENUMID) {
			return new NamespacePath(accept().spelling, PATH_TERMINATION.ENUM);
		}
		else if (current.type == TokenType.IDENTIFIER) {
			return new NamespacePath(accept().spelling);
		}
		else token = accept(TokenType.NAMESPACE_IDENTIFIER);
		
		return this.parseNamespacePath(token);
	}
	
	protected NamespacePath parseNamespacePath(Token first) throws PARSE_EXCEPTION {
		List<String> ids = new ArrayList();
		ids.add(first.spelling);
		
		PATH_TERMINATION term = PATH_TERMINATION.UNKNOWN;
		
		while (current.type == TokenType.COLON && 
				tokenStream.get(0).type == TokenType.COLON && 
				tokenStream.get(1).type != TokenType.LPAREN) {
			accept();
			accept(TokenType.COLON);
			
			if (current.type == TokenType.NAMESPACE_IDENTIFIER) {
				ids.add(accept().spelling);
			}
			else {
				if (current.type == TokenType.STRUCTID) {
					term = PATH_TERMINATION.STRUCT;
					ids.add(accept().spelling);
				}
				else if (current.type == TokenType.ENUMID) {
					term = PATH_TERMINATION.ENUM;
					ids.add(accept().spelling);
				}
				else ids.add(accept(TokenType.IDENTIFIER).spelling);
				
				if (current.type != TokenType.COLON) {
					break;
				}
			}
		}
		
		assert(!ids.isEmpty());
		
		return new NamespacePath(ids, term);
	}
	
	protected List<Statement> parseCompoundStatement(boolean forceBraces) throws PARSE_EXCEPTION {
		this.scopes.push(new ArrayList());
		
		List<Statement> body = new ArrayList();
		
		/* Compound Statement with braces */
		if (current.type == TokenType.LBRACE || forceBraces) {
			accept(TokenType.LBRACE);
			while (current.type != TokenType.RBRACE) 
				body.add(this.parseStatement());
			accept(TokenType.RBRACE);
		}
		/* Without braces, one statement only */
		else body.add(this.parseStatement());
		
		this.scopes.pop();
		return body;
	}
	
	protected MODIFIER parseModifier() {
		MODIFIER mod = MODIFIER.SHARED;
		
		if (current.type.group == TokenGroup.MODIFIER) {
			Token modT = accept();
			mod = this.resolve(modT);
		}
		
		return mod;
	}
	
	protected MODIFIER resolve(Token t) {
		if (t.type == TokenType.SHARED) return MODIFIER.SHARED;
		else if (t.type == TokenType.RESTRICTED) return MODIFIER.RESTRICTED;
		else return MODIFIER.EXCLUSIVE;
	}
	
}
