package Par;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Stack;
import java.util.stream.Collectors;

import CGen.Util.LabelUtil;
import Exc.CTEX_EXC;
import Exc.PARS_EXC;
import Exc.SNIPS_EXC;
import Imm.ASM.Util.Operands.RegOp;
import Imm.ASM.Util.Operands.RegOp.REG;
import Imm.AST.Function;
import Imm.AST.Namespace;
import Imm.AST.Program;
import Imm.AST.SyntaxElement;
import Imm.AST.Expression.AddressOf;
import Imm.AST.Expression.ArrayInit;
import Imm.AST.Expression.ArraySelect;
import Imm.AST.Expression.Atom;
import Imm.AST.Expression.Deref;
import Imm.AST.Expression.Expression;
import Imm.AST.Expression.FunctionRef;
import Imm.AST.Expression.IDOfExpression;
import Imm.AST.Expression.IDRef;
import Imm.AST.Expression.IDRefWriteback;
import Imm.AST.Expression.InlineCall;
import Imm.AST.Expression.InlineFunction;
import Imm.AST.Expression.OperatorExpression;
import Imm.AST.Expression.RegisterAtom;
import Imm.AST.Expression.SizeOfExpression;
import Imm.AST.Expression.SizeOfType;
import Imm.AST.Expression.StructSelect;
import Imm.AST.Expression.StructSelectWriteback;
import Imm.AST.Expression.StructureInit;
import Imm.AST.Expression.TempAtom;
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
import Imm.AST.Statement.DirectASMStatement;
import Imm.AST.Statement.DoWhileStatement;
import Imm.AST.Statement.ForEachStatement;
import Imm.AST.Statement.ForStatement;
import Imm.AST.Statement.FunctionCall;
import Imm.AST.Statement.IfStatement;
import Imm.AST.Statement.OperatorStatement;
import Imm.AST.Statement.ReturnStatement;
import Imm.AST.Statement.SignalStatement;
import Imm.AST.Statement.Statement;
import Imm.AST.Statement.SwitchStatement;
import Imm.AST.Statement.TryStatement;
import Imm.AST.Statement.WatchStatement;
import Imm.AST.Statement.WhileStatement;
import Imm.AST.Typedef.EnumTypedef;
import Imm.AST.Typedef.InterfaceTypedef;
import Imm.AST.Typedef.StructTypedef;
import Imm.TYPE.AUTO;
import Imm.TYPE.PROVISO;
import Imm.TYPE.TYPE;
import Imm.TYPE.COMPOSIT.ARRAY;
import Imm.TYPE.COMPOSIT.INTERFACE;
import Imm.TYPE.COMPOSIT.POINTER;
import Imm.TYPE.COMPOSIT.STRUCT;
import Imm.TYPE.PRIMITIVES.BOOL;
import Imm.TYPE.PRIMITIVES.CHAR;
import Imm.TYPE.PRIMITIVES.FUNC;
import Imm.TYPE.PRIMITIVES.INT;
import Imm.TYPE.PRIMITIVES.NULL;
import Imm.TYPE.PRIMITIVES.VOID;
import Par.Token.TokenType;
import Par.Token.TokenType.TokenGroup;
import Res.Const;
import Snips.CompilerDriver;
import Util.ASTDirective;
import Util.ASTDirective.DIRECTIVE;
import Util.MODIFIER;
import Util.NamespacePath;
import Util.NamespacePath.PATH_TERMINATION;
import Util.Pair;
import Util.Source;
import Util.Logging.LogPoint;
import Util.Logging.Message;
import Util.Logging.ProgressMessage;

/**
 * Parses an AST out of the token stream. The process is similar to this symbolic representation:
 * 
 * 	          --> www.reddit.com/r/ProgrammerHumor/comments/h83mqx/parser_be_like/ <--
 * 
 */
public class Parser {

	/** List of tokens that were created by the scanner */
	private List<Token> tokenStream;
	
	/** 
	 * Current token. The current token is not in the {@link #tokenStream}, 
	 * but is removed and set to the remove element.
	 */
	private Token current;
	
	/** Active Struct Ids */
	private List<Pair<NamespacePath, Function>> functions = new ArrayList();
	
	/** Active Struct Ids */
	private List<Pair<NamespacePath, StructTypedef>> structIds = new ArrayList();
	
	/** Active Interface Ids */
	private List<Pair<NamespacePath, InterfaceTypedef>> interfaceIds = new ArrayList();
	
	/** Active Enum Ids */
	private List<Pair<NamespacePath, EnumTypedef>> enumIds = new ArrayList();
	
	/** All active Provisos, like T, V */
	private List<String> activeProvisos = new ArrayList();
	
	/** The currently open namespaces */
	private Stack<NamespacePath> namespaces = new Stack();
	
	private Stack<List<Declaration>> scopes = new Stack();
	
	private Stack<List<ASTDirective>> bufferedAnnotations = new Stack();
	
	/** Generated warn messages that are flushed at the end */
	private List<Message> buffered = new ArrayList();
	
	private ProgressMessage progress;
	
	private int done = 0, toGo;
	
	public Parser(List tokens, ProgressMessage progress) throws SNIPS_EXC {
		if (tokens == null) {
			this.progress.abort();
			throw new SNIPS_EXC(Const.TOKENS_ARE_NULL);
		}
		tokenStream = tokens;
		
		this.progress = progress;
		
		this.toGo = tokens.size();
		
		current = tokenStream.get(0);
		tokenStream.remove(0);
	}
	
	/**
	 * Accept a token based on its type.
	 * @param types Accepted types
	 * @return The accepted Token.
	 * @throws PARS_EXC Thrown then the token does not have one of the given types.
	 */
	private Token accept(TokenType...types) throws PARS_EXC {
		/* Convert tokens dynamically based on the currently active provisos */
		if (this.activeProvisos.contains(current.spelling)) 
			current.type = TokenType.PROVISO;
		
		for (TokenType type : types) {
			if (current.type() == type) return accept();
		}
		
		this.progress.abort();
		throw new PARS_EXC(current.source, current.type(), types);
	}
	
	/**
	 * Accept a token based on its type.
	 * @param tokenType The type the token should match.
	 * @return The accepted Token.
	 * @throws PARS_EXC Thrown then the token does not have the given type.
	 */
	private Token accept(TokenType tokenType) throws PARS_EXC {
		/* Convert tokens dynamically based on the currently active provisos */
		if (this.activeProvisos.contains(current.spelling)) 
			current.type = TokenType.PROVISO;
		
		if (current.type() == tokenType) return accept();
		else {
			this.progress.abort();
			throw new PARS_EXC(current.source, current.type(), tokenType);
		}
	}
	
	/**
	 * Accept a token based on its token group.
	 * @param group The group the token should match.
	 * @return The accepted Token.
	 * @throws PARS_EXC Thrown when the token does not have the given token group.
	 */
	private Token accept(TokenGroup group) throws PARS_EXC {
		/* Convert tokens dynamically based on the currently active provisos */
		if (this.activeProvisos.contains(current.spelling)) 
			current.type = TokenType.PROVISO;
		
		if (current.type().group() == group)return accept();
		else {
			this.progress.abort();
			throw new PARS_EXC(current.source, current.type());
		}
	}
	
	/**
	 * Accept a token without any checks.
	 * @return The accepted token.
	 */
	private Token accept() {
		
		/* Store the current source, is used to give aproximation when crash occurs */
		//CompilerDriver.lastSource = current.source;
		
		/* Convert tokens dynamically based on the currently active provisos */
		if (this.activeProvisos.contains(current.spelling)) 
			current.type = TokenType.PROVISO;
		
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
	
	public SyntaxElement parse() throws PARS_EXC {
		Program p = parseProgram();
		if (this.progress != null) this.progress.incProgress(1);
		return p;
	}
	
	private Program parseProgram() throws PARS_EXC {
		this.scopes.push(new ArrayList());
		Source source = this.current.source;
		
		List<SyntaxElement> elements = this.parseProgramElements(TokenType.EOF);
		accept(TokenType.EOF);
		
		Program program = new Program(elements, source);
		
		for (Message m : buffered) m.flush();
		
		this.scopes.pop();
		return program;
	}
	
	private List<SyntaxElement> parseProgramElements(TokenType close) throws PARS_EXC {
		List<SyntaxElement> elements = new ArrayList();
		
		boolean push = true;
		while (this.current.type != close) {
			this.activeProvisos.clear();
			
			if (push) this.bufferedAnnotations.push(new ArrayList());
			push = true;
			
			SyntaxElement element = this.parseProgramElement();
			
			if (element != null) {
				element.activeAnnotations.addAll(this.bufferedAnnotations.pop());
				elements.add(element);
			}
			else push = false;
			
			if (element instanceof Function) {
				Function f = (Function) element;
				
				if (f.hasDirective(DIRECTIVE.OPERATOR)) {
					ASTDirective dir = f.getDirective(DIRECTIVE.OPERATOR);
					
					Optional<Entry<String, String>> first = dir.properties().entrySet().stream().findFirst();
					
					if (first.isPresent()) {
						/* 
						 * Transform all tokens that use the symbol of 
						 * the operator to operator tokens. 
						 */
						String symbol = first.get().getKey();
						
						for (int i = 0; i < this.tokenStream.size(); i++) {
							Token t = this.tokenStream.get(i);
							
							String spelling = t.spelling;
							if (spelling == null) 
								spelling = t.type().spelling();
							
							if (symbol.startsWith(spelling)) {
								String buffer = spelling;
								for (int a = i + 1; a < this.tokenStream.size(); a++) {
									if (buffer.length() > symbol.length()) break;
									else {
										if (buffer.equals(symbol)) {
											t.markedAsOperator = true;
											t.operatorSymbol = symbol;
											break;
										}
										else buffer += this.tokenStream.get(a).spelling;
									}
								}
							}
						}
					}
				}
			}
		}
		
		return elements;
	}
	
	private Namespace parseNamespace() throws PARS_EXC {
		Source source = accept(TokenType.NAMESPACE).source();
		
		NamespacePath path = this.parseNamespacePath();
		
		this.namespaces.push(path);
		
		accept(TokenType.LBRACE);
		
		List<SyntaxElement> elements = this.parseProgramElements(TokenType.RBRACE);
		
		this.namespaces.pop();
		
		accept(TokenType.RBRACE);
		
		return new Namespace(path, elements, source);
	}
	
	private ASTDirective parseASTAnnotation() throws PARS_EXC {
		accept(TokenType.DIRECTIVE);
		
		Token type = accept(TokenType.IDENTIFIER);
		DIRECTIVE type0 = null;
		
		try {
			type0 = DIRECTIVE.valueOf(type.spelling.toUpperCase());
		} catch (IllegalArgumentException e) {
			throw new SNIPS_EXC("Unknown directive: '" + type.spelling + "', " + type.source.getSourceMarker());
		}
		
		HashMap<String, String> arguments = new HashMap();
		
		while (current.source.row == type.source.row) {
			String key = accept().spelling;
			
			while (current.source.row == type.source.row && 
					!(current.type == TokenType.LET || current.type == TokenType.COMMA)) {
				key += accept().spelling;
			}
			
			String value = null;
			
			if (current.type == TokenType.LET) {
				accept(TokenType.LET);
				value = accept(TokenType.IDENTIFIER, TokenType.INTLIT).spelling;
			}
			
			if (arguments.containsKey(key.toLowerCase())) 
				throw new SNIPS_EXC("Found duplicate directive argument key: " + key + ", " + type.source.getSourceMarker());
			
			arguments.put(key, value);
			
			if (current.source.row == type.source.row && current.type == TokenType.COMMA) accept();
			else break;
		}
		
		return new ASTDirective(type0, arguments);
	}
	
	private SyntaxElement parseProgramElement() throws PARS_EXC {
		if (current.type == TokenType.COMMENT) {
			return this.parseComment();
		}
		else if (current.type == TokenType.DIRECTIVE) {
			this.bufferedAnnotations.peek().add(this.parseASTAnnotation());
			return null;
		}
		else if (current.type == TokenType.STRUCT || (current.type.group() == TokenGroup.MODIFIER && this.tokenStream.get(0).type == TokenType.STRUCT)) {
			return this.parseStructTypedef();
		}
		else if (current.type == TokenType.INTERFACE || (current.type.group() == TokenGroup.MODIFIER && this.tokenStream.get(0).type == TokenType.INTERFACE)) {
			return this.parseInterfaceTypedef();
		}
		else if (current.type == TokenType.ENUM || (current.type.group() == TokenGroup.MODIFIER && this.tokenStream.get(0).type == TokenType.ENUM)) {
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
				element = this.parseFunction(type, identifier, mod, false, false);
			}
			else {
				Declaration d = this.parseGlobalDeclaration(type, identifier, mod);
				this.scopes.peek().add(d);
				element = d;
			}
			
			return element;
		}
	}
	
