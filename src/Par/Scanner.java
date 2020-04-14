package Par;

import java.util.LinkedList;
import java.util.List;

import Par.Token.TokenType;
import Util.Source;

public class Scanner {

	List<String> input;
	
	public Scanner(List<String> input) {
		this.input = input;
	}
	
	public LinkedList<Token> scan() {
		ScannerFSM sFSM = new ScannerFSM(new LinkedList());
		
		for (int i = 0; i < input.size(); i++) {
			input.set(i, input.get(i).replace("\t", "    "));
			for (int a = 0; a < input.get(i).length(); a++) {
				sFSM.readChar(input.get(i).charAt(a), i + 1, a);
			}
		}
		
		LinkedList<Token> tokens = sFSM.tokens;
		tokens.add(new Token(TokenType.EOF, new Source(0, 0), null));
		
		return tokens;
	}
	
	private static class ScannerFSM {
		
		//List<String> structIds = new ArrayList();
		
		/**
		 * Defines the current accumulation state of the scanner.
		 */
		private enum ACC_STATE {
			NONE, ID, STRUCT_ID, INT, FLOAT, COMMENT
		}
		
		private ACC_STATE state = ACC_STATE.NONE;
		
		private int lastLine = 0;
		
		private LinkedList<Token> tokens;
		
		private String buffer = "";
		
		public ScannerFSM(LinkedList<Token> tokens) {
			this.tokens = tokens;
		}
		
		public boolean readChar(char c, int i, int a) {
			if (this.buffer.isEmpty() && ("" + c).trim().equals(""))return false;
			else {
				if (this.state != ACC_STATE.COMMENT) this.buffer = this.buffer.trim();
				this.buffer = buffer + c;
				boolean b = this.checkState(i, a);
				this.lastLine = i;
				return b;
			}
		}
		
