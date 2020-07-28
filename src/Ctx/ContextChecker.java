package Ctx;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Stack;

import Exc.CTX_EXC;
import Imm.ASM.Util.Operands.RegOp;
import Imm.ASM.Util.Operands.RegOp.REG;
import Imm.AST.Function;
import Imm.AST.Namespace;
import Imm.AST.Program;
import Imm.AST.SyntaxElement;
import Imm.AST.Expression.AddressOf;
import Imm.AST.Expression.ArrayInit;
import Imm.AST.Expression.ArraySelect;
import Imm.AST.Expression.Atom;
import Imm.AST.Expression.BinaryExpression;
import Imm.AST.Expression.Deref;
import Imm.AST.Expression.Expression;
import Imm.AST.Expression.FunctionRef;
import Imm.AST.Expression.IDRef;
import Imm.AST.Expression.IDRefWriteback;
import Imm.AST.Expression.InlineCall;
import Imm.AST.Expression.InstanceofExpression;
import Imm.AST.Expression.RegisterAtom;
import Imm.AST.Expression.SizeOfExpression;
import Imm.AST.Expression.SizeOfType;
import Imm.AST.Expression.StructSelect;
import Imm.AST.Expression.StructSelectWriteback;
import Imm.AST.Expression.StructureInit;
import Imm.AST.Expression.TypeCast;
import Imm.AST.Expression.UnaryExpression;
import Imm.AST.Expression.Arith.Add;
import Imm.AST.Expression.Arith.BitNot;
import Imm.AST.Expression.Arith.Mul;
import Imm.AST.Expression.Arith.UnaryMinus;
import Imm.AST.Expression.Boolean.BoolBinaryExpression;
import Imm.AST.Expression.Boolean.BoolUnaryExpression;
import Imm.AST.Expression.Boolean.Compare;
import Imm.AST.Expression.Boolean.Ternary;
import Imm.AST.Lhs.PointerLhsId;
import Imm.AST.Lhs.SimpleLhsId;
import Imm.AST.Statement.AssignWriteback;
import Imm.AST.Statement.Assignment;
import Imm.AST.Statement.Assignment.ASSIGN_ARITH;
import Imm.AST.Statement.BreakStatement;
import Imm.AST.Statement.CaseStatement;
import Imm.AST.Statement.CompoundStatement;
import Imm.AST.Statement.ContinueStatement;
import Imm.AST.Statement.Declaration;
import Imm.AST.Statement.DefaultStatement;
import Imm.AST.Statement.DirectASMStatement;
import Imm.AST.Statement.DoWhileStatement;
import Imm.AST.Statement.ForEachStatement;
import Imm.AST.Statement.ForStatement;
import Imm.AST.Statement.FunctionCall;
import Imm.AST.Statement.IfStatement;
import Imm.AST.Statement.ReturnStatement;
import Imm.AST.Statement.SignalStatement;
import Imm.AST.Statement.Statement;
import Imm.AST.Statement.StructTypedef;
import Imm.AST.Statement.SwitchStatement;
import Imm.AST.Statement.TryStatement;
import Imm.AST.Statement.WatchStatement;
import Imm.AST.Statement.WhileStatement;
import Imm.AsN.AsNNode.MODIFIER;
import Imm.TYPE.PROVISO;
import Imm.TYPE.TYPE;
import Imm.TYPE.COMPOSIT.ARRAY;
import Imm.TYPE.COMPOSIT.POINTER;
import Imm.TYPE.COMPOSIT.STRUCT;
import Imm.TYPE.PRIMITIVES.BOOL;
import Imm.TYPE.PRIMITIVES.FUNC;
import Imm.TYPE.PRIMITIVES.INT;
import Imm.TYPE.PRIMITIVES.NULL;
import Imm.TYPE.PRIMITIVES.PRIMITIVE;
import Imm.TYPE.PRIMITIVES.VOID;
import Snips.CompilerDriver;
import Util.NamespacePath;
import Util.Pair;
import Util.Source;
import Util.Logging.Message;
import Util.Logging.ProgressMessage;

public class ContextChecker {

	Program head;
	
	List<Function> functions = new ArrayList();
	
	Stack<Function> currentFunction = new Stack();
	
	SyntaxElement AST;
	
	Stack<CompoundStatement> compoundStack = new Stack();
	
	Stack<Scope> scopes = new Stack();
	
	Stack<List<TYPE>> signalStack = new Stack();
	
	Stack<SyntaxElement> exceptionEscapeStack = new Stack();
	
	public Statement currentStatement = null;
	
	public List<Declaration> toLink = new ArrayList();
	
	public static ContextChecker checker;
	
	public static ProgressMessage progress;
	
	List<Message> messages = new ArrayList();
	
	/* Contains the structs that have no extensions */
	List<StructTypedef> tLStructs = new ArrayList();
	
	public ContextChecker(SyntaxElement AST, ProgressMessage progress) {
		this.AST = AST;
		ContextChecker.progress = progress;
		checker = this;
	}
	
	public TYPE check() throws CTX_EXC {
		this.checkProgram((Program) AST);
		
		if (!this.tLStructs.isEmpty()) {
			int SIDStart = 1;
			if (this.tLStructs.size() == 1) 
				this.tLStructs.get(0).propagateSIDs(SIDStart, null);
			else {
				/* Apply to first n - 1 */
				for (int i = 1; i < this.tLStructs.size(); i++) { 
					SIDStart = this.tLStructs.get(i - 1).propagateSIDs(SIDStart, this.tLStructs.get(i));
					
					/* Set neighbour of n - 1 to n */
					this.tLStructs.get(i - 1).SIDNeighbour = this.tLStructs.get(i);
				}
				
				/* Apply to last */
				this.tLStructs.get(this.tLStructs.size() - 1).propagateSIDs(SIDStart, null);
			}
		}
		
		/* Flush warn messages */
		for (Message m : this.messages) m.flush();
		
		return null;
	}
	
	public TYPE checkProgram(Program p) throws CTX_EXC {
		scopes.push(new Scope(null));
		
		/* Add global reserved declarations */
		scopes.peek().addDeclaration(CompilerDriver.HEAP_START);
		
		boolean gotMain = false;
		
		this.head = p;
		for (int i = 0; i < p.programElements.size(); i++) {
			SyntaxElement s = p.programElements.get(i);
			if (s instanceof Function) {
				Function f = (Function) s;
				
				if (f.path.build().equals("main")) gotMain = true;
				
				/* Check main function as entrypoint, if a function is called, 
				 * context is provided and then checked */
				if (f.path.build().equals("main") && !f.provisosTypes.isEmpty()) 
					throw new CTX_EXC(f.getSource(), "Function main cannot hold proviso types");
				
				/* Check for duplicate function name */
				for (Function f0 : head.functions) {
					if (f0.path.build().equals(f.path.build())) 
						throw new CTX_EXC(f.getSource(), "Duplicate function name: " + f.path.build());
				}
				
				this.functions.add(f);
				
				/* Check only functions with no provisos, proviso functions will be hot checked. */
				if (f.provisosTypes.isEmpty()) f.check(this);
			}
			else if (s instanceof Declaration) s.check(this);
			else if (s instanceof Namespace) {
				p.namespaces.add((Namespace) s);
				s.check(this);
			}
			else s.check(this);
			
			if (progress != null) 
				progress.incProgress((double) i / p.programElements.size());
		}
		
		if (!gotMain) 
			throw new CTX_EXC(p.getSource(), "Missing main function");
		
		if (progress != null) progress.incProgress(1);
		
		for (Declaration d : toLink) 
			if (d.last != null) 
				d.last.free.add(d);
		
		return null;
	}
	
