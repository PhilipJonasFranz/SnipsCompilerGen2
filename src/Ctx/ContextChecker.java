package Ctx;

import java.util.ArrayList;
import java.util.List;
import java.util.Stack;

import Exc.CTX_EXCEPTION;
import Imm.AST.Function;
import Imm.AST.Program;
import Imm.AST.SyntaxElement;
import Imm.AST.Expression.AddressOf;
import Imm.AST.Expression.ArrayInit;
import Imm.AST.Expression.ArraySelect;
import Imm.AST.Expression.Atom;
import Imm.AST.Expression.BinaryExpression;
import Imm.AST.Expression.Deref;
import Imm.AST.Expression.Expression;
import Imm.AST.Expression.IDRef;
import Imm.AST.Expression.IDRefWriteback;
import Imm.AST.Expression.InlineCall;
import Imm.AST.Expression.SizeOfExpression;
import Imm.AST.Expression.SizeOfType;
import Imm.AST.Expression.StructSelect;
import Imm.AST.Expression.StructureInit;
import Imm.AST.Expression.TypeCast;
import Imm.AST.Expression.UnaryExpression;
import Imm.AST.Expression.Arith.BitNot;
import Imm.AST.Expression.Arith.UnaryMinus;
import Imm.AST.Expression.Boolean.BoolBinaryExpression;
import Imm.AST.Expression.Boolean.BoolUnaryExpression;
import Imm.AST.Expression.Boolean.Compare;
import Imm.AST.Expression.Boolean.Ternary;
import Imm.AST.Lhs.PointerLhsId;
import Imm.AST.Statement.AssignWriteback;
import Imm.AST.Statement.Assignment;
import Imm.AST.Statement.Assignment.ASSIGN_ARITH;
import Imm.AST.Statement.BreakStatement;
import Imm.AST.Statement.CaseStatement;
import Imm.AST.Statement.CompoundStatement;
import Imm.AST.Statement.ContinueStatement;
import Imm.AST.Statement.Declaration;
import Imm.AST.Statement.DefaultStatement;
import Imm.AST.Statement.DoWhileStatement;
import Imm.AST.Statement.ForStatement;
import Imm.AST.Statement.FunctionCall;
import Imm.AST.Statement.IfStatement;
import Imm.AST.Statement.ReturnStatement;
import Imm.AST.Statement.Statement;
import Imm.AST.Statement.StructTypedef;
import Imm.AST.Statement.SwitchStatement;
import Imm.AST.Statement.WhileStatement;
import Imm.TYPE.PROVISO;
import Imm.TYPE.TYPE;
import Imm.TYPE.COMPOSIT.ARRAY;
import Imm.TYPE.COMPOSIT.POINTER;
import Imm.TYPE.COMPOSIT.STRUCT;
import Imm.TYPE.PRIMITIVES.BOOL;
import Imm.TYPE.PRIMITIVES.INT;
import Imm.TYPE.PRIMITIVES.PRIMITIVE;
import Imm.TYPE.PRIMITIVES.VOID;
import Snips.CompilerDriver;
import Util.Logging.Message;

public class ContextChecker {

	Program head;
	
	List<Function> functions = new ArrayList();
	
	Stack<Function> currentFunction = new Stack();
	
	SyntaxElement AST;
	
	Stack<CompoundStatement> compoundStack = new Stack();
	
	Stack<Scope> scopes = new Stack();
	
	public static ContextChecker checker;
	
	public ContextChecker(SyntaxElement AST) {
		this.AST = AST;
		checker = this;
	}
	
	public TYPE check() throws CTX_EXCEPTION {
		this.checkProgram((Program) AST);
		return null;
	}
	
