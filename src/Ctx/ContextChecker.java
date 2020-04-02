package Ctx;

import java.util.ArrayList;
import java.util.List;
import java.util.Stack;

import Exc.CTX_EXCEPTION;
import Imm.AST.Function;
import Imm.AST.Program;
import Imm.AST.SyntaxElement;
import Imm.AST.Expression.Atom;
import Imm.AST.Expression.IDRef;
import Imm.AST.Expression.InlineCall;
import Imm.AST.Expression.Arith.BinaryExpression;
import Imm.AST.Expression.Arith.UnaryExpression;
import Imm.AST.Expression.Arith.UnaryMinus;
import Imm.AST.Expression.Boolean.Compare;
import Imm.AST.Expression.Boolean.Not;
import Imm.AST.Expression.Boolean.Ternary;
import Imm.AST.Statement.Assignment;
import Imm.AST.Statement.BreakStatement;
import Imm.AST.Statement.CaseStatement;
import Imm.AST.Statement.CompoundStatement;
import Imm.AST.Statement.ContinueStatement;
import Imm.AST.Statement.Declaration;
import Imm.AST.Statement.DefaultStatement;
import Imm.AST.Statement.DoWhileStatement;
import Imm.AST.Statement.ForStatement;
import Imm.AST.Statement.IfStatement;
import Imm.AST.Statement.ReturnStatement;
import Imm.AST.Statement.Statement;
import Imm.AST.Statement.SwitchStatement;
import Imm.AST.Statement.WhileStatement;
import Imm.TYPE.TYPE;
import Imm.TYPE.PRIMITIVES.BOOL;
import Imm.TYPE.PRIMITIVES.PRIMITIVE;

public class ContextChecker {

	Program head;
	
	List<Function> functions = new ArrayList();
	
	Function currentFunction;
	
	SyntaxElement AST;
	
	Stack<CompoundStatement> compoundStack = new Stack();
	
	Stack<Scope> scopes = new Stack();
	
	public ContextChecker(SyntaxElement AST) {
		this.AST = AST;
	}
	
	public TYPE check() throws CTX_EXCEPTION {
		this.checkProgram((Program) AST);
		return null;
	}
	
	public TYPE checkProgram(Program p) throws CTX_EXCEPTION {
		scopes.push(new Scope(null));
		this.head = p;
		for (SyntaxElement s : p.programElements) {
			s.check(this);
		}
		
		return null;
	}
	
