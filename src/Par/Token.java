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
		
		IDENTIFIER("", 				TokenGroup.IDENTIFIER),
		
		/* Statements */
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
		
		/* Functions */
		RETURN("return"),
		
		SEMICOLON(";"),
		COLON(":"),
		COMMA(","),
		LET("="),
		TERN("?"),
		
		/* Arithmetic Operators */
		ADD("+"),
		SUB("-"),
		MUL("*"),
		DIV("/"),
		
		/* Logic Operators */
		OR("||"),
		AND("&&"),
		NOT("!"),
		
		/* Comparators */
		CMPEQ("==", 				TokenGroup.COMPARE),
		CMPNE("!=", 				TokenGroup.COMPARE),
		CMPLE("<=", 				TokenGroup.COMPARE),
		CMPLT("<",					TokenGroup.COMPARE),
		CMPGE(">=", 				TokenGroup.COMPARE),
		CMPGT(">", 					TokenGroup.COMPARE),
		
		/* Shift Operators */
		LSL("<<"),
		LSR(">>"),
		
		/* Types */
		INT("int", 					TokenGroup.TYPE),
		BOOL("bool", 				TokenGroup.TYPE),
		TYPE("type", 				TokenGroup.TYPE),
		
		/* Primitive Literals */
		INTLIT("intlit",			TokenGroup.LITERAL);
		
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
