package Imm.TYPE.PRIMITIVES;

import Imm.TYPE.TYPE;
import Par.Token;
import Par.Token.TokenType;

public abstract class PRIMITIVE<T> extends TYPE<T> {

			/* --- CONSTRUCTORS --- */
	public PRIMITIVE() {
		super();
	}
	
	public PRIMITIVE(String initialValue) {
		this.setValue(initialValue);
	}
	
	public int wordsize() {
		return 1;
	}
	
	public static PRIMITIVE fromToken(Token token) {
		if (token.type() == TokenType.INT) {
			return new INT();
		}
		else if (token.type() == TokenType.BOOL) {
			return new BOOL();
		}
		else if (token.type() == TokenType.CHAR) {
			return new CHAR();
		}
		else if (token.type() == TokenType.VOID) {
			return new VOID();
		}
		else if (token.type() == TokenType.FUNC) {
			return new FUNC();
		}
		else return null;
	}
	
	public TYPE getCoreType() {
		return this;
	}
	
	public TYPE provisoFree() {
		return this.clone();
	}
	
	public TYPE remapProvisoName(String name, TYPE newType) {
		return this;
	}
	
	public TYPE mappable(TYPE mapType, String searchedProviso) {
		return null;
	}
	
	public boolean hasProviso() {
		return false;
	}
	
} 
