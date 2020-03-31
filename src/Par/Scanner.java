package Par;

import java.util.ArrayList;
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
		
		List<Token> directives = new ArrayList();
		
		ScannerFSM sFSM = new ScannerFSM(new LinkedList());
		for (int i = 0; i < input.size(); i++) {
			input.set(i, input.get(i).replace("\t", "    "));
			for (int a = 0; a < input.get(i).length(); a++) {
				if (input.get(i).charAt(a) == '#') {
					String dir = input.get(i).substring(a);
					directives.add(new Token(TokenType.DIRECTIVE, new Source(i, a), dir));
					a = input.get(i).length();
					break;
				}
				else sFSM.readChar(input.get(i).charAt(a), i + 1, a);
			}
		}
		
		LinkedList<Token> tokens = sFSM.tokens;
		for (int i = 0; i < directives.size(); i++)tokens.add(i, directives.get(i));
		tokens.add(new Token(TokenType.EOF, new Source(0, 0), null));
		
		return tokens;
	}
	
	private static class ScannerFSM {
		
		/**
		 * Defines the current accumulation state of the scanner.
		 */
		private enum ACCUM_STATE {
			NONE, ID, INT, FLOAT
		}
		
		private ACCUM_STATE ACC_STATE = ACCUM_STATE.NONE;
		
		private LinkedList<Token> tokens;
		
		private String buffer = "";
		
		public ScannerFSM(LinkedList<Token> tokens) {
			this.tokens = tokens;
		}
		
		public boolean readChar(char c, int i, int a) {
			if (this.buffer.isEmpty() && ("" + c).trim().equals(""))return false;
			else {
				this.buffer = this.buffer.trim();
				this.buffer = buffer + c;
				return this.checkState(i, a);
			}
		}
		
		public boolean checkState(int i, int a) {
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
				this.ACC_STATE = ACCUM_STATE.NONE;
				this.emptyBuffer();
			}
			else if (this.buffer.equals("}")) {
				tokens.add(new Token(TokenType.RBRACE, new Source(i, a)));
				this.emptyBuffer();
			}
			else if (this.buffer.equals(";")) {
				tokens.add(new Token(TokenType.SEMICOLON, new Source(i, a)));
				this.emptyBuffer();
			}
			else if (this.buffer.equals(",")) {
				tokens.add(new Token(TokenType.COMMA, new Source(i, a)));
				this.emptyBuffer();
			}
			else if (this.buffer.equals("*")) {
				tokens.add(new Token(TokenType.MUL, new Source(i, a)));
				this.emptyBuffer();
			}
			else if (this.buffer.equals("/")) {
				tokens.add(new Token(TokenType.DIV, new Source(i, a)));
				this.emptyBuffer();
			}
			else if (this.buffer.equals("+")) {
				tokens.add(new Token(TokenType.ADD, new Source(i, a - this.buffer.length()), this.buffer.substring(0, 1)));
				this.buffer = this.buffer.substring(1);
				this.checkState(i, a);
			}
			else if (this.buffer.equals("-")) {
				tokens.add(new Token(TokenType.SUB, new Source(i, a - this.buffer.length()), this.buffer.substring(0, 1)));
				this.buffer = this.buffer.substring(1);
				this.checkState(i, a);
			}
			else if (this.buffer.equals("if")) {
				tokens.add(new Token(TokenType.IF, new Source(i, a)));
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
			else if (this.buffer.equals("int")) {
				tokens.add(new Token(TokenType.INT, new Source(i, a)));
				this.emptyBuffer();
				this.ACC_STATE = ACCUM_STATE.NONE;
				return true;
			}
			else if (this.buffer.equals("bool")) {
				tokens.add(new Token(TokenType.BOOL, new Source(i, a)));
				this.emptyBuffer();
				this.ACC_STATE = ACCUM_STATE.NONE;
				return true;
			}
			else if (this.buffer.equals("return")) {
				tokens.add(new Token(TokenType.RETURN, new Source(i, a)));
				this.emptyBuffer();
				this.ACC_STATE = ACCUM_STATE.NONE;
				return true;
			}
			else if (this.buffer.equals("while")) {
				tokens.add(new Token(TokenType.WHILE, new Source(i, a)));
				this.emptyBuffer();
				this.ACC_STATE = ACCUM_STATE.NONE;
				return true;
			}
			else if (this.buffer.equals("||")) {
				tokens.add(new Token(TokenType.OR, new Source(i, a)));
				this.emptyBuffer();
			}
			else if (this.buffer.equals("&&")) {
				tokens.add(new Token(TokenType.AND, new Source(i, a)));
				this.emptyBuffer();
			}
			else if (this.buffer.startsWith("!")) {
				if (this.buffer.length() == 1)return false;
				if (this.buffer.equals("!=")) {
					tokens.add(new Token(TokenType.CMPNE, new Source(i, a), this.buffer));
					this.emptyBuffer();
				}
				else {
					tokens.add(new Token(TokenType.NOT, new Source(i, a), this.buffer.substring(0, 1)));
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
				if (this.buffer.matches("([a-z]|[A-Z])([a-z]|[A-Z]|[0-9])*")) {
					this.ACC_STATE = ACCUM_STATE.ID;
				}
				else if (this.buffer.matches("[0-9]+")) {
					this.ACC_STATE = ACCUM_STATE.INT;
				}
				
				if ((this.buffer.endsWith(" ") || !this.buffer.matches("([a-z]|[A-Z])([a-z]|[A-Z]|[0-9])*")) && this.ACC_STATE == ACCUM_STATE.ID) {
					String ID = this.buffer.substring(0, this.buffer.length() - 1);
					tokens.add(new Token(TokenType.IDENTIFIER, new Source(i, a), ID));
					
					this.buffer = this.buffer.substring(this.buffer.length() - 1);
					this.ACC_STATE = ACCUM_STATE.NONE;
					this.checkState(i, a);
				}
				if ((this.buffer.endsWith(" ") || !this.buffer.matches("[0-9]+")) && this.ACC_STATE == ACCUM_STATE.INT) {
					tokens.add(new Token(TokenType.INTLIT, new Source(i, a), this.buffer.substring(0, this.buffer.length() - 1)));
					this.buffer = this.buffer.substring(this.buffer.length() - 1);
					this.ACC_STATE = ACCUM_STATE.NONE;
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