	private Comment parseComment() throws PARS_EXC {
		Token comment = accept(TokenType.COMMENT);
		return new Comment(comment, comment.source());
	}
	
	private StructTypedef parseStructTypedef() throws PARS_EXC {
		
		MODIFIER mod = this.parseModifier();
		
		Source source = accept(TokenType.STRUCT).source();
		Token id = accept(TokenType.STRUCTID);
		
		List<TYPE> proviso = this.parseProviso();
		NamespacePath path = this.buildPath(id.spelling);
		
		StructTypedef ext = null;
		List<TYPE> extProviso = new ArrayList();
		List<Declaration> extendDecs = new ArrayList();
		
		List<INTERFACE> implemented = new ArrayList();
		
		if (current.type == TokenType.COLON) {
			accept();
			
			while (current.type != TokenType.LBRACE) {
				NamespacePath ext0 = this.parseNamespacePath();
				
				if (this.getStructTypedef(ext0, source) != null) {
					/* Attempt to find struct extension */
					
					ext = this.getStructTypedef(ext0, source);
					
					if (current.type == TokenType.CMPLT) 
						extProviso.addAll(this.parseProviso());
					
					/* Copy the extended fields */
					for (Declaration d : ext.getFields()) {
						Declaration c = d.clone();
						
						/* If number of provisos are not equal an exception will be thrown in CTX anyway */
						if (ext.proviso.size() == extProviso.size()) 
							/* Remap type of declaration to provided provisos */
							for (int i = 0; i < ext.proviso.size(); i++) {
								if (!(ext.proviso.get(i) instanceof PROVISO)) continue;
								PROVISO prov = (PROVISO) ext.proviso.get(i);
								c.setType(c.getType().remapProvisoName(prov.placeholderName, extProviso.get(i)));
							}
						
						extendDecs.add(c);
					}
					
					if (current.type == TokenType.COMMA)
						accept();
					else break;
				}
				else {
					/* Must be an implemented interface */
					
					InterfaceTypedef def = this.getInterfaceTypedef(ext0, source);
					
					if (def == null) {
						this.progress.abort();
						throw new SNIPS_EXC("Unknown extension: %s in " + source.getSourceMarker(), ext0.build());
					}
					
					implemented.add(new INTERFACE(def, this.parseProviso()));
					
					if (current.type == TokenType.COMMA)
						accept();
					else break;
				}
			}
		}
		
		/*
		 * Create Struct typedef here already, since struct may be linked and have a pointer
		 * to another instance of this struct. The struct definition needs to exist before
		 * such a declaration is parsed.
		 */
		StructTypedef def = new StructTypedef(path, proviso, new ArrayList(), new ArrayList(), ext, implemented, extProviso, mod, source);
		
		/*
		 * In preparation that this struct might be the implementation of a struct typedef
		 * that was included from the header, we already have to take action here to ensure
		 * references from here on out are correct. So, we check if there is already a typedef
		 * that has the same namespace path. If there is no such typedef, we use the current one.
		 */
		StructTypedef head = this.getStructTypedef(def.path, def.getSource());
		if (head == null) head = def;
		
		this.structIds.add(new Pair<NamespacePath, StructTypedef>(path, def));
		
		/* Add the extended fields */
		def.getFields().addAll(extendDecs);
		
		accept(TokenType.LBRACE);
		
		/* Parse the regular struct fields */
		while (current.type != TokenType.RBRACE) {
			if (current.type == TokenType.COMMENT) {
				accept();
				continue;
			}
			
			boolean field = false;
			for (int i = 1; i < this.tokenStream.size(); i++) {
				if (this.tokenStream.get(i - 1).type == TokenType.IDENTIFIER && this.tokenStream.get(i).type == TokenType.SEMICOLON) {
					field = true;
				}
				else if (this.tokenStream.get(i - 1).type == TokenType.LBRACE) {
					break;
				}
			}
			
			if (field) 
				/* Declaration */
				def.getFields().add(this.parseDeclaration(MODIFIER.SHARED, false, true));
			else {
				/* Nested function */
				MODIFIER m = this.parseModifier();
				
				TYPE type = this.parseType();
				
				Function f = this.parseFunction(type, accept(TokenType.IDENTIFIER), m, false, true);
				
				/* Insert Struct Name */
				f.path.path.add(f.path.path.size() - 1, def.path.getLast());
				
				if (f.modifier != MODIFIER.STATIC) {
					/* 
					 * Inject Self Reference. Here it is crucial, if this typedef is an implementation
					 * of a header, that we use the local variable 'head', that holds a reference to it.
					 * This is important because later when context checking, the correct struct typedef 
					 * reference is present and type comparisons work accordingly. 
					 */
					Declaration self = new Declaration(new NamespacePath("self"), new POINTER(head.self.clone()), MODIFIER.SHARED, f.getSource());
					f.parameters.add(0, self);
				}
				
				def.functions.add(f);
			}
		}
		
		accept(TokenType.RBRACE);

		/* 
		 * Initialize the Struct typedef, mainly copy the functions from the extension.
		 * This is nessesary to be done here since the functions in the typedef are present
		 * from this point. To check if functions from the extension are overwritten, all
		 * functions need to be present, so we do the init here.
		 */
		def.postInitialize();
		
		return def;
	}
	
	private InterfaceTypedef parseInterfaceTypedef() throws PARS_EXC {
		MODIFIER mod = this.parseModifier();
		
		Source source = accept(TokenType.INTERFACE).source();
		Token id = accept(TokenType.INTERFACEID);
		
		List<TYPE> proviso = this.parseProviso();
		NamespacePath path = this.buildPath(id.spelling);
		
		List<Function> functions = new ArrayList();
		
		List<INTERFACE> implemented = new ArrayList();
		
		if (current.type == TokenType.COLON) {
			accept();
			while (current.type != TokenType.LBRACE) {
				INTERFACE i = (INTERFACE) this.parseType();
				implemented.add(i);
				
				if (current.type == TokenType.COMMA) accept();
				else break;
			}
		}
		
		InterfaceTypedef def = new InterfaceTypedef(path, proviso, implemented, functions, mod, source);
		this.interfaceIds.add(new Pair<NamespacePath, InterfaceTypedef>(path, def));
		
		accept(TokenType.LBRACE);
		
		while (current.type != TokenType.RBRACE) {
			if (current.type == TokenType.COMMENT) {
				accept();
				continue;
			}
			
			MODIFIER fmod = this.parseModifier();
			
			TYPE ret = this.parseType();
			Function f = this.parseFunction(ret, accept(TokenType.IDENTIFIER), fmod, true, true);
			
			/* Insert Struct Name */
			f.path.path.add(f.path.path.size() - 1, def.path.getLast());
			
			if (f.modifier != MODIFIER.STATIC) {
				/* Inject Self Reference */
				Declaration self = new Declaration(new NamespacePath("self"), new POINTER(new VOID()), MODIFIER.SHARED, f.getSource());
				f.parameters.add(0, self);
			}
			
			def.functions.add(f);
		}
		
		accept(TokenType.RBRACE);
		
		return def;
	}
	