	public TYPE checkProgram(Program p) throws CTX_EXCEPTION {
		scopes.push(new Scope(null));
		
		/* Add global reserved declarations */
		scopes.peek().addDeclaration(CompilerDriver.HEAP_START);
		
		this.head = p;
		for (SyntaxElement s : p.programElements) {
			if (s instanceof Function) {
				Function f = (Function) s;
				/* Check main function as entrypoint, if a function is called, context is provided
				 * and then checked */
				if (f.functionName.equals("main") && !f.manager.provisosTypes.isEmpty()) {
					throw new CTX_EXCEPTION(f.getSource(), "Function main cannot hold proviso types");
				}
				
				/* Check for duplicate function name */
				for (Function f0 : head.functions) {
					if (f0.functionName.equals(f.functionName)) {
						throw new CTX_EXCEPTION(f.getSource(), "Duplicate function name: " + f.functionName);
					}
				}
				
				this.functions.add(f);
				
				/* Check only functions with no provisos, proviso functions will be hot checked. */
				if (f.manager.provisosTypes.isEmpty()) {
					f.check(this);
				}
			}
			else s.check(this);
		}
		
		return null;
	}
	
	public TYPE checkExpression(Expression e) throws CTX_EXCEPTION {
		return e.check(this);
	}
	
	public TYPE checkFunction(Function f) throws CTX_EXCEPTION {
		
		if (f.getReturnType().wordsize() > 1) {
			throw new CTX_EXCEPTION(f.getSource(), "Functions can only return primitive types or pointers, actual : " + f.getReturnType().typeString());
		}
		
		scopes.push(new Scope(scopes.peek()));
		
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
			if (d.getType().getCoreType() instanceof VOID) {
				new Message("Unchecked type " + new VOID().typeString() + ", " + d.getSource().getSourceMarker(), Message.Type.WARN);
			}
		}
		
		head.functions.add(f);
		this.currentFunction.push(f);
		
		/* Check body */
		for (Statement s : f.body) {
			s.check(this);
		}
		