	public TYPE checkFunction(Function f) throws CTX_EXC {
		/* Proviso Types are already set at this point */
		
		scopes.push(new Scope(scopes.peek()));
		
		this.signalStack.push(new ArrayList());
		this.exceptionEscapeStack.push(f);
		
		if (f.path.build().equals("main") && f.signals) 
			throw new CTX_EXC(f.getSource(), "Entry function 'main' cannot signal exceptions");
		
		/* Check for duplicate function parameters */
		if (f.parameters.size() > 1) {
			for (int i = 0; i < f.parameters.size(); i++) {
				for (int a = i + 1; a < f.parameters.size(); a++) {
					if (f.parameters.get(i).path.build().equals(f.parameters.get(a).path.build())) 
						throw new CTX_EXC(f.getSource(), "Duplicate parameter name: " + f.parameters.get(i).path.build() + " in function: " + f.path.build());
				}
			}
		}
		
		for (Declaration d : f.parameters) {
			d.check(this);
			if (d.getType().getCoreType() instanceof VOID && !CompilerDriver.disableWarnings) 
				messages.add(new Message("Unchecked type " + new VOID().typeString() + ", " + d.getSource().getSourceMarker(), Message.Type.WARN, true));
		}
		
		if (f.signals && f.signalsTypes.isEmpty()) 
			throw new CTX_EXC(f.getSource(), "Function must signal at least one exception type");
		
		/* Check body */
		head.functions.add(f);
		this.currentFunction.push(f);
		for (Statement s : f.body) {
			currentStatement = s;
			s.check(this);
		}
		this.currentFunction.pop();
		
		/* Check for signaled types that are not thrown */
		for (TYPE t : f.signalsTypes) {
			boolean contains = false;
			for (int i = 0; i < this.signalStack.peek().size(); i++) 
				contains |= this.signalStack.peek().get(i).isEqual(t);
			
			if (!contains) 
				messages.add(new Message("Watched exception " + t.typeString() + " is not thrown in function '" + f.path.build() + "', " + f.getSource().getSourceMarker(), Message.Type.WARN, true));
		}
		
		/* Remove function signaled exceptions */
		for (TYPE t : f.signalsTypes) {
			for (int i = 0; i < this.signalStack.peek().size(); i++) {
				if (this.signalStack.peek().get(i).isEqual(t)) {
					this.signalStack.peek().remove(i);
					break;
				}
			}
		}
		
		/* Exception types are not watched or signaled */
		if (!this.signalStack.peek().isEmpty()) {
			String unwatched = "Unwatched exceptions for function " + f.path.build() + ": ";
			for (TYPE t : this.signalStack.peek()) unwatched += t.typeString() + ", ";
			unwatched = unwatched.substring(0, unwatched.length() - 2);
			throw new CTX_EXC(f.getSource(), unwatched);
		}
		
		this.exceptionEscapeStack.pop();
		this.signalStack.pop();
		scopes.pop();
		
		return f.getReturnType().clone();
	}
	
	public TYPE checkStructTypedef(StructTypedef e) throws CTX_EXC {
		Optional<TYPE> opt = e.proviso.stream().filter(x -> !(x instanceof PROVISO)).findFirst();
		if (opt.isPresent())
			throw new CTX_EXC(e.getSource(), "Found non proviso type in proviso header: " + opt.get().typeString());
		
		if (e.extension != null && e.extension.proviso.size() != e.extProviso.size()) 
			throw new CTX_EXC(e.getSource(), "Incorrect number of proviso for extension " + e.extension.self.typeString() + ", expected " + e.extension.proviso.size() + ", got " + e.extProviso.size());
		
		/* 
		 * Add to topLevelStructExtenders, since this typedef is the root
		 * of an extension tree, and is used to assign SIDs.
		 */
		if (e.extension == null)
			this.tLStructs.add(e);
		
		/* Set the declarations in the struct type */
		return new VOID();
	}
	
	public TYPE checkSignal(SignalStatement e) throws CTX_EXC {
		TYPE exc = e.exceptionInit.check(this);
		e.watchpoint = this.exceptionEscapeStack.peek();
		
		/* Add to signal stack */
		if (!this.signalStackContains(exc)) 
			this.signalStack.peek().add(exc);
		
		return new VOID();
	}
	
	public TYPE checkTryStatement(TryStatement e) throws CTX_EXC {
		this.scopes.push(new Scope(this.scopes.peek()));
		this.signalStack.push(new ArrayList());
		
		/* If exception is thrown that is not watched by this statement, relay to this watchpoint */
		e.watchpoint = this.exceptionEscapeStack.peek();
		
		/* Setup new watchpoint target */
		this.exceptionEscapeStack.push(e);
		
		for (Statement s : e.body) {
			currentStatement = s;
			s.check(this);
		}
		
		for (int i = 0; i < e.watchpoints.size(); i++) {
			for (int a = i + 1; a < e.watchpoints.size(); a++) {
				if (e.watchpoints.get(i).watched.getType().isEqual(e.watchpoints.get(a).watched.getType())) 
					throw new CTX_EXC(e.getSource(), "Found multiple watchpoints for exception " + e.watchpoints.get(i).watched.getType().typeString());
			}
		}
		
		this.scopes.pop();
		this.exceptionEscapeStack.pop();
		
		for (WatchStatement w : e.watchpoints) {
			w.check(this);
			
			for (TYPE t : this.signalStack.peek()) {
				if (t.isEqual(w.watched.getType())) {
					w.hasTarget = true;
					this.signalStack.peek().remove(t);
					break;
				}
			}
			
			if (!w.hasTarget) 
				messages.add(new Message("Watched exception type " + w.watched.getType().typeString() + " is not thrown in try block, " + e.getSource().getSourceMarker(), Message.Type.WARN, true));
		}
		
		/* Add all unwatched to the previous signal level */
		List<TYPE> unwatched = this.signalStack.pop();
		e.unwatched = unwatched;
		for (TYPE t : unwatched) {
			boolean contains = this.signalStack.peek().stream().filter(x -> x.isEqual(t)).count() > 0;
			if (!contains) this.signalStack.peek().add(t);
		}
		
		return new VOID();
	}
	
	public TYPE checkWatchStatement(WatchStatement e) throws CTX_EXC {
		this.scopes.push(new Scope(this.scopes.peek()));
		
		e.watched.check(this);
		
		for (Statement s : e.body) {
			currentStatement = s;
			s.check(this);
		}
		
		this.scopes.pop();
		return new VOID();
	}
	
	public TYPE checkStructureInit(StructureInit e) throws CTX_EXC {
		e.setType(e.structType);
		
		if (!this.currentFunction.isEmpty()) 
			ProvisoUtil.mapNTo1(e.getType(), this.currentFunction.peek().provisosTypes);
		
		if (e.elements.size() != e.structType.getTypedef().getFields().size()) 
			throw new CTX_EXC(e.getSource(), "Missmatching argument count: Expected " + e.structType.getTypedef().getFields().size() + " but got " + e.elements.size());
		
		/* Make sure that all field types are equal to the expected types */
		for (int i = 0; i < e.elements.size(); i++) {
			TYPE valType = e.elements.get(i).check(this);
			TYPE strType = e.structType.getField(e.structType.getTypedef().getFields().get(i).path).getType();
			
			if (!valType.isEqual(strType)) {
				if (valType instanceof POINTER || strType instanceof POINTER) 
					CompilerDriver.printProvisoTypes = true;
				throw new CTX_EXC(e.getSource(), "Argument type does not match struct field (" + (i + 1) + ") type: " + valType.typeString() + " vs " + strType.typeString());
			}
		}
		
		/* Struct may have modifier restrictions */
		this.checkModifier(e.structType.getTypedef().modifier, e.structType.getTypedef().path, e.getSource());
		
		return e.getType();
	}
	
