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
		DIRECTIVE("#"),
		EOF("<EOF>"),
		
		LPAREN("("),
		RPAREN(")"),
		LBRACE("{"),
		RBRACE("}"),
		LBRACKET("["),
		RBRACKET("]"),
		
		/* Structs */
		STRUCT("struct"),
		IDENTIFIER("", 				TokenGroup.IDENTIFIER),
		
		/* Functions */
		RETURN("return"),
		
		/* Control Structures */
		IF("if"),
		ELSE("else"),
		
		/* Loop Statements */
		BREAK("break"),
		FOR("for"),
		WHILE("while"),
		DO("do"),
		
		/* Switch Statement */
		SWITCH("switch"),
		CASE("case"),
		DEFAULT("default"),
		
		COLON(":"),
		SEMICOLON(";"),
		COMMA(","),
		TERN("?"),
		ELEMSEL("."),
		LET("="),
		ADDROF("&"),
		
		/* Compare Operators */
		CMPEQ("==", 				TokenGroup.COMPARE),
		CMPNE("!=", 				TokenGroup.COMPARE),
		CMPLT("<", 					TokenGroup.COMPARE),
		CMPGT(">", 					TokenGroup.COMPARE),
		CMPLE("<=", 				TokenGroup.COMPARE),
		CMPGE(">=",					TokenGroup.COMPARE),
		
		/* Arithmetic Operators */
		ADD("+"),
		SUB("-"),
		MUL("*"),
		DIV("/"),
		LSL("<<"),
		LSR(">>"),
		
		/* Boolean Operators */
		OR("||"),
		AND("&&"),
		NOT("!"),
		
		GLOBAL("global"),
		
		/* Types */
		INT("int", 					TokenGroup.TYPE),
		FLOAT("float", 				TokenGroup.TYPE),
		BOOL("boolean", 			TokenGroup.TYPE),
		CHAR("char", 				TokenGroup.TYPE),
		TYPE("type", 				TokenGroup.TYPE),
		
		ID("", 						TokenGroup.IDENTIFIER),
		
		/* Primitive Literals */
		INTLIT("intlit",			TokenGroup.LITERAL),
		CHARLIT("charlit", 			TokenGroup.LITERAL),
		FLOATLIT("floatlit", 		TokenGroup.LITERAL),
		BOOLLIT("boollit", 			TokenGroup.LITERAL),
		
		TYPEDEF("typedef");
		
		public enum TokenGroup {
			IDENTIFIER,
			TYPE,
			LITERAL,
			COMPARE,
			MISC;
		}
		
		final String spelling;
		
		final TokenGroup group;
		
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
