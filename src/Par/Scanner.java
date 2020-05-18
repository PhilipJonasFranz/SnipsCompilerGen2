package Par;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import Exc.SNIPS_EXCEPTION;
import Par.Token.TokenType;
import PreP.PreProcessor.LineObject;
import Util.Source;
import Util.Logging.ProgressMessage;

public class Scanner {

	List<LineObject> input;
	
	public ProgressMessage progress;
	
	public Scanner(List<LineObject> input, ProgressMessage progress) {
		this.input = input;
		this.progress = progress;
	}
	
	public LinkedList<Token> scan() {
		
		ScannerFSM sFSM = new ScannerFSM(new LinkedList(), progress);
		
		for (int i = 0; i < input.size(); i++) {
			input.get(i).line = input.get(i).line.replace("\t", "    ");
			for (int a = 0; a < input.get(i).line.length(); a++) {
				sFSM.readChar(input.get(i).line.charAt(a), input.get(i).lineNumber, a, input.get(i).fileName);
			}
			
			if (progress != null) {
				progress.incProgress((double) i / input.size());
			}
		}
		
		if (progress != null) progress.incProgress(1);
		
		LinkedList<Token> tokens = sFSM.tokens;
		tokens.add(new Token(TokenType.EOF, new Source(null, 0, 0), null));
		
		return tokens;
	}
	
	private static class ScannerFSM {
		
		List<String> structIds = new ArrayList();
		
		List<String> enumIds = new ArrayList();
		
		List<String> namespaces = new ArrayList();
		
		/**
		 * Defines the current accumulation state of the scanner.
		 */
		private enum ACC_STATE {
			NONE, ID, STRUCT_ID, NAMESPACE_ID, ENUM_ID, INT, HEX_INT, BIN_INT, FLOAT, COMMENT, CHARLIT, STRINGLIT
		}
		
		private ACC_STATE state = ACC_STATE.NONE;
		
		private int lastLine = 0;
		
		private LinkedList<Token> tokens;
		
		private String buffer = "";
		
		private ProgressMessage progress;
		
		public ScannerFSM(LinkedList<Token> tokens, ProgressMessage progress) {
			this.tokens = tokens;
			this.progress = progress;
		}
		
		public boolean readChar(char c, int i, int a, String fileName) {
			if (this.buffer.isEmpty() && ("" + c).trim().equals(""))return false;
			else {
				if (this.state != ACC_STATE.COMMENT && this.state != ACC_STATE.CHARLIT && this.state != ACC_STATE.STRINGLIT) 
					this.buffer = this.buffer.trim();
				this.buffer = buffer + c;
				boolean b = this.checkState(i, a, fileName);
				this.lastLine = i;
				return b;
			}
		}
		