	public TYPE checkStructSelect(StructSelect e) throws CTX_EXC {
		/* 
		 * This method links origins and types manually. This is partially due to the fact
		 * that for example, a field in a struct does not count as a field identifier in the
		 * current scope when referencing it. If it was to the check() method, it would report
		 * an duplicate identifier. 
		 */
		
		TYPE type = null;
		
		/* Get Base Type */
		if (e.selector instanceof IDRef) {
			IDRef sel = (IDRef) e.selector;
			/* Link automatically, identifier is local */
			type = sel.check(this);
		}
		else if (e.selector instanceof TypeCast) {
			TypeCast tc = (TypeCast) e.selector;
			
			if (!(tc.expression instanceof IDRef))
				throw new CTX_EXC(e.getSource(), "Base must be variable reference");
			
			type = tc.check(this);
		}
		else if (e.selector instanceof ArraySelect) {
			ArraySelect arr = (ArraySelect) e.selector;
			type = arr.check(this);
		}
		else throw new CTX_EXC(e.getSource(), "Base must be variable reference");
		
		if (type == null) 
			throw new CTX_EXC(e.getSource(), "Cannot determine type");
		
		/* First selections does deref, this means that the base must be a pointer */
		if (e.deref) {
			if (type instanceof POINTER) {
				POINTER p0 = (POINTER) type;
				type = p0.targetType;
			}
			else throw new CTX_EXC(e.selector.getSource(), "Cannot deref non pointer, actual " + type.typeString());
		}
		
		if (!(type instanceof STRUCT)) 
			throw new CTX_EXC(e.getSource(), "Can only select from struct type, actual " + type.typeString());
		
		Expression selection = e.selection;
		
		while (true) {
			if (type instanceof STRUCT) {
				STRUCT struct = (STRUCT) type;
				
				if (selection instanceof StructSelect) {
					StructSelect sel0 = (StructSelect) selection;
					
					if (sel0.selector instanceof IDRef) {
						IDRef ref = (IDRef) sel0.selector;
						
						type = findField(struct, ref);
						
						if (sel0.deref) {
							if (!(type instanceof POINTER)) 
								throw new CTX_EXC(selection.getSource(), "Cannot deref non pointer, actual " + type.typeString());
							else {
								/* Unwrap pointer, selection does dereference */
								POINTER p0 = (POINTER) type;
								type = p0.targetType;
							}
						}
					}
					else if (sel0.selector instanceof ArraySelect) {
						/* Push new scope to house the struct fields */
						this.scopes.push(new Scope(this.scopes.peek()));
						
						/* Add declarations for struct */
						for (int i = 0; i < struct.getNumberOfFields(); i++) 
							/* 
							 * Add the struct fields to the current scope, so that the select expresssion
							 * from the array select can be checked and finds the field its selecting from.
							 * The fields are added without checking for duplicates. This is not a big problem,
							 * since the same scope is instantly popped afterwards.
							 */
							this.scopes.peek().addDeclaration(struct.getFieldNumber(i), false);
						
						ArraySelect arr = (ArraySelect) sel0.selector;
						type = arr.check(this);
						
						this.scopes.pop();
					}
					else throw new CTX_EXC(selection.getSource(), sel0.selector.getClass().getName() + " cannot be a selector");
					
					/* Next selection in chain */
					selection = sel0.selection;
				}
				else if (selection instanceof IDRef) {
					IDRef ref = (IDRef) selection;
					
					/* Last selection */
					type = findField(struct, ref);
					
					TYPE type0 = type;
					if (type0 instanceof POINTER) 
						type0 = ((POINTER) type0).targetType;
					
					break;
				}
				else if (selection instanceof ArraySelect) {
					/* Push new scope to house the struct fields */
					this.scopes.push(new Scope(this.scopes.peek()));

					/* Add declarations for struct */
					for (int i = 0; i < struct.getNumberOfFields(); i++) 
						/* 
						 * Add the struct fields to the current scope, so that the select expresssion
						 * from the array select can be checked and finds the field its selecting from.
						 * The fields are added without checking for duplicates. This is not a big problem,
						 * since the same scope is instantly popped afterwards.
						 */
						this.scopes.peek().addDeclaration(struct.getFieldNumber(i), false);
					
					ArraySelect arr = (ArraySelect) selection;
					type = arr.check(this);
					
					this.scopes.pop();
					
					break;
				}
				else throw new CTX_EXC(selection.getSource(), selection.getClass().getName() + " cannot be a selector");
			}
			else throw new CTX_EXC(e.getSource(), "Cannot select from non struct, actual " + type.typeString());
			
		}
		
		e.setType(type.clone());
		return e.getType();
	}
	
	private TYPE findField(STRUCT struct, IDRef ref0) throws CTX_EXC {
		Declaration field = struct.getField(ref0.path);
		
		/* The ID the current selection targets */
		if (field != null) {
			/* Link manually, identifier is not part of current scope */
			ref0.origin = field;
			ref0.setType(ref0.origin.getType());
			
			/* Next type in chain */
			return ref0.getType();
		}
		else throw new CTX_EXC(ref0.getSource(), "The selected field " + ref0.path.build() + " in the structure " + struct.typeString() + " does not exist");
	}
	
	public TYPE checkWhileStatement(WhileStatement w) throws CTX_EXC {
		this.compoundStack.push(w);
		
		TYPE cond = w.condition.check(this);
		if (!(cond instanceof BOOL)) 
			throw new CTX_EXC(w.getSource(), "Condition is not boolean");
		
		this.scopes.push(new Scope(this.scopes.peek(), true));
		for (Statement s : w.body) {
			currentStatement = s;
			s.check(this);
		}
		this.scopes.pop();

		this.compoundStack.pop();
		return null;
	}
	
	public TYPE checkDoWhileStatement(DoWhileStatement w) throws CTX_EXC {
		this.compoundStack.push(w);
		
		TYPE cond = w.condition.check(this);
		if (!(cond instanceof BOOL)) 
			throw new CTX_EXC(w.getSource(), "Condition is not boolean");
		
		this.scopes.push(new Scope(this.scopes.peek(), true));
		for (Statement s : w.body) {
			currentStatement = s;
			s.check(this);
		}
		this.scopes.pop();

		this.compoundStack.pop();
		return null;
	}
	
	public TYPE checkForStatement(ForStatement f) throws CTX_EXC {
		this.compoundStack.push(f);
		
		this.scopes.push(new Scope(this.scopes.peek(), true));
		f.iterator.check(this);
		if (f.iterator.value == null) 
			throw new CTX_EXC(f.getSource(), "Iterator must have initial value");
		
		this.scopes.push(new Scope(this.scopes.peek(), true));
		
		TYPE cond = f.condition.check(this);
		if (!(cond instanceof BOOL)) 
			throw new CTX_EXC(f.getSource(), "Condition is not boolean");
		
		f.increment.check(this);
		
		for (Statement s : f.body) {
			currentStatement = s;
			s.check(this);
		}
		
		this.scopes.pop();
		this.scopes.pop();

		this.compoundStack.pop();
		return null;
	}
	