		this.currentFunction.pop();
		scopes.pop();
		return f.getReturnType();
	}
	
	public TYPE checkStructTypedef(StructTypedef e) throws CTX_EXCEPTION {
		/* Set the declarations in the struct type */
		return e.struct;
	}
	
	public TYPE checkStructureInit(StructureInit e) throws CTX_EXCEPTION {
		/* Create a new Struct Instance and apply provisos provided by e */
		try {
			e.createStructInstance();
		} catch (StackOverflowError e0) {
			throw new CTX_EXCEPTION(e.getSource(), e0.getMessage());
		}
		
		if (e.elements.size() != e.structType.typedef.fields.size()) {
			throw new CTX_EXCEPTION(e.getSource(), "Missmatching argument count");
		}
		
		for (int i = 0; i < e.elements.size(); i++) {
			TYPE t = e.elements.get(i).check(this);
			if (!t.isEqual(e.structType.typedef.fields.get(i).getType())) {
				throw new CTX_EXCEPTION(e.getSource(), "Parameter type does not match struct field type: " + t.typeString() + " vs " + e.structType.typedef.fields.get(i).getType().typeString());
			}
		}
		
		e.setType(e.structType);
		
		return e.getType();
	}
	
	public TYPE checkStructSelect(StructSelect e) throws CTX_EXCEPTION {
		
		/* This method links origins and types manually. This is partially due to the fact
		 * that for example, a field in a struct does not count as a field identifier in the
		 * current scope when referencing it. If it was to the check() method, it would report
		 * an duplicate identifier. */
		
		TYPE type = null;
		
		/* Get Base Type */
		if (e.selector instanceof IDRef) {
			IDRef sel = (IDRef) e.selector;
			/* Link automatically, identifier is local */
			type = sel.check(this);
		}
		else if (e.selector instanceof ArraySelect) {
			ArraySelect arr = (ArraySelect) e.selector;
			type = arr.check(this);
		}
		else {
			throw new CTX_EXCEPTION(e.getSource(), "Base must be variable reference");
		}
		
		if (type == null) {
			throw new CTX_EXCEPTION(e.getSource(), "Cannot determine type");
		}
		
		/* First selections does deref, this means that the base must be a pointer */
		if (e.deref) {
			if (type instanceof POINTER) {
				POINTER p0 = (POINTER) type;
				type = p0.targetType;
			}
			else {
				throw new CTX_EXCEPTION(e.selector.getSource(), "Cannot deref non pointer, actual " + type.typeString());
			}
		}
		
		if (!(type instanceof STRUCT)) {
			throw new CTX_EXCEPTION(e.getSource(), "Can only select from struct type, actual " + type.typeString());
		}
		
		Expression selection = e.selection;
		
		while (true) {
			if (type instanceof STRUCT) {
				STRUCT struct = (STRUCT) type;
				
				if (selection instanceof StructSelect) {
					StructSelect sel0 = (StructSelect) selection;
					
					if (sel0.selector instanceof IDRef) {
						type = findField(struct, (IDRef) sel0.selector);
						
						if (sel0.deref) {
							if (!(type instanceof POINTER)) {
								throw new CTX_EXCEPTION(selection.getSource(), "Cannot deref non pointer, actual " + type.typeString());
							}
							else {
								/* Unwrap pointer, selection does dereference */
								POINTER p0 = (POINTER) type;
								type = p0.targetType;
							}
						}
					}
					else if (sel0.selector instanceof ArraySelect) {
						/* Push disjunct scope */
						this.scopes.push(new Scope(null));
						
						/* Add declarations for struct */
						for (Declaration dec : struct.typedef.fields) 
							this.scopes.peek().addDeclaration(dec);
						
						ArraySelect arr = (ArraySelect) sel0.selector;
						type = arr.check(this);
						
						this.scopes.pop();
					}
					else {
						throw new CTX_EXCEPTION(selection.getSource(), sel0.selector.getClass().getName() + " cannot be a selector");
					}
					
					/* Next selection in chain */
					selection = sel0.selection;
				}
				else if (selection instanceof IDRef) {
					/* Last selection */
					type = findField(struct, (IDRef) selection);
					break;
				}
				else if (selection instanceof ArraySelect) {
					/* Push disjunct scope */
					this.scopes.push(new Scope(null));
					
					/* Add declarations for struct */
					for (Declaration dec : struct.typedef.fields) 
						this.scopes.peek().addDeclaration(dec);
					
					ArraySelect arr = (ArraySelect) selection;
					type = arr.check(this);
					
					this.scopes.pop();
					
					break;
				}
				else {
					throw new CTX_EXCEPTION(selection.getSource(), selection.getClass().getName() + " cannot be a selector");
				}
			}
			else {
				throw new CTX_EXCEPTION(e.getSource(), "Cannot select from non struct, actual " + type.typeString());
			}
			
		}
		
		e.setType(type);
		return e.getType();
	}
	
	private TYPE findField(STRUCT struct, IDRef ref0) throws CTX_EXCEPTION {
		/* The ID the current selection targets */
		if (struct.hasField(ref0.id)) {
			/* Link manually, identifier is not part of current scope */
			ref0.origin = struct.getField(ref0.id);
			ref0.setType(ref0.origin.getType());
			
			/* Next type in chain */
			return ref0.getType();
		}
		else {
			throw new CTX_EXCEPTION(ref0.getSource(), "The selected field " + ref0.id + " in the structure " + struct.typeString() + " does not exist");
		}
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
		d.setType(ProvisoManager.setHiddenContext(d.getType()));
		
		if (d.value != null) {
			TYPE t = d.value.check(this);
			
			if (!d.getType().isEqual(t)) {
				throw new CTX_EXCEPTION(d.getSource(), ((t instanceof STRUCT)? "Struct" : "Variable") + " type does not match expression type: " + d.getType().typeString() + " vs. " + t.typeString());
			}
		}
		
		scopes.peek().addDeclaration(d);
		
		/* No need to set type here, is done while parsing */
		return null;
	}
	
	public TYPE checkAssignment(Assignment a) throws CTX_EXCEPTION {
		TYPE targetType = a.lhsId.check(this);
		
		if (a.lhsId instanceof PointerLhsId) targetType = new POINTER(targetType);
		
		String fieldName = a.lhsId.getFieldName();
		
		Declaration dec = scopes.peek().getField(fieldName);
		a.origin = dec;
		
		TYPE t = a.value.check(this);
		
		TYPE ctype = t;
		
		/* If target type is a pointer, only the core types have to match */
		if (targetType instanceof POINTER) {
			POINTER p = (POINTER) targetType;
			
			if (!p.getCoreType().isEqual(t.getCoreType())) {
				throw new CTX_EXCEPTION(a.getSource(), "Pointer type does not match expression type: " + p.getCoreType().typeString() + " vs. " + t.getCoreType().typeString());
			}
			
			ctype = t.getCoreType();
		}
		/* If not, the types have to be equal */
		else if (!t.isEqual(targetType)) {
			throw new CTX_EXCEPTION(a.getSource(), "Variable type does not match expression type: " + targetType.typeString() + " vs. " + t.typeString());
		}
		
		if (a.assignArith != ASSIGN_ARITH.NONE) {
			if (!(t instanceof PRIMITIVE)) {
				throw new CTX_EXCEPTION(a.getSource(), "Assign arith operation is only applicable for primitive types");
			}
			
			if (a.assignArith == ASSIGN_ARITH.AND_ASSIGN || a.assignArith == ASSIGN_ARITH.ORR_ASSIGN || a.assignArith == ASSIGN_ARITH.XOR_ASSIGN) {
				if (!(ctype instanceof BOOL)) {
					throw new CTX_EXCEPTION(a.getSource(), "Expression type " + t.typeString() + " is not applicable for boolean assign operator");
				}
			}
			else if (a.assignArith != ASSIGN_ARITH.NONE) {
				if (!(ctype instanceof INT)) {
					throw new CTX_EXCEPTION(a.getSource(), "Expression type " + t.typeString() + " is not applicable for assign operator");
				}
			}
		}
		
		a.lhsId.expressionType = t;
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
		
		if (!type.isEqual(c.superStatement.condition.getType())) {
			throw new CTX_EXCEPTION(c.condition.getSource(), "Condition type " + type.typeString() + " does not switch condition type " + c.superStatement.condition.getType().typeString());
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
		if (r.value != null) {
			TYPE t = r.value.check(this);

			if (t.isEqual(this.currentFunction.peek().getReturnType())) {
				return t;
			}
			else {
				throw new CTX_EXCEPTION(r.getSource(), "Return type " + t.typeString() + " does not match function return type " + this.currentFunction.peek().getReturnType().typeString());
			}
		}
		else {
			if (!(currentFunction.peek().getReturnType() instanceof VOID)) {
				throw new CTX_EXCEPTION(r.getSource(), "Return type does not match function type, " + new VOID().typeString() + " vs " + currentFunction.peek().getReturnType().typeString());
			}
			
			return new VOID();
		}
	}
	
	public TYPE checkTernary(Ternary t) throws CTX_EXCEPTION {
		TYPE type = t.condition.check(this);
		if (!(type instanceof BOOL)) {
			throw new CTX_EXCEPTION(t.condition.getSource(), "Ternary condition has to be of type BOOL, actual " + type.typeString());
		}
		
		if (t.condition instanceof ArrayInit) {
			throw new CTX_EXCEPTION(t.condition.getSource(), "Structure Init can only be a sub expression of structure init");
		}
		
		TYPE t0 = t.leftOperand.check(this);
		TYPE t1 = t.rightOperand.check(this);
		
		if (t.leftOperand instanceof ArrayInit) {
			throw new CTX_EXCEPTION(t.leftOperand.getSource(), "Structure Init can only be a sub expression of structure init");
		}
		
		if (t.rightOperand instanceof ArrayInit) {
			throw new CTX_EXCEPTION(t.rightOperand.getSource(), "Structure Init can only be a sub expression of structure init");
		}
		
		if (!t0.isEqual(t1)) {
			throw new CTX_EXCEPTION(t.condition.getSource(), "Both results of ternary operation have to be of the same type, " + t0.typeString() + " vs " + t1.typeString());
		}
		
		t.setType(t0);
		return t.getType();
	}
	
	public TYPE checkBinaryExpression(BinaryExpression b) throws CTX_EXCEPTION {
		TYPE left = b.getLeft().check(this);
		TYPE right = b.getRight().check(this);
		
		if (b.left instanceof ArrayInit) {
			throw new CTX_EXCEPTION(b.left.getSource(), "Structure Init can only be a sub expression of structure init");
		}
		
		if (b.right instanceof ArrayInit) {
			throw new CTX_EXCEPTION(b.right.getSource(), "Structure Init can only be a sub expression of structure init");
		}
		
		if (left.wordsize() > 1) {
			throw new CTX_EXCEPTION(b.left.getSource(), "Can only apply to primitive or pointer, actual " + left.typeString());
		}
		
		if (right.wordsize() > 1) {
			throw new CTX_EXCEPTION(b.left.getSource(), "Can only apply to primitive or pointer, actual " + left.typeString());
		}
		
		if (left instanceof POINTER) {
			if (!(right instanceof INT)) {
				throw new CTX_EXCEPTION(b.getSource(), "Pointer arithmetic is only supported for " + new INT().typeString() + ", actual " + right.typeString());
			}
			
			b.setType(left);
		}
		else if (right instanceof POINTER) {
			if (!(left instanceof INT)) {
				throw new CTX_EXCEPTION(b.getSource(), "Pointer arithmetic is only supported for " + new INT().typeString() + ", actual " + left.typeString());
			}
			
			b.setType(left);
		}
		else if (left.isEqual(right)) {
			b.setType(left);
		}
		else {
			throw new CTX_EXCEPTION(b.getSource(), "Operand types do not match: " + left.typeString() + " vs. " + right.typeString());
		}
	
		return b.getType();
	}
	
	/**
	 * Overrides binary expression.<br>
	 * Checks for:<br>
	 * - Both operand types have to be of type BOOL<br>
	 */
	public TYPE checkBoolBinaryExpression(BoolBinaryExpression b) throws CTX_EXCEPTION {
		TYPE left = b.getLeft().check(this);
		TYPE right = b.getRight().check(this);
		
		if (!(left instanceof BOOL)) {
			throw new CTX_EXCEPTION(b.left.getSource(), "Expected " + new BOOL().typeString() + ", actual " + left.typeString());
		}
		
		if (!(right instanceof BOOL)) {
			throw new CTX_EXCEPTION(b.right.getSource(), "Expected " + new BOOL().typeString() + ", actual " + right.typeString());
		}
		
		b.setType(left);
		return b.getType();
	}
	
	/**
	 * Overrides unary expression.<br>
	 * Checks for:<br>
	 * - Operand type has to be of type BOOL<br>
	 */
	public TYPE checkBoolUnaryExpression(BoolUnaryExpression b) throws CTX_EXCEPTION {
		TYPE t = b.getOperand().check(this);
		
		if (!(t instanceof BOOL)) {
			throw new CTX_EXCEPTION(b.getOperand().getSource(), "Expected bool, actual " + t.typeString());
		}
		
		b.setType(t);
		return b.getType();
	}
	
	public TYPE checkUnaryExpression(UnaryExpression u) throws CTX_EXCEPTION {
		TYPE op = u.getOperand().check(this);
		
		if (u.getOperand() instanceof ArrayInit) {
			throw new CTX_EXCEPTION(u.getOperand().getSource(), "Structure Init can only be a sub expression of structure init");
		}
		
		if (u instanceof BitNot && op instanceof PRIMITIVE) {
			u.setType(op);
			return u.getType();
		}
		else if (u instanceof UnaryMinus && op instanceof PRIMITIVE) {
			u.setType(op);
			return u.getType();
		}
		else {
			throw new CTX_EXCEPTION(u.getSource(), "Unknown Expression: " + u.getClass().getName());
		}
	}
	
	public TYPE checkCompare(Compare c) throws CTX_EXCEPTION {
		TYPE left = c.getLeft().check(this);
		TYPE right = c.getRight().check(this);
		
		if (c.left instanceof ArrayInit) {
			throw new CTX_EXCEPTION(c.left.getSource(), "Structure Init can only be a sub expression of structure init");
		}
		
		if (c.right instanceof ArrayInit) {
			throw new CTX_EXCEPTION(c.right.getSource(), "Structure Init can only be a sub expression of structure init");
		}
		
		if (left.isEqual(right)) {
			c.setType(new BOOL());
			return c.getType();
		}
		else {
			throw new CTX_EXCEPTION(c.getSource(), "Operand types do not match: " + left.typeString() + " vs. " + right.typeString());
		}
	}
	
	public TYPE checkInlineCall(InlineCall i) throws CTX_EXCEPTION {
		/* Find the called function */
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
		
		if (!f.manager.provisosTypes.isEmpty()) {
			if (f.manager.containsMapping(i.provisosTypes)) {
				/* Mapping already exists, just return return type of this specific mapping */
				i.setType(f.manager.getMappingReturnType(i.provisosTypes));
			}
			else {
				/* Create a new context, check function for this specific context */
				f.setContext(i.provisosTypes);
				this.scopes.push(new Scope(this.scopes.get(0)));
				f.check(this);
				this.scopes.pop();
				i.setType(f.manager.getMappingReturnType(i.provisosTypes));
			}
		}
		
		if (i.parameters.size() != f.parameters.size()) {
			throw new CTX_EXCEPTION(i.getSource(), "Missmatching argument number in inline call: " + i.functionName);
		}
		
		for (int a = 0; a < f.parameters.size(); a++) {
			if (i.parameters.get(a) instanceof ArrayInit) {
				throw new CTX_EXCEPTION(i.getSource(), "Structure Init can only be a sub expression of structure init");
			}
			
			TYPE paramType = i.parameters.get(a).check(this);
			
			TYPE functionParamType = f.parameters.get(a).getType();
			
			/* Switch out function parameter dynamically with inline call proviso type */
			if (f.parameters.get(a).getRawType() instanceof PROVISO) {
				PROVISO p = (PROVISO) f.parameters.get(a).getRawType();
				for (int k = 0; k < f.manager.provisosTypes.size(); k++) {
					PROVISO p0 = (PROVISO) f.manager.provisosTypes.get(k);
					if (p0.placeholderName.equals(p.placeholderName)) {
						functionParamType = i.provisosTypes.get(k);
						break;
					}
				}
			}
			
			if (!paramType.isEqual(functionParamType)) {
				throw new CTX_EXCEPTION(i.parameters.get(a).getSource(), "Missmatching argument type: " + paramType.typeString() + " vs " + f.parameters.get(a).getType().typeString());
			}
		}
		
		if (f.manager.provisosTypes.isEmpty() || !f.manager.containsMapping(i.provisosTypes)) {
			i.setType(f.getReturnType().clone());
		}
		
		if (i.getType() instanceof VOID) {
			throw new CTX_EXCEPTION(i.getSource(), "Expected return value, got " + i.getType().typeString());
		}
		
		return i.getType();
	}
	
	public TYPE checkFunctionCall(FunctionCall i) throws CTX_EXCEPTION {
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

		if (!f.manager.provisosTypes.isEmpty()) {
			if (!f.manager.containsMapping(i.provisosTypes)) {
				/* Create a new context, check function for this specific context */
				f.setContext(i.provisosTypes);
				
				/* Create new scope that points to the global scope */
				this.scopes.push(new Scope(this.scopes.get(0)));
				f.check(this);
				this.scopes.pop();
			}
		}
		
		if (i.parameters.size() != f.parameters.size()) {
			throw new CTX_EXCEPTION(i.getSource(), "Missmatching argument number in inline call: " + i.functionName);
		}
		
		for (int a = 0; a < f.parameters.size(); a++) {
			TYPE paramType = i.parameters.get(a).check(this);
			
			if (!paramType.isEqual(f.parameters.get(a).getType())) {
				throw new CTX_EXCEPTION(i.parameters.get(a).getSource(), "Missmatching argument type: " + paramType.typeString() + " vs " + f.parameters.get(a).getType().typeString());
			}
		}
		
		return new VOID();
	}
	
	/**
	 * Checks for:<br>
	 * - Sets the origin of the reference<br>
	 * - Sets the type of the reference
	 */
	public TYPE checkIDRef(IDRef i) throws CTX_EXCEPTION {
		Declaration d = this.scopes.peek().getField(i.id);
		
		if (d != null) {
			i.origin = d;
			i.setType(d.getType());
			return i.getType();
		}
		else {
			throw new CTX_EXCEPTION(i.getSource(), "Unknown variable: " + i.id);
		}
	}
	
	public TYPE checkArrayInit(ArrayInit init) throws CTX_EXCEPTION {
		if (init.elements.isEmpty()) {
			throw new CTX_EXCEPTION(init.getSource(), "Structure init must have at least one element");
		}
		
		TYPE type0 = init.elements.get(0).check(this);
		if (init.elements.size() > 1) {
			for (int i = 1; i < init.elements.size(); i++) {
				TYPE typeX = init.elements.get(i).check(this);
				if (!typeX.isEqual(type0)) {
					throw new CTX_EXCEPTION(init.getSource(), "Structure init elements have to have same type: " + type0.typeString() + " vs " + typeX.typeString());
				}
			}
		}
		
		init.setType(new ARRAY(type0, init.elements.size()));
		return init.getType();
	}
	
	public TYPE checkSizeOfType(SizeOfType sot) throws CTX_EXCEPTION {
		sot.setType(new INT());
		return sot.getType();
	}
	
	public TYPE checkSizeOfExpression(SizeOfExpression soe) throws CTX_EXCEPTION {
		TYPE t = soe.expression.check(this);
		soe.sizeType = t;
		
		soe.setType(new INT());
		return soe.getType();
	}
	
	public TYPE checkAddressOf(AddressOf aof) throws CTX_EXCEPTION {
		TYPE t = aof.expression.check(this);
		
		if (!(aof.expression instanceof IDRef) && !(aof.expression instanceof ArraySelect)) {
			throw new CTX_EXCEPTION(aof.getSource(), "Can only get address of variable reference or element select.");
		}
		
		aof.setType(new POINTER(t.getCoreType()));
		
		return aof.getType();
	}
	
	/**
	 * Checks for:<br>
	 * - Operand type is pointer.y
	 */
	public TYPE checkDeref(Deref deref) throws CTX_EXCEPTION {
		TYPE t = deref.expression.check(this);
		
		/* Dereference pointer or primitive type */
		if (t instanceof PRIMITIVE) {
			/* Set to core type */
			deref.setType(t.getCoreType());
		}
		else if (t instanceof POINTER) {
			POINTER p = (POINTER) t;
			deref.setType(p.targetType);
		}
		else {
			throw new CTX_EXCEPTION(deref.expression.getSource(), "Cannot dereference type " + t.typeString());
		}
		
		/* Dereferencing a primitive can be a valid statement, but it can be unsafe. A pointer would be safer. */
		if (t instanceof PRIMITIVE) {
			if (!CompilerDriver.disableWarnings) new Message("Operand is not a pointer, may cause unexpected behaviour, " + deref.getSource().getSourceMarker(), Message.Type.WARN);
		}
		
		return deref.getType();
	}
	
	public TYPE checkTypeCast(TypeCast tc) throws CTX_EXCEPTION {
		TYPE t = tc.expression.check(this);
		
		if (t.wordsize() != tc.castType.wordsize()) {
			throw new CTX_EXCEPTION(tc.getSource(), "Cannot cast " + t.typeString() + " to " + tc.castType.typeString());
		}
		
		tc.setType(tc.castType);
		return tc.castType;
	}
	
	public TYPE checkIDRefWriteback(IDRefWriteback i) throws CTX_EXCEPTION {
		if (i.getShadowRef() instanceof IDRef) {
			IDRef ref = (IDRef) i.getShadowRef();
			i.idRef = ref;
			
			TYPE t = ref.check(this);
			
			if (!(t instanceof PRIMITIVE)) {
				throw new CTX_EXCEPTION(i.idRef.getSource(), "Can only be applied to primitive types");
			}
			
			i.setType(t);
		}
		else {
			throw new CTX_EXCEPTION(i.getSource(), "Can only apply to id reference");
		}
		
		return i.getType();
	}
	
	public TYPE checkAssignWriteback(AssignWriteback i) throws CTX_EXCEPTION {
		if (i.getShadowRef() instanceof IDRefWriteback) {
			IDRefWriteback wb = (IDRefWriteback) i.getShadowRef();
			wb.check(this);
			i.idWb = wb;
		}
		else {
			throw new CTX_EXCEPTION(i.getSource(), "Can only apply to id reference");
		}
		
		return null;
	}
	
	/**
	 * Checks for:<br>
	 * - At least one selection has to be made<br>
	 * - Checks that the shadow ref is an IDRef<br>
	 * - Checks that the types of the selection are of type int<br>
	 * - Amount of selections does not exceed structure dimension<br>
	 */
	public TYPE checkArraySelect(ArraySelect select) throws CTX_EXCEPTION {
		if (select.selection.isEmpty()) {
			throw new CTX_EXCEPTION(select.getSource(), "Element select must have at least one element (how did we even get here?)");
		}
		
		if (select.getShadowRef() instanceof IDRef) {
			IDRef ref = (IDRef) select.getShadowRef();
			select.idRef = ref;
			
			TYPE type0 = ref.check(this);
			
			/* If pointer, unwrap it */
			TYPE chain = (type0 instanceof POINTER)? ((POINTER) type0).targetType : type0;
			
			/* Check selection chain */
			for (int i = 0; i < select.selection.size(); i++) {
				TYPE stype = select.selection.get(i).check(this);
				if (!(stype instanceof INT)) {
					throw new CTX_EXCEPTION(select.selection.get(i).getSource(), "Selection has to be of type " + new INT().typeString() + ", actual " + stype.typeString());
				}
				else {
					if (!(chain instanceof ARRAY)) {
						throw new CTX_EXCEPTION(select.selection.get(i).getSource(), "Cannot select from type " + type0.typeString());
					}
					else {
						ARRAY arr = (ARRAY) chain;
						
						if (select.selection.get(i) instanceof Atom) {
							Atom a = (Atom) select.selection.get(i);
							int value = (int) a.getType().getValue();
							if (value < 0 || value >= arr.getLength()) {
								throw new CTX_EXCEPTION(select.selection.get(i).getSource(), "Array out of bounds: " + value + ", type: " + chain.typeString());
							}
						}
						
						chain = ((ARRAY) chain).elementType;
					}
				}
			}
			
			if (type0 instanceof POINTER) chain = new POINTER(chain);
			
			select.setType(chain);
			return select.getType();
		}
		else {
			throw new CTX_EXCEPTION(select.getShadowRef().getSource(), "Can only select from variable reference");
		}
	}
	
	public TYPE checkAtom(Atom a) throws CTX_EXCEPTION {
		return a.getType();
	}
	
}