		public boolean checkState(int i, int a, String fileName) {
			if (this.state == ACC_STATE.COMMENT) {
				if (i != this.lastLine && this.buffer.startsWith("//")) {
					/* End of single line comment */
					this.state = ACC_STATE.NONE;
					tokens.add(new Token(TokenType.COMMENT, new Source(fileName, i, a), this.buffer.substring(3, this.buffer.length() - 1).trim()));
					this.buffer = this.buffer.substring(this.buffer.length() - 1);
				}
				else if ((i != this.lastLine || this.buffer.endsWith("*/")) && this.buffer.startsWith("/*")) {
					if (this.buffer.endsWith("*/")) {
						/* End of multiple line comment */
						this.state = ACC_STATE.NONE;
						this.buffer = this.buffer.trim();
						tokens.add(new Token(TokenType.COMMENT, new Source(fileName, i, a), this.buffer.substring(2, this.buffer.length() - 2).trim()));
						this.emptyBuffer();
					}
					else {
						if (!buffer.trim().equals("/*"))
							tokens.add(new Token(TokenType.COMMENT, new Source(fileName, i, a), this.buffer.substring(2, this.buffer.length() - 1).trim()));
						this.buffer = this.buffer.substring(this.buffer.length() - 1);
					}
				}
				else if (this.buffer.trim().startsWith("*")) {
					if (this.buffer.trim().endsWith("*/")) {
						/* End of multiple line comment */
						this.state = ACC_STATE.NONE;
						this.buffer = this.buffer.trim();
						if (!this.buffer.equals("*/"))
							tokens.add(new Token(TokenType.COMMENT, new Source(fileName, i, a), this.buffer.substring(2, this.buffer.length() - 2)));
						
						this.emptyBuffer();
					}
					else if (i != this.lastLine) {
						tokens.add(new Token(TokenType.COMMENT, new Source(fileName, i, a), this.buffer.trim().substring(1).trim()));
						this.emptyBuffer();
					}
				}
				return false;
			}
			
			if (this.buffer.equals("(")) {
				tokens.add(new Token(TokenType.LPAREN, new Source(fileName, i, a)));
				this.emptyBuffer();
			}
			else if (this.buffer.equals(")")) {
				tokens.add(new Token(TokenType.RPAREN, new Source(fileName, i, a)));
				this.emptyBuffer();
			}
			else if (this.buffer.equals("@")) {
				tokens.add(new Token(TokenType.AT, new Source(fileName, i, a)));
				this.emptyBuffer();
			}
			else if (this.buffer.equals("{")) {
				tokens.add(new Token(TokenType.LBRACE, new Source(fileName, i, a)));
				this.state = ACC_STATE.NONE;
				this.emptyBuffer();
			}
			else if (this.buffer.equals("}")) {
				tokens.add(new Token(TokenType.RBRACE, new Source(fileName, i, a)));
				this.emptyBuffer();
			}
			else if (this.buffer.equals("[")) {
				tokens.add(new Token(TokenType.LBRACKET, new Source(fileName, i, a)));
				this.emptyBuffer();
			}
			else if (this.buffer.equals("]")) {
				tokens.add(new Token(TokenType.RBRACKET, new Source(fileName, i, a)));
				this.emptyBuffer();
			}
			else if (this.buffer.equals("'")) {
				this.state = ACC_STATE.CHARLIT;
				return false;
			}
			else if (this.buffer.equals("\"")) {
				this.state = ACC_STATE.STRINGLIT;
				return false;
			}
			else if (this.buffer.equals(".")) {
				tokens.add(new Token(TokenType.DOT, new Source(fileName, i, a)));
				this.emptyBuffer();
			}
			else if (this.buffer.equals(";")) {
				tokens.add(new Token(TokenType.SEMICOLON, new Source(fileName, i, a)));
				this.emptyBuffer();
			}
			else if (this.buffer.equals(":")) {
				tokens.add(new Token(TokenType.COLON, new Source(fileName, i, a)));
				this.emptyBuffer();
			}
			else if (this.buffer.equals(",")) {
				tokens.add(new Token(TokenType.COMMA, new Source(fileName, i, a)));
				this.emptyBuffer();
			}
			else if (this.buffer.equals("?")) {
				tokens.add(new Token(TokenType.TERN, new Source(fileName, i, a)));
				this.emptyBuffer();
			}
			else if (this.buffer.equals("*")) {
				tokens.add(new Token(TokenType.MUL, new Source(fileName, i, a)));
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
					tokens.add(new Token(TokenType.DIV, new Source(fileName, i, a - this.buffer.length())));
					this.buffer = this.buffer.substring(1);
					this.checkState(i, a, fileName);
				}
			}
			else if (this.buffer.startsWith("+")) {
				if (this.buffer.length() == 1)return false;
				if (this.buffer.equals("++")) {
					tokens.add(new Token(TokenType.INCR, new Source(fileName, i, a), this.buffer));
					this.emptyBuffer();
				}
				else {
					tokens.add(new Token(TokenType.ADD, new Source(fileName, i, a - this.buffer.length()), this.buffer.substring(0, 1)));
					this.buffer = this.buffer.substring(1);
					this.checkState(i, a, fileName);
				}
			}
			else if (this.buffer.startsWith("-")) {
				if (this.buffer.length() == 1)return false;
				if (this.buffer.equals("--")) {
					tokens.add(new Token(TokenType.DECR, new Source(fileName, i, a), this.buffer));
					this.emptyBuffer();
				}
				else if (this.buffer.equals("->")) {
					tokens.add(new Token(TokenType.UNION_ACCESS, new Source(fileName, i, a), this.buffer));
					this.emptyBuffer();
				}
				else {
					tokens.add(new Token(TokenType.SUB, new Source(fileName, i, a - this.buffer.length()), this.buffer.substring(0, 1)));
					this.buffer = this.buffer.substring(1);
					this.checkState(i, a, fileName);
				}
			}
			else if (this.buffer.equals("^")) {
				tokens.add(new Token(TokenType.XOR, new Source(fileName, i, a - this.buffer.length()), this.buffer.substring(0, 1)));
				this.buffer = this.buffer.substring(1);
				this.checkState(i, a, fileName);
			}
			else if (this.buffer.equals("~")) {
				tokens.add(new Token(TokenType.NOT, new Source(fileName, i, a - this.buffer.length()), this.buffer.substring(0, 1)));
				this.buffer = this.buffer.substring(1);
				this.checkState(i, a, fileName);
			}
			else if (this.buffer.equals("#")) {
				tokens.add(new Token(TokenType.DIRECTIVE, new Source(fileName, i, a)));
				this.emptyBuffer();
			}
			else if (this.buffer.equals("namespace")) {
				tokens.add(new Token(TokenType.NAMESPACE, new Source(fileName, i, a)));
				this.state = ACC_STATE.NAMESPACE_ID;
				this.emptyBuffer();
			}
			else if (this.buffer.equals("include")) {
				tokens.add(new Token(TokenType.INCLUDE, new Source(fileName, i, a)));
				this.emptyBuffer();
			}
			else if (this.buffer.equals("sizeof")) {
				tokens.add(new Token(TokenType.SIZEOF, new Source(fileName, i, a)));
				this.emptyBuffer();
			}
			else if (this.buffer.equals("try")) {
				tokens.add(new Token(TokenType.TRY, new Source(fileName, i, a)));
				this.emptyBuffer();
			}
			else if (this.buffer.equals("watch")) {
				tokens.add(new Token(TokenType.WATCH, new Source(fileName, i, a)));
				this.emptyBuffer();
			}
			else if (this.buffer.equals("\\")) {
				tokens.add(new Token(TokenType.BACKSL, new Source(fileName, i, a)));
				this.emptyBuffer();
			}
			else if (this.buffer.equals("if")) {
				tokens.add(new Token(TokenType.IF, new Source(fileName, i, a)));
				this.emptyBuffer();
			}
			else if (this.buffer.equals("true")) {
				tokens.add(new Token(TokenType.BOOLLIT, new Source(fileName, i, a), "true"));
				this.emptyBuffer();
			}
			else if (this.buffer.equals("false")) {
				tokens.add(new Token(TokenType.BOOLLIT, new Source(fileName, i, a), "false"));
				this.emptyBuffer();
			}
			else if (this.buffer.equals("else")) {
				tokens.add(new Token(TokenType.ELSE, new Source(fileName, i, a)));
				this.emptyBuffer();
			}
			else if (this.buffer.equals("||")) {
				tokens.add(new Token(TokenType.OR, new Source(fileName, i, a)));
				this.emptyBuffer();
			}
			else if (this.buffer.equals("void")) {
				tokens.add(new Token(TokenType.VOID, new Source(fileName, i, a)));
				this.emptyBuffer();
				this.state = ACC_STATE.NONE;
				return true;
			}
			else if (this.buffer.equals("int")) {
				tokens.add(new Token(TokenType.INT, new Source(fileName, i, a)));
				this.emptyBuffer();
				this.state = ACC_STATE.NONE;
				return true;
			}
			else if (this.buffer.equals("char")) {
				tokens.add(new Token(TokenType.CHAR, new Source(fileName, i, a)));
				this.emptyBuffer();
				this.state = ACC_STATE.NONE;
				return true;
			}
			else if (this.buffer.equals("bool")) {
				tokens.add(new Token(TokenType.BOOL, new Source(fileName, i, a)));
				this.emptyBuffer();
				this.state = ACC_STATE.NONE;
				return true;
			}
			else if (this.buffer.equals("return")) {
				tokens.add(new Token(TokenType.RETURN, new Source(fileName, i, a)));
				this.emptyBuffer();
				this.state = ACC_STATE.NONE;
				return true;
			}
			else if (this.buffer.equals("break")) {
				tokens.add(new Token(TokenType.BREAK, new Source(fileName, i, a)));
				this.emptyBuffer();
				this.state = ACC_STATE.NONE;
				return true;
			}
			else if (this.buffer.equals("continue")) {
				tokens.add(new Token(TokenType.CONTINUE, new Source(fileName, i, a)));
				this.emptyBuffer();
				this.state = ACC_STATE.NONE;
				return true;
			}
			else if (this.buffer.equals("while")) {
				tokens.add(new Token(TokenType.WHILE, new Source(fileName, i, a)));
				this.emptyBuffer();
				this.state = ACC_STATE.NONE;
				return true;
			}
			else if (this.buffer.equals("do")) {
				tokens.add(new Token(TokenType.DO, new Source(fileName, i, a)));
				this.emptyBuffer();
				this.state = ACC_STATE.NONE;
				return true;
			}
			else if (this.buffer.equals("for")) {
				tokens.add(new Token(TokenType.FOR, new Source(fileName, i, a)));
				this.emptyBuffer();
				this.state = ACC_STATE.NONE;
				return true;
			}
			else if (this.buffer.equals("switch")) {
				tokens.add(new Token(TokenType.SWITCH, new Source(fileName, i, a)));
				this.emptyBuffer();
				this.state = ACC_STATE.NONE;
				return true;
			}
			else if (this.buffer.equals("case")) {
				tokens.add(new Token(TokenType.CASE, new Source(fileName, i, a)));
				this.emptyBuffer();
				this.state = ACC_STATE.NONE;
				return true;
			}
			else if (this.buffer.equals("default")) {
				tokens.add(new Token(TokenType.DEFAULT, new Source(fileName, i, a)));
				this.emptyBuffer();
				this.state = ACC_STATE.NONE;
				return true;
			}
			else if (this.buffer.equals("struct")) {
				tokens.add(new Token(TokenType.STRUCT, new Source(fileName, i, a)));
				this.emptyBuffer();
				this.state = ACC_STATE.STRUCT_ID;
				return true;
			}
			else if (this.buffer.equals("enum")) {
				tokens.add(new Token(TokenType.ENUM, new Source(fileName, i, a)));
				this.emptyBuffer();
				this.state = ACC_STATE.ENUM_ID;
				return true;
			}
			else if (this.buffer.startsWith("signal")) {
				if (this.buffer.length() == 6)return false;
				if (this.buffer.equals("signals")) {
					tokens.add(new Token(TokenType.SIGNALS, new Source(fileName, i, a)));
					this.emptyBuffer();
				}
				else {
					tokens.add(new Token(TokenType.SIGNAL, new Source(fileName, i, a - this.buffer.length())));
					this.buffer = this.buffer.substring(this.buffer.length() - 1);
					this.checkState(i, a, fileName);
				}
			}
			else if (this.buffer.startsWith("|")) {
				if (this.buffer.length() == 1)return false;
				if (this.buffer.equals("||")) {
					tokens.add(new Token(TokenType.OR, new Source(fileName, i, a)));
					this.emptyBuffer();
				}
				else {
					tokens.add(new Token(TokenType.BITOR, new Source(fileName, i, a - this.buffer.length())));
					this.buffer = this.buffer.substring(1);
					this.checkState(i, a, fileName);
				}
			}
			else if (this.buffer.startsWith("&")) {
				if (this.buffer.length() == 1)return false;
				if (this.buffer.equals("&&")) {
					tokens.add(new Token(TokenType.AND, new Source(fileName, i, a)));
					this.emptyBuffer();
				}
				else {
					tokens.add(new Token(TokenType.ADDROF, new Source(fileName, i, a - this.buffer.length())));
					this.buffer = this.buffer.substring(1);
					this.checkState(i, a, fileName);
				}
			}
			else if (this.buffer.equals("%")) {
				tokens.add(new Token(TokenType.MOD, new Source(fileName, i, a)));
				this.emptyBuffer();
			}
			else if (this.buffer.startsWith("!")) {
				if (this.buffer.length() == 1)return false;
				if (this.buffer.equals("!=")) {
					tokens.add(new Token(TokenType.CMPNE, new Source(fileName, i, a), this.buffer));
					this.emptyBuffer();
				}
				else {
					tokens.add(new Token(TokenType.NEG, new Source(fileName, i, a), this.buffer.substring(0, 1)));
					this.buffer = this.buffer.substring(1);
					this.checkState(i, a, fileName);
				}
			}
			else if (this.buffer.startsWith("=")) {
				if (this.buffer.length() == 1)return false;
				if (this.buffer.equals("==")) {
					tokens.add(new Token(TokenType.CMPEQ, new Source(fileName, i, a), this.buffer));
					this.emptyBuffer();
				}
				else {
					tokens.add(new Token(TokenType.LET, new Source(fileName, i, a - this.buffer.length()), this.buffer.substring(0, 1)));
					this.buffer = this.buffer.substring(1);
					this.checkState(i, a, fileName);
				}
			}
			else if (this.buffer.startsWith("<")) {
				if (this.buffer.length() == 1)return false;
				if (this.buffer.equals("<=")) {
					tokens.add(new Token(TokenType.CMPLE, new Source(fileName, i, a), this.buffer));
					this.emptyBuffer();
				}
				else {
					tokens.add(new Token(TokenType.CMPLT, new Source(fileName, i, a - this.buffer.length()), this.buffer.substring(0, 1)));
					this.buffer = this.buffer.substring(1);
					this.checkState(i, a, fileName);
				}
			}
			else if (this.buffer.startsWith(">")) {
				if (this.buffer.length() == 1)return false;
				if (this.buffer.equals(">=")) {
					tokens.add(new Token(TokenType.CMPGE, new Source(fileName, i, a), this.buffer));
					this.emptyBuffer();
				}
				else {
					tokens.add(new Token(TokenType.CMPGT, new Source(fileName, i, a - this.buffer.length()), this.buffer.substring(0, 1)));
					this.buffer = this.buffer.substring(1);
					this.checkState(i, a, fileName);
				}
			}
			else {
				if (this.buffer.matches("([a-z]|[A-Z]|_)([a-z]|[A-Z]|[0-9]|_)*")) {
					if (this.state != ACC_STATE.STRUCT_ID && this.state != ACC_STATE.ENUM_ID && this.state != ACC_STATE.NAMESPACE_ID) {
						this.state = ACC_STATE.ID;
					}
				}
				
				if (this.buffer.matches(int_match)) {
					this.state = ACC_STATE.INT;
				}
				
				if (this.buffer.matches(bin_match)) {
					this.state = ACC_STATE.BIN_INT;
				}
				
				if (this.buffer.matches(hex_match)) {
					this.state = ACC_STATE.HEX_INT;
				}
				
				if ((this.buffer.endsWith(" ") || !this.buffer.matches("([a-z]|[A-Z]|_)([a-z]|[A-Z]|[0-9]|_)*")) && (this.state == ACC_STATE.ID || this.state == ACC_STATE.STRUCT_ID || this.state == ACC_STATE.ENUM_ID || this.state == ACC_STATE.NAMESPACE_ID)) {
					/* Ignore Empty buffer */
					if (this.buffer.trim().isEmpty()) {
						return false;
					}
					
					String id = this.buffer.substring(0, this.buffer.length() - 1);
				
					if (this.state == ACC_STATE.ID) {
						if (this.structIds.contains(id))
							tokens.add(new Token(TokenType.STRUCTID, new Source(fileName, i, a), id));
						else if (this.enumIds.contains(id))
							tokens.add(new Token(TokenType.ENUMID, new Source(fileName, i, a), id));
						else if (this.namespaces.contains(id))
							tokens.add(new Token(TokenType.NAMESPACE_IDENTIFIER, new Source(fileName, i, a), id));
						else tokens.add(new Token(TokenType.IDENTIFIER, new Source(fileName, i, a), id));
					}
					else if (this.state == ACC_STATE.STRUCT_ID) {
						this.structIds.add(id);
						tokens.add(new Token(TokenType.STRUCTID, new Source(fileName, i, a), id));
					}
					else if (this.state == ACC_STATE.ENUM_ID) {
						this.enumIds.add(id);
						tokens.add(new Token(TokenType.ENUMID, new Source(fileName, i, a), id));
					}
					else if (this.state == ACC_STATE.NAMESPACE_ID) {
						tokens.add(new Token(TokenType.NAMESPACE_IDENTIFIER, new Source(fileName, i, a), id));
						this.namespaces.add(id);
					}
					
					this.buffer = this.buffer.substring(this.buffer.length() - 1);
					this.state = ACC_STATE.NONE;
					this.checkState(i, a, fileName);
				}
				else if (this.buffer.matches("\"(.)*\"") && (this.state == ACC_STATE.STRINGLIT)) {
					String id = this.buffer.substring(1, this.buffer.length() - 1);
				
					tokens.add(new Token(TokenType.STRINGLIT, new Source(fileName, i, a), id));
					
					this.emptyBuffer();
					this.state = ACC_STATE.NONE;
					return true;
				}
				else if (this.buffer.matches("'.'") && (this.state == ACC_STATE.CHARLIT)) {
					String id = this.buffer.substring(1, this.buffer.length() - 1);
				
					tokens.add(new Token(TokenType.CHARLIT, new Source(fileName, i, a), id));
					
					this.emptyBuffer();
					this.state = ACC_STATE.NONE;
					return true;
				}
				/* Invalid Char lit */
				else if (this.buffer.startsWith("'") && this.buffer.length() > 3) {
					this.state = ACC_STATE.NONE;
				}
				
				if ((this.buffer.endsWith(" ") || !this.buffer.matches(int_match)) && this.state == ACC_STATE.INT) {
					String lit = this.buffer.substring(0, this.buffer.length() - 1);
					tokens.add(new Token(TokenType.INTLIT, new Source(fileName, i, a), lit));
					this.buffer = this.buffer.substring(this.buffer.length() - 1);
					this.state = ACC_STATE.NONE;
					this.checkState(i, a, fileName);
				}
				else if ((this.buffer.endsWith(" ") || !this.buffer.matches(hex_match)) && this.state == ACC_STATE.HEX_INT) {
					String lit = this.buffer.substring(0, this.buffer.length() - 1);
					
					if (lit.length() < 3) {
						this.progress.abort();
						throw new SNIPS_EXCEPTION("Bad HEX literal, " + new Source(fileName, i, a).getSourceMarker());
					}
					
					lit = lit.substring(2);
					String [] sp = lit.split("");
					int s = 0;
					for (int k = 0; k < sp.length; k++) {
						s += Math.pow(16, sp.length - k - 1) * Character.digit(sp [k].charAt(0), 16);
					}
				
					lit = s + "";
					
					tokens.add(new Token(TokenType.INTLIT, new Source(fileName, i, a), lit));
					this.buffer = this.buffer.substring(this.buffer.length() - 1);
					this.state = ACC_STATE.NONE;
					this.checkState(i, a, fileName);
				}
				else if ((this.buffer.endsWith(" ") || !this.buffer.matches(bin_match)) && this.state == ACC_STATE.BIN_INT) {
					String lit = this.buffer.substring(0, this.buffer.length() - 1);
					
					if (lit.length() < 3) {
						this.progress.abort();
						throw new SNIPS_EXCEPTION("Bad BIN literal, " + new Source(fileName, i, a).getSourceMarker());
					}
					
					lit = lit.substring(2);
					String [] sp = lit.split("");
					int s = 0;
					for (int k = 0; k < sp.length; k++) {
						s += Math.pow(2, sp.length - k - 1) * Character.digit(sp [k].charAt(0), 2);
					}
					
					lit = s + "";
					
					tokens.add(new Token(TokenType.INTLIT, new Source(fileName, i, a), lit));
					this.buffer = this.buffer.substring(this.buffer.length() - 1);
					this.state = ACC_STATE.NONE;
					this.checkState(i, a, fileName);
				}
			}
			
			return false;
		}
		
		public void emptyBuffer() {
			this.buffer = "";
		}
		
	}
	
			/* --- REGEXES --- */
	public static String int_match = "[0-9]+";
	public static String hex_match = "hx([0-9]|[a-f]|[A-F])+";
	public static String bin_match = "bx[0-1]+";
	
}