	public TYPE checkForEachStatement(ForEachStatement f) throws CTX_EXC {
		this.compoundStack.push(f);
		
		this.scopes.push(new Scope(this.scopes.peek(), true));
		
		TYPE itType = f.iterator.check(this);
		TYPE refType = f.shadowRef.check(this);
		
		f.counter.check(this);
		f.ref.check(this);
		
		if (refType instanceof POINTER) {
			POINTER p = (POINTER) refType;
			if (!p.targetType.isEqual(itType) && !p.targetType.getCoreType().isEqual(itType))
				throw new CTX_EXC(f.getSource(), "Pointer type does not match iterator type: " + p.targetType.typeString() + " vs " + itType.typeString());
			
			/* Construct expression to calculate address based on address of the shadowRef, counter and the size of the type */
			Expression sof = new SizeOfType(itType.clone(), f.shadowRef.getSource());
			Expression mul = new Mul(f.ref, sof, f.shadowRef.getSource());
			Expression add = new Add(f.shadowRef, mul, f.shadowRef.getSource());
			
			/* Set as new shadowRef, will be casted during code generation */
			f.shadowRef = new Deref(add, f.shadowRef.getSource());
			f.shadowRef.check(this);
		}
		else if (refType instanceof ARRAY) {
			ARRAY a = (ARRAY) refType;
			if (!a.elementType.isEqual(itType))
				throw new CTX_EXC(f.getSource(), "Array element type does not match iterator type: " + a.elementType.typeString() + " vs " + itType.typeString());
			
			/* Select first value from array */
			List<Expression> select = new ArrayList();
			select.add(f.ref);
			f.select = new ArraySelect(f.shadowRef, select, f.shadowRef.getSource());
			
			f.select.check(this);
		}
		else throw new CTX_EXC(f.getSource(), "Only available for pointers and arrays, actual " + refType.typeString());
		
		this.scopes.push(new Scope(this.scopes.peek(), true));
		
		for (Statement s : f.body) {
			currentStatement = s;
			s.check(this);
		}
		
		this.scopes.pop();
		this.scopes.pop();

		this.compoundStack.pop();
		return null;
	}
	
	public TYPE checkIfStatement(IfStatement i) throws CTX_EXC {
		/* Since else statement is directley checked, we need to set this explicitly here */
		this.currentStatement = i;
		
		if (i.condition != null) {
			TYPE cond = i.condition.check(this);
			if (!(cond instanceof BOOL)) 
				throw new CTX_EXC(i.getSource(), "Condition is not boolean");
		}
		else {
			if (i.elseStatement != null) 
				throw new CTX_EXC(i.getSource(), "If Statement can only have one else statement");
		}
		
		this.scopes.push(new Scope(this.scopes.peek()));
		
		for (Statement s : i.body) {
			currentStatement = s;
			s.check(this);
		}
		
		this.scopes.pop();
		
		if (i.elseStatement != null) 
			i.elseStatement.check(this);
		
		return null;
	}
	
	public TYPE checkDeclaration(Declaration d) throws CTX_EXC {
		
		if (!this.currentFunction.isEmpty()) 
			ProvisoUtil.mapNTo1(d.getType(), this.currentFunction.peek().provisosTypes);
		
		/* Set self as last, implicitly unused */
		d.last = d;
		
		if (d.value != null) {
			TYPE t = d.value.check(this);
			
			if (t instanceof FUNC) {
				FUNC d0 = (FUNC) d.getType();
				FUNC t0 = (FUNC) t;
				
				if (!t.isEqual(d.getType())) 
					throw d0.getInequality(t0, d.getSource());
				
				/* 
				 * If func head provided by declaration is anonymous, 
				 * replace with func head of value. May still be null.
				 */
				if (d0.funcHead == null) {
					d0.funcHead = t0.funcHead;
					d0.proviso = t0.proviso;
				}
				
				/* Wont be able to check, set to null */
				d.last = null;
			}
			
			if (!d.getType().isEqual(t) || d.getType().wordsize() != t.wordsize()) {
				if (t instanceof POINTER || d.getType() instanceof POINTER) 
					CompilerDriver.printProvisoTypes = true;
				
				if (this.checkPolymorphViolation(t, d.getType(), d.getSource())) 
					throw new CTX_EXC(d.getSource(), "Polymorphism only via pointers, actual " + t.typeString() + " vs " + d.getType().typeString());
				
				throw new CTX_EXC(d.getSource(), "Expression type does not match the declaration type: " + t.typeString() + " vs " + d.getType().typeString());
			}
		}
		
		/* When function type, can not only collide with over vars, but also function names */
		if (d.getType() instanceof FUNC) {
			for (Function f0 : this.functions) {
				if (f0.path.getLast().equals(d.path.getLast()) && f0.path.path.size() == 1) 
					throw new CTX_EXC(d.getSource(), "Predicate name shadows function name '" + d.path.build() + "'");
			}
		}
		
		Message m = scopes.peek().addDeclaration(d);
		if (m != null) this.messages.add(m);
		
		this.toLink.add(d);
		
		/* No need to set type here, is done while parsing */
		return d.getType();
	}
	
	public TYPE checkAssignment(Assignment a) throws CTX_EXC {
		TYPE targetType = a.lhsId.check(this);
		
		if (a.lhsId instanceof PointerLhsId) targetType = new POINTER(targetType);
		
		NamespacePath path = a.lhsId.getFieldName();
		
		Declaration dec = null;
		if (path != null) scopes.peek().getField(path, a.getSource());
		a.origin = dec;
		
		TYPE t = a.value.check(this);
		TYPE ctype = t;
		
		/* If target type is a pointer, only the core types have to match */
		if (!targetType.isEqual(t) || (targetType.wordsize() != t.wordsize() && a.lhsId instanceof SimpleLhsId)) {
			if (targetType instanceof POINTER || t instanceof POINTER) 
				CompilerDriver.printProvisoTypes = true;
			
			if (this.checkPolymorphViolation(t, targetType, a.getSource())) 
				throw new CTX_EXC(a.getSource(), "Variable type does not match expression type, polymorphism only via pointers, actual " + t.typeString() + " vs " + targetType.typeString());
			
			throw new CTX_EXC(a.getSource(), "Variable type does not match expression type: " + targetType.typeString() + " vs. " + t.typeString());
		}
		
		if (a.assignArith != ASSIGN_ARITH.NONE) {
			if (t.wordsize() > 1) 
				throw new CTX_EXC(a.getSource(), "Assign arith operation is only applicable for 1-Word types");
			
			if (a.assignArith == ASSIGN_ARITH.AND_ASSIGN || a.assignArith == ASSIGN_ARITH.ORR_ASSIGN || a.assignArith == ASSIGN_ARITH.BIT_XOR_ASSIGN) {
				if (!(ctype instanceof BOOL)) 
					throw new CTX_EXC(a.getSource(), "Expression type " + t.typeString() + " is not applicable for boolean assign operator");
			}
			else if (a.assignArith != ASSIGN_ARITH.NONE) {
				if (!(ctype instanceof INT)) 
					throw new CTX_EXC(a.getSource(), "Expression type " + t.typeString() + " is not applicable for assign operator");
			}
		}
		
		a.lhsId.expressionType = t;
		return null;
	}
	
	public TYPE checkBreak(BreakStatement b) throws CTX_EXC {
		if (this.compoundStack.isEmpty()) 
			throw new CTX_EXC(b.getSource(), "Can only break out of the scope of a loop");
		else b.superLoop = this.compoundStack.peek();
		return null;
	}
	
	public TYPE checkContinue(ContinueStatement c) throws CTX_EXC {
		if (this.compoundStack.isEmpty()) 
			throw new CTX_EXC(c.getSource(), "Can only continue in the scope of a loop");
		else c.superLoop = this.compoundStack.peek();
		return null;
	}
	
	public TYPE checkSwitchStatement(SwitchStatement s) throws CTX_EXC {
		if (!(s.condition instanceof IDRef)) 
			throw new CTX_EXC(s.condition.getSource(), "Switch Condition has to be variable reference");
		
		TYPE type = s.condition.check(this);
		if (!(type instanceof PRIMITIVE)) 
			throw new CTX_EXC(s.condition.getSource(), "Switch Condition type " + type.typeString() + " has to be a primitive type");
		
		if (s.defaultStatement == null) 
			throw new CTX_EXC(s.getSource(), "Missing default statement");
		
		for (CaseStatement c : s.cases) c.check(this);
		s.defaultStatement.check(this);
		return null;
	}
	
	public TYPE checkCaseStatement(CaseStatement c) throws CTX_EXC {
		TYPE type = c.condition.check(this);
		
		if (!type.isEqual(c.superStatement.condition.getType())) 
			throw new CTX_EXC(c.condition.getSource(), "Condition type " + type.typeString() + " does not switch condition type " + c.superStatement.condition.getType().typeString());
		
		this.scopes.push(new Scope(this.scopes.peek(), true));
		for (Statement s : c.body) {
			currentStatement = s;
			s.check(this);
		}
		this.scopes.pop();
		return null;
	}
	
