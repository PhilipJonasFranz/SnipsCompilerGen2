package Par;

import Util.Source;

public class Token {

	TokenType type;
	
	public String spelling;
	
	Source source;
	
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
	
	public boolean equals(Token token) {
		return this.type == token.type;
	}
	
	public Token clone() {
		Token t = new Token(this.type, this.source.clone(), this.spelling);
		return t;
	}
	
	public TokenType type() {
		return this.type;
	}
	
	public Source getSource() {
		return this.source;
	}
	
	public enum TokenType {
		
		/* Structural */
		COMMENT(""),
		DIRECTIVE("#"),
		AT("@"),
		BACKSL("\\"),
		USCORE("_"),
		STRUCT("struct"),
		ENUM("enum"),
		NAMESPACE("namespace"),
		TRY("try"),
		WATCH("watch"),
		SIGNAL("signal"),
		SIGNALS("signals"),
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
		INCLUDE("include"),
		SIZEOF("sizeof"),
		INSTANCEOF("instanceof"),
		RETURN("return"),
		ASM("asm"),
		
		DOT("."),
		APOS("'"),
		SEMICOLON(";"),
		COLON(":"),
		COMMA(","),
		LET("="),
		TERN("?"),
		
		/* Modifiers */
		SHARED("shared", 			TokenGroup.MODIFIER),
		RESTRICTED("restricted", 	TokenGroup.MODIFIER),
		EXCLUSIVE("exclusive", 		TokenGroup.MODIFIER),
		
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
		FUNC("func", 				TokenGroup.TYPE),
		INT("int", 					TokenGroup.TYPE),
		CHAR("char",				TokenGroup.TYPE),
		BOOL("bool", 				TokenGroup.TYPE),
		PROVISO("",					TokenGroup.TYPE), 
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
		
		final String spelling;
		
		TokenGroup group;
		
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
	}
	
}
