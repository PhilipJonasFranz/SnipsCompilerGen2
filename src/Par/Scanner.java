package Par;

import java.util.ArrayList;
import java.util.HashMap;
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
		
				/* --- NESTED --- */
		public class ScannableToken {
			
			String base;
			
			String [] resetTokens;
			
			boolean setCustomSpelling = false;
			
			TokenType type;
			
			ACC_STATE resultingState;
			
			public ScannableToken(String base, TokenType type, ACC_STATE resultingState, String...resetTokens) {
				this.base = base;
				this.resetTokens = resetTokens;
				this.type = type;
				this.resultingState = resultingState;
			}
			
			public ScannableToken(String base, TokenType type, ACC_STATE resultingState, boolean customSpelling, String...resetTokens) {
				this.base = base;
				this.resetTokens = resetTokens;
				this.type = type;
				this.resultingState = resultingState;
				this.setCustomSpelling = customSpelling;
			}
			
		}
		
		/**
		 * Defines the current accumulation state of the scanner.
		 */
		private enum ACC_STATE {
			/* Default */
			NONE, 
			
			/* IDs */
			ID, STRUCT_ID, NAMESPACE_ID, ENUM_ID, 
			
			/* Literals */
			INT, HEX_INT, BIN_INT, FLOAT, COMMENT, CHARLIT, STRINGLIT
		}
		
		
				/* --- FIELDS --- */
		/* All struct ids that have been scanned */
		List<String> structIds = new ArrayList();
		
		/* All namespace ids that have been scanned */
		List<String> enumIds = new ArrayList();
		
		/* All namespace ids that have been scanned */
		List<String> namespaces = new ArrayList();
		
		private ACC_STATE state = ACC_STATE.NONE;
		
		private int lastLine = 0;
		
		private LinkedList<Token> tokens;
		
		private String buffer = "";
		
		private ProgressMessage progress;

		/**
		 * All simple scannable tokens, represented in base/reset token format.
		 */
		public ScannableToken [] scannables = {
				new ScannableToken("(", TokenType.LPAREN, ACC_STATE.NONE, ""),
				new ScannableToken(")", TokenType.RPAREN, ACC_STATE.NONE, ""),
				new ScannableToken("@", TokenType.AT, ACC_STATE.NONE, ""),
				new ScannableToken("{", TokenType.LBRACE, ACC_STATE.NONE, ""),
				new ScannableToken("}", TokenType.RBRACE, ACC_STATE.NONE, ""),
				new ScannableToken("[", TokenType.LBRACKET, ACC_STATE.NONE, ""),
				new ScannableToken("]", TokenType.RBRACKET, ACC_STATE.NONE, ""),
				new ScannableToken(".", TokenType.DOT, ACC_STATE.NONE, ""),
				new ScannableToken(";", TokenType.SEMICOLON, ACC_STATE.NONE, ""),
				new ScannableToken(":", TokenType.COLON, ACC_STATE.NONE, ""),
				new ScannableToken(",", TokenType.COMMA, ACC_STATE.NONE, ""),
				new ScannableToken("?", TokenType.TERN, ACC_STATE.NONE, ""),
				new ScannableToken("*", TokenType.MUL, ACC_STATE.NONE, ""),
				new ScannableToken("^", TokenType.XOR, ACC_STATE.NONE, ""),
				new ScannableToken("~", TokenType.NOT, ACC_STATE.NONE, ""),
				new ScannableToken("#", TokenType.DIRECTIVE, ACC_STATE.NONE, ""),
				new ScannableToken("namespace", TokenType.NAMESPACE, ACC_STATE.NAMESPACE_ID, " "),
				new ScannableToken("shared", TokenType.SHARED, ACC_STATE.NONE, " "),
				new ScannableToken("restricted", TokenType.RESTRICTED, ACC_STATE.NONE, " "),
				new ScannableToken("exclusive", TokenType.EXCLUSIVE, ACC_STATE.NONE, " "),
				new ScannableToken("null", TokenType.NULL, ACC_STATE.NONE, ""),
				new ScannableToken("sizeof", TokenType.SIZEOF, ACC_STATE.NONE, " ", "("),
				new ScannableToken("try", TokenType.TRY, ACC_STATE.NONE, " ", "{"),
				new ScannableToken("watch", TokenType.WATCH, ACC_STATE.NONE, " ", "("),
				new ScannableToken("\\", TokenType.BACKSL, ACC_STATE.NONE, ""),
				new ScannableToken("if", TokenType.IF, ACC_STATE.NONE, ""),
				new ScannableToken("true", TokenType.BOOLLIT, ACC_STATE.NONE, true, ""),
				new ScannableToken("false", TokenType.BOOLLIT, ACC_STATE.NONE, true, ""),
				new ScannableToken("else", TokenType.ELSE, ACC_STATE.NONE, ""),
				new ScannableToken("void", TokenType.VOID, ACC_STATE.NONE, ""),
				new ScannableToken("func", TokenType.FUNC, ACC_STATE.NONE, ""),
				new ScannableToken("int", TokenType.INT, ACC_STATE.NONE, ""),
				new ScannableToken("char", TokenType.CHAR, ACC_STATE.NONE, ""),
				new ScannableToken("bool", TokenType.BOOL, ACC_STATE.NONE, ""),
				new ScannableToken("return", TokenType.RETURN, ACC_STATE.NONE, " ", ";"),
				new ScannableToken("break", TokenType.BREAK, ACC_STATE.NONE, " ", ";"),
				new ScannableToken("continue", TokenType.CONTINUE, ACC_STATE.NONE, " ", ";"),
				new ScannableToken("while", TokenType.WHILE, ACC_STATE.NONE, " ", "("),
				new ScannableToken("do", TokenType.DO, ACC_STATE.NONE, " ", "{"),
				new ScannableToken("for", TokenType.FOR, ACC_STATE.NONE, " ", "("),
				new ScannableToken("switch", TokenType.SWITCH, ACC_STATE.NONE, " ", "("),
				new ScannableToken("case", TokenType.CASE, ACC_STATE.NONE, " ", "("),
				new ScannableToken("default", TokenType.DEFAULT, ACC_STATE.NONE, " ", ":"),
				new ScannableToken("struct", TokenType.STRUCT, ACC_STATE.STRUCT_ID, " "),
				new ScannableToken("enum", TokenType.ENUM, ACC_STATE.ENUM_ID, " "),
				new ScannableToken("%", TokenType.MOD, ACC_STATE.NONE, "")
		};
		
		HashMap<String, ScannableToken> scannableMap = new HashMap();
		
		public ScannerFSM(LinkedList<Token> tokens, ProgressMessage progress) {
			this.tokens = tokens;
			this.progress = progress;
			
			for (ScannableToken t : this.scannables) {
				for (String reset : t.resetTokens) {
					scannableMap.put(t.base + reset, t);
				}
			}
		}
		
		public void readChar(char c, int i, int a, String fileName) {
			if (this.buffer.isEmpty() && ("" + c).trim().equals("")) return;
			else {
				if (this.state != ACC_STATE.COMMENT && this.state != ACC_STATE.CHARLIT && this.state != ACC_STATE.STRINGLIT) 
					this.buffer = this.buffer.trim();
				this.buffer = buffer + c;
				boolean b = true;
				
				while (b) {
					b = this.checkState(i, a, fileName);
				}
				
				this.lastLine = i;
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
			
			ScannableToken t = this.scannableMap.get(buffer);
			if (t != null) {
				/* Create new token, either with or without custom spelling */
				if (t.setCustomSpelling) {
					tokens.add(new Token(t.type, new Source(fileName, i, a), t.base));
				}
				else {
					tokens.add(new Token(t.type, new Source(fileName, i, a)));
				}
				
				/* Set resulting state defined by scannable */
				this.state = t.resultingState;
				
				/* Adjust buffer */
				this.buffer = buffer.substring(t.base.length());
				
				/* Signal new token found */
				return true;
			}
				
			if (this.buffer.equals("'")) {
				this.state = ACC_STATE.CHARLIT;
				return false;
			}
			else if (this.buffer.equals("\"")) {
				this.state = ACC_STATE.STRINGLIT;
				return false;
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
				
				if (this.buffer.matches(int_match)) 
					this.state = ACC_STATE.INT;
				
				if (this.buffer.matches(bin_match)) 
					this.state = ACC_STATE.BIN_INT;
				
				if (this.buffer.matches(hex_match)) 
					this.state = ACC_STATE.HEX_INT;
				
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
				else if (this.buffer.matches("\"(.)*\"") && (this.state == ACC_STATE.STRINGLIT) && !this.buffer.endsWith("\\\"")) {
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