	public TYPE checkDefaultStatement(DefaultStatement d) throws CTX_EXC {
		this.scopes.push(new Scope(this.scopes.peek(), true));
		for (Statement s : d.body) {
			currentStatement = s;
			s.check(this);
		}
		this.scopes.pop();
		return null;
	}
	
	public TYPE checkReturn(ReturnStatement r) throws CTX_EXC {
		if (r.value != null) {
			TYPE t = r.value.check(this);

			this.currentFunction.peek().hasReturn = true;
			
			/* There was a return statement with no return value previously */
			if (this.currentFunction.peek().noReturn != null) 
				throw new CTX_EXC(this.currentFunction.peek().noReturn.getSource(), "Return statement has no return value, expected " + this.currentFunction.peek().getReturnType().typeString());
			
			if (t.isEqual(this.currentFunction.peek().getReturnType())) 
				return t;
			else throw new CTX_EXC(r.getSource(), "Return type " + t.typeString() + " does not match function return type " + this.currentFunction.peek().getReturnType().typeString());
		}
		else {
			if (this.currentFunction.peek().hasReturn) 
				throw new CTX_EXC(r.getSource(), "Return statement has no return value, expected " + this.currentFunction.peek().getReturnType().typeString());
			else 
				this.currentFunction.peek().noReturn = r;
			
			if (!(currentFunction.peek().getReturnType() instanceof VOID)) 
				throw new CTX_EXC(r.getSource(), "Return type does not match function type, " + new VOID().typeString() + " vs " + currentFunction.peek().getReturnType().typeString());
			
			return new VOID();
		}
	}
	
	public TYPE checkTernary(Ternary t) throws CTX_EXC {
		TYPE type = t.condition.check(this);
		if (!(type instanceof BOOL)) 
			throw new CTX_EXC(t.condition.getSource(), "Ternary condition has to be of type BOOL, actual " + type.typeString());
		
		if (t.condition instanceof ArrayInit) 
			throw new CTX_EXC(t.condition.getSource(), "Structure Init can only be a sub expression of structure init");
		
		TYPE t0 = t.leftOperand.check(this);
		TYPE t1 = t.rightOperand.check(this);
		
		if (t.leftOperand instanceof ArrayInit) 
			throw new CTX_EXC(t.leftOperand.getSource(), "Structure Init can only be a sub expression of structure init");
		
		if (t.rightOperand instanceof ArrayInit) 
			throw new CTX_EXC(t.rightOperand.getSource(), "Structure Init can only be a sub expression of structure init");
		
		if (!t0.isEqual(t1)) 
			throw new CTX_EXC(t.condition.getSource(), "Both results of ternary operation have to be of the same type, " + t0.typeString() + " vs " + t1.typeString());
		
		t.setType(t0);
		return t.getType();
	}
	
	public TYPE checkBinaryExpression(BinaryExpression b) throws CTX_EXC {
		TYPE left = b.getLeft().check(this);
		TYPE right = b.getRight().check(this);
		
		if (left instanceof NULL) 
			throw new CTX_EXC(b.left.getSource(), "Cannot perform arithmetic on null");
		
		if (right instanceof NULL) 
			throw new CTX_EXC(b.right.getSource(), "Cannot perform arithmetic on null");
		
		if (b.left instanceof ArrayInit) 
			throw new CTX_EXC(b.left.getSource(), "Structure Init can only be a sub expression of structure init");
		
		if (b.right instanceof ArrayInit) 
			throw new CTX_EXC(b.right.getSource(), "Structure Init can only be a sub expression of structure init");
		
		if (left.wordsize() > 1) 
			throw new CTX_EXC(b.left.getSource(), "Can only apply to primitive or pointer, actual " + left.typeString());
		
		if (right.wordsize() > 1) {
			throw new CTX_EXC(b.left.getSource(), "Can only apply to primitive or pointer, actual " + right.typeString());
		}
		
		if (left instanceof POINTER) {
			if (!(right.getCoreType() instanceof INT)) 
				throw new CTX_EXC(b.getSource(), "Pointer arithmetic is only supported for " + new INT().typeString() + ", actual " + right.typeString());
			
			b.setType(left);
		}
		else if (right instanceof POINTER) {
			if (!(left.getCoreType() instanceof INT)) 
				throw new CTX_EXC(b.getSource(), "Pointer arithmetic is only supported for " + new INT().typeString() + ", actual " + left.typeString());
			
			b.setType(left);
		}
		else if (left.isEqual(right)) 
			b.setType(left);
		else throw new CTX_EXC(b.getSource(), "Operand types do not match: " + left.typeString() + " vs. " + right.typeString());
	
		return b.getType();
	}
	
	/**
	 * Overrides binary expression.<br>
	 * Checks for:<br>
	 * - Both operand types have to be of type BOOL<br>
	 */
	public TYPE checkBoolBinaryExpression(BoolBinaryExpression b) throws CTX_EXC {
		TYPE left = b.getLeft().check(this);
		TYPE right = b.getRight().check(this);
		
		if (!(left instanceof BOOL)) 
			throw new CTX_EXC(b.left.getSource(), "Expected " + new BOOL().typeString() + ", actual " + left.typeString());
		
		if (!(right instanceof BOOL)) 
			throw new CTX_EXC(b.right.getSource(), "Expected " + new BOOL().typeString() + ", actual " + right.typeString());
		
		b.setType(left);
		return b.getType();
	}
	
	/**
	 * Overrides unary expression.<br>
	 * Checks for:<br>
	 * - Operand type has to be of type BOOL<br>
	 */
	public TYPE checkBoolUnaryExpression(BoolUnaryExpression b) throws CTX_EXC {
		TYPE t = b.getOperand().check(this);
		
		if (!(t instanceof BOOL)) 
			throw new CTX_EXC(b.getOperand().getSource(), "Expected bool, actual " + t.typeString());
		
		b.setType(t);
		return b.getType();
	}
	
	public TYPE checkUnaryExpression(UnaryExpression u) throws CTX_EXC {
		TYPE op = u.getOperand().check(this);
		
		if (op instanceof NULL) 
			throw new CTX_EXC(u.getOperand().getSource(), "Cannot perform arithmetic on null");
		
		if (u.getOperand() instanceof ArrayInit) 
			throw new CTX_EXC(u.getOperand().getSource(), "Structure Init can only be a sub expression of structure init");
		
		if (u instanceof BitNot && op instanceof PRIMITIVE) {
			u.setType(op);
			return u.getType();
		}
		else if (u instanceof UnaryMinus && op instanceof PRIMITIVE) {
			u.setType(op);
			return u.getType();
		}
		else throw new CTX_EXC(u.getSource(), "Unknown Expression: " + u.getClass().getName());
	}
	
	public TYPE checkCompare(Compare c) throws CTX_EXC {
		TYPE left = c.getLeft().check(this);
		TYPE right = c.getRight().check(this);
		
		if (c.left instanceof ArrayInit) 
			throw new CTX_EXC(c.left.getSource(), "Structure Init can only be a sub expression of structure init");
		
		if (c.right instanceof ArrayInit) 
			throw new CTX_EXC(c.right.getSource(), "Structure Init can only be a sub expression of structure init");
		
		if (left.isEqual(right)) {
			c.setType(new BOOL());
			return c.getType();
		}
		else throw new CTX_EXC(c.getSource(), "Operand types do not match: " + left.typeString() + " vs. " + right.typeString());
	}
	
