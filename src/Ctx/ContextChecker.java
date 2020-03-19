package Ctx;

import java.util.Stack;

import Exc.CTX_EXCEPTION;
import Imm.AST.Function;
import Imm.AST.Program;
import Imm.AST.SyntaxElement;
import Imm.AST.Expression.Atom;
import Imm.AST.Expression.IDRef;
import Imm.AST.Expression.Arith.BinaryExpression;
import Imm.AST.Statement.Assignment;
import Imm.AST.Statement.Declaration;
import Imm.AST.Statement.Return;
import Imm.AST.Statement.Statement;
import Imm.TYPE.TYPE;

public class ContextChecker {

	Program head;
	
	Function currentFunction;
	
	SyntaxElement AST;
	
	Stack<Scope> scopes = new Stack();
	
	public ContextChecker(SyntaxElement AST) {
		this.AST = AST;
	}
	
	public TYPE check() throws CTX_EXCEPTION {
		if (AST instanceof Program) {
			scopes.push(new Scope(null));
			
			Program p = (Program) AST;
			this.head = p;
			for (SyntaxElement s : p.programElements) {
				s.check(this);
			}
			
			return null;
		}
		else {
			throw new CTX_EXCEPTION(AST.getSource(), "Head of AST is not instance of Program...How?");
		}
	}
	
	public TYPE checkFunction(Function f) throws CTX_EXCEPTION {
		scopes.push(new Scope(scopes.peek()));
		
		/* Check for duplicate function name */
		for (Function f0 : head.functions) {
			if (f0.functionName.equals(f.functionName)) {
				throw new CTX_EXCEPTION(f.getSource(), "Duplicate function name: " + f.functionName);
			}
		}
		
		/* Check for duplicate function parameters */
		if (f.parameters.size() > 1) {
			for (int i = 0; i < f.parameters.size(); i++) {
				for (int a = 1; a < f.parameters.size(); a++) {
					if (f.parameters.get(i).fieldName.equals(f.parameters.get(a).fieldName)) {
						throw new CTX_EXCEPTION(f.getSource(), "Duplicate parameter name: " + f.parameters.get(i).fieldName + " in function: " + f.functionName);
					}
				}
			}
		}
		
		for (Declaration d : f.parameters) {
			d.check(this);
			this.scopes.peek().addDeclaration(d);
		}
		
		head.functions.add(f);
		this.currentFunction = f;
		
		/* Check body */
		for (Statement s : f.statements) {
			s.check(this);
		}
		
		scopes.pop();
		return null;
	}
	
	public TYPE checkDeclaration(Declaration d) throws CTX_EXCEPTION {
		if (d.value != null) {
			TYPE t = d.value.check(this);
			if (t.isEqual(d.type)) {
				scopes.peek().addDeclaration(d);
			}
			else {
				throw new CTX_EXCEPTION(d.getSource(), "Variable type does not match expression type: " + d.type.typeString() + " vs. " + t.typeString());
			}
		}
		
		return null;
	}
	
	public TYPE checkAssignment(Assignment a) throws CTX_EXCEPTION {
		TYPE t = a.value.check(this);
		Declaration dec = scopes.peek().getField(a.fieldName);
		a.origin = dec;
		if (!t.isEqual(dec.type)) {
			throw new CTX_EXCEPTION(a.getSource(), "Variable type does not match expression type: " + dec.type.typeString() + " vs. " + t.typeString());
		}
		return null;
	}
	
	public TYPE checkReturn(Return r) throws CTX_EXCEPTION {
		TYPE t = r.value.check(this);
		
		if (t.isEqual(this.currentFunction.returnType)) return t;
		else {
			throw new CTX_EXCEPTION(r.getSource(), "Return type " + t.typeString() + " does not match function return type " + this.currentFunction.returnType.typeString());
		}
	}
	
	public TYPE checkBinaryExpression(BinaryExpression b) throws CTX_EXCEPTION {
		TYPE left = b.left().check(this);
		TYPE right = b.right().check(this);
		
		if (left.isEqual(right)) return left;
		else {
			throw new CTX_EXCEPTION(b.getSource(), "Operand types do not match: " + left.typeString() + " vs. " + right.typeString());
		}
	}
	
	public TYPE checkIDRef(IDRef i) throws CTX_EXCEPTION {
		Declaration d = this.scopes.peek().getField(i.id);
		if (d != null) {
			i.origin = d;
			return d.type;
		}
		else {
			throw new CTX_EXCEPTION(i.getSource(), "Unknown variable: " + i.id);
		}
	}
	
	public TYPE checkAtom(Atom a) throws CTX_EXCEPTION {
		return a.type;
	}
	
}
