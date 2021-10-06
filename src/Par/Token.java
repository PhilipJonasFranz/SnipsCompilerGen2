package Par;

import Util.Source;

public class Token {

			/* ---< FIELDS >--- */
	TokenType type;
	
	String spelling;
	
	Source source;
	
	boolean markedAsOperator = false;
	
	String operatorSymbol;
	
	
			/* ---< CONSTRUCTORS >--- */
	public Token(TokenType type, Source source, String spelling) {
		this.type = type;
		this.spelling = spelling; 
		this.source = source;
	}
	
	public Token(TokenType type, Source source) {
		this.type = type;
		this.source = source;
		this.spelling = type.spelling;
	}
	
	
			/* ---< METHODS >--- */
	public boolean equals(Token token) {
		return this.type == token.type;
	}
	
	public Token clone() {
		Token token = new Token(this.type, this.source.clone(), this.spelling);
		token.markedAsOperator = this.markedAsOperator;
		token.operatorSymbol = this.operatorSymbol;
		return token;
	}
	
	public TokenType type() {
		return this.type;
	}
	
	public Source source() {
		return this.source;
	}
	
	public String spelling() {
		return this.spelling;
	}
	
			/* ---< NESTED >--- */
	public enum TokenType {
		
		/* Structural */
		COMMENT(""),
		DIRECTIVE("#"),
		BACKSL("\\"),
		EOF("<EOF>"),
		
		LPAREN("("),
		RPAREN(")"),
		LBRACE("{"),
		RBRACE("}"),
		LBRACKET("["),
		RBRACKET("]"),
		
		IDENTIFIER("", 				TokenGroup.IDENTIFIER),
		NAMESPACE_IDENTIFIER("", 	TokenGroup.IDENTIFIER),
		UNION_ACCESS("->"),
		
		/* Keywords */
		IF("if"),
		ELSE("else"),
		WHILE("while"),
		DO("do"),
		FOR("for"),
		BREAK("break"),
		CONTINUE("continue"),
		SWITCH("switch"),
		CASE("case"),
		DEFAULT("default"),
		SIZEOF("sizeof"),
		IDOF("idof"),
		RETURN("return"),
		ASM("asm"),
		INTERFACE("interface"),
		STRUCT("struct"),
		ENUM("enum"),
		NAMESPACE("namespace"),
		TRY("try"),
		WATCH("watch"),
		SIGNAL("signal"),
		SIGNALS("signals"),
		
		DOT("."),
		SEMICOLON(";"),
		COLON(":"),
		COMMA(","),
		LET("="),
		TERN("?"),
		
		/* Modifiers */
		STATIC("static", 			TokenGroup.MODIFIER),
		SHARED("shared", 			TokenGroup.MODIFIER),
		RESTRICTED("restricted", 	TokenGroup.MODIFIER),
		EXCLUSIVE("exclusive", 		TokenGroup.MODIFIER),
		
		/* Overloaded Operator */
		OPERATOR("operator"),
		
		/* Arithmetic Operators */
		ADD("+"),
		SUB("-"),
		MUL("*"),
		DIV("/"),
		MOD("%"),
		INCR("++"),
		DECR("--"),
		
		/* Logic Operators */
		OR("||"),
		AND("&&"),
		NEG("!"),
		NOT("~"),
		XOR("^"),
		BITOR("|"),
		ADDROF("&"),
		
		/* Comparators */
		CMPEQ("==", 				TokenGroup.COMPARE),
		CMPNE("!=", 				TokenGroup.COMPARE),
		CMPLE("<=", 				TokenGroup.COMPARE),
		CMPLT("<",					TokenGroup.COMPARE),
		CMPGE(">=", 				TokenGroup.COMPARE),
		CMPGT(">", 					TokenGroup.COMPARE),
		
		/* Types */
		VOID("void", 				TokenGroup.TYPE),
		AUTO("auto", 				TokenGroup.TYPE),
		FUNC("func", 				TokenGroup.TYPE),
		INT("int", 					TokenGroup.TYPE),
		CHAR("char",				TokenGroup.TYPE),
		BOOL("bool", 				TokenGroup.TYPE),
		PROVISO("",					TokenGroup.TYPE), 
		INTERFACEID("", 			TokenGroup.TYPE),
		STRUCTID("", 				TokenGroup.TYPE),
		ENUMID("", 					TokenGroup.TYPE),
		TYPE("type", 				TokenGroup.TYPE),
		
		/* Primitive Literals */
		INTLIT("intlit",			TokenGroup.LITERAL),
		CHARLIT("charlit",			TokenGroup.LITERAL),
		STRINGLIT("",				TokenGroup.LITERAL),
		BOOLLIT("boollit",			TokenGroup.LITERAL),
		ENUMLIT("enumlit",			TokenGroup.LITERAL),
		NULL("null",				TokenGroup.LITERAL);
		
		public enum TokenGroup {
			IDENTIFIER,
			TYPE,
			LITERAL,
			COMPARE,
			MODIFIER,
			MISC;
		}
		
		private final String spelling;
		
		private TokenGroup group;
		
		private TokenType(String spelling, TokenGroup tokenGroup) {
			this.spelling = spelling;
			this.group = tokenGroup;
		}
		
		private TokenType(String spelling) {
			this.spelling = spelling;
			this.group = TokenGroup.MISC;
		}
		
		public TokenGroup group() {
			return this.group;
		}
		
		public String spelling() {
			return this.spelling;
		}
	}
	
} 