	public Function findFunction(NamespacePath path, Source source, boolean isPredicate) throws CTX_EXC {
		Function f = null;
		for (Function f0 : this.functions) {
			if (f0.path.build().equals(path.build())) {
				f = f0;
				break;
			}
		}
		
		if (f != null) return f;
		else if (path.path.size() == 1) {
			List<Function> funcs = new ArrayList();
			
			for (Function f0 : this.functions) {
				if (f0.path.getLast().equals(path.getLast())) funcs.add(f0);
			}
			
			for (Declaration d : this.currentFunction.peek().parameters) {
				if (d.getType() instanceof FUNC) {
					FUNC f0 = (FUNC) d.getType();
					
					if (f0.funcHead != null) f0.funcHead.lambdaDeclaration = d;
					
					if (d.path.getLast().equals(path.getLast())) 
						funcs.add(f0.funcHead);
				}
			}
			
			/* Return if there is only one result */
			if (funcs.isEmpty()) return null;
			else if (funcs.size() == 1) return funcs.get(0);
			/* Multiple results, cannot determine correct one, return null */
			else {
				String s = "";
				for (Function f0 : funcs) s += f0.path.build() + ", ";
				s = s.substring(0, s.length() - 2);
				throw new CTX_EXC(source, "Multiple matches for " + ((isPredicate)? "predicate" : "function") + " '" + path.build() + "': " + s + ". Ensure namespace path is explicit and correct");
			}
		}
		else throw new CTX_EXC(source, "Unknown " + ((isPredicate)? "predicate" : "function") + " '" + path.build() + "'");
	}
	
	public boolean signalStackContains(TYPE newSignal) {
		for (TYPE t : this.signalStack.peek()) 
			if (t.isEqual(newSignal)) return true;
		return false;
	}
	
	public Function linkFunction(NamespacePath path, SyntaxElement i, Source source) throws CTX_EXC {
		List<TYPE> proviso = null;
		
		if (i instanceof InlineCall) {
			InlineCall i0 = (InlineCall) i;
			proviso = i0.proviso;
		}
		else {
			FunctionCall i0 = (FunctionCall) i;
			proviso = i0.proviso;
		}
		
		/* Find the called function */
		Function f = this.findFunction(path, source, false);
		
		Declaration anonTarget = null;
		
		/* Function not found, may be a lambda call */
		if (f == null) {
			anonTarget = this.scopes.peek().getFieldNull(path, source);
			
			if (anonTarget != null && anonTarget.getType() instanceof FUNC) {
				FUNC f0 = (FUNC) anonTarget.getType();
				
				if (proviso.size() != 0) 
					throw new CTX_EXC(source, "Proviso for inline call are provided by predicate '" + anonTarget.path.build() + "', cannot provide proviso at this location");
				
				/* Proviso types provided through lambda */
				proviso = f0.proviso;
				
				f = f0.funcHead;
				
				if (f == null) {
					if (!CompilerDriver.disableWarnings) 
						this.messages.add(new Message("Unsafe operation, predicate '" + path.build() + "' is anonymous, " + source.getSourceMarker(), Message.Type.WARN, true));
				}
			}
		}
		
		/* Neither regular function or predicate was found, undefined */
		if (f == null && anonTarget == null) 
			throw new CTX_EXC(source, "Undefined function or predicate '" + path.build() + "'");
		
		if (i instanceof InlineCall) {
			InlineCall i0 = (InlineCall) i;
			i0.anonTarget = anonTarget;
			i0.proviso = proviso;
		}
		else {
			FunctionCall i0 = (FunctionCall) i;
			i0.anonTarget = anonTarget;
			i0.proviso = proviso;
		}
		
		return f;
	}
	
	public TYPE checkInlineCall(InlineCall i) throws CTX_EXC {
		/* Find the called function */
		Function f = this.linkFunction(i.path, i, i.getSource());
		
		i.calledFunction = f;
		
		if (!this.exceptionEscapeStack.isEmpty()) i.watchpoint = this.exceptionEscapeStack.peek();
		
		if (f != null) {
			/* Inline calls made during global setup may not signal, since exception cannot be watched */
			if (f.signals && this.scopes.size() == 1) 
				throw new CTX_EXC(i.getSource(), "Calls made during initial setup may not signal, but '" + f.path.build() + "' does");
			
			checkModifier(f.modifier, f.path, i.getSource());
			
			/* Add signaled types */
			if (f.signals) {
				for (TYPE s : f.signalsTypes) 
					if (!this.signalStackContains(s)) 
						this.signalStack.peek().add(s);
			}
			
			if (!f.provisosTypes.isEmpty()) {
				if (f.containsMapping(i.proviso)) {
					/* Mapping already exists, just return return type of this specific mapping */
					f.setContext(i.proviso);
					i.setType(f.getMappingReturnType(i.proviso));
				}
				else {
					/* Create a new context, check function for this specific context */
					f.setContext(i.proviso);
					
					this.scopes.push(new Scope(this.scopes.get(0)));
					f.check(this);
					this.scopes.pop();
					i.setType(f.getMappingReturnType(i.proviso));
				}
			}
			else 
				/* 
				 * Add default proviso mapping, so mapping is present,
				 * function was called and will be compiled.
				 */
				f.addProvisoMapping(f.getReturnType(), new ArrayList());
			
			if (i.parameters.size() != f.parameters.size()) 
				throw new CTX_EXC(i.getSource(), "Missmatching argument number in inline call: Expected " + f.parameters.size() + " but got " + i.parameters.size());
			
			for (int a = 0; a < f.parameters.size(); a++) {
				TYPE paramType = i.parameters.get(a).check(this);
				
				TYPE functionParamType = f.parameters.get(a).getType();
				
				if (!paramType.isEqual(functionParamType)) {
					if (paramType instanceof POINTER || functionParamType instanceof POINTER) 
						CompilerDriver.printProvisoTypes = true;
					
					if (this.checkPolymorphViolation(paramType, functionParamType, i.getSource()))
						throw new CTX_EXC(i.parameters.get(a).getSource(), "Argument (" + (a + 1) + ") does not match parameter, polymorphism only via pointers, actual " + paramType.typeString() + " vs " + functionParamType.typeString());
					
					throw new CTX_EXC(i.parameters.get(a).getSource(), "Argument (" + (a + 1) + ") does not match parameter: " + paramType.typeString() + " vs " + functionParamType.typeString());
				}
			}
			
			if (f.provisosTypes.isEmpty() || !f.containsMapping(i.proviso)) 
				i.setType(f.getReturnType().clone());
			
			if (i.getType() instanceof VOID && !f.hasReturn) 
				throw new CTX_EXC(i.getSource(), "Expected return value from inline call");
		}
		else {
			/* Set void as return type */
			i.setType(new VOID());
			
			for (int a = 0; a < i.parameters.size(); a++) 
				i.parameters.get(a).check(this);
		}
		
		return i.getType();
	}
	
	public void checkModifier(MODIFIER mod, NamespacePath path, Source source) throws CTX_EXC {
		String currentPath = (this.currentFunction.isEmpty())? "" : this.currentFunction.peek().path.buildPathOnly();
		
		if (mod == MODIFIER.SHARED) return;
		else if (mod == MODIFIER.RESTRICTED) {
			if (!currentPath.startsWith(path.buildPathOnly())) {
				if (CompilerDriver.disableModifiers) {
					if (!CompilerDriver.disableWarnings) 
						this.messages.add(new Message("Modifier violation: " + path.build() + " from " + this.currentFunction.peek().path.build() + " at " + source.getSourceMarker(), Message.Type.WARN, true));
				}
				else throw new CTX_EXC(source, "Modifier violation: " + path.build() + " from " + this.currentFunction.peek().path.build());
			}
		}
		else if (mod == MODIFIER.EXCLUSIVE) {
			if (!currentPath.equals(path.buildPathOnly())) {
				if (CompilerDriver.disableModifiers) {
					if (!CompilerDriver.disableWarnings) 
						this.messages.add(new Message("Modifier violation: " + path.build() + " from " + this.currentFunction.peek().path.build() + " at " + source.getSourceMarker(), Message.Type.WARN, true));
				}
				else throw new CTX_EXC(source, "Modifier violation: " + path.build() + " from " + this.currentFunction.peek().path.build());
			}
		}
	}	
	