	private EnumTypedef parseEnumTypedef() throws PARS_EXC {
		Source source = accept(TokenType.ENUM).source();
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
	
	private NamespacePath buildPath(String id) {
		NamespacePath path = new NamespacePath(new ArrayList());
		for (int i = 0; i < this.namespaces.size(); i++) {
			path.path.addAll(this.namespaces.get(i).path);
		}
		path.path.add(id);
		return path;
	}
	
	private NamespacePath buildPath() {
		NamespacePath path = new NamespacePath(new ArrayList());
		for (int i = 0; i < this.namespaces.size(); i++) {
			path.path.addAll(this.namespaces.get(i).path);
		}
		return path;
	}
	
	private Function parseFunction(TYPE returnType, Token identifier, MODIFIER mod, boolean parseHeadOnly, boolean isNestedFunction) throws PARS_EXC {
		this.scopes.push(new ArrayList());
		
		/* Check if a function name is a reserved identifier */
		String name = identifier.spelling;
		if (
				/* Name is a reserved identifier */
				(name.equals("resv") || name.equals("init") || name.equals("hsize") || name.equals("free")) 
				/* Currently not parsing any of the libraries that define these reserved function names */
				&& !identifier.source().sourceFile.equals(name + ".sn")) {
			this.progress.abort();
			throw new SNIPS_EXC(Const.USED_RESERVED_NAME, name, identifier.source().getSourceMarker());
		}
		
		List<TYPE> proviso = this.parseProviso();
		
		for (TYPE t : proviso) {
			PROVISO p = (PROVISO) t;
			this.activeProvisos.add(p.placeholderName);
		}
		
		accept(TokenType.LPAREN);
		
		List<Declaration> parameters = new ArrayList();
		while (current.type != TokenType.RPAREN) {
			parameters.add(this.parseDeclaration(MODIFIER.SHARED, false, false));
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
		
		List<Statement> body = null;
		if (current.type == TokenType.SEMICOLON) accept();
		else if (!parseHeadOnly) body = this.parseCompoundStatement(true);
		
		NamespacePath path = this.buildPath(identifier.spelling);
		Function f = new Function(returnType, path, proviso, parameters, signals, signalsTypes, body, mod, identifier.source);
		
		/* 
		 * Perform merge only for functions that are not nested here,
		 * for nested functions merge will be done later.
		 */
		if (!isNestedFunction) {
			for (Pair<NamespacePath, Function> pair : this.functions) {
				boolean useProvisoFreeInCheck = STRUCT.useProvisoFreeInCheck;
				STRUCT.useProvisoFreeInCheck = false;
				if (Function.signatureMatch(f, pair.second, false, false, true)) {
					STRUCT.useProvisoFreeInCheck = useProvisoFreeInCheck;
					pair.second.body = f.body;
					this.scopes.pop();
					return null;
				}
				STRUCT.useProvisoFreeInCheck = useProvisoFreeInCheck;
			}
		}
		
		this.functions.add(new Pair<NamespacePath, Function>(path, f));
		this.scopes.pop();
		return f;
	}
	
	private Declaration parseDeclaration(MODIFIER mod, boolean parseValue, boolean acceptSemicolon) throws PARS_EXC {
		TYPE type = this.parseType();
		Token id = accept(TokenType.IDENTIFIER);

		Expression value = null;
		
		if (parseValue) {
			accept(TokenType.LET);
			value = this.parseExpression();
		}

		if (parseValue || acceptSemicolon) 
			accept(TokenType.SEMICOLON);
		
		Declaration d = new Declaration(new NamespacePath(id.spelling), type, value, mod, id.source());
		this.scopes.peek().add(d);
		return d;
	}
	
	private Statement parseStatement() throws PARS_EXC {
		/* Convert next token */
		if (this.activeProvisos.contains(current.spelling)) {
			current.type = TokenType.PROVISO;
		}
		
		if (current.type == TokenType.DIRECTIVE) {
			this.bufferedAnnotations.peek().add(this.parseASTAnnotation());
			return null;
		}
		
		Token modT = (current.type.group() == TokenGroup.MODIFIER)? current : null;
		MODIFIER mod = this.parseModifier();
		
		boolean functionCheck = current.type == TokenType.NAMESPACE_IDENTIFIER || current.type == TokenType.IDENTIFIER;
		for (int i = 0; i < this.tokenStream.size(); i += 3) {
			if (tokenStream.get(i).type == TokenType.LPAREN || (tokenStream.get(i).type == TokenType.CMPLT && 
					!(tokenStream.get(i).markedAsOperator && tokenStream.get(i).operatorSymbol.equals("<<")))) {
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
		
		boolean decCheck = current.type.group() == TokenGroup.TYPE || current.type == TokenType.NAMESPACE_IDENTIFIER || current.type == TokenType.ENUMID;
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
				/* Call to static union member */
				if (tokenStream.get(i + 3).type == TokenType.COLON) 
					decCheck = false;
				
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
		
		/* Can only be func type dec */
		if (current.type == TokenType.FUNC) decCheck = true;
		
		if (decCheck) {
			return this.parseDeclaration(mod, true, true);
		}
		else {
			if (modT != null) {
				throw new PARS_EXC(modT.source, modT.type, 
						TokenType.TYPE, TokenType.RETURN, TokenType.WHILE, 
						TokenType.DO, TokenType.FOR, TokenType.BREAK, 
						TokenType.CONTINUE, TokenType.SWITCH, TokenType.IDENTIFIER, 
						TokenType.IF);
			}
			else if (current.type == TokenType.ASM) {
				return this.parseDirectASM();
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
			/*
			 * LPAREN in case of statement like
			 * 
			 * (ll->lp)->size();
			 * 
			 */
			else if (current.type == TokenType.IDENTIFIER || current.type == TokenType.MUL || current.type == TokenType.NAMESPACE_IDENTIFIER || current.type == TokenType.LPAREN) {
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
			else if (current.type == TokenType.STRUCTID) {
				/* Static nested function call */
				Token sid = accept();
				accept(TokenType.COLON);
				accept(TokenType.COLON);
				
				FunctionCall call = this.parseFunctionCall();
				
				/* 
				 * Add the sid infront of the last element of the path, function
				 * will be re-written out of the struct and will become a regular
				 * function later.
				 */
				call.path.path.add(call.path.path.size() - 1, sid.spelling);
				
				return call;
			}
			else {
				/* Use this as default, multiple edge-cases are handled here */
				return this.parseAssignment(false);
			}
		}
	}
	
	private DirectASMStatement parseDirectASM() throws PARS_EXC {
		Source source = current.source();
		
		accept(TokenType.ASM);
		
		List<Pair<Expression, REG>> dataIn = new ArrayList();
		
		List<Pair<Expression, REG>> dataOut = new ArrayList();
		
		if (current.type == TokenType.LPAREN) {
			accept();
			
			while (current.type != TokenType.RPAREN) {
				Expression in = this.parseExpression();
				
				accept(TokenType.COLON);
				
				REG reg = RegOp.convertStringToReg(accept(TokenType.IDENTIFIER).spelling);
				
				dataIn.add(new Pair<Expression, REG>(in, reg));
				
				if (current.type == TokenType.COMMA) accept();
				else break;
			}
			
			accept(TokenType.RPAREN);
		}
		
		accept(TokenType.LBRACE);
		
		List<String> assembly = new ArrayList();
		String c = "";
		
		int last = -1;
		
		while (!(current.type == TokenType.RBRACE && (this.tokenStream.get(0).type == TokenType.LPAREN || this.tokenStream.get(0).type == TokenType.SEMICOLON))) {
			if (last == -1) 
				last = current.source().row;
			
			if (current.type == TokenType.COLON) {
				if (!c.trim().equals("")) assembly.add(c);
				c = "";
				last = current.source().row;
				accept(TokenType.COLON);
			}
			else {
				Token t = accept();
				if (t.type != TokenType.COMMA && t.type != TokenType.DIRECTIVE && t.type != TokenType.COMMENT) c += t.spelling + " ";
				else if (t.type == TokenType.COMMENT) {
					c += "/* " + t.spelling + " */";
					assembly.add(c);
					c = "";
				}
				else if (t.type == TokenType.COMMA) c = c.substring(0, c.length() - 1) + ", ";
				else c += t.spelling;
				
				if (t.type == TokenType.DIRECTIVE) {
					c += current.spelling;
					current = tokenStream.get(0);
					tokenStream.remove(0);
				}
			}
		}
		
		if (!c.trim().equals("")) assembly.add(c);
		
		accept(TokenType.RBRACE);
		
		if (current.type == TokenType.LPAREN) {
			accept();
			
			while (current.type != TokenType.RPAREN) {
				REG reg = RegOp.convertStringToReg(accept(TokenType.IDENTIFIER).spelling);
				
				accept(TokenType.COLON);
				
				Expression out = this.parseExpression();
				
				dataOut.add(new Pair<Expression, REG>(out, reg));
				
				if (current.type == TokenType.COMMA) accept();
				else break;
			}
			
			accept(TokenType.RPAREN);
		}
		
		accept(TokenType.SEMICOLON);
		
		return new DirectASMStatement(assembly, dataIn, dataOut, source);
	}
	
	private FunctionCall parseFunctionCall() throws PARS_EXC {
		Source source = current.source();
		
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
			CompilerDriver.driver.referencedLibaries.add("release/lib/mem/free.sn");
		}
		
		return new FunctionCall(path, provisos, params, source);
	}
	
	private SwitchStatement parseSwitch() throws PARS_EXC {
		Source source = accept(TokenType.SWITCH).source();
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
	
	private CaseStatement parseCase() throws PARS_EXC {
		Source source = accept(TokenType.CASE).source();
		accept(TokenType.LPAREN);
		Expression condition = this.parseExpression();
		accept(TokenType.RPAREN);
		accept(TokenType.COLON);
		
		List<Statement> body = this.parseCompoundStatement(false);
		
		return new CaseStatement(condition, body, source);
	}
	
	private DefaultStatement parseDefault() throws PARS_EXC {
		Source source = accept(TokenType.DEFAULT).source();
		accept(TokenType.COLON);
		
		List<Statement> body = this.parseCompoundStatement(false);
		
		return new DefaultStatement(body, source);
	}
	
	private BreakStatement parseBreak() throws PARS_EXC {
		Source source = accept(TokenType.BREAK).source();
		accept(TokenType.SEMICOLON);
		return new BreakStatement(source);
	}
	
	private ContinueStatement parseContinue() throws PARS_EXC {
		Source source = accept(TokenType.CONTINUE).source();
		accept(TokenType.SEMICOLON);
		return new ContinueStatement(source);
	}
	
	private IfStatement parseIf() throws PARS_EXC {
		Source source = current.source();
		accept(TokenType.IF);
		accept(TokenType.LPAREN);
		Expression condition = this.parseExpression();
		accept(TokenType.RPAREN);
		
		List<Statement> body = this.parseCompoundStatement(false);
		
		IfStatement if0 = new IfStatement(condition, body, source);
		
		if (current.type == TokenType.ELSE) {
			Source elseSource = accept().source();
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
	
	private TryStatement parseTry() throws PARS_EXC {
		Source source = current.source();
		accept(TokenType.TRY);
		
		List<Statement> body = this.parseCompoundStatement(true);
		
		List<WatchStatement> watchpoints = new ArrayList();
		while (current.type == TokenType.WATCH) {
			watchpoints.add(this.parseWatch());
		}
		
		return new TryStatement(body, watchpoints, source);
	}
	
	private WatchStatement parseWatch() throws PARS_EXC {
		this.scopes.push(new ArrayList());
		
		Source source = current.source();
		accept(TokenType.WATCH);
		
		accept(TokenType.LPAREN);
		
		Declaration watched = this.parseDeclaration(MODIFIER.SHARED, false, false);
		this.scopes.peek().add(watched);
		
		accept(TokenType.RPAREN);
		
		List<Statement> body = this.parseCompoundStatement(false);
		
		this.scopes.pop();
		return new WatchStatement(body, watched, source);
	}
	
	private SignalStatement parseSignal() throws PARS_EXC {
		Source source = current.source();
		
		accept(TokenType.SIGNAL);
		
		Expression ex0 = this.parseExpression();
		
		accept(TokenType.SEMICOLON);
		
		return new SignalStatement(ex0, source);
	}
	
	private Statement parseFor() throws PARS_EXC {
		this.scopes.push(new ArrayList());
		
		Source source = accept(TokenType.FOR).source();
		
		boolean writeBackIterator = false;
		if (current.type == TokenType.LBRACKET) {
			accept();
			writeBackIterator = true;
		}
		else accept(TokenType.LPAREN);
		
		TYPE itType = this.parseType();
		Token itId = accept(TokenType.IDENTIFIER);
		
		if (current.type == TokenType.COLON || writeBackIterator) 
			return this.parseForEach(itType, itId, writeBackIterator);
		
		accept(TokenType.LET);
		
		Expression value = this.parseExpression();
		accept(TokenType.SEMICOLON);
		
		Declaration iterator = new Declaration(new NamespacePath(itId.spelling), itType, value, MODIFIER.SHARED, itId.source());
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
	
	private Statement parseForEach(TYPE itType, Token itId, boolean writeBackIterator) throws PARS_EXC {
		this.scopes.push(new ArrayList());
		
		Declaration iterator = new Declaration(new NamespacePath(itId.spelling), itType, null, MODIFIER.SHARED, itId.source());
		this.scopes.peek().add(iterator);
		
		accept(TokenType.COLON);
		
		Expression shadowRef = this.parseExpression();
		
		Expression range = null;
		
		if (current.type == TokenType.COMMA) {
			accept();
			range = this.parseExpression();
		}
		
		if (writeBackIterator) accept(TokenType.RBRACKET);
		else accept(TokenType.RPAREN);
		
		List<Statement> body = this.parseCompoundStatement(false);
		
		return new ForEachStatement(iterator, writeBackIterator, shadowRef, range, body, itId.source());
	}
	
	private WhileStatement parseWhile() throws PARS_EXC {
		Source source = accept(TokenType.WHILE).source();
		accept(TokenType.LPAREN);
		Expression condition = this.parseExpression();
		accept(TokenType.RPAREN);
		List<Statement> body = this.parseCompoundStatement(false);
		return new WhileStatement(condition, body, source);
	}
	
	private DoWhileStatement parseDoWhile() throws PARS_EXC {
		Source source = accept(TokenType.DO).source();
		
		List<Statement> body = this.parseCompoundStatement(true);
		
		accept(TokenType.WHILE);
		
		accept(TokenType.LPAREN);
		Expression condition = this.parseExpression();
		accept(TokenType.RPAREN);
		
		accept(TokenType.SEMICOLON);
		
		return new DoWhileStatement(condition, body, source);
	}
	
	private Statement parseAssignment(boolean acceptSemicolon) throws PARS_EXC {
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
		
		Source source = current.source();
		
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
			SyntaxElement expr = this.parseLhsIdentifer();
			
			if (expr instanceof OperatorStatement) {
				return (OperatorStatement) expr;
			}
			else if (expr instanceof InlineCall) {
				InlineCall ic = (InlineCall) expr;
				
				FunctionCall fc = new FunctionCall(ic.path, ic.proviso, ic.parameters, ic.getSource());
				fc.isNestedCall = true;
				
				accept(TokenType.SEMICOLON);
				return fc;
			}
			else {
				LhsId target = (LhsId) expr;
				
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
				
				ASSIGN_ARITH arith = this.parseAssignOperator();
				
				if (arith == null) {
					/*
					 * Struct Nesting Call.
					 * 
					 * If the arith is null at this location, this means we have a nested struct call. The inline call
					 * is parsed into the LhsId. So we need to extract the inline call from the lhs, convert it to an
					 * Function Call, accept a semicolon and return the function call.
					 * 
					 * The Syntax where this occurs could be:
					 * 
					 * x.getValue(x);
					 * 
					 * where x is a struct variable, and getValue a function defined in the struct type definition.
					 * 
					 */
					StructSelect select = ((StructSelectLhsId) target).select;
					InlineCall ic = (InlineCall) select.selection;
					FunctionCall fc = new FunctionCall(ic.path, ic.proviso, ic.parameters, ic.getSource());
					fc.baseRef = select.selector;
					fc.nestedDeref = ic.nestedDeref;
					accept(TokenType.SEMICOLON);
					return fc;
				}
				else {
					/*
					 * Normal behaviour.
					 */
					Expression value = this.parseExpression();
					if (acceptSemicolon) accept(TokenType.SEMICOLON);
					return new Assignment(arith, target, value, target.getSource());
				}
			}
		}
	}
	
	private ASSIGN_ARITH parseAssignOperator() throws PARS_EXC {
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
			CompilerDriver.driver.referencedLibaries.add("release/lib/op/__op_div.sn");
			return ASSIGN_ARITH.DIV_ASSIGN;
		}
		else if (current.type == TokenType.MOD) {
			accept();
			accept(TokenType.LET);
			CompilerDriver.driver.referencedLibaries.add("release/lib/op/__op_mod.sn");
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
			throw new SNIPS_EXC(Const.CHECK_FOR_MISSPELLED_TYPES, current.spelling , current.source().getSourceMarker());
		}
		else return null;
	}
	
	private SyntaxElement parseLhsIdentifer() throws PARS_EXC {
		if (current.type == TokenType.MUL) {
			Source source = current.source();
			return new PointerLhsId(this.parseDeref(), source);
		}
		else {
			Token curr1 = this.current;
			List<Token> tokenStreamCopy = this.tokenStream.stream().collect(Collectors.toList());
			
			Expression target = this.parseStructSelect();
			
			if (current.markedAsOperator && (tokenStream.get(0).type != TokenType.LET && tokenStream.get(1).type != TokenType.LET)) {
				this.tokenStream = tokenStreamCopy;
				this.current = curr1;
				
				target = this.parseExpression();
			}
			
			if (target instanceof ArraySelect) {
				return new ArraySelectLhsId((ArraySelect) target, target.getSource());
			}
			else if (target instanceof IDRef) {
				return new SimpleLhsId((IDRef) target, target.getSource());
			}
			else if (target instanceof StructSelect) {
				return new StructSelectLhsId((StructSelect) target, target.getSource());
			}
			else if (target instanceof OperatorExpression) {
				accept(TokenType.SEMICOLON);
				return new OperatorStatement((OperatorExpression) target, target.getSource());
			}
			else if (target instanceof InlineCall) 
				return target;
			else {
				this.progress.abort();
				
				if (this.progress != null) this.progress.abort();
				throw new PARS_EXC(current.source, current.type, 
				TokenType.TYPE, TokenType.RETURN, TokenType.WHILE, 
				TokenType.DO, TokenType.FOR, TokenType.BREAK, 
				TokenType.CONTINUE, TokenType.SWITCH, TokenType.IDENTIFIER, 
				TokenType.IF);
			}
		}
	}
	
	private Declaration parseGlobalDeclaration(TYPE type, Token identifier, MODIFIER mod) throws PARS_EXC {
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
	
	private ReturnStatement parseReturn() throws PARS_EXC {
		Token ret = accept(TokenType.RETURN);
		
		if (current.type == TokenType.SEMICOLON) {
			/* Void return */
			accept();
			return new ReturnStatement(null, ret.source());
		}
		else {
			/* Value return */
			Expression expr = this.parseExpression();
			accept(TokenType.SEMICOLON);
			return new ReturnStatement(expr, ret.source());
		}
	}
	
	/**
	 * Parses an expression. Expressions are inductiveley defined. See the parsing methods below
	 * for possible parsing patterns.
	 */
	private Expression parseExpression() throws PARS_EXC {
			return this.parseInlineFunction();
	}
	
	private Expression parseInlineFunction() throws PARS_EXC {
		/*
		 * Check if the structure lying ahead is an inline function. This can be determined by
		 * traversing forward until the bracket balance is zero. Then check if a : { follows. For example:
		 * 
		 * ( ... ( ... ) ... ) : { ...
		 */
		boolean inlineCheck = current.type == TokenType.LPAREN;
		int bbalance = 1;
		boolean allowLparen = false;
		if (inlineCheck) for (int i = 0; i < this.tokenStream.size(); i++) {
			if (bbalance == 0) {
				inlineCheck &= tokenStream.get(i).type == TokenType.COLON;
				inlineCheck &= tokenStream.get(i + 1).type == TokenType.LBRACE;
				break;
			}
			
			if (current.type == TokenType.FUNC)
				allowLparen = true;
			else if (tokenStream.get(i).type == TokenType.LPAREN) {
				if (allowLparen)
					bbalance++;
				else {
					inlineCheck = false;
					break;
				}
				
				allowLparen = false;
			}
			else if (tokenStream.get(i).type == TokenType.RPAREN) 
				bbalance--;
		}
		
		if (inlineCheck) {
			accept(TokenType.LPAREN);
			
			List<Declaration> params = new ArrayList();
			while (current.type != TokenType.UNION_ACCESS) {
				params.add(this.parseDeclaration(MODIFIER.SHARED, false, false));
				if (current.type == TokenType.COMMA) accept();
				else break;
			}
			
			accept(TokenType.UNION_ACCESS);
			
			TYPE ret = this.parseType();
			
			accept(TokenType.RPAREN);
			
			accept(TokenType.COLON);
			
			List<Statement> body = this.parseCompoundStatement(true);
			
			Function function = new Function(ret, this.buildPath(LabelUtil.getAnonLabel()), new ArrayList(), params, false, new ArrayList(), body, MODIFIER.SHARED, current.source);
		
			return new InlineFunction(function, current.source);
		}
		else return this.parseStructureInit();
	}
	
	
	/**
	 * Parses a structure initialization operation with multiple operands. The parsed pattern is:<br>
	 * <br>
	 * 		<code>TYPE::((EXPRESSION(,EXPRESSION)*)?)</code>
	 */
	private Expression parseStructureInit() throws PARS_EXC {
		boolean structInitCheck = current.type == TokenType.IDENTIFIER || current.type == TokenType.NAMESPACE_IDENTIFIER;
		for (int i = 0; i < this.tokenStream.size(); i += 3) {
			structInitCheck &= tokenStream.get(i).type == TokenType.COLON;
			structInitCheck &= tokenStream.get(i + 1).type == TokenType.COLON;
			if (tokenStream.get(i + 2).type == TokenType.LPAREN) {
				break;
			}
			else {
				Token t = tokenStream.get(i + 2);
				
				/* Found Struct ID, must be a structure init or static member function access */
				if (t.type == TokenType.STRUCTID) {
					/* Namespace::StructId::create<...>(); */
					if (tokenStream.get(i + 5).type == TokenType.IDENTIFIER)
						structInitCheck = false;
					
					break;
				}
				else if (t.type != TokenType.IDENTIFIER && t.type != TokenType.NAMESPACE_IDENTIFIER && t.type != TokenType.STRUCTID) {
					structInitCheck = false;
					break;
				}
			}
		}
		
		if ((current.type == TokenType.STRUCTID && this.tokenStream.get(2).type != TokenType.IDENTIFIER) || structInitCheck) {
			Source source = current.source();
			
			TYPE type = this.parseType();
			
			if (!(type instanceof STRUCT)) {
				/* Something is definetly wrong at this point */
				this.progress.abort();
				throw new SNIPS_EXC(new CTEX_EXC(source, Const.EXPECTED_STRUCT_TYPE, type).getMessage());
			}
			
			accept(TokenType.COLON);
			accept(TokenType.COLON);
			
			accept(TokenType.LPAREN);
			
			List<Expression> elements = new ArrayList();
			while (current.type != TokenType.RPAREN) {
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
	
	/**
	 * Parses an array initialization operation with multiple operands. The parsed pattern is:<br>
	 * <br>
	 * 		<code>{(EXPRESSION(,EXPRESSION)*)?}|[(EXPRESSION(,EXPRESSION)*)?]</code>
	 */
	private Expression parseArrayInit() throws PARS_EXC {
		if (current.type == TokenType.LBRACE || current.type == TokenType.LBRACKET) {
			boolean dontCare = current.type == TokenType.LBRACKET;
			
			Source source = accept().source();
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
	
	/**
	 * Parses a ternary operation with a condition and two cases. The parsed pattern is:<br>
	 * <br>
	 * 		<code>EXPRESSION?EXPRESSION:EXPRESSION</code>
	 */
	private Expression parseTernary() throws PARS_EXC {
		Source source = current.source();
		
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
	
	/**
	 * Parses a OR operation between two operands. The parsed pattern is:<br>
	 * <br>
	 * 		<code>EXPRESSION||EXPRESSION</code><br>
	 * <br>
	 * Multiple successive OR operations are stacked on the left side.
	 */
	private Expression parseOr() throws PARS_EXC {
		Expression left = this.parseAnd();
		
		while (current.type == TokenType.OR) {
			Token orT = accept();
			left = new Or(left, this.parseAnd(), current.source);
			
			if (orT.markedAsOperator && orT.operatorSymbol.equals("||")) {
				left.operatorSymbolOverride = "||";
				left = new OperatorExpression(left, orT.operatorSymbol, left.getSource());
			}
		}
		
		return left;
	}
	
	/**
	 * Parses a AND operation between two operands. The parsed pattern is:<br>
	 * <br>
	 * 		<code>EXPRESSION&&EXPRESSION</code><br>
	 * <br>
	 * Multiple successive AND operations are stacked on the left side.
	 */
	private Expression parseAnd() throws PARS_EXC {
		Expression left = this.parseBitOr();
		
		while (current.type == TokenType.AND) {
			Token andT = accept();
			left = new And(left, this.parseBitOr(), current.source);
			
			if (andT.markedAsOperator && andT.operatorSymbol.equals("&&")) {
				left.operatorSymbolOverride = "&&";
				left = new OperatorExpression(left, andT.operatorSymbol, left.getSource());
			}
		}
		
		return left;
	}
	
	/**
	 * Parses a bitwise OR operation between two operands. The parsed pattern is:<br>
	 * <br>
	 * 		<code>EXPRESSION|EXPRESSION</code><br>
	 * <br>
	 * Multiple successive OR operations are stacked on the left side.
	 */
	private Expression parseBitOr() throws PARS_EXC {
		Expression left = this.parseBitXor();
		
		while (current.type == TokenType.BITOR) {
			Token orT = accept();
			left = new BitOr(left, this.parseBitXor(), current.source);
			
			if (orT.markedAsOperator && orT.operatorSymbol.equals("|")) {
				left.operatorSymbolOverride = "|";
				left = new OperatorExpression(left, orT.operatorSymbol, left.getSource());
			}
		}
		
		return left;
	}
	
	/**
	 * Parses a bitwise XOR operation between two operands. The parsed pattern is:<br>
	 * <br>
	 * 		<code>EXPRESSION^EXPRESSION</code><br>
	 * <br>
	 * Multiple successive XOR operations are stacked on the left side.
	 */
	private Expression parseBitXor() throws PARS_EXC {
		Expression left = this.parseBitAnd();
		
		while (current.type == TokenType.XOR) {
			Token xorT = accept();
			left = new BitXor(left, this.parseBitAnd(), current.source);
			
			if (xorT.markedAsOperator && xorT.operatorSymbol.equals("^")) {
				left.operatorSymbolOverride = "^";
				left = new OperatorExpression(left, xorT.operatorSymbol, left.getSource());
			}
		}
		
		return left;
	}
	
	/**
	 * Parses a bitwise AND operation between two operands. The parsed pattern is:<br>
	 * <br>
	 * 		<code>EXPRESSION&EXPRESSION</code><br>
	 * <br>
	 * Multiple successive AND operations are stacked on the left side.
	 */
	private Expression parseBitAnd() throws PARS_EXC {
		Expression left = this.parseCompare();
		
		while (current.type == TokenType.ADDROF) {
			Token andT = accept();
			left = new BitAnd(left, this.parseCompare(), current.source);
			
			if (andT.markedAsOperator && andT.operatorSymbol.equals("&")) {
				left.operatorSymbolOverride = "&";
				left = new OperatorExpression(left, andT.operatorSymbol, left.getSource());
			}
		}
		
		return left;
	}
	
	/**
	 * Parses a comparison operation between two operands. The parsed pattern is:<br>
	 * <br>
	 * 		<code>EXPRESSION(==|!=|>=|>|<=|<)EXPRESSION</code>
	 */
	private Expression parseCompare() throws PARS_EXC {
		Expression left = this.parseShift();
		
		if (current.type.group() == TokenGroup.COMPARE) {
			Token cmpT = current;
			
			Compare cmp = null;
			
			Source source = current.source();
			if (current.type == TokenType.CMPEQ) {
				accept();
				cmp = new Compare(left, this.parseShift(), COMPARATOR.EQUAL, source);
			}
			else if (current.type == TokenType.CMPNE) {
				accept();
				cmp = new Compare(left, this.parseShift(), COMPARATOR.NOT_EQUAL, source);
			}
			else if (current.type == TokenType.CMPGE) {
				accept();
				cmp = new Compare(left, this.parseShift(), COMPARATOR.GREATER_SAME, source);
			}
			else if (current.type == TokenType.CMPGT) {
				accept();
				cmp = new Compare(left, this.parseShift(), COMPARATOR.GREATER_THAN, source);
			}
			else if (current.type == TokenType.CMPLE) {
				accept();
				cmp = new Compare(left, this.parseShift(), COMPARATOR.LESS_SAME, source);
			}
			else if (current.type == TokenType.CMPLT) {
				accept();
				cmp = new Compare(left, this.parseShift(), COMPARATOR.LESS_THAN, source);
			}
			
			left = cmp;
			
			if (cmpT.markedAsOperator && cmpT.operatorSymbol.equals(cmp.comparator.toString())) {
				left.operatorSymbolOverride = cmpT.operatorSymbol;
				left = new OperatorExpression(left, cmpT.operatorSymbol, left.getSource());
			}
		}
		
		return left;
	}
	
	/**
	 * Parses a shft operation between two operands. The parsed pattern is:<br>
	 * <br>
	 * 		<code>EXPRESSION(<<|>>)EXPRESSION</code><br>
	 * <br>
	 * Multiple successive shift operations are stacked on the left side.
	 */
	private Expression parseShift() throws PARS_EXC {
		Expression left = this.parseAddSub();
		
		while ((current.type == TokenType.CMPLT && this.tokenStream.get(0).type == TokenType.CMPLT) || 
			   (current.type == TokenType.CMPGT && this.tokenStream.get(0).type == TokenType.CMPGT)) {
			if (current.type == TokenType.CMPLT) {
				Token lslT = accept();
				accept();
				left = new Lsl(left, this.parseAddSub(), current.source);
				
				if (lslT.markedAsOperator && lslT.operatorSymbol.equals("<<")) {
					left.operatorSymbolOverride = "<<";
					left = new OperatorExpression(left, lslT.operatorSymbol, left.getSource());
				}
			}
			else {
				Token lsrT = accept();
				accept();
				left = new Lsr(left, this.parseAddSub(), current.source);
				
				if (lsrT.markedAsOperator && lsrT.operatorSymbol.equals(">>")) {
					left.operatorSymbolOverride = ">>";
					left = new OperatorExpression(left, lsrT.operatorSymbol, left.getSource());
				}
			}
		}
		
		return left;
	}
	
	/**
	 * Parses a addition or subtraction operation between two operands. The parsed pattern is:<br>
	 * <br>
	 * 		<code>EXPRESSION(+|-)EXPRESSION</code><br>
	 * <br>
	 * Multiple successive operations are stacked on the left side.
	 */
	private Expression parseAddSub() throws PARS_EXC {
		Expression left = this.parseMulDivMod();
		
		while (current.type == TokenType.ADD || current.type == TokenType.SUB) {
			if (current.type == TokenType.ADD) {
				Token addT = accept();
				left = new Add(left, this.parseMulDivMod(), current.source);
				
				if (addT.markedAsOperator && addT.operatorSymbol.equals("+")) {
					left.operatorSymbolOverride = "+";
					left = new OperatorExpression(left, addT.operatorSymbol, left.getSource());
				}
			}
			else {
				Token subT = accept();
				left = new Sub(left, this.parseMulDivMod(), current.source);
				
				if (subT.markedAsOperator && subT.operatorSymbol.equals("-")) {
					left.operatorSymbolOverride = "-";
					left = new OperatorExpression(left, subT.operatorSymbol, left.getSource());
				}
			}
		}
		
		return left;
	}
		
	/**
	 * Parses a multiplication, division or modulo operation between two operands. The parsed pattern is:<br>
	 * <br>
	 * 		<code>EXPRESSION(*|/|%)EXPRESSION</code><br>
	 * <br>
	 * Multiple successive operations are stacked on the left side.
	 */
	private Expression parseMulDivMod() throws PARS_EXC {
		Expression left = this.parseSizeOf();
		
		while (current.type == TokenType.MUL || current.type == TokenType.DIV || current.type == TokenType.MOD) {
			if (current.type == TokenType.MUL) {
				Token mulT = accept();
				left = new Mul(left, this.parseSizeOf(), current.source);
				
				if (mulT.markedAsOperator && mulT.operatorSymbol.equals("*")) {
					left.operatorSymbolOverride = "*";
					left = new OperatorExpression(left, mulT.operatorSymbol, left.getSource());
				}
			}
			else if (current.type == TokenType.DIV) {
				Token divT = accept();
				Source source = divT.source;
				
				List<Expression> params = new ArrayList();
				params.add(left);
				params.add(this.parseSizeOf());
				
				/* Create inline call to libary function, add div operator to referenced libaries */
				left = new InlineCall(new NamespacePath("__op_div"), new ArrayList(), params, source);
				CompilerDriver.driver.referencedLibaries.add("release/lib/op/__op_div.sn");
				
				if (divT.markedAsOperator && divT.operatorSymbol.equals("/")) {
					left.operatorSymbolOverride = "/";
					left = new OperatorExpression(left, divT.operatorSymbol, left.getSource());
				}
			}
			else {
				Token modT = accept();
				Source source = modT.source;
				
				List<Expression> params = new ArrayList();
				params.add(left);
				params.add(this.parseSizeOf());
				
				/* Create inline call to libary function, add mod operator to referenced libaries */
				left = new InlineCall(new NamespacePath("__op_mod"), new ArrayList(), params, source);
				CompilerDriver.driver.referencedLibaries.add("release/lib/op/__op_mod.sn");
				
				if (modT.markedAsOperator && modT.operatorSymbol.equals("%")) {
					left.operatorSymbolOverride = "%";
					left = new OperatorExpression(left, modT.operatorSymbol, left.getSource());
				}
			}
		}
		
		return left;
	}
	
	/**
	 * Parses a sizeof operation for a single operand. The parsed pattern is:<br>
	 * <br>
	 * 		<code>sizeof(TYPE|EXPRESSION)</code>
	 */
	private Expression parseSizeOf() throws PARS_EXC {
		if (current.type == TokenType.SIZEOF) {
			Expression sof = null;
			
			Source source = accept().source();
			accept(TokenType.LPAREN);
			
			/* Convert next token */
			if (this.activeProvisos.contains(current.spelling)) {
				current.type = TokenType.PROVISO;
			}
			
			/* Size of Type */
			if (current.type.group() == TokenGroup.TYPE) {
				TYPE type = this.parseType();
				sof = new SizeOfType(type, source);
			}
			/* Size of Expression */
			else sof = new SizeOfExpression(this.parseExpression(), source);
			
			accept(TokenType.RPAREN);
			
			return sof;
		}
		else return this.parseIDOf();
	}
	
	/**
	 * Parses a sizeof operation for a single operand. The parsed pattern is:<br>
	 * <br>
	 * 		<code>idof(TYPE)</code>
	 */
	private Expression parseIDOf() throws PARS_EXC {
		if (current.type == TokenType.IDOF) {
			Expression iof = null;
			
			Source source = accept().source();
			accept(TokenType.LPAREN);
			
			/* Convert next token */
			if (this.activeProvisos.contains(current.spelling)) {
				current.type = TokenType.PROVISO;
			}
			
			/* Size of Type */
			if (current.type.group() == TokenGroup.TYPE) {
				TYPE type = this.parseType();
				iof = new IDOfExpression(type, source);
			}
			
			accept(TokenType.RPAREN);
			
			return iof;
		}
		else return this.parseAddressOf();
	}
	
	/**
	 * Parses a address of operation for a single operand. The parsed pattern is:<br>
	 * <br>
	 * 		<code>&EXPRESSION</code>
	 */
	private Expression parseAddressOf() throws PARS_EXC {
		if (current.type == TokenType.ADDROF) {
			Source source = accept().source();
			
			if (current.type == TokenType.TYPE || current.type == TokenType.STRUCTID || current.type == TokenType.NAMESPACE_IDENTIFIER)
				return new AddressOf(this.parseExpression(), source);
			else 
				return new AddressOf(this.parseDeref(), source);
		}
		else return this.parseDeref();
	}
	
	/**
	 * Parses a dereference operation for a single operand. The parsed pattern is:<br>
	 * <br>
	 * 		<code>*EXPRESSION</code>
	 */
	private Expression parseDeref() throws PARS_EXC {
		if (current.type == TokenType.MUL) {
			Source source = accept().source();
			return new Deref(this.parseDeref(), source);
		}
		else return this.parseTypeCast();
	}
	
	/**
	 * Parses a type cast operation for a single operand. The parsed pattern is:<br>
	 * <br>
	 * 		<code>(TYPE)EXPRESSION</code>
	 */
	private Expression parseTypeCast() throws PARS_EXC {
		/* Convert next token */
		if (this.activeProvisos.contains(this.tokenStream.get(0).spelling)) 
			this.tokenStream.get(0).type = TokenType.PROVISO;
		
		if (this.castCheck()) {
			Source source = accept().source();
			TYPE castType = this.parseType();
			accept(TokenType.RPAREN);
			
			Expression cast0 = this.parseNot();
			
			return new TypeCast(cast0, castType, source);
		}
		else return this.parseNot();
	}
	
	private boolean castCheck() {
		boolean castCheck = current.type == TokenType.LPAREN;
		if (!castCheck) return false;
		
		if (this.tokenStream.get(0).type == TokenType.FUNC) return true;
		
		for (int i = 2; i < tokenStream.size(); i += 3) {
			/* 
			 * First type token, from here only allowed token are RPAREN and all other type related
			 * tokens like [, ], *. If a colon is seen, the current structure cannot be a cast.
			 */
			if (tokenStream.get(i - 2).type.group() == TokenGroup.TYPE) {
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
	
	/**
	 * Parses a boolean negation operation for a single operand. The parsed pattern is:<br>
	 * <br>
	 * 		<code>!EXPRESSION</code>
	 */
	private Expression parseNot() throws PARS_EXC {
		if (current.type == TokenType.NEG || current.type == TokenType.NOT) {
			if (current.type == TokenType.NEG) {
				Token negT = accept();
				Expression left = new Not(this.parseNot(), current.source);
				
				if (negT.markedAsOperator && negT.operatorSymbol.equals("!")) {
					left.operatorSymbolOverride = "!";
					left = new OperatorExpression(left, negT.operatorSymbol, left.getSource());
				}
					
				return left;
			}
			else {
				Token notT = accept(TokenType.NOT);
				Expression left = new BitNot(this.parseNot(), current.source);
				
				if (notT.markedAsOperator && notT.operatorSymbol.equals("~")) {
					left.operatorSymbolOverride = "~";
					left = new OperatorExpression(left, notT.operatorSymbol, left.getSource());
				}
				
				return left;
			}
		}
		else return this.parseUnaryMinus();
	}
	
	/**
	 * Parses an arithmetic negation operation for a single operand. The parsed pattern is:<br>
	 * <br>
	 * 		<code>-EXPRESSION</code>
	 */
	private Expression parseUnaryMinus() throws PARS_EXC {
		if (current.type == TokenType.SUB) {
			Token subT = accept();
			Expression left = new UnaryMinus(this.parseUnaryMinus(), current.source);
			
			if (subT.markedAsOperator && subT.operatorSymbol.equals("-")) {
				left.operatorSymbolOverride = "-";
				left = new OperatorExpression(left, subT.operatorSymbol, left.getSource());
			}
			
			return left;
		}
		else return this.parseIncrDecr();
	}
	
	/**
	 * Parses a increment or decrement operation for a single operand. The parsed pattern is:<br>
	 * <br>
	 * 		<code>EXPRESSION(++|--)</code>
	 */
	private Expression parseIncrDecr() throws PARS_EXC {
		Expression ref = this.parseStructSelect();
		
		if (current.type == TokenType.INCR || current.type == TokenType.DECR) {
			Source source = current.source();
			if (current.type == TokenType.INCR) {
				Token incrT = accept();
				Expression e = null;
				
				if (ref instanceof IDRef)
					e = new IDRefWriteback(WRITEBACK.INCR, ref, source);
				else e = new StructSelectWriteback(WRITEBACK.INCR, ref, source);
				
				if (incrT.markedAsOperator && incrT.operatorSymbol.equals("++")) {
					e.operatorSymbolOverride = "++";
					e = new OperatorExpression(e, incrT.operatorSymbol, e.getSource());
				}
				
				return e;
			}
			else {
				Token decrT = accept();
				Expression e = null;
				
				if (ref instanceof IDRef)
					e = new IDRefWriteback(WRITEBACK.DECR, ref, source);
				else e = new StructSelectWriteback(WRITEBACK.DECR, ref, source);
				
				if (decrT.markedAsOperator && decrT.operatorSymbol.equals("--")) {
					e.operatorSymbolOverride = "--";
					e = new OperatorExpression(e, decrT.operatorSymbol, e.getSource());
				}
				
				return e;
			}
		}
		else return ref;
	}
	
	/**
	 * Parses a structure select or union access operation for a chain of operands. The parsed pattern is:<br>
	 * <br>
	 * 		<code>EXPRESSION((.|->)EXPRESSION)+</code>
	 */
	private Expression parseStructSelect() throws PARS_EXC {
		Expression ref = this.parseArraySelect();
		
		boolean dot = false;
		
		if (current.type == TokenType.DOT) {
			dot = true;
			accept();
			ref = new StructSelect(ref, this.parseStructSelect(), false, ref.getSource());
		}
		else if (current.type == TokenType.UNION_ACCESS) {
			accept();
			ref = new StructSelect(ref, this.parseStructSelect(), true, ref.getSource());
		}
		
		if (ref instanceof StructSelect) {
			StructSelect select = (StructSelect) ref;
			
			/* 
			 * Nested call, transform AST by nesting the chained calls within each other.
			 * Only do the transformation if the head of the select is not an inline call,
			 * because if this is the case, this means we are currently recursiveley in the
			 * middle of the chain. We need a head to start the chain, which was already
			 * parsed, but not attatched. Just return, a previous recursice call will transform
			 * the AST eventually.
			 */
			if (!(select.selector instanceof InlineCall)) {
				if (select.selection instanceof InlineCall) {
					/* Single call */
					InlineCall call = (InlineCall) select.selection;
					call.isNestedCall = true;
					call.nestedDeref = !dot;
					
					if (dot) call.parameters.add(0, new AddressOf(select.selector, select.selection.getSource()));
					else call.parameters.add(0, select.selector);
					
					ref = call;
				}
				else if (select.selection instanceof FunctionRef && select.selector instanceof IDRef) {
					FunctionRef base = (FunctionRef) select.selection;
					base.base = (IDRef) select.selector;
					ref = base;
				}
				else if (select.selection instanceof StructSelect && ((StructSelect) select.selection).selector instanceof InlineCall) {
					/* Chained nested call */
					StructSelect nested = (StructSelect) select.selection;
					
					InlineCall call = (InlineCall) nested.selector;
					call.isNestedCall = true;
					call.nestedDeref = !dot;
					
					/* Nest based on the chain head an address of of the head or just the head in the call parameters */
					if (dot) call.parameters.add(0, new AddressOf(select.selector, select.selection.getSource()));
					else call.parameters.add(0, select.selector);
					
					/* Multiple chains */
					if (((StructSelect) select.selection).selection instanceof StructSelect) {
						nested = (StructSelect) (((StructSelect) select.selection).selection);
						
						while (nested instanceof StructSelect) {
							
							/* Nest the previous call as parameter in the next call */
							InlineCall call0 = (InlineCall) nested.selector;
							call0.isNestedCall = true;
							
							call0.parameters.add(0, call);
							
							call = call0;
							
							if (nested.selection instanceof StructSelect) 
								nested = (StructSelect) nested.selection;
							else break;
						}
					}
					
					/* Final call in chain, nest the current call as parameter in the final call */
					InlineCall end = (InlineCall) nested.selection;
					end.isNestedCall = true;
					
					end.parameters.add(0, call);
					
					call = end;
					ref = call;
				}
			}
		}
		
		return ref;
	}
	
	/**
	 * Parses an array select operation for a chain of operands. The parsed pattern is:<br>
	 * <br>
	 * 		<code>EXPRESSION([EXPRESSION])+</code>
	 */
	private Expression parseArraySelect() throws PARS_EXC {
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
	
	/**
	 * Parses an range of different base operations. The parsed patterns are:<br>
	 * <br>
	 * 		<code>(EXPRESSION)</code><br>
	 * <br>
	 * 		<code>ATOM</code>
	 */
	private Expression parseAtom() throws PARS_EXC {
		if (current.type == TokenType.LPAREN) {
			accept();
			Expression expression = this.parseExpression();
			accept(TokenType.RPAREN);
			return this.wrapPlaceholder(expression);
		}
		else if (current.type == TokenType.NULL) {
			Token id = accept();
			CompilerDriver.null_referenced = true;
			return this.wrapPlaceholder(new Atom(new NULL(), id.source()));
		}
		else if (current.type == TokenType.IDENTIFIER || current.type == TokenType.ENUMID || current.type == TokenType.NAMESPACE_IDENTIFIER) {
			Source source = current.source();
			
			NamespacePath path = this.parseNamespacePath();
			
			if (current.type == TokenType.COLON && tokenStream.get(0).type == TokenType.COLON) {
				this.progress.abort();
				throw new SNIPS_EXC("Unknown namespace '" + path + "', " + source.getSourceMarker());
			}
			
			/* Convert next token */
			if (this.activeProvisos.contains(this.tokenStream.get(0).spelling)) 
				this.tokenStream.get(0).type = TokenType.PROVISO;
			
			if (current.type == TokenType.LPAREN || (current.type == TokenType.CMPLT && (tokenStream.get(0).type.group() == TokenGroup.TYPE || tokenStream.get(0).type == TokenType.CMPGT))) {
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
					
					this.checkAutoInclude(path.getPath().get(0));
					
					return this.wrapPlaceholder(new InlineCall(path, proviso, parameters, source));
				}
			}
			else if (current.type == TokenType.DOT && (tokenStream.get(0).type == TokenType.ENUMLIT || path.termination == PATH_TERMINATION.ENUM)) {
				accept(TokenType.DOT);
				
				if (current.type != TokenType.ENUMLIT) {
					this.progress.abort();
					throw new SNIPS_EXC(Const.UNKNOWN_ENUM_FIELD, current.spelling, path, source.getSourceMarker());
				}
				
				/* Actual enum field value */
				Token value = accept(TokenType.ENUMLIT);
				
				/* Get enum type mapped to this value */
				EnumTypedef def = this.getEnumTypedef(path, source);
				
				if (def == null) {
					this.progress.abort();
					throw new SNIPS_EXC(Const.UNKNOWN_ENUM, path, source.getSourceMarker());
				}
				
				return this.wrapPlaceholder(new Atom(def.getEnumField(value.spelling, source), source));
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
					for (int a = 0; a < scope.size(); a++) 
						if (scope.get(a).path.equals(path)) 
							/* Path referres to a variable, set lambda to null */
							lambda = null;
				}
				
				if (lambda != null) 
					/* Predicate without proviso */
					return this.wrapPlaceholder(new FunctionRef(new ArrayList(), lambda, source));
				else 
					/* Identifier Reference */
					return this.wrapPlaceholder(new IDRef(path, source));
			}
		}
		else if (current.type == TokenType.STRUCTID && this.tokenStream.get(2).type == TokenType.IDENTIFIER) {
			Source source = current.source();
			
			/* Static nested function call */
			Token sid = accept();
			
			accept(TokenType.COLON);
			accept(TokenType.COLON);
			
			StructTypedef def = this.getStructTypedef(new NamespacePath(sid.spelling), source);
			
			NamespacePath path = def.path.clone();
			
			path.path.add(accept(TokenType.IDENTIFIER).spelling);
			
			/* Convert next token */
			if (this.activeProvisos.contains(this.tokenStream.get(0).spelling)) 
				this.tokenStream.get(0).type = TokenType.PROVISO;
			
			List<TYPE> proviso = this.parseProviso();
			
			/* Inline Call */
			accept(TokenType.LPAREN);
			
			List<Expression> parameters = new ArrayList();
			while (current.type != TokenType.RPAREN) {
				parameters.add(this.parseExpression());
				if (current.type != TokenType.COMMA) break;
				accept(TokenType.COMMA);
			}
			accept(TokenType.RPAREN);
			
			this.checkAutoInclude(path.getPath().get(0));
			
			return this.wrapPlaceholder(new InlineCall(path, proviso, parameters, source));
		}
		else if (current.type == TokenType.DIRECTIVE) {
			/* Direct Reg Targeting */
			Source source = accept().source();
			Token reg = accept(TokenType.IDENTIFIER);
			return new RegisterAtom(reg, source);
		}
		else if (current.type == TokenType.INTLIT) {
			Token token = accept();
			return this.wrapPlaceholder(new Atom(new INT(token.spelling), token.source));
		}
		else if (current.type == TokenType.CHARLIT) {
			Token token = accept();
			return this.wrapPlaceholder(new Atom(new CHAR(token.spelling), token.source));
		}
		else if (current.type == TokenType.STRINGLIT) {
			Token token = accept();
			List<Expression> charAtoms = new ArrayList();
			String [] sp = token.spelling.split("");
			
			/* Create a list of expressions of char atoms */
			for (int i = 0; i < sp.length; i++) 
				charAtoms.add(new Atom(new CHAR(sp [i]), token.source));
			
			/* Insert null-termination character */
			charAtoms.add(new Atom(new CHAR(null), token.source));
			
			return new ArrayInit(charAtoms, false, token.source());
		}
		else if (current.type == TokenType.BOOLLIT) {
			Token token = accept();
			return this.wrapPlaceholder(new Atom(new BOOL(token.spelling), token.source));
		}
		else if (this.checkPlaceholder()) {
			/* Pure placeholder token */
			Token token = current;
			
			accept();
			accept();
			accept();
			
			return new TempAtom(null, token.source());
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
					if (this.progress != null) this.progress.abort();
					throw new PARS_EXC(current.source, current.type, TokenType.LPAREN, TokenType.IDENTIFIER, TokenType.INTLIT);
				}
				else {
					curr0 = current;
					return this.parseExpression();
				}
			}
		}
	}
	
	/**
	 * Check if the given name is either resv, init, free or hsize. If yes, 
	 * add this library with the prefix 'lib/mem/*' to the referenced libraries
	 * in the compiler driver, so they are included in the result.
	 * 
	 * @param name The name of the function, does not have to match one of the
	 * 		listed names. Should not be null.
	 */
	private void checkAutoInclude(String name) {
		if (name.equals("resv")) {
			CompilerDriver.heap_referenced = true;
			CompilerDriver.driver.referencedLibaries.add("release/lib/mem/resv.sn");
		}
		else if (name.equals("init")) {
			CompilerDriver.driver.referencedLibaries.add("release/lib/mem/resv.sn");
			CompilerDriver.driver.referencedLibaries.add("release/lib/mem/init.sn");
		}
		else if (name.equals("isa") || name.equals("isar")) {
			CompilerDriver.driver.referencedLibaries.add("release/lib/mem/isa.sn");
		}
		else if (name.equals("free")) {
			CompilerDriver.driver.referencedLibaries.add("release/lib/mem/free.sn");
		}
		else if (name.equals("hsize")) {
			CompilerDriver.driver.referencedLibaries.add("release/lib/mem/hsize.sn");
		}
	}

	/**
	 * Wraps a placeholder operation for a single operand. The parsed pattern is:<br>
	 * <br>
	 * 		<code>EXPRESSION(...)?</code><br>
	 * <br>
	 * Returns the given expression if '...' is not seen.
	 */
	private Expression wrapPlaceholder(Expression base) {
		if (this.checkPlaceholder()) {
			Source source = current.source();
			
			/* Accept ... */
			accept();
			accept();
			accept();
			
			return new TempAtom(base, source);
		}
		else return base;
	}
	
	/**
	 * Returns true iff:<br>
	 * 		- The current token is a '.'<br>
	 * 		- The next token is a '.'<br>
	 * 		- The token after that is a '.'
	 */
	private boolean checkPlaceholder() {
		return (current.type == TokenType.DOT && tokenStream.get(0).type == TokenType.DOT && tokenStream.get(1).type == TokenType.DOT);
	}
	
	private Function findFunction(NamespacePath path) {
		Function lambda = null;
		
		for (Pair<NamespacePath, Function> p : this.functions) 
			if (p.first.equals(path)) 
				return p.second;

		if (lambda == null) {
			if (path.path.size() == 1) {
				List<Function> f0 = new ArrayList();
				
				for (Pair<NamespacePath, Function> p : this.functions) 
					if (p.first.getLast().equals(path.getLast())) 
						f0.add(p.second);

				/* Return if there is only one result */
				if (f0.size() == 1) return f0.get(0);
			}
		}
		
		return null;
	}
	
	/* Anchor used for expression parsing. */
	private Token curr0 = null;
	
	/**
	 * Attempts to find a interface typedef based on the given namespace path.
	 * 
	 * @param path The path that is equal or similar to the path of the searched typedef.
	 * @param source The source of the syntax element from where this method was called.
	 * 
	 * @return The InterfaceTypedef that matches the give path.
	 * 
	 * @throws SNIPS_EXC When multiple matches are found for the given path.
	 */
	private InterfaceTypedef getInterfaceTypedef(NamespacePath path, Source source) {
		for (Pair<NamespacePath, InterfaceTypedef> p : this.interfaceIds) 
			if (p.getFirst().equals(path)) 
				return p.getSecond();

		List<InterfaceTypedef> defs = new ArrayList();
		for (Pair<NamespacePath, InterfaceTypedef> p : this.interfaceIds) 
			if (p.getFirst().getLast().equals(path.getLast())) 
				defs.add(p.getSecond());

		if (defs.isEmpty()) return null;
		else if (defs.size() == 1) {
			return defs.get(0);
		}
		else {
			this.progress.abort();
			String s = defs.stream().map(x -> x.path.build()).collect(Collectors.joining(", "));
			throw new SNIPS_EXC(Const.MULTIPLE_MATCHES_FOR_STRUCT_TYPE, path, s, source.getSourceMarker());
		}
	}
	
	/**
	 * Attempts to find a struct typedef based on the given namespace path.
	 * 
	 * @param path The path that is equal or similar to the path of the searched typedef.
	 * @param source The source of the syntax element from where this method was called.
	 * 
	 * @return The StructTypedef that matches the give path.
	 * 
	 * @throws SNIPS_EXC When multiple matches are found for the given path.
	 */
	private StructTypedef getStructTypedef(NamespacePath path, Source source) {
		for (Pair<NamespacePath, StructTypedef> p : this.structIds) 
			if (p.getFirst().equals(path)) 
				return p.getSecond();

		List<StructTypedef> defs = new ArrayList();
		for (Pair<NamespacePath, StructTypedef> p : this.structIds) 
			if (p.getFirst().getLast().equals(path.getLast())) 
				defs.add(p.getSecond());

		if (defs.isEmpty()) return null;
		else {
			// TODO Make sure only one header and one implementation exists
			return defs.get(0);
		}
	}
	
	/**
	 * Attempts to find a enum typedef based on the given namespace path.
	 * 
	 * @param path The path that is equal or similar to the path of the searched typedef.
	 * @param source The source of the syntax element from where this method was called.
	 * 
	 * @return The EnumTypedef that matches the give path.
	 * 
	 * @throws SNIPS_EXC When multiple matches are found for the given path.
	 */
	private EnumTypedef getEnumTypedef(NamespacePath path, Source source) {
		for (Pair<NamespacePath, EnumTypedef> p : this.enumIds) 
			if (p.getFirst().equals(path)) 
				return p.getSecond();

		List<EnumTypedef> defs = new ArrayList();
		for (Pair<NamespacePath, EnumTypedef> p : this.enumIds) 
			if (p.getFirst().getLast().equals(path.getLast())) 
				defs.add(p.getSecond());

		if (defs.isEmpty()) return null;
		else if (defs.size() == 1) {
			return defs.get(0);
		}
		else {
			this.progress.abort();
			String s = defs.stream().map(x -> x.path.build()).collect(Collectors.joining(", "));
			throw new SNIPS_EXC(Const.MULTIPLE_MATCHES_FOR_ENUM_TYPE, path, s, source.getSourceMarker());
		}
	}

	/**
	 * Returns true iff a InterfaceTypedef exists in the {@link #interfaceIds} list which path
	 * ends with the given string.
	 */
	private boolean containsInterfaceTypedef(String name) {
		for (Pair<NamespacePath, InterfaceTypedef> p : this.interfaceIds) 
			if (p.getFirst().getLast().equals(name)) 
				return true;

		return false;
	}
	
	/**
	 * Returns true iff a StructTypedef exists in the {@link #structIds} list which path
	 * ends with the given string.
	 */
	private boolean containsStructTypedef(String name) {
		for (Pair<NamespacePath, StructTypedef> p : this.structIds) 
			if (p.getFirst().getLast().equals(name)) 
				return true;
			
		return false;
	}
	
	/**
	 * Returns true iff a EnumTypedef exists in the {@link #structIds} list which path
	 * ends with the given string.
	 */
	private boolean containsEnumTypedef(String token) {
		for (Pair<NamespacePath, EnumTypedef> p : this.enumIds) 
			if (p.getFirst().getLast().equals(token)) 
				return true;

		return false;
	}

	private TYPE parseType() throws PARS_EXC {
		TYPE type = null;
		Token token = null;
		
		if (current.type == TokenType.FUNC) token = accept();
		else if (current.type == TokenType.IDENTIFIER) token = accept();
		else if (current.type == TokenType.NAMESPACE_IDENTIFIER) token = accept();
		else token = accept(TokenGroup.TYPE);
		
		if (token.type() == TokenType.AUTO) 
			/* Auto type is always a standalone, so no wrapping in Pointer etc. */
			return new AUTO();
		
		InterfaceTypedef intf = null;
		StructTypedef stru = null;
		EnumTypedef enu = null;
		NamespacePath path = null;
		
		if (this.containsInterfaceTypedef(token.spelling) || this.containsStructTypedef(token.spelling) || this.containsEnumTypedef(token.spelling) || 
				(current.type == TokenType.COLON && 
				tokenStream.get(0).type == TokenType.COLON && 
				tokenStream.get(1).type != TokenType.LPAREN)) {
			
			path = this.parseNamespacePath(token);

			/* Search with relative path */
			intf = this.getInterfaceTypedef(path, token.source());
			
			/* Nothing found, attempt to convert to current absolut path and try again */
			if (intf == null) {
				path.path.addAll(0, this.buildPath().path);
				intf = this.getInterfaceTypedef(path, token.source());
			}
			
			/* Search with relative path */
			stru = this.getStructTypedef(path, token.source());
			
			/* Nothing found, attempt to convert to current absolut path and try again */
			if (stru == null) {
				path.path.addAll(0, this.buildPath().path);
				stru = this.getStructTypedef(path, token.source());
			}
			
			/* Search with relative path */
			enu = this.getEnumTypedef(path, token.source());
			
			/* Nothing found, attempt to convert to current absolut path and try again */
			if (enu == null) {
				path.path.addAll(0, this.buildPath().path);
				enu = this.getEnumTypedef(path, token.source());
			}
			
			/* Nothing found, error */
			if (enu == null && stru == null && intf == null) {
				this.progress.abort();
				throw new SNIPS_EXC(Const.UNKNOWN_STRUCT_OR_ENUM_OR_INTERFACE, path, token.source().getSourceMarker());
			}
		}
		
		if (intf != null) {
			/* Parse the provided proviso */
			List<TYPE> proviso = this.parseProviso();
			
			/* Construct new Interface Instance with reference to SSOT */
			type = new INTERFACE(intf, proviso);
		}
		else if (stru != null) {
			/* Parse the provided proviso */
			List<TYPE> proviso = this.parseProviso();
			
			/* Construct new Struct Instance with reference to SSOT */
			type = new STRUCT(stru, proviso);
		}
		else if (enu != null) {
			/* Enum def was found, set reference to default enum field */
			type = enu.enumType;
		}
		else if (token.type == TokenType.FUNC) {
			Source source = current.source();
			
			List<TYPE> proviso = new ArrayList();
			List<TYPE> types = new ArrayList();
			
			TYPE ret = null;
			
			boolean anonymous = false;
			
			/* Convert next token */
			if (this.activeProvisos.contains(current.spelling)) 
				current.type = TokenType.PROVISO;
			
			/* Non-anonymous */
			if (current.type != TokenType.IDENTIFIER && current.type != TokenType.RPAREN && current.type != TokenType.LBRACKET && current.type != TokenType.MUL) {
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
			
			if (anonymous) {
				/* Anonymous call, pass null function */
				type = new FUNC(null, proviso);
			}
			else {
				/* Need identifer next, or RPAREN when type casting */
				if (current.type != TokenType.IDENTIFIER && current.type != TokenType.RPAREN) {
					accept(TokenType.IDENTIFIER);
				}
				
				Token id = current;
				List<String> path0 = new ArrayList();
				path0.add(id.spelling);
				
				List<Declaration> params = new ArrayList();
				int c = 0;
				for (TYPE t : types) {
					params.add(new Declaration(new NamespacePath("param" + c++), t, null, source));
				}
				
				/* Wrap parsed function head in function object, wrap created head in declaration */
				Function lambda = new Function(ret, new NamespacePath(path0, PATH_TERMINATION.UNKNOWN), new ArrayList(), params, false, new ArrayList(), new ArrayList(), MODIFIER.SHARED, source);
				lambda.isLambdaHead = true;
				
				type = new FUNC(lambda, proviso);
			}
		}
		else {
			/* Create a new type from the token type */
			if (token.type() == TokenType.INT) type = new INT();
			else if (token.type() == TokenType.BOOL) type = new BOOL();
			else if (token.type() == TokenType.CHAR) type = new CHAR();
			else if (token.type() == TokenType.VOID) type = new VOID();
			else if (token.type() == TokenType.FUNC) type = new FUNC();
			else {
				type = new PROVISO(token.spelling());
				
				if (!this.activeProvisos.contains(token.spelling)) 
					this.activeProvisos.add(token.spelling);
			}
		}
		
		while (true) {
			Token c0 = current;
			
			while (current.type == TokenType.MUL) {
				accept();
				type = new POINTER(type);
			}
			
			/* Need to flip dimensions from parsed order */
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
	
	private List<TYPE> parseProviso() throws PARS_EXC {
		List<TYPE> pro = new ArrayList();
		
		if (current.type == TokenType.CMPLT) {
			/* Set type of all identifiers to proviso until a CMPGT */
			for (int i = 0; i < this.tokenStream.size(); i++) {
				if (this.tokenStream.get(i).type == TokenType.CMPGT || 
						(this.tokenStream.get(i).type != TokenType.COMMA &&
						this.tokenStream.get(i).type != TokenType.IDENTIFIER &&
						this.tokenStream.get(i).type != TokenType.CMPGT)) break;
				
				if (this.tokenStream.get(i).type == TokenType.IDENTIFIER) 
					this.tokenStream.get(i).type = TokenType.PROVISO;
			}
			
			accept();
			
			while (current.type != TokenType.CMPGT) {
				TYPE type = this.parseType();
				
				if (current.type == TokenType.COLON) {
					accept();
					
					TYPE def = this.parseType();
					
					if (!(type instanceof PROVISO)) 
						throw new SNIPS_EXC("Cannot parse a default proviso at this location, " + current.source().getSourceMarker() + " (" + CompilerDriver.inputFile.getPath() + ")");
				
					PROVISO prov = (PROVISO) type;
					prov.defaultContext = def;
				}
				
				pro.add(type);
				
				if (current.type == TokenType.COMMA) accept();
				else break;
			}
			
			accept(TokenType.CMPGT);
		}
		
		return pro;
	}
	
	private NamespacePath parseNamespacePath() throws PARS_EXC {
		Token token;
		
		if (current.type == TokenType.INTERFACEID) {
			return new NamespacePath(accept().spelling, PATH_TERMINATION.INTERFACE);
		}
		else if (current.type == TokenType.STRUCTID) {
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
	
	private NamespacePath parseNamespacePath(Token first) throws PARS_EXC {
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
				if (current.type == TokenType.INTERFACEID) {
					term = PATH_TERMINATION.INTERFACE;
					ids.add(accept().spelling);
				}
				else if (current.type == TokenType.STRUCTID) {
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
	
	/**
	 * Parses a collection of statements into a list. The parsed pattern is:<br>
	 * <br>
	 * 		<code>({STATEMENT*})|STATEMENT</code><br>
	 * <br>
	 * @param forceBraces If set to true, braces around the body must exist. If set to false,
	 * 		only a single statement without braces is parsed.
	 */
	private List<Statement> parseCompoundStatement(boolean forceBraces) throws PARS_EXC {
		this.scopes.push(new ArrayList());
		
		List<Statement> body = new ArrayList();
		
		/* Compound Statement with braces */
		if (current.type == TokenType.LBRACE || forceBraces) {
			accept(TokenType.LBRACE);
			
			boolean push = true;
			while (current.type != TokenType.RBRACE) {
				if (push) this.bufferedAnnotations.push(new ArrayList());
				push = true;
				
				Statement s = this.parseStatement();
				
				if (s != null) {
					s.activeAnnotations.addAll(this.bufferedAnnotations.pop());
					body.add(s);
				}
				else push = false;
			}
			
			accept(TokenType.RBRACE);
		}
		/* Without braces, one statement only */
		else {
			boolean push = true;
			while (true) {
				if (push) this.bufferedAnnotations.push(new ArrayList());
				push = true;
				
				Statement s = this.parseStatement();
				
				if (s != null) {
					s.activeAnnotations.addAll(this.bufferedAnnotations.pop());
					body.add(s);
					break;
				}
				else push = false;
			}
		}
		
		this.scopes.pop();
		return body;
	}
	
	/**
	 * Parses a single modifier. The parsed pattern is:<br>
	 * <br>
	 * 		<code>MODIFIER?</code><br>
	 * <br>
	 * Defaults to SHARED modifier if no modifier exists is not seen.
	 */
	private MODIFIER parseModifier() {
		MODIFIER mod = MODIFIER.SHARED;
		
		if (current.type.group() == TokenGroup.MODIFIER) {
			Token modT = accept();
			mod = this.resolve(modT);
		}
		
		return mod;
	}
	
	/**
	 * Converts given token spelling to the corresponding modifier enum. 
	 * Defaults to SHARED modifier if the token spelling does not match any modifier.
	 */
	private MODIFIER resolve(Token token) {
		for (MODIFIER mod : MODIFIER.values())
			if (token.spelling.toUpperCase().equals(mod.toString()))
				return mod;
		
		/* Add a warning to signal the defaulting to SHARED modifier */
		buffered.add(new Message(String.format(Const.UNKNOWN_MODIFIER, token.spelling), LogPoint.Type.WARN, true));
		
		return MODIFIER.SHARED;
	}
	
} 