	public TYPE checkFunction(Function f) throws CTX_EXCEPTION {
		this.functions.add(f);
		
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
				for (int a = i + 1; a < f.parameters.size(); a++) {
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
	
	public TYPE checkWhileStatement(WhileStatement w) throws CTX_EXCEPTION {
		this.compoundStack.push(w);
		
		TYPE cond = w.condition.check(this);
		if (!(cond instanceof BOOL)) {
			throw new CTX_EXCEPTION(w.getSource(), "Condition is not boolean");
		}
		
		this.scopes.push(new Scope(this.scopes.peek()));
		for (Statement s : w.body) {
			s.check(this);
		}
		this.scopes.pop();

		this.compoundStack.pop();
		return null;
	}
	
	public TYPE checkDoWhileStatement(DoWhileStatement w) throws CTX_EXCEPTION {
		this.compoundStack.push(w);
		
		TYPE cond = w.condition.check(this);
		if (!(cond instanceof BOOL)) {
			throw new CTX_EXCEPTION(w.getSource(), "Condition is not boolean");
		}
		
		this.scopes.push(new Scope(this.scopes.peek()));
		for (Statement s : w.body) {
			s.check(this);
		}
		this.scopes.pop();

		this.compoundStack.pop();
		return null;
	}
	
	public TYPE checkForStatement(ForStatement f) throws CTX_EXCEPTION {
		this.compoundStack.push(f);
		
		this.scopes.push(new Scope(this.scopes.peek()));
		f.iterator.check(this);
		if (f.iterator.value == null) {
			throw new CTX_EXCEPTION(f.getSource(), "Iterator must have initial value");
		}
		
		this.scopes.push(new Scope(this.scopes.peek()));
		
		TYPE cond = f.condition.check(this);
		if (!(cond instanceof BOOL)) {
			throw new CTX_EXCEPTION(f.getSource(), "Condition is not boolean");
		}
		
		f.increment.check(this);
		
		for (Statement s : f.body) {
			s.check(this);
		}
		
		this.scopes.pop();
		this.scopes.pop();

		this.compoundStack.pop();
		return null;
	}
	
	public TYPE checkIfStatement(IfStatement i) throws CTX_EXCEPTION {
		if (i.condition != null) {
			TYPE cond = i.condition.check(this);
			if (!(cond instanceof BOOL)) {
				throw new CTX_EXCEPTION(i.getSource(), "Condition is not boolean");
			}
		}
		else {
			if (i.elseStatement != null) {
				throw new CTX_EXCEPTION(i.getSource(), "If Statement can only have one else statement");
			}
		}
		
		this.scopes.push(new Scope(this.scopes.peek()));
		for (Statement s : i.body) {
			s.check(this);
		}
		this.scopes.pop();

		if (i.elseStatement != null) {
			i.elseStatement.check(this);
		}
		
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
		else return d.type;
		
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
	
	public TYPE checkBreak(BreakStatement b) throws CTX_EXCEPTION {
		if (this.compoundStack.isEmpty()) {
			throw new CTX_EXCEPTION(b.getSource(), "Can only break out of the scope of a loop");
		}
		else b.superLoop = this.compoundStack.peek();
		
		return null;
	}
	
	public TYPE checkContinue(ContinueStatement c) throws CTX_EXCEPTION {
		if (this.compoundStack.isEmpty()) {
			throw new CTX_EXCEPTION(c.getSource(), "Can only continue in the scope of a loop");
		}
		else c.superLoop = this.compoundStack.peek();
		
		return null;
	}
	
	public TYPE checkSwitchStatement(SwitchStatement s) throws CTX_EXCEPTION {
		if (!(s.condition instanceof IDRef)) {
			throw new CTX_EXCEPTION(s.condition.getSource(), "Switch Condition has to be variable reference");
		}
		
		TYPE type = s.condition.check(this);
		if (!(type instanceof PRIMITIVE)) {
			throw new CTX_EXCEPTION(s.condition.getSource(), "Switch Condition type " + type.typeString() + " has to be a primitive type");
		}
		
		if (s.defaultStatement == null) {
			throw new CTX_EXCEPTION(s.getSource(), "Missing default statement");
		}
		
		for (CaseStatement c : s.cases) {
			c.check(this);
		}
		
		s.defaultStatement.check(this);
		
		return null;
	}
	
	public TYPE checkCaseStatement(CaseStatement c) throws CTX_EXCEPTION {
		TYPE type = c.condition.check(this);
		
		if (!type.isEqual(c.superStatement.condition.type)) {
			throw new CTX_EXCEPTION(c.condition.getSource(), "Condition type " + type.typeString() + " does not switch condition type " + c.superStatement.condition.type.typeString());
		}
		
		this.scopes.push(new Scope(this.scopes.peek()));
		
		for (Statement s : c.body) {
			s.check(this);
		}
		
		this.scopes.pop();
		
		return null;
	}
	
	public TYPE checkDefaultStatement(DefaultStatement d) throws CTX_EXCEPTION {
		this.scopes.push(new Scope(this.scopes.peek()));
		
		for (Statement s : d.body) {
			s.check(this);
		}
		
		this.scopes.pop();
		
		return null;
	}
	
	public TYPE checkReturn(ReturnStatement r) throws CTX_EXCEPTION {
		TYPE t = r.value.check(this);
		
		if (t.isEqual(this.currentFunction.returnType)) {
			return t;
		}
		else {
			throw new CTX_EXCEPTION(r.getSource(), "Return type " + t.typeString() + " does not match function return type " + this.currentFunction.returnType.typeString());
		}
	}
	
	public TYPE checkTernary(Ternary t) throws CTX_EXCEPTION {
		TYPE type = t.condition.check(this);
		if (!(type instanceof BOOL)) {
			throw new CTX_EXCEPTION(t.condition.getSource(), "Ternary condition has to be of type BOOL, actual " + type.typeString());
		}
		
		TYPE t0 = t.leftOperand.check(this);
		TYPE t1 = t.rightOperand.check(this);
		
		if (!t0.isEqual(t1)) {
			throw new CTX_EXCEPTION(t.condition.getSource(), "Both results of ternary operation have to be of the same type, " + t0.typeString() + " vs " + t1.typeString());
		}
		
		t.type = t0;
		return t.type;
	}
	
	public TYPE checkBinaryExpression(BinaryExpression b) throws CTX_EXCEPTION {
		TYPE left = b.left().check(this);
		TYPE right = b.right().check(this);
		
		if (left.isEqual(right)) {
			b.type = left;
			return left;
		}
		else {
			throw new CTX_EXCEPTION(b.getSource(), "Operand types do not match: " + left.typeString() + " vs. " + right.typeString());
		}
	}
	
	public TYPE checkUnaryExpression(UnaryExpression u) throws CTX_EXCEPTION {
		TYPE op = u.operand().check(this);
		
		if (u instanceof Not && op.isEqual(new BOOL())) {
			u.type = new BOOL();
			return u.type;
		}
		else if (u instanceof UnaryMinus && op instanceof PRIMITIVE) {
			u.type = op;
			return u.type;
		}
		else {
			throw new CTX_EXCEPTION(u.getSource(), "Unknown Expression: " + u.getClass().getName());
		}
	}
	
	public TYPE checkCompare(Compare c) throws CTX_EXCEPTION {
		TYPE left = c.left().check(this);
		TYPE right = c.right().check(this);
		
		if (left.isEqual(right)) {
			c.type = new BOOL();
			return c.type;
		}
		else {
			throw new CTX_EXCEPTION(c.getSource(), "Operand types do not match: " + left.typeString() + " vs. " + right.typeString());
		}
	}
	
	public TYPE checkInlineCall(InlineCall i) throws CTX_EXCEPTION {
		Function f = null;
		for (Function f0 : this.functions) {
			if (f0.functionName.equals(i.functionName)) {
				f = f0;
				break;
			}
		}
		
		if (f == null) {
			throw new CTX_EXCEPTION(i.getSource(), "Undefined Function: " + i.functionName);
		}
		else {
			i.calledFunction = f;
		}
		
		if (i.parameters.size() != f.parameters.size()) {
			throw new CTX_EXCEPTION(i.getSource(), "Missmatching argument number in inline call: " + i.functionName);
		}
		
		for (int a = 0; a < f.parameters.size(); a++) {
			TYPE paramType = i.parameters.get(a).check(this);
			if (!paramType.isEqual(f.parameters.get(a).type)) {
				throw new CTX_EXCEPTION(i.getSource(), "Missmatching argument type: " + paramType.typeString() + " vs " + f.parameters.get(a).type.typeString());
			}
		}
		
		i.type = f.returnType;
		return i.type;
	}
	
	public TYPE checkIDRef(IDRef i) throws CTX_EXCEPTION {
		Declaration d = this.scopes.peek().getField(i.id);
		if (d != null) {
			i.origin = d;
			i.type = d.type;
			return i.type;
		}
		else {
			throw new CTX_EXCEPTION(i.getSource(), "Unknown variable: " + i.id);
		}
	}
	
	public TYPE checkAtom(Atom a) throws CTX_EXCEPTION {
		return a.type;
	}
	
}