	public TYPE checkFunctionCall(FunctionCall i) throws CTX_EXC {
		Function f = this.linkFunction(i.path, i, i.getSource());
		
		i.calledFunction = f;
		i.watchpoint = this.exceptionEscapeStack.peek();
		
		if (f != null) {
			checkModifier(f.modifier, f.path, i.getSource());
			
			/* Add signaled types */
			if (f.signals) {
				for (TYPE s : f.signalsTypes) {
					if (!this.signalStackContains(s)) 
						this.signalStack.peek().add(s);
				}
			}
			
			if (!f.provisosTypes.isEmpty()) {
				if (!f.containsMapping(i.proviso)) {
					/* Create new scope that points to the global scope */
					f.setContext(i.proviso);
					this.scopes.push(new Scope(this.scopes.get(0)));
					
					f.check(this);
					this.scopes.pop();
				}
				else f.setContext(i.proviso);
			}
			else 
				/* 
				 * Add default proviso mapping, so mapping is present,
				 * function was called and will be compiled.
				 */
				f.addProvisoMapping(f.getReturnType(), new ArrayList());
			
			if (i.parameters.size() != f.parameters.size()) 
				throw new CTX_EXC(i.getSource(), "Missmatching argument number in function call: Expected " + f.parameters.size() + " but got " + i.parameters.size());
			
			for (int a = 0; a < f.parameters.size(); a++) {
				TYPE paramType = i.parameters.get(a).check(this);
				
				if (!paramType.isEqual(f.parameters.get(a).getType())) {
					if (paramType instanceof POINTER || f.parameters.get(a).getType() instanceof POINTER) 
						CompilerDriver.printProvisoTypes = true;
					
					if (this.checkPolymorphViolation(paramType, f.parameters.get(a).getType(), i.getSource()))
						throw new CTX_EXC(i.parameters.get(a).getSource(), "Argument (" + (a + 1) + ") does not match parameter, polymorphism only via pointers, actual " + paramType.typeString() + " vs " + f.parameters.get(a).getType().typeString());
					
					throw new CTX_EXC(i.parameters.get(a).getSource(), "Argument (" + (a + 1) + ") does not match parameter type: " + paramType.typeString() + " vs " + f.parameters.get(a).getType().typeString());
				}
			}
		}
		else {
			for (int a = 0; a < i.parameters.size(); a++) {
				if (i.parameters.get(a) instanceof ArrayInit) 
					throw new CTX_EXC(i.getSource(), "Structure Init can only be a sub expression of structure init");
				
				i.parameters.get(a).check(this);
			}
		}
		
		return new VOID();
	}
	
	/**
	 * Checks for:<br>
	 * - Sets the origin of the reference<br>
	 * - Sets the type of the reference
	 */
	public TYPE checkIDRef(IDRef i) throws CTX_EXC {
		/* Search for the declaration in the scopes */
		Declaration d = this.scopes.peek().getField(i.path, i.getSource());
		
		if (d != null) {
			/* Link origin */
			i.origin = d;
			
			/* Apply type */
			i.setType(d.getType());
			
			/* Check for modifier restrictions */
			this.checkModifier(i.origin.modifier, i.origin.path, i.getSource());

			boolean contains = false;
			for (int a = this.scopes.size() - 1; a >= 0; a--) {
				if (this.scopes.get(a).isLoopedScope) break;
				
				if (this.scopes.get(a).declarations.containsKey(i.path.build())) {
					contains = true;
					break;
				}
			}
			
			if (contains && !(i.origin.getType() instanceof FUNC)) {
				i.origin.last = currentStatement;
			}
			else i.origin.last = null;
			
			return i.getType();
		}
		else throw new CTX_EXC(i.getSource(), "Unknown variable: " + i.path.build());
	}
	
	public TYPE checkFunctionRef(FunctionRef r) throws CTX_EXC {
		
		/* If not already linked, find referenced function */
		Function lambda = (r.origin != null)? r.origin : this.findFunction(r.path, r.getSource(), true);
		if (lambda == null) 
			throw new CTX_EXC(r.getSource(), "Unknown predicate: " + r.path.build());
		
		/* Provided number of provisos does not match number of provisos of lambda */
		if (lambda.provisosTypes.size() != r.proviso.size()) 
			throw new CTX_EXC(r.getSource(), "Missmatching number of provided provisos for predicate, expected " + lambda.provisosTypes.size() + ", got " + r.proviso.size());
		
		/* A lambda cannot signal exceptions, since it may become anonymous */
		if (lambda.signals) 
			throw new CTX_EXC(r.getSource(), "Predicates may not signal exceptions");
		
		/* Set context and add mapping */
		if (!this.currentFunction.isEmpty()) 
			ProvisoUtil.mapNToNMaybe(r.proviso, this.currentFunction.peek().provisosTypes);
		
		lambda.setContext(r.proviso);
		lambda.addProvisoMapping(lambda.getReturnType(), r.proviso);
		
		/* Make sure function is check, may only be called through anonymous predicate */
		this.scopes.push(new Scope(this.scopes.get(0)));
		lambda.check(this);
		this.scopes.pop();
		
		r.origin = lambda;
		
		/* Set flag that this function was targeted as a lambda */
		lambda.isLambdaTarget = true;
		
		r.setType(new FUNC(lambda, r.proviso));
		return r.getType();
	}
	
	public TYPE checkArrayInit(ArrayInit init) throws CTX_EXC {
		/* Array must at least contain one element */
		if (init.elements.isEmpty()) 
			throw new CTX_EXC(init.getSource(), "Structure init must have at least one element");
		
		TYPE type0 = init.elements.get(0).check(this);
		
		int dontCareSize = 0;
		
		if (init.elements.size() > 1 || init.dontCareTypes) {
			for (int i = 0; i < init.elements.size(); i++) {
				TYPE typeX = init.elements.get(i).check(this);
				
				if (init.dontCareTypes) 
					dontCareSize += typeX.wordsize();
				else {
					if (!typeX.isEqual(type0)) 
						throw new CTX_EXC(init.getSource(), "Structure init elements have to have same type: " + type0.typeString() + " vs " + typeX.typeString());
				}
			}
		}
		
		init.setType(new ARRAY((init.dontCareTypes)? new VOID() : type0, (init.dontCareTypes)? dontCareSize : init.elements.size()));
		return init.getType();
	}
	
	public TYPE checkInstanceofExpression(InstanceofExpression iof) throws CTX_EXC {
		iof.expression.check(this);
		
		if (CompilerDriver.disableStructSIDHeaders) 
			throw new CTX_EXC(iof.getSource(), "SID headers are disabled, instanceof is not available");
		
		if (!(iof.instanceType instanceof STRUCT)) 
			throw new CTX_EXC(iof.getSource(), "Expected struct type, got " + iof.instanceType.typeString());
		
		iof.setType(new BOOL());
		return iof.getType();
	}
	
	public TYPE checkSizeOfType(SizeOfType sot) throws CTX_EXC {
		if (!this.currentFunction.isEmpty()) 
			ProvisoUtil.mapNTo1(sot.sizeType, this.currentFunction.peek().provisosTypes);
		
		sot.setType(new INT());
		return sot.getType();
	}
	
	public TYPE checkSizeOfExpression(SizeOfExpression soe) throws CTX_EXC {
		soe.sizeType = soe.expression.check(this);
		soe.setType(new INT());
		return soe.getType();
	}
	