		public boolean checkState(int i, int a) {
			if (this.state == ACC_STATE.COMMENT) {
				if (i != this.lastLine && this.buffer.startsWith("//")) {
					/* End of single line comment */
					this.state = ACC_STATE.NONE;
					tokens.add(new Token(TokenType.COMMENT, new Source(i, a), this.buffer.substring(3, this.buffer.length() - 1).trim()));
					this.buffer = this.buffer.substring(this.buffer.length() - 1);
				}
				else if ((i != this.lastLine || this.buffer.endsWith("*/")) && this.buffer.startsWith("/*")) {
					if (this.buffer.endsWith("*/")) {
						/* End of multiple line comment */
						this.state = ACC_STATE.NONE;
						this.buffer = this.buffer.trim();
						tokens.add(new Token(TokenType.COMMENT, new Source(i, a), this.buffer.substring(2, this.buffer.length() - 2).trim()));
						this.emptyBuffer();
					}
					else {
						tokens.add(new Token(TokenType.COMMENT, new Source(i, a), this.buffer.substring(2, this.buffer.length() - 1).trim()));
						this.buffer = this.buffer.substring(this.buffer.length() - 1);
					}
				}
				else if (this.buffer.trim().startsWith("*")) {
					if (this.buffer.trim().endsWith("*/")) {
						/* End of multiple line comment */
						this.state = ACC_STATE.NONE;
						this.buffer = this.buffer.trim();
						if (!this.buffer.equals("*/"))
							tokens.add(new Token(TokenType.COMMENT, new Source(i, a), this.buffer.substring(2, this.buffer.length() - 2)));
						
						this.emptyBuffer();
					}
					else if (i != this.lastLine) {
						tokens.add(new Token(TokenType.COMMENT, new Source(i, a), this.buffer.trim().substring(1).trim()));
						this.emptyBuffer();
					}
				}
				return false;
			}
			
			if (this.buffer.equals("(")) {
				tokens.add(new Token(TokenType.LPAREN, new Source(i, a)));
				this.emptyBuffer();
			}
			else if (this.buffer.equals(")")) {
				tokens.add(new Token(TokenType.RPAREN, new Source(i, a)));
				this.emptyBuffer();
			}
			else if (this.buffer.equals("{")) {
				tokens.add(new Token(TokenType.LBRACE, new Source(i, a)));
				this.state = ACC_STATE.NONE;
				this.emptyBuffer();
			}
			else if (this.buffer.equals("}")) {
				tokens.add(new Token(TokenType.RBRACE, new Source(i, a)));
				this.emptyBuffer();
			}
			else if (this.buffer.equals("[")) {
				tokens.add(new Token(TokenType.LBRACKET, new Source(i, a)));
				this.emptyBuffer();
			}
			else if (this.buffer.equals("]")) {
				tokens.add(new Token(TokenType.RBRACKET, new Source(i, a)));
				this.emptyBuffer();
			}
			else if (this.buffer.equals(".")) {
				tokens.add(new Token(TokenType.DOT, new Source(i, a)));
				this.emptyBuffer();
			}
			else if (this.buffer.equals(";")) {
				tokens.add(new Token(TokenType.SEMICOLON, new Source(i, a)));
				this.emptyBuffer();
			}
			else if (this.buffer.equals(":")) {
				tokens.add(new Token(TokenType.COLON, new Source(i, a)));
				this.emptyBuffer();
			}
			else if (this.buffer.equals(",")) {
				tokens.add(new Token(TokenType.COMMA, new Source(i, a)));
				this.emptyBuffer();
			}
			else if (this.buffer.equals("?")) {
				tokens.add(new Token(TokenType.TERN, new Source(i, a)));
				this.emptyBuffer();
			}
			else if (this.buffer.equals("*")) {
				tokens.add(new Token(TokenType.MUL, new Source(i, a)));
				this.emptyBuffer();
			}
			else if (this.buffer.startsWith("/")) {
				if (this.buffer.length() == 1)return false;
				if (this.buffer.equals("//")) {
					this.state = ACC_STATE.COMMENT;
					return false;
				}
				else if (this.buffer.equals("/*")) {
					this.state = ACC_STATE.COMMENT;
					return false;
				}
				else {
					tokens.add(new Token(TokenType.DIV, new Source(i, a - this.buffer.length())));
					this.buffer = this.buffer.substring(1);
					this.checkState(i, a);
				}
			}
			else if (this.buffer.startsWith("+")) {
				if (this.buffer.length() == 1)return false;
				if (this.buffer.equals("++")) {
					tokens.add(new Token(TokenType.INCR, new Source(i, a), this.buffer));
					this.emptyBuffer();
				}
				else {
					tokens.add(new Token(TokenType.ADD, new Source(i, a - this.buffer.length()), this.buffer.substring(0, 1)));
					this.buffer = this.buffer.substring(1);
					this.checkState(i, a);
				}
			}
			else if (this.buffer.startsWith("-")) {
				if (this.buffer.length() == 1)return false;
				if (this.buffer.equals("--")) {
					tokens.add(new Token(TokenType.DECR, new Source(i, a), this.buffer));
					this.emptyBuffer();
				}
				else {
					tokens.add(new Token(TokenType.SUB, new Source(i, a - this.buffer.length()), this.buffer.substring(0, 1)));
					this.buffer = this.buffer.substring(1);
					this.checkState(i, a);
				}
			}
			else if (this.buffer.equals("^")) {
				tokens.add(new Token(TokenType.XOR, new Source(i, a - this.buffer.length()), this.buffer.substring(0, 1)));
				this.buffer = this.buffer.substring(1);
				this.checkState(i, a);
			}
			else if (this.buffer.equals("~")) {
				tokens.add(new Token(TokenType.NOT, new Source(i, a - this.buffer.length()), this.buffer.substring(0, 1)));
				this.buffer = this.buffer.substring(1);
				this.checkState(i, a);
			}
			else if (this.buffer.equals("#")) {
				tokens.add(new Token(TokenType.DIRECTIVE, new Source(i, a)));
				this.emptyBuffer();
			}
			else if (this.buffer.equals("include")) {
				tokens.add(new Token(TokenType.INCLUDE, new Source(i, a)));
				this.emptyBuffer();
			}
			else if (this.buffer.equals("sizeof")) {
				tokens.add(new Token(TokenType.SIZEOF, new Source(i, a)));
				this.emptyBuffer();
			}
			else if (this.buffer.equals("\\")) {
				tokens.add(new Token(TokenType.BACKSL, new Source(i, a)));
				this.emptyBuffer();
			}
			else if (this.buffer.equals("if")) {
				tokens.add(new Token(TokenType.IF, new Source(i, a)));
				this.emptyBuffer();
			}
			else if (this.buffer.equals("true")) {
				tokens.add(new Token(TokenType.BOOLLIT, new Source(i, a), "true"));
				this.emptyBuffer();
			}
			else if (this.buffer.equals("false")) {
				tokens.add(new Token(TokenType.BOOLLIT, new Source(i, a), "false"));
				this.emptyBuffer();
			}
			else if (this.buffer.equals("else")) {
				tokens.add(new Token(TokenType.ELSE, new Source(i, a)));
				this.emptyBuffer();
			}
			else if (this.buffer.equals("||")) {
				tokens.add(new Token(TokenType.OR, new Source(i, a)));
				this.emptyBuffer();
			}
			else if (this.buffer.equals("void")) {
				tokens.add(new Token(TokenType.VOID, new Source(i, a)));
				this.emptyBuffer();
				this.state = ACC_STATE.NONE;
				return true;
			}
			else if (this.buffer.equals("int")) {
				tokens.add(new Token(TokenType.INT, new Source(i, a)));
				this.emptyBuffer();
				this.state = ACC_STATE.NONE;
				return true;
			}
			else if (this.buffer.equals("bool")) {
				tokens.add(new Token(TokenType.BOOL, new Source(i, a)));
				this.emptyBuffer();
				this.state = ACC_STATE.NONE;
				return true;
			}
			else if (this.buffer.equals("return")) {
				tokens.add(new Token(TokenType.RETURN, new Source(i, a)));
				this.emptyBuffer();
				this.state = ACC_STATE.NONE;
				return true;
			}
			else if (this.buffer.equals("break")) {
				tokens.add(new Token(TokenType.BREAK, new Source(i, a)));
				this.emptyBuffer();
				this.state = ACC_STATE.NONE;
				return true;
			}
			else if (this.buffer.equals("continue")) {
				tokens.add(new Token(TokenType.CONTINUE, new Source(i, a)));
				this.emptyBuffer();
				this.state = ACC_STATE.NONE;
				return true;
			}
			else if (this.buffer.equals("while")) {
				tokens.add(new Token(TokenType.WHILE, new Source(i, a)));
				this.emptyBuffer();
				this.state = ACC_STATE.NONE;
				return true;
			}
			else if (this.buffer.equals("do")) {
				tokens.add(new Token(TokenType.DO, new Source(i, a)));
				this.emptyBuffer();
				this.state = ACC_STATE.NONE;
				return true;
			}
			else if (this.buffer.equals("for")) {
				tokens.add(new Token(TokenType.FOR, new Source(i, a)));
				this.emptyBuffer();
				this.state = ACC_STATE.NONE;
				return true;
			}
			else if (this.buffer.equals("switch")) {
				tokens.add(new Token(TokenType.SWITCH, new Source(i, a)));
				this.emptyBuffer();
				this.state = ACC_STATE.NONE;
				return true;
			}
			else if (this.buffer.equals("case")) {
				tokens.add(new Token(TokenType.CASE, new Source(i, a)));
				this.emptyBuffer();
				this.state = ACC_STATE.NONE;
				return true;
			}
			else if (this.buffer.equals("default")) {
				tokens.add(new Token(TokenType.DEFAULT, new Source(i, a)));
				this.emptyBuffer();
				this.state = ACC_STATE.NONE;
				return true;
			}
			else if (this.buffer.startsWith("|")) {
				if (this.buffer.length() == 1)return false;
				if (this.buffer.equals("||")) {
					tokens.add(new Token(TokenType.OR, new Source(i, a)));
					this.emptyBuffer();
				}
				else {
					tokens.add(new Token(TokenType.BITOR, new Source(i, a - this.buffer.length())));
					this.buffer = this.buffer.substring(1);
					this.checkState(i, a);
				}
			}
			else if (this.buffer.startsWith("&")) {
				if (this.buffer.length() == 1)return false;
				if (this.buffer.equals("&&")) {
					tokens.add(new Token(TokenType.AND, new Source(i, a)));
					this.emptyBuffer();
				}
				else {
					tokens.add(new Token(TokenType.ADDROF, new Source(i, a - this.buffer.length())));
					this.buffer = this.buffer.substring(1);
					this.checkState(i, a);
				}
			}
			else if (this.buffer.equals("%")) {
				tokens.add(new Token(TokenType.MOD, new Source(i, a)));
				this.emptyBuffer();
			}
			else if (this.buffer.startsWith("!")) {
				if (this.buffer.length() == 1)return false;
				if (this.buffer.equals("!=")) {
					tokens.add(new Token(TokenType.CMPNE, new Source(i, a), this.buffer));
					this.emptyBuffer();
				}
				else {
					tokens.add(new Token(TokenType.NEG, new Source(i, a), this.buffer.substring(0, 1)));
					this.buffer = this.buffer.substring(1);
					this.checkState(i, a);
				}
			}
			else if (this.buffer.startsWith("=")) {
				if (this.buffer.length() == 1)return false;
				if (this.buffer.equals("==")) {
					tokens.add(new Token(TokenType.CMPEQ, new Source(i, a), this.buffer));
					this.emptyBuffer();
				}
				else {
					tokens.add(new Token(TokenType.LET, new Source(i, a - this.buffer.length()), this.buffer.substring(0, 1)));
					this.buffer = this.buffer.substring(1);
					this.checkState(i, a);
				}
			}
			else if (this.buffer.startsWith("<")) {
				if (this.buffer.length() == 1)return false;
				if (this.buffer.equals("<=")) {
					tokens.add(new Token(TokenType.CMPLE, new Source(i, a), this.buffer));
					this.emptyBuffer();
				}
				else if (this.buffer.equals("<<")) {
					tokens.add(new Token(TokenType.LSL, new Source(i, a), this.buffer));
					this.emptyBuffer();
				}
				else {
					tokens.add(new Token(TokenType.CMPLT, new Source(i, a - this.buffer.length()), this.buffer.substring(0, 1)));
					this.buffer = this.buffer.substring(1);
					this.checkState(i, a);
				}
			}
			else if (this.buffer.startsWith(">")) {
				if (this.buffer.length() == 1)return false;
				if (this.buffer.equals(">=")) {
					tokens.add(new Token(TokenType.CMPGE, new Source(i, a), this.buffer));
					this.emptyBuffer();
				}
				else if (this.buffer.equals(">>")) {
					tokens.add(new Token(TokenType.LSR, new Source(i, a), this.buffer));
					this.emptyBuffer();
				}
				else {
					tokens.add(new Token(TokenType.CMPGT, new Source(i, a - this.buffer.length()), this.buffer.substring(0, 1)));
					this.buffer = this.buffer.substring(1);
					this.checkState(i, a);
				}
			}
			else {
				if (this.buffer.matches("([a-z]|[A-Z]|_)([a-z]|[A-Z]|[0-9]|_)*")) {
					this.state = ACC_STATE.ID;
				}
				else if (this.buffer.matches("[0-9]+")) {
					this.state = ACC_STATE.INT;
				}
				
				if ((this.buffer.endsWith(" ") || !this.buffer.matches("([a-z]|[A-Z]|_)([a-z]|[A-Z]|[0-9]|_)*")) && this.state == ACC_STATE.ID) {
					String ID = this.buffer.substring(0, this.buffer.length() - 1);
					tokens.add(new Token(TokenType.IDENTIFIER, new Source(i, a), ID));
					
					this.buffer = this.buffer.substring(this.buffer.length() - 1);
					this.state = ACC_STATE.NONE;
					this.checkState(i, a);
				}
				if ((this.buffer.endsWith(" ") || !this.buffer.matches("[0-9]+")) && this.state == ACC_STATE.INT) {
					tokens.add(new Token(TokenType.INTLIT, new Source(i, a), this.buffer.substring(0, this.buffer.length() - 1)));
					this.buffer = this.buffer.substring(this.buffer.length() - 1);
					this.state = ACC_STATE.NONE;
					this.checkState(i, a);
				}
			}
			
			return false;
		}
		
		public void emptyBuffer() {
			this.buffer = "";
		}
		
	}
	
}