	public TYPE checkAddressOf(AddressOf aof) throws CTX_EXC {
		TYPE t = aof.expression.check(this);
		
		if (!(aof.expression instanceof IDRef || aof.expression instanceof ArraySelect || aof.expression instanceof StructSelect)) 
			throw new CTX_EXC(aof.getSource(), "Can only get address of variable reference or array select");
		
		aof.setType(new POINTER(t.getCoreType()));
		
		return aof.getType();
	}
	
	/**
	 * Checks for:<br>
	 * - Operand type is pointer.y
	 */
	public TYPE checkDeref(Deref deref) throws CTX_EXC {
		TYPE t = deref.expression.check(this);
		
		/* Dereference pointer or primitive type */
		if (t instanceof PRIMITIVE) 
			/* Set to core type */
			deref.setType(t.getCoreType());
		else if (t instanceof POINTER) {
			POINTER p = (POINTER) t;
			deref.setType(p.targetType);
		}
		else throw new CTX_EXC(deref.expression.getSource(), "Cannot dereference type " + t.typeString());
		
		/* Dereferencing a primitive can be a valid statement, but it can be unsafe. A pointer would be safer. */
		if (t instanceof PRIMITIVE) {
			if (!CompilerDriver.disableWarnings) 
				this.messages.add(new Message("Operand is not a pointer, may cause unexpected behaviour, " + deref.getSource().getSourceMarker(), Message.Type.WARN, true));
		}
		
		return deref.getType();
	}
	
	public TYPE checkTypeCast(TypeCast tc) throws CTX_EXC {
		TYPE t = tc.expression.check(this);
		
		if (tc.expression instanceof InlineCall) {
			InlineCall ic = (InlineCall) tc.expression;
			/* Anonymous inline call */
			if (ic.calledFunction == null) {
				ic.setType(tc.castType);
				t = tc.castType;
				
				if (!CompilerDriver.disableWarnings) 
					messages.add(new Message("Using implicit anonymous type " + tc.castType.typeString() + ", " + tc.getSource().getSourceMarker(), Message.Type.WARN, true));
			}
		}
		
		/* Allow only casting to equal word sizes or from or to void types */
		if ((t != null && t.wordsize() != tc.castType.wordsize()) && !(tc.castType.getCoreType() instanceof VOID || t instanceof VOID)) 
			throw new CTX_EXC(tc.getSource(), "Cannot cast " + t.typeString() + " to " + tc.castType.typeString());
		
		tc.setType(tc.castType);
		return tc.castType;
	}
	
	public TYPE checkIDRefWriteback(IDRefWriteback i) throws CTX_EXC {
		if (i.getShadowRef() instanceof IDRef) {
			IDRef ref = (IDRef) i.getShadowRef();
			i.idRef = ref;
			
			TYPE t = ref.check(this);
			
			if (!(t instanceof PRIMITIVE)) throw new CTX_EXC(i.idRef.getSource(), "Can only be applied to primitive types");
			
			i.setType(t);
		}
		else throw new CTX_EXC(i.getSource(), "Can only apply to id reference");
		
		return i.getType();
	}
	
	public TYPE checkStructSelectWriteback(StructSelectWriteback i) throws CTX_EXC {
		if (i.getShadowSelect() instanceof StructSelect) {
			StructSelect ref = (StructSelect) i.getShadowSelect();
			i.select = ref;
			
			TYPE t = ref.check(this);
			
			if (!(t instanceof PRIMITIVE)) throw new CTX_EXC(i.select.getSource(), "Can only be applied to primitive types");
			
			i.setType(t);
		}
		else throw new CTX_EXC(i.getSource(), "Can only apply to id reference");
		
		return i.getType();
	}
	
	public TYPE checkAssignWriteback(AssignWriteback i) throws CTX_EXC {
		if (i.reference instanceof IDRefWriteback || i.reference instanceof StructSelectWriteback) i.reference.check(this);
		else throw new CTX_EXC(i.getSource(), "Can only apply to id reference");
		return null;
	}
	
	/**
	 * Checks for:<br>
	 * - At least one selection has to be made<br>
	 * - Checks that the shadow ref is an IDRef<br>
	 * - Checks that the types of the selection are of type int<br>
	 * - Amount of selections does not exceed structure dimension<br>
	 */
	public TYPE checkArraySelect(ArraySelect select) throws CTX_EXC {
		if (select.selection.isEmpty()) 
			throw new CTX_EXC(select.getSource(), "Element select must have at least one element (how did we even get here?)");
		
		if (select.getShadowRef() instanceof IDRef) {
			IDRef ref = (IDRef) select.getShadowRef();
			select.idRef = ref;
			
			TYPE type0 = ref.check(this);
			
			/* If pointer, unwrap it */
			TYPE chain = (type0 instanceof POINTER)? ((POINTER) type0).targetType : type0;
			
			/* Check selection chain */
			for (int i = 0; i < select.selection.size(); i++) {
				TYPE stype = select.selection.get(i).check(this);
				if (!(stype instanceof INT)) 
					throw new CTX_EXC(select.selection.get(i).getSource(), "Selection has to be of type " + new INT().typeString() + ", actual " + stype.typeString());
				else {
					if (!(chain instanceof ARRAY)) 
						throw new CTX_EXC(select.selection.get(i).getSource(), "Cannot select from type " + type0.typeString());
					else {
						ARRAY arr = (ARRAY) chain;
						
						if (select.selection.get(i) instanceof Atom) {
							Atom a = (Atom) select.selection.get(i);
							int value = (int) a.getType().getValue();
							if (value < 0 || value >= arr.getLength()) 
								throw new CTX_EXC(select.selection.get(i).getSource(), "Array out of bounds: " + value + ", type: " + chain.typeString());
						}
						
						chain = ((ARRAY) chain).elementType;
					}
				}
			}
			
			if (type0 instanceof POINTER) chain = new POINTER(chain);
			
			select.setType(chain);
			return select.getType();
		}
		else throw new CTX_EXC(select.getShadowRef().getSource(), "Can only select from variable reference");
	}
	
	public TYPE checkAtom(Atom a) throws CTX_EXC {
		return a.getType();
	}
	
	public TYPE checkRegisterAtom(RegisterAtom a) throws CTX_EXC {
		String reg = a.spelling.toLowerCase();
		
		REG reg0 = RegOp.convertStringToReg(reg);
		
		if (reg0 == null) 
			throw new CTX_EXC(a.getSource(), "Unknown register: " + reg);
		else a.reg = reg0;
		
		return a.getType();
	}
	
	public TYPE checkDirectASMStatement(DirectASMStatement d) throws CTX_EXC {
		for (Pair<Expression, REG> p : d.dataIn) {
			TYPE t = p.first.check(this);
			
			if (t.wordsize() > 1) 
				throw new CTX_EXC(p.first.getSource(), "All data typs of direct asm must be 1 data word large, got " + t.typeString());
		}
		
		for (Pair<Expression, REG> p : d.dataOut) {
			TYPE t = p.first.check(this);
			
			if (!(p.first instanceof IDRef)) 
				throw new CTX_EXC(p.first.getSource(), "Expected IDRef, got " + p.first.getClass().getName());
			
			if (t.wordsize() > 1) 
				throw new CTX_EXC(p.first.getSource(), "All data typs of direct asm must be 1 data word large, got " + t.typeString());
		}
		
		if (d.dataOut.isEmpty()) {
			if (!CompilerDriver.disableWarnings) 
				messages.add(new Message("Direct ASM Operation has no explicit outputs, " + d.getSource().getSourceMarker(), Message.Type.WARN, true));
		}
		
		return new VOID();
	}
	
	public boolean checkPolymorphViolation(TYPE child, TYPE target, Source source) throws CTX_EXC {
		if (!(target instanceof STRUCT)) return false;
		if (child.getCoreType() instanceof STRUCT) {
			if (((STRUCT) child.getCoreType()).isPolymorphTo(target) && !((STRUCT) child).getTypedef().equals(((STRUCT) target).getTypedef())) {
				return true;
			}
		}
		return false;
	}
	
}
