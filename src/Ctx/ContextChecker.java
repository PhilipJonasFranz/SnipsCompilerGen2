package Ctx;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.Stack;
import java.util.stream.Collectors;

import Ctx.Util.CheckUtil.Callee;
import Ctx.Util.ProvisoUtil;
import Ctx.Util.CtxCallUtil;
import Exc.CTEX_EXC;
import Exc.SNIPS_EXC;
import Imm.ASM.Util.Operands.RegOp;
import Imm.ASM.Util.Operands.RegOp.REG;
import Imm.AST.Function;
import Imm.AST.Program;
import Imm.AST.SyntaxElement;
import Imm.AST.Expression.AddressOf;
import Imm.AST.Expression.ArrayInit;
import Imm.AST.Expression.ArraySelect;
import Imm.AST.Expression.Atom;
import Imm.AST.Expression.Deref;
import Imm.AST.Expression.Expression;
import Imm.AST.Expression.FunctionRef;
import Imm.AST.Expression.IDOfExpression;
import Imm.AST.Expression.IDRef;
import Imm.AST.Expression.IDRefWriteback;
import Imm.AST.Expression.InlineCall;
import Imm.AST.Expression.InlineFunction;
import Imm.AST.Expression.NFoldExpression;
import Imm.AST.Expression.RegisterAtom;
import Imm.AST.Expression.SizeOfExpression;
import Imm.AST.Expression.SizeOfType;
import Imm.AST.Expression.StructSelect;
import Imm.AST.Expression.StructSelectWriteback;
import Imm.AST.Expression.StructureInit;
import Imm.AST.Expression.TempAtom;
import Imm.AST.Expression.TypeCast;
import Imm.AST.Expression.UnaryExpression;
import Imm.AST.Expression.Arith.Add;
import Imm.AST.Expression.Arith.BitNot;
import Imm.AST.Expression.Arith.Mul;
import Imm.AST.Expression.Arith.UnaryMinus;
import Imm.AST.Expression.Boolean.BoolNFoldExpression;
import Imm.AST.Expression.Boolean.BoolUnaryExpression;
import Imm.AST.Expression.Boolean.Compare;
import Imm.AST.Expression.Boolean.Ternary;
import Imm.AST.Lhs.ArraySelectLhsId;
import Imm.AST.Lhs.LhsId;
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
import Imm.AST.Statement.SwitchStatement;
import Imm.AST.Statement.TryStatement;
import Imm.AST.Statement.WatchStatement;
import Imm.AST.Statement.WhileStatement;
import Imm.AST.Typedef.InterfaceTypedef;
import Imm.AST.Typedef.StructTypedef;
import Imm.AsN.AsNNode.MODIFIER;
import Imm.TYPE.AUTO;
import Imm.TYPE.PROVISO;
import Imm.TYPE.TYPE;
import Imm.TYPE.COMPOSIT.ARRAY;
import Imm.TYPE.COMPOSIT.INTERFACE;
import Imm.TYPE.COMPOSIT.POINTER;
import Imm.TYPE.COMPOSIT.STRUCT;
import Imm.TYPE.PRIMITIVES.BOOL;
import Imm.TYPE.PRIMITIVES.FUNC;
import Imm.TYPE.PRIMITIVES.INT;
import Imm.TYPE.PRIMITIVES.PRIMITIVE;
import Imm.TYPE.PRIMITIVES.VOID;
import Res.Const;
import Snips.CompilerDriver;
import Util.ASTDirective;
import Util.ASTDirective.DIRECTIVE;
import Util.NamespacePath;
import Util.Pair;
import Util.Source;
import Util.Logging.LogPoint;
import Util.Logging.Message;
import Util.Logging.ProgressMessage;

/**
 * When applied to a given AST, the Context-Checker
 * sets references, fields, and enforces grammar rules
 * and type safety. If a rule is violated, a CTEX_EXC
 * is generated and thrown. The exception will contain
 * a message with a description of the rule that was violated,
 * and at which location in the code. Also, a trace dump
 * will be generated, which contains locations throughout
 * the path in the AST where the exception was generated.
 */
public class ContextChecker {

			/* ---< FIELDS >--- */
	/**
	 * Set to the current progress message instance for 
	 * the context checking process.
	 */
	public static ProgressMessage progress;
	
	/**
	 * The Root AST Syntax Element.
	 */
	protected Program AST;
	
	/**
	 * Contains all functions in the AST, as well as
	 * struct nested functions. During the context checking
	 * process, this list is filled. This way the total 
	 * dependency order of the functions can be ensured.
	 */
	protected List<Function> functions = new ArrayList();
	
	/**
	 * This stack is filled with functions when a program
	 * is checked recursiveley. A new function is pushed on
	 * the stack either if a new function is checked or, within
	 * this function, a function is called. In this case, this
	 * function is pushed. Since then this function is checked,
	 * the cycle may continue.
	 */
	protected Stack<Function> currentFunction = new Stack();
	
	/**
	 * This list contains all struct nested functions. The list is used,
	 * when a function is called, to determine wether the function is a nested
	 * function or not, and thus can be accessed or not.
	 */
	protected List<Function> nestedFunctions = new ArrayList();
	
	/**
	 * This stack is filled with compound statements when
	 * a program structure is recursiveley checked. The
	 * stack is used to determine the next highest super-loop
	 * when checking break and continue statements.
	 */
	protected Stack<CompoundStatement> compoundStack = new Stack();
	
	/**
	 * This stack is filled with scopes when a program is
	 * recursiveley checked. Each scope will contain program
	 * variables that are defined in the current scope. The
	 * stack is used to localize the highest scoped variable
	 * at any point, to link origins and to check for duplicate
	 * variable names.
	 */
	protected Stack<Scope> scopes = new Stack();
	
	/**
	 * This stack is filled with exception type lists when
	 * a program is recursiveley checked. Each list corresponds
	 * to an exception scope. This is either a function scope
	 * or a scope opened by a try statement. The stack is used
	 * to determine where which exception types can be signaled
	 * and to ensure the correct 'signals' annotations are present.
	 * This is done by adding non-watched exceptions in a higher scope
	 * to the next scope until the base scope is reached. Each
	 * of the remaining exception types needs to be signaled in
	 * the function header.
	 */
	protected Stack<List<TYPE>> signalStack = new Stack();
	
	/**
	 * This stack is filled with syntax elements when a program
	 * is recursiveley checked. Each syntax element is either a
	 * function or a try-statement. The stack is used to determine
	 * the highest scoped watchpoint. When checking inline or function
	 * calls or a signal statement, the watchpoint is set to the 
	 * highest scoped watchpoint.
	 */
	protected Stack<SyntaxElement> watchpointStack = new Stack();
	
	/**
	 * This statement is always set to the statement that is 
	 * currently being checked. When checking IDRefs, the last
	 * statement may be determined, so that during ASM casting,
	 * at one point where the IDRef is not being used anymore, 
	 * the register can be freed.
	 */
	protected Statement currentStatement = null;
	
	/**
	 * Contains a collection of all declarations in the AST.
	 */
	protected List<Declaration> declarations = new ArrayList();
	
	/**
	 * All generated (WARN) messages. These messages will
	 * be flushed after the context checking process is over.
	 */
	protected List<Message> messages = new ArrayList();
	
	/**
	 * Contains all registered struct typedefs. When a struct typedef
	 * is checked, it will be added once all checks are finished.
	 */
	protected List<StructTypedef> structTypedefs = new ArrayList();
	
	/**
	 * During struct-typedef checking, this field will be set to a
	 * struct typedef that is the signature of a header file. When
	 * the implementation is merged with the header typedef, the function
	 * bodies are transferred. To ensure that declaration-references
	 * are correctly set to the new function head, we need to check
	 * the result of the merge again.
	 */
	protected StructTypedef reCheckTypedef = null;
	 
	/**
	 * Contains the current trace. Everytime a SyntaxElement
	 * is checked, the syntax element pushes itself
	 * on the stack. When the check method returns, the
	 * element is popped of the stack. When a CTEX_EXC is thrown,
	 * this stack is used to create the check-trace.
	 */
	public Stack<SyntaxElement> stackTrace = new Stack();
	
	
			/* ---< CONSTRUCTORS >--- */
	public ContextChecker(SyntaxElement AST, ProgressMessage progress) {
		this.AST = (Program) AST;
		ContextChecker.progress = progress;
	}
	
	
			/* ---< AST Check MethodS >--- */
	public TYPE check() throws CTEX_EXC {
		/* Set reference to own trace */
		CompilerDriver.stackTrace = this.stackTrace;
		
		this.checkProgram((Program) AST);
		
		/* Flush warn messages */
		this.messages.stream().forEach(Message::flush);
		
		CompilerDriver.stackTrace = null;
		return null;
	}
	
	public void checkProgram(Program p) throws CTEX_EXC {
		scopes.push(new Scope(null));
		
		/* Add global reserved declarations */
		scopes.peek().addDeclaration(CompilerDriver.HEAP_START);
		
		boolean gotMain = false;
		
		for (int i = 0; i < p.programElements.size(); i++) {
			SyntaxElement s = p.programElements.get(i);
			if (s instanceof Function) {
				Function f = (Function) s;
				
				if (f.path.build().equals(Const.MAIN)) gotMain = true;
				
				/* Check main function as entrypoint, if a function is called, 
				 * context is provided and then checked */
				if (f.path.build().equals(Const.MAIN) && !f.provisoTypes.isEmpty()) 
					throw new CTEX_EXC(f.getSource(), Const.MAIN_CANNOT_HOLD_PROVISOS);
				
				/* Check for duplicate function name */
				STRUCT.useProvisoFreeInCheck = false;
				
				for (Function f0 : this.functions) 
					if (f0.path.equals(f.path) && Function.signatureMatch(f0, f, false, true, false)) {
						/* 
						 * Already seen function has no body, this function has body,
						 * so already seen function is from a header file, this function
						 * is from a target file.
						 */
						if (f0.body == null && f.body != null) {
							this.functions.remove(f0);
							break;
						}
						else throw new CTEX_EXC(f.getSource(), Const.DUPLICATE_FUNCTION_NAME, f.path);
					}
				
				STRUCT.useProvisoFreeInCheck = true;
				
				/* 
				 * Add the function to the function pool here already, 
				 * since through recursion the same function may be called. 
				 */
				this.functions.add(f);
				
				/* Check only functions with no provisos, proviso functions will be hot checked. */
				if (f.provisoTypes.isEmpty()) f.check(this);
			}
			else {
				s.check(this);
				
				if (s instanceof StructTypedef) {
					StructTypedef typedef = (StructTypedef) s;
					
					/* 
					 * The last checked typedef was the implementation of another typedef,
					 * its ressources have now been moved to the typedef in this.reCheckTypedef.
					 * We need to re-check this typedef to make sure all references etc. are set.
					 */
					if (this.reCheckTypedef != null) {
						p.programElements.remove(i);
						i--;
						
						/* Interface typedef still contains both typedefs, remove implementation */
						for (INTERFACE intf : typedef.implemented) 
							intf.getTypedef().implementers.remove(typedef);
						
						/* Remove all added functions to prevent duplicate error */
						this.functions.removeAll(this.reCheckTypedef.functions);
						this.nestedFunctions.removeAll(this.reCheckTypedef.functions);
						
						/* Simply re-check */
						this.reCheckTypedef.check(this);
						this.reCheckTypedef = null;
					}
				}
			}
			
			if (progress != null) 
				progress.incProgress((double) i / p.programElements.size());
		}
		
		/* Did not find a function that matches main signature */
		if (!gotMain) 
			throw new CTEX_EXC(p.getSource(), Const.MISSING_MAIN_FUNCTION);
		
		if (progress != null) 
			progress.finish();

		/* If the declarations is not used, add itself to the free list. */
		for (Declaration d : declarations) 
			if (d.last != null) 
				d.last.free.add(d);
	}
	
	public TYPE checkFunction(Function f) throws CTEX_EXC {
		/* Proviso Types are already set at this point */
		
		scopes.push(new Scope(this.scopes.get(0)));
		
		this.signalStack.push(new ArrayList());
		this.watchpointStack.push(f);
		
		if (f.path.build().equals(Const.MAIN) && f.signals()) 
			throw new CTEX_EXC(Const.MAIN_CANNOT_SIGNAL);
		
		/* Check for duplicate function parameters */
		if (f.parameters.size() > 1) {
			for (int i = 0; i < f.parameters.size(); i++) {
				for (int a = i + 1; a < f.parameters.size(); a++) {
					if (f.parameters.get(i).path.equals(f.parameters.get(a).path)) 
						throw new CTEX_EXC(Const.DUPLICATE_PARAMETER_NAME, f.parameters.get(i).path, f.path);
				}
			}
		}
		
		for (Declaration d : f.parameters) d.check(this);
		
		if (f.signals() && f.signalsTypes.isEmpty()) 
			throw new CTEX_EXC(Const.MUST_SIGNAL_AT_LEAST_ONE_TYPE);
		
		/* Check body */
		this.currentFunction.push(f);
		if (f.body != null) this.checkBody(f.body, false, false);
		this.currentFunction.pop();
		
		/* Set actual signaled types for linter */
		f.actualSignals.addAll(this.signalStack.peek());
		
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
			String unwatched = this.signalStack.peek().stream().map(TYPE::toString).collect(Collectors.joining(", "));
			throw new CTEX_EXC(Const.UNWATCHED_EXCEPTIONS_FOR_FUNCTION, f.path, unwatched);
		}
		
		this.watchpointStack.pop();
		this.signalStack.pop();
		scopes.pop();
		
		if (f.inheritLink != null) {
			/* 
			 * This function is part of a struct type and was inherited by an extension.
			 * The function was not overridden in the child, so this acts as a reference
			 * to the parent function. We need to also check the parent function here to
			 * register a mapping so it is casted / it is symbolically called. During casting,
			 * this function will become a relay to the cast of the parent function.
			 * 
			 * We also have to set the proviso here, but since they are a 1 to 1 mapping,
			 * we can just get the context and apply it to the function.
			 */
			List<TYPE> context = f.provisoTypes.stream().map(x -> x.provisoFree()).collect(Collectors.toList());
			f.inheritLink.setContext(context);
			f.inheritLink.check(this);
		}
		
		return f.getReturnType().clone();
	}
	
	public TYPE checkStructTypedef(StructTypedef e) throws CTEX_EXC {
		
		e.copyInheritedFunctions();
		
		boolean useProvisoFree = STRUCT.useProvisoFreeInCheck;
		STRUCT.useProvisoFreeInCheck = false;
		
		/*
		 * Check if this typedef is the implementation of an existing typedef.
		 * If yes, move the function bodies to the exsiting typedef.
		 */
		boolean addedToHeaderDef = false;
		for (StructTypedef def : this.structTypedefs) {
			if (def.path.equals(e.path) && !def.equals(e)) {
				for (int i = 0; i < def.functions.size(); i++) {
					Function f = def.functions.get(i);
					if (f.body == null) {
						for (int a = 0; a < e.functions.size(); a++) {
							Function f0 = e.functions.get(a);
							if (Function.signatureMatch(f0, f, false, false, false)) {
								f.body = f0.body;
								break;
							}
						}
					}
				}
				
				this.reCheckTypedef = def;
				addedToHeaderDef = true;
			}
		}
		
		/*
		 * We only need to check all this if this is not an implementation,
		 * since the implementation has already been tested.
		 */
		if (!addedToHeaderDef) {
			for (Function f : e.functions) {
				
				if (f.modifier != MODIFIER.STATIC) {
					
					/* Dynamically add-in the struct head provisos, if not present already */
					if (!e.inheritedFunctions.contains(f)) {
						for (TYPE t : e.proviso) {
							boolean found = f.provisoTypes.stream().filter(x -> x.isEqual(t)).count() > 0;
							
							/* Add the proviso to the function signature */
							if (!found) f.provisoTypes.add(t.clone());
						}
					}
					
					/* Add to a pool of nested functions */
					this.nestedFunctions.add(f);
				 
					/* Check for duplicate function name */
					for (Function f0 : this.functions) {
						if (f0.path.equals(f.path) && Function.signatureMatch(f0, f, false, true, false))
							throw new CTEX_EXC(f.getSource(), Const.DUPLICATE_FUNCTION_NAME, f.path);
					}
				}
				
				/* 
				 * Add the function to the function pool here already, 
				 * since through recursion the same function may be called. 
				 */
				this.functions.add(f);
				
				if (f.provisoTypes.isEmpty()) 
					f.check(this);
				
				if (f.modifier != MODIFIER.STATIC) {
					
					List<TYPE> missing = this.missingProvisoTypes(f.provisoTypes, e.proviso);
					if (!missing.isEmpty() && !e.inheritedFunctions.contains(f)) {
						/* 
						 * There are provisos missing and the function is not inherited, throw an error. 
						 * If the function is inherited, the same check has been done to the function by
						 * the parent, so we dont need to check it here.
						 */
						String s = missing.stream().map(TYPE::toString).collect(Collectors.joining(", "));
						throw new CTEX_EXC(Const.FUNCTION_MISSING_REQUIRED_PROVISOS, f.path.getLast(), e.path, s);
					}
				}
			}
			
			/* Make sure implemented interface requirements are satisfied */
			for (INTERFACE inter : e.implemented) {
				
				for (Function f : inter.getTypedef().functions) {
					
					Function ftranslated = f.cloneSignature();
					
					ftranslated.translateProviso(inter.getTypedef().proviso, inter.proviso);
					
					boolean found = false;
					for (int i = 0; i < e.functions.size(); i++) {
						Function structFunction = e.functions.get(i);
						
						if (Function.signatureMatch(structFunction, ftranslated, false, true, false)) {
							/* Add default context to make sure it is casted */
							if (structFunction.provisoTypes.isEmpty())
								structFunction.addProvisoMapping(f.getReturnType(), new ArrayList());
							
							structFunction.requireR10Reset = true;
							
							found = true;
							break;
						}
					}
					
					if (!found) 
						throw new CTEX_EXC(Const.IMPLEMENTED_FUNCTION_MISSING, f.path.getLast(), inter.getTypedef().path);
				}
			}
			
			Optional<TYPE> opt = e.proviso.stream().filter(x -> !(x instanceof PROVISO)).findFirst();
			
			if (opt.isPresent())
				throw new CTEX_EXC(Const.NON_PROVISO_TYPE_IN_HEADER, opt.get().provisoFree());
			
			if (e.extension != null && e.extension.proviso.size() != e.extProviso.size()) 
				throw new CTEX_EXC(Const.MISSMATCHING_NUMBER_OF_PROVISOS_EXTENSION, e.extension.self.provisoFree(), e.extension.proviso.size(), e.extProviso.size());
			
			if (!this.structTypedefs.contains(e))
				this.structTypedefs.add(e);
			
			/* Make sure at least one field is in the struct */
			if (e.getFields().isEmpty())
				throw new CTEX_EXC(Const.STRUCT_TYPEDEF_MUST_CONTAIN_FIELD);
		}
		
		STRUCT.useProvisoFreeInCheck = useProvisoFree;
		
		/* Set the declarations in the struct type */
		return new VOID();
	}
	
	public TYPE checkInterfaceTypedef(InterfaceTypedef e) throws CTEX_EXC {
		/* Require SIDs since we need to check in it interface table */
		if (CompilerDriver.disableStructSIDHeaders)
			throw new CTEX_EXC(Const.SID_DISABLED_NO_INTERFACES);
		
		for (Function f : e.functions) {
			f.definedInInterface = e;
			
			if (f.modifier != MODIFIER.STATIC) {

				/* Dynamically add-in the struct head provisos, if not present already */
				for (TYPE t : e.proviso) {
					boolean found = f.provisoTypes.stream().filter(x -> x.isEqual(t)).count() > 0;
					
					/* Add the proviso to the function signature */
					if (!found) f.provisoTypes.add(t.clone());
				}
				
				/* Add to a pool of nested functions */
				this.nestedFunctions.add(f);
			 
				/* Check for duplicate function name */
				for (Function f0 : this.functions) {
					if (f0.path.equals(f.path) && Function.signatureMatch(f0, f, false, true, false))
						throw new CTEX_EXC(f.getSource(), Const.DUPLICATE_FUNCTION_NAME, f.path);
				}
			}
			
			/* 
			 * Add the function to the function pool here already, 
			 * since through recursion the same function may be called. 
			 */
			this.functions.add(f);
			
			if (f.provisoTypes.isEmpty()) 
				f.check(this);
			
			if (f.modifier != MODIFIER.STATIC) {
				/* Check if all required provisos are present */
				List<TYPE> missing = this.missingProvisoTypes(f.provisoTypes, e.proviso);
				
				/* 
				 * There are provisos missing and the function is not inherited, throw an error. 
				 * If the function is inherited, the same check has been done to the function by
				 * the parent, so we dont need to check it here.
				 */
				if (!missing.isEmpty()) {
					String s = missing.stream().map(TYPE::toString).collect(Collectors.joining(", "));
					throw new CTEX_EXC(Const.FUNCTION_MISSING_REQUIRED_PROVISOS, f.path.getLast(), e.path, s);
				}
			}
		}
		
		Optional<TYPE> opt = e.proviso.stream().filter(x -> !x.isProviso()).findFirst();
		
		if (opt.isPresent())
			throw new CTEX_EXC(Const.NON_PROVISO_TYPE_IN_HEADER, opt.get().provisoFree());
		
		/* Set the declarations in the struct type */
		return new VOID();
	}
	
	public TYPE checkSignal(SignalStatement e) throws CTEX_EXC {
		TYPE exc = e.exceptionBuilder.check(this);
		
		if (!exc.isStruct()) 
			throw new CTEX_EXC(Const.EXPECTED_STRUCT_TYPE, exc);
		
		e.watchpoint = this.watchpointStack.peek();
		
		/* Add to signal stack */
		if (!this.signalStackContains(exc)) 
			this.signalStack.peek().add(exc);
		
		return new VOID();
	}
	
	public TYPE checkTryStatement(TryStatement e) throws CTEX_EXC {
		this.scopes.push(new Scope(this.scopes.peek()));
		this.signalStack.push(new ArrayList());
		
		/* If exception is thrown that is not watched by this statement, relay to this watchpoint */
		e.watchpoint = this.watchpointStack.peek();
		
		/* Setup new watchpoint target */
		this.watchpointStack.push(e);
		
		this.checkBody(e.body, false, false);
		
		for (int i = 0; i < e.watchpoints.size(); i++) {
			for (int a = i + 1; a < e.watchpoints.size(); a++) {
				if (e.watchpoints.get(i).watched.getType().isEqual(e.watchpoints.get(a).watched.getType())) 
					throw new CTEX_EXC(Const.MULTIPLE_WATCHPOINTS_FOR_EXCEPTION, e.watchpoints.get(i).watched.getType().provisoFree());
			}
		}
		
		this.scopes.pop();
		this.watchpointStack.pop();
		
		e.actualSignals.addAll(this.signalStack.peek());
		
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
				messages.add(new Message(String.format(Const.WATCHED_EXCEPTION_NOT_THROWN_IN_TRY, w.watched.getType().provisoFree(), e.getSource().getSourceMarker()), LogPoint.Type.WARN, true));
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
	
	public TYPE checkWatchStatement(WatchStatement e) throws CTEX_EXC {
		this.scopes.push(new Scope(this.scopes.peek()));
		
		e.watched.check(this);
		
		this.checkBody(e.body, false, false);
		
		this.scopes.pop();
		return new VOID();
	}
	
	public TYPE checkStructureInit(StructureInit e) throws CTEX_EXC {
		e.setType(e.structType);
		
		StructTypedef extension = e.structType.getTypedef().extension;
		
		boolean covered = false;
		
		/* Make sure the correct number of provisos are supplied. If the provided provisos are empty, an auto mapping still can be created. */
		if (e.structType.proviso.size() != e.structType.getTypedef().proviso.size() && !e.structType.proviso.isEmpty())
			throw new CTEX_EXC(Const.MISSMATCHING_NUMBER_OF_PROVISOS, e.structType.getTypedef().proviso.size(), e.structType.proviso.size());
		
		/* Check if the first element of the call is the super constructor */
		if (e.elements.get(0) instanceof InlineCall) {
			InlineCall call = (InlineCall) e.elements.get(0);
			
			/* Calls to super constructor */
			if (call.path.build().equals("super")) {
				
				/* Calls to super, but no extension */
				if (e.structType.getTypedef().extension == null)
					throw new CTEX_EXC(Const.CANNOT_INVOKE_SUPER_NO_EXTENSION, e.structType);
				
				/* Search for constructor of extension */
				for (Function f : e.structType.getTypedef().extension.functions) 
					if (f.path.build().endsWith("create") && f.modifier == MODIFIER.STATIC) 
						/* Found static constructor, switch out 'super' with path to constructor */
						call.path = f.path.clone();
				
				/* No super constructor was found */
				if (call.path.build().equals("super"))
					throw new CTEX_EXC(Const.CANNOT_INVOKE_SUPER_NO_CONSTRUCTOR, e.structType.getTypedef().extension.self);
				else
					covered = true;
			}
			/* Does not call directley to super constructor, but calls the super constructor manually */
			else if (call.path.path.size() > 1 && (call.path.path.get(call.path.path.size() - 2) + ".create").equals(extension.path.getLast() + ".create")) {
				covered = true;
			}
		}
		/* Creates new instance of extension manually */
		else if (extension != null && !(e.elements.get(0) instanceof TempAtom))
			covered = e.elements.get(0).check(this).isEqual(extension.self);
		
		/* No provisos were supplied, but provisos are expected. Attempt to auto map. */
		if (e.structType.proviso.isEmpty() && !e.structType.getTypedef().proviso.isEmpty()) {
			/* Attempt to find auto-provisos */
			List<TYPE> expected = new ArrayList();
			for (int i = 0; i < e.structType.getNumberOfFields(); i++) 
				expected.add(e.structType.getFieldNumberDirect(i).getRawType());
			
			List<TYPE> provided = new ArrayList();
			for (Expression x : e.elements) 
				provided.add(x.check(this));
			
			e.structType.proviso = this.autoProviso(e.structType.getTypedef().proviso, expected, provided, e.getSource());
		}
		
		/* Map the current function provisos to the resulting struct type */
		if (!this.currentFunction.isEmpty()) 
			ProvisoUtil.mapNTo1(e.getType(), this.currentFunction.peek().provisoTypes);
		
		/* 
		 * It it is not a temp atom, check the first element here so in the 
		 * line after this line getType() can be called safely 
		 */
		if (!(e.elements.get(0) instanceof TempAtom))
			e.elements.get(0).check(this);
		
		/* Check that type that is covering is a struct type */
		if (extension != null && covered && !(e.elements.get(0).getType().isStruct())) 
				throw new CTEX_EXC(Const.CAN_ONLY_COVER_WITH_STRUCT, e.elements.get(0).getType());
		
		/* Absolute placeholder case */
		if (e.elements.size() == 1 && e.elements.get(0) instanceof TempAtom) {
			TempAtom a = (TempAtom) e.elements.get(0);
			a.inheritType = e.structType;
			a.check(this);
		}
		/* Covered parameter case */
		else if (covered) {
			e.hasCoveredParam = true;
			
			int expected = e.structType.getTypedef().getFields().size() - extension.getFields().size() + 1;
			
			/* Make sure the correct number or parameters is supplied */
			if (e.elements.size() != expected)
				throw new CTEX_EXC(Const.MISSMATCHING_ARGUMENT_NUMBER, expected, e.elements.size());
			
			for (int i = 1; i < e.elements.size(); i++) {
				TYPE strType = e.structType.getField(e.structType.getTypedef().getFields().get(i + extension.getFields().size() - 1).path).getType();
				
				/* Single placeholder case */
				if (e.elements.get(i) instanceof TempAtom) {
					TempAtom a = (TempAtom) e.elements.get(i);
					a.inheritType = strType;
				}
				
				TYPE valType = e.elements.get(i).check(this);
				
				if (e.elements.get(i) instanceof StructureInit) {
					StructureInit init = (StructureInit) e.elements.get(i);
					init.isTopLevelExpression = false;
				}
				
				if (!valType.isEqual(strType) && !(e.elements.get(i) instanceof TempAtom)) {
					if (valType.isPointer() || strType.isPointer()) 
						CompilerDriver.printProvisoTypes = true;
					
					throw new CTEX_EXC(Const.ARGUMENT_DOES_NOT_MATCH_STRUCT_FIELD_TYPE, i + 1, valType.provisoFree(), strType.provisoFree());
				}
			}
		}
		else {
			/* Make sure the correct number or parameters is supplied, at this point we can strictly compare */
			if (e.elements.size() != e.structType.getTypedef().getFields().size() && e.elements.size() > 1) 
				throw new CTEX_EXC(Const.MISSMATCHING_ARGUMENT_NUMBER, e.structType.getTypedef().getFields().size(), e.elements.size());
			
			/* Make sure that all field types are equal to the expected types */
			for (int i = 0; i < e.elements.size(); i++) {
				
				TYPE strType = null;

				try {
					/* Last selection */
					strType = e.structType.getField(e.structType.getTypedef().getFields().get(i).path).getType();
				} catch (SNIPS_EXC ex) {
					if (ex.getDirectMessage().equals(Const.CANNOT_FREE_CONTEXTLESS_PROVISO))
						throw new SNIPS_EXC(Const.MISSING_PROVISOS, e.getClass().getSimpleName(), e.getSource().getSourceMarker());
					else 
						throw ex;
				}
				
				/* Single placeholder case */
				if (e.elements.get(i) instanceof TempAtom) {
					TempAtom a = (TempAtom) e.elements.get(i);
					a.inheritType = strType;
				}
				
				TYPE valType = e.elements.get(i).check(this);
				
				if (e.elements.get(i) instanceof StructureInit) {
					StructureInit init = (StructureInit) e.elements.get(i);
					init.isTopLevelExpression = false;
				}
				
				if (!valType.isEqual(strType) && !(e.elements.get(i) instanceof TempAtom)) {
					if (valType.isPointer() || strType.isPointer()) 
						CompilerDriver.printProvisoTypes = true;
					
					throw new CTEX_EXC(Const.ARGUMENT_DOES_NOT_MATCH_STRUCT_FIELD_TYPE, i + 1, valType.provisoFree(), strType.provisoFree());
				}
			}
		}
		
		/* Struct may have modifier restrictions */
		this.checkModifier(e.structType.getTypedef().modifier, e.structType.getTypedef().path, e.getSource());
		
		/* Check if all required provisos are present */
		e.structType.checkProvisoPresent(e.getSource());
		
		return e.getType();
	}
	
	public TYPE checkStructSelect(StructSelect e) throws CTEX_EXC {
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
				throw new CTEX_EXC(Const.BASE_MUST_BE_VARIABLE_REFERENCE);
			
			type = tc.check(this);
		}
		else if (e.selector instanceof ArraySelect) {
			ArraySelect arr = (ArraySelect) e.selector;
			type = arr.check(this);
		}
		else throw new CTEX_EXC(Const.BASE_MUST_BE_VARIABLE_REFERENCE);
		
		if (type == null) 
			throw new CTEX_EXC(Const.CANNOT_DETERMINE_TYPE);
		
		/* First selections does deref, this means that the base must be a pointer */
		if (e.deref) {
			if (type.isPointer()) {
				POINTER p0 = (POINTER) type;
				type = p0.targetType;
			}
			else throw new CTEX_EXC(e.selector.getSource(), Const.CANNOT_DEREF_NON_POINTER, type.provisoFree());
		}
		
		Expression selection = e.selection;
		
		while (true) {
			selection.setType(type.clone());
			
			if (type.isStruct()) {
				STRUCT struct = (STRUCT) type;
					
				if (selection instanceof StructSelect) {
					StructSelect sel0 = (StructSelect) selection;
					
					if (sel0.selector instanceof IDRef) {
						IDRef ref = (IDRef) sel0.selector;
						
						type = findAndLinkField(struct, ref);
						
						if (sel0.deref) {
							if (!type.isPointer()) 
								throw new CTEX_EXC(selection.getSource(), Const.CANNOT_DEREF_NON_POINTER, type.provisoFree());
							else 
								/* Unwrap pointer, selection does dereference */
								type = type.getContainedType();
						}
						
						ref.setType(type.clone());
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
						
						arr.setType(type.clone());
						
						this.scopes.pop();
					}
					else throw new CTEX_EXC(selection.getSource(), Const.CLASS_CANNOT_BE_SELECTOR, sel0.selector.getClass().getSimpleName());
					
					/* Next selection in chain */
					selection = sel0.selection;
				}
				else if (selection instanceof IDRef) {
					IDRef ref = (IDRef) selection;
					
					try {
						/* Last selection */
						type = findAndLinkField(struct, ref);
					} catch (SNIPS_EXC ex) {
						if (ex.getDirectMessage().equals(Const.CANNOT_FREE_CONTEXTLESS_PROVISO))
							throw new SNIPS_EXC(Const.MISSING_PROVISOS, e.getClass().getSimpleName(), e.getSource().getSourceMarker());
						else 
							throw ex;
					}
					
					TYPE type0 = type;
					if (type0.isPointer()) 
						type0 = type0.getContainedType();
					
					ref.setType(type.clone());
					
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
					
					arr.setType(type.clone());
					
					break;
				}
				else throw new CTEX_EXC(Const.CLASS_CANNOT_BE_SELECTOR, selection.getClass().getSimpleName());
			}
			else throw new CTEX_EXC(Const.CANNOT_SELECT_FROM_NON_STRUCT, type.provisoFree());
			
		}
		
		e.setType(type.clone());
		return e.getType();
	}
	
	public TYPE checkWhileStatement(WhileStatement w) throws CTEX_EXC {
		this.compoundStack.push(w);
		
		if (w.hasDirective(DIRECTIVE.UNROLL)) {
			ASTDirective directive = w.getDirective(DIRECTIVE.UNROLL);
			if (directive.hasProperty("depth")) {
				int depth = Integer.parseInt(directive.getProperty("depth"));
				w.CURR_UNROLL_DEPTH = depth;
			}
		}
		
		TYPE cond = w.condition.check(this);
		if (cond.wordsize() > 1) 
			throw new CTEX_EXC(Const.CONDITION_TYPE_MUST_BE_32_BIT);
		
		this.checkBody(w.body, true, true);
		
		this.compoundStack.pop();
		return null;
	}
	
	public TYPE checkDoWhileStatement(DoWhileStatement w) throws CTEX_EXC {
		this.compoundStack.push(w);
		
		TYPE cond = w.condition.check(this);
		if (cond.wordsize() > 1) 
			throw new CTEX_EXC(Const.CONDITION_TYPE_MUST_BE_32_BIT);
		
		this.checkBody(w.body, true, true);

		this.compoundStack.pop();
		return null;
	}
	
	public TYPE checkForStatement(ForStatement f) throws CTEX_EXC {
		this.compoundStack.push(f);
		
		if (f.hasDirective(DIRECTIVE.UNROLL)) {
			ASTDirective directive = f.getDirective(DIRECTIVE.UNROLL);
			if (directive.hasProperty("depth")) {
				int depth = Integer.parseInt(directive.getProperty("depth"));
				f.CURR_UNROLL_DEPTH = depth;
			}
		}
		
		this.scopes.push(new Scope(this.scopes.peek(), true));
		f.iterator.check(this);
		
		if (f.iterator instanceof Declaration) {
			Declaration dec = (Declaration) f.iterator;
			if (dec.value == null) 
				throw new CTEX_EXC(Const.ITERATOR_MUST_HAVE_INITIAL_VALUE);
		}
		else if (!(f.iterator instanceof IDRef)) {
			/* Make sure iterator can only be IDRef or declaration */
			throw new CTEX_EXC(Const.EXPECTED_IDREF_ACTUAL, f.iterator.getClass().getSimpleName());
		}
		
		this.scopes.push(new Scope(this.scopes.peek(), true));
		
		TYPE cond = f.condition.check(this);
		if (cond.wordsize() > 1) 
			throw new CTEX_EXC(Const.CONDITION_TYPE_MUST_BE_32_BIT);
		
		f.increment.check(this);
		
		this.checkBody(f.body, false, false);
		
		this.scopes.pop();
		this.scopes.pop();

		this.compoundStack.pop();
		return null;
	}
	
	public TYPE checkForEachStatement(ForEachStatement f) throws CTEX_EXC {
		this.compoundStack.push(f);
		
		this.scopes.push(new Scope(this.scopes.peek(), true));
		
		TYPE itType = f.iterator.check(this);
		TYPE refType = f.shadowRef.check(this);
		
		f.counter.check(this);
		f.counterRef.check(this);
		
		LhsId iteratorWritebackLhs = null;
		
		if (refType.isPointer()) {
			POINTER p = (POINTER) refType;
			
			if (!p.targetType.isEqual(itType) && !p.targetType.getCoreType().isEqual(itType))
				throw new CTEX_EXC(Const.POINTER_TYPE_DOES_NOT_MATCH_ITERATOR_TYPE, p.targetType.provisoFree(), itType.provisoFree());
			
			/* Construct expression to calculate address based on address of the shadowRef, counter and the size of the type */
			Expression add = new Add(f.shadowRef, f.counterRef, f.shadowRef.getSource());
			
			/* Set as new shadowRef, will be casted during code generation */
			f.shadowRef = new Deref(add, f.shadowRef.getSource());
			f.shadowRef.check(this);
			
			if (f.range == null)
				throw new CTEX_EXC(Const.CANNOT_ITERATE_WITHOUT_RANGE);
			
			f.range = new Mul(f.range, new Atom(new INT("" + itType.wordsize()), f.shadowRef.getSource()), f.range.getSource());
			f.range.check(this);
			
			if (f.writeBackIterator) 
				/* Construct the lhs that uses a pointer deref to store the iterator */
				iteratorWritebackLhs = new PointerLhsId(f.shadowRef.clone(), f.iterator.getSource());
		}
		else if (refType.isArray()) {
			ARRAY a = (ARRAY) refType;
			
			if (!a.elementType.isEqual(itType))
				throw new CTEX_EXC(Const.ARRAY_TYPE_DOES_NOT_MATCH_ITERATOR_TYPE, a.elementType.provisoFree(), itType.provisoFree());
			
			/* Select first value from array */
			List<Expression> select = Arrays.asList(f.counterRef);
			f.select = new ArraySelect(f.shadowRef, select, f.shadowRef.getSource());
			
			f.select.check(this);
			
			if (f.writeBackIterator) 
				/* Construct the lhs that uses an array select to store the iterator */
				iteratorWritebackLhs = new ArraySelectLhsId(f.select.clone(), f.iterator.getSource());	
		}
		else throw new CTEX_EXC(Const.ONLY_AVAILABLE_FOR_POINTERS_AND_ARRAYS, refType.provisoFree());
		
		if (f.writeBackIterator) {
			/* Construct an id-ref that points to the iterator */
			IDRef ref = new IDRef(f.iterator.path, f.iterator.getSource());
			
			/* Create the assignment */
			f.writeback = new Assignment(ASSIGN_ARITH.NONE, iteratorWritebackLhs, ref, f.iterator.getSource());
			
			/* Check the expression to link all ressources */
			f.writeback.check(this);
		}
		
		this.scopes.push(new Scope(this.scopes.peek(), true));
		
		this.checkBody(f.body, false, false);
		
		this.scopes.pop();
		this.scopes.pop();

		this.compoundStack.pop();
		return null;
	}
	
	public TYPE checkIfStatement(IfStatement i) throws CTEX_EXC {
		/* Since else statement is directley checked, we need to set this explicitly here */
		this.currentStatement = i;
		
		if (i.condition != null) {
			TYPE cond = i.condition.check(this);
			if (cond.wordsize() > 1) 
				throw new CTEX_EXC(Const.CONDITION_TYPE_MUST_BE_32_BIT);
		}
		else {
			if (i.elseStatement != null) 
				throw new CTEX_EXC(Const.MULTIPLE_ELSE_STATEMENTS);
		}
		
		this.checkBody(i.body, true, false);
		
		if (i.elseStatement != null) 
			i.elseStatement.check(this);
		
		return null;
	}
	
	public TYPE checkDeclaration(Declaration d) throws CTEX_EXC {
		
		if (!this.currentFunction.isEmpty()) 
			ProvisoUtil.mapNTo1(d.getType(), this.currentFunction.peek().provisoTypes);
		
		/* Set self as last, implicitly unused */
		d.last = d;
		
		if (d.value != null) {
			/* Apply parameter type if atom is placeholder */
			if (d.value instanceof TempAtom) {
				TempAtom a = (TempAtom) d.value;
				a.inheritType = d.getType();
			}
			
			TYPE t = d.value.check(this);
			if (d.getType() instanceof AUTO || d.hadAutoType) {
				d.hadAutoType = true;
				d.setType(d.value.getType().clone());
			}
			
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
			
			if (!t.isEqual(d.getType()) || d.getType().wordsize() != t.wordsize()) {
				if (t.isPointer() || d.getType().isPointer()) 
					CompilerDriver.printProvisoTypes = true;
				
				if (this.checkPolymorphViolation(t, d.getType())) 
					throw new CTEX_EXC(Const.POLY_ONLY_VIA_POINTER, t.provisoFree(), d.getType().provisoFree());
				
				if (d.hadAutoType)
					throw new CTEX_EXC(Const.AUTO_TYPE_PROBLEMATIC_AT_THIS_LOCATION);
				else
					throw new CTEX_EXC(Const.EXPRESSION_TYPE_DOES_NOT_MATCH_DECLARATION, d.path, t.provisoFree(), d.getType().provisoFree());
			}
		}
		else {
			if (d.getType() instanceof AUTO) 
				throw new CTEX_EXC(Const.AUTO_TYPE_REQUIRES_VALUE);
		}
		
		/* When function type, can not only collide with over vars, but also function names */
		if (d.getType() instanceof FUNC) {
			for (Function f0 : this.functions) {
				if (f0.path.getLast().equals(d.path.getLast()) && f0.path.path.size() == 1) 
					throw new CTEX_EXC(Const.PREDICATE_SHADOWS_FUNCTION, d.path);
			}
		}
		
		Message m = scopes.peek().addDeclaration(d);
		if (m != null) this.messages.add(m);
		
		this.declarations.add(d);
		
		/* If the type is a struct, make sure all required provisos are present */
		if (d.getType().getCoreType().isStruct()) {
			STRUCT s = (STRUCT) d.getType().getCoreType();
			s.checkProvisoPresent(d.getSource());
		}

		/* If the type is an interface, make sure the modifier is not violated */
		if (d.getType().getCoreType().isInterface()) { 
			INTERFACE i = (INTERFACE) d.getType().getCoreType();
			
			/* Check for modifier restrictions */
			this.checkModifier(i.getTypedef().modifier, i.getTypedef().path, d.getSource());
		}

		/* No need to set type here, is done while parsing */
		return d.getType();
	}
	
	public TYPE checkAssignment(Assignment a) throws CTEX_EXC {
		TYPE targetType = a.lhsId.check(this);
		
		if (a.lhsId instanceof PointerLhsId) targetType = new POINTER(targetType);
		
		NamespacePath path = a.lhsId.getFieldName();
		
		if (path != null) scopes.peek().getField(path, a.getSource());
		
		/* Override for placeholder atom */
		if (a.value instanceof TempAtom) {
			TempAtom atom = (TempAtom) a.value;
			atom.inheritType = targetType;
		}
		
		TYPE t = a.value.check(this);
		TYPE ctype = t;
		
		/* If target type is a pointer, only the core types have to match */
		if (!targetType.isEqual(t) || (targetType.wordsize() != t.wordsize() && a.lhsId instanceof SimpleLhsId)) {
			if (targetType.isPointer() || t.isPointer()) 
				CompilerDriver.printProvisoTypes = true;
			
			if (this.checkPolymorphViolation(t, targetType)) 
				throw new CTEX_EXC(Const.VARIABLE_DOES_NOT_MATCH_EXPRESSION_POLY, t.provisoFree(), targetType.provisoFree());
			
			throw new CTEX_EXC(Const.EXPRESSION_TYPE_DOES_NOT_MATCH_VARIABLE, t.provisoFree(), targetType.provisoFree());
		}
		
		if (a.assignArith != ASSIGN_ARITH.NONE) {
			if (t.wordsize() > 1) 
				throw new CTEX_EXC(Const.ONLY_APPLICABLE_FOR_ONE_WORD_TYPE);
			
			if (a.assignArith == ASSIGN_ARITH.AND_ASSIGN || a.assignArith == ASSIGN_ARITH.ORR_ASSIGN || a.assignArith == ASSIGN_ARITH.BIT_XOR_ASSIGN) {
				if (ctype.wordsize() > 1) 
					throw new CTEX_EXC(Const.EXPRESSIONT_TYPE_NOT_APPLICABLE_FOR_TYPE, t.provisoFree());
			}
			else if (a.assignArith != ASSIGN_ARITH.NONE) {
				if (!(ctype instanceof INT)) 
					throw new CTEX_EXC(Const.EXPRESSIONT_TYPE_NOT_APPLICABLE_FOR_ASSIGN_OP, t.provisoFree());
			}
		}
		
		a.lhsId.expressionType = t;
		return null;
	}
	
	public TYPE checkBreak(BreakStatement b) throws CTEX_EXC {
		if (this.compoundStack.isEmpty()) 
			throw new CTEX_EXC(Const.CAN_ONLY_BREAK_OUT_OF_LOOP);
		else b.superLoop = this.compoundStack.peek();
		return null;
	}
	
	public TYPE checkContinue(ContinueStatement c) throws CTEX_EXC {
		if (this.compoundStack.isEmpty()) 
			throw new CTEX_EXC(Const.CAN_ONLY_CONTINUE_IN_LOOP);
		else c.superLoop = this.compoundStack.peek();
		return null;
	}
	
	public TYPE checkSwitchStatement(SwitchStatement s) throws CTEX_EXC {
		if (!(s.condition instanceof IDRef)) 
			throw new CTEX_EXC(s.condition.getSource(), Const.SWITCH_COND_MUST_BE_VARIABLE);
		
		TYPE type = s.condition.check(this);
		if (!type.isPrimitive()) 
			throw new CTEX_EXC(s.condition.getSource(), Const.CAN_ONLY_APPLY_TO_PRIMITIVE);
		
		if (s.defaultStatement == null) 
			throw new CTEX_EXC(Const.MISSING_DEFAULT_STATEMENT);
		
		for (CaseStatement c : s.cases) c.check(this);
		s.defaultStatement.check(this);
		return null;
	}
	
	public TYPE checkCaseStatement(CaseStatement c) throws CTEX_EXC {
		TYPE type = c.condition.check(this);
		
		if (!type.isEqual(c.superStatement.condition.getType())) 
			throw new CTEX_EXC(c.condition.getSource(), Const.EXPRESSION_TYPE_DOES_NOT_MATCH_VARIABLE, type.provisoFree(), c.superStatement.condition.getType().provisoFree());
		
		this.checkBody(c.body, true, true);
		return null;
	}
	
	public TYPE checkDefaultStatement(DefaultStatement d) throws CTEX_EXC {
		this.checkBody(d.body, true, true);
		return null;
	}
	
	public TYPE checkReturn(ReturnStatement r) throws CTEX_EXC {
		if (r.value != null) {
			TYPE t = r.value.check(this);

			this.currentFunction.peek().hasReturn = true;
			
			/* There was a return statement with no return value previously */
			if (this.currentFunction.peek().noReturn != null) 
				throw new CTEX_EXC(this.currentFunction.peek().noReturn.getSource(), Const.NO_RETURN_VALUE, this.currentFunction.peek().getReturnType().provisoFree());
			
			if (t.isEqual(this.currentFunction.peek().getReturnType())) 
				return t;
			else throw new CTEX_EXC(Const.RETURN_TYPE_DOES_NOT_MATCH, t.provisoFree(), this.currentFunction.peek().getReturnType().provisoFree());
		}
		else {
			if (this.currentFunction.peek().hasReturn) 
				throw new CTEX_EXC(Const.NO_RETURN_VALUE, this.currentFunction.peek().getReturnType().provisoFree());
			else 
				this.currentFunction.peek().noReturn = r;
			
			if (!(currentFunction.peek().getReturnType() instanceof VOID)) 
				throw new CTEX_EXC(Const.RETURN_TYPE_DOES_NOT_MATCH, new VOID(), currentFunction.peek().getReturnType().provisoFree());
			
			return new VOID();
		}
	}
	
	public TYPE checkTernary(Ternary t) throws CTEX_EXC {
		TYPE type = t.condition.check(this);
		if (type.wordsize() > 1) 
			throw new CTEX_EXC(t.condition.getSource(), Const.CONDITION_TYPE_MUST_BE_32_BIT, type.provisoFree());
		
		if (t.condition instanceof ArrayInit) 
			throw new CTEX_EXC(t.condition.getSource(), Const.STRUCT_INIT_CAN_ONLY_BE_SUB_EXPRESSION_OF_STRUCT_INIT);
		
		TYPE t0 = t.left.check(this);
		TYPE t1 = t.right.check(this);
		
		if (t.left instanceof ArrayInit) 
			throw new CTEX_EXC(t.left.getSource(), Const.STRUCT_INIT_CAN_ONLY_BE_SUB_EXPRESSION_OF_STRUCT_INIT);
		
		if (t.right instanceof ArrayInit) 
			throw new CTEX_EXC(t.right.getSource(), Const.STRUCT_INIT_CAN_ONLY_BE_SUB_EXPRESSION_OF_STRUCT_INIT);
		
		if (!t0.isEqual(t1)) 
			throw new CTEX_EXC(t.condition.getSource(), Const.OPERAND_TYPES_DO_NOT_MATCH, t0.provisoFree(), t1.provisoFree());
		
		t.setType(t0);
		return t.getType();
	}
	
	public TYPE checkNFoldExpression(NFoldExpression b) throws CTEX_EXC {
		
		for (Expression e : b.operands) e.check(this);
		
		for (Expression e : b.operands)
			if (e.getType().isNull()) 
				throw new CTEX_EXC(e.getSource(), Const.CANNOT_PERFORM_ARITH_ON_NULL);
		
		for (Expression e : b.operands)
			if (e instanceof ArrayInit) 
				throw new CTEX_EXC(e.getSource(), Const.STRUCT_INIT_CAN_ONLY_BE_SUB_EXPRESSION_OF_STRUCT_INIT);
		
		for (Expression e : b.operands)
			if (e.getType().wordsize() > 1) 
				throw new CTEX_EXC(e.getSource(), Const.CAN_ONLY_APPLY_TO_PRIMITIVE_OR_POINTER, e.getType().provisoFree());
		
		boolean gotPointer = false;
		for (Expression e : b.operands) {
			if (e.getType().isPointer()) {
				gotPointer = true;
				for (Expression e0 : b.operands) {
					if (e.equals(e0)) continue;
					if (!(e0.getType().getCoreType() instanceof INT)) 
						throw new CTEX_EXC(Const.POINTER_ARITH_ONLY_SUPPORTED_FOR_TYPE, new INT(), e0.getType().provisoFree());
				}
			}
		}
		
		if (!gotPointer) {
			for (Expression e : b.operands) {
				if (!e.getType().isEqual(b.operands.get(0).getType()))
					throw new CTEX_EXC(Const.OPERAND_TYPES_DO_NOT_MATCH, b.operands.get(0).getType().provisoFree(), e.getType().provisoFree());
			}
		}
		
		b.setType(b.operands.get(0).getType().clone());
		return b.getType();
	}
	
	/**
	 * Overrides binary expression.<br>
	 * Checks for:<br>
	 * - Both operand types have to be of type BOOL<br>
	 */
	public TYPE checkBoolNFoldExpression(BoolNFoldExpression b) throws CTEX_EXC {
		for (Expression e : b.operands) {
			e.check(this);
			if (e.getType().wordsize() > 1) 
				throw new CTEX_EXC(e.getSource(), Const.CONDITION_TYPE_MUST_BE_32_BIT);
		}

		b.setType(b.operands.get(0).getType().clone());
		return b.getType();
	}
	
	/**
	 * Overrides unary expression.<br>
	 * Checks for:<br>
	 * - Operand type has to be of type BOOL<br>
	 */
	public TYPE checkBoolUnaryExpression(BoolUnaryExpression b) throws CTEX_EXC {
		TYPE t = b.getOperand().check(this);
		
		if (t.wordsize() > 1) 
			throw new CTEX_EXC(b.getOperand().getSource(), Const.CONDITION_TYPE_MUST_BE_32_BIT);
		
		b.setType(t);
		return b.getType();
	}
	
	public TYPE checkUnaryExpression(UnaryExpression u) throws CTEX_EXC {
		TYPE op = u.getOperand().check(this);
		
		if (op.isNull()) 
			throw new CTEX_EXC(u.getOperand().getSource(), Const.CANNOT_PERFORM_ARITH_ON_NULL);
		
		if (u.getOperand() instanceof ArrayInit) 
			throw new CTEX_EXC(u.getOperand().getSource(), Const.STRUCT_INIT_CAN_ONLY_BE_SUB_EXPRESSION_OF_STRUCT_INIT);
		
		if (u instanceof BitNot && op instanceof PRIMITIVE) {
			u.setType(op);
			return u.getType();
		}
		else if (u instanceof UnaryMinus && op instanceof PRIMITIVE) {
			u.setType(op);
			return u.getType();
		}
		else throw new CTEX_EXC(Const.UNKNOWN_EXPRESSION, u.getClass().getName());
	}
	
	public TYPE checkCompare(Compare c) throws CTEX_EXC {
		TYPE t0 = null;
		
		for (Expression e : c.operands) {
			TYPE t = e.check(this);
			if (t0 == null) t0 = t;
			else {
				if (!t.isEqual(t0)) 
					throw new CTEX_EXC(Const.OPERAND_TYPES_DO_NOT_MATCH, t0.provisoFree(), t.provisoFree());
			}
			
			if (e instanceof ArrayInit) 
				throw new CTEX_EXC(e.getSource(), Const.STRUCT_INIT_CAN_ONLY_BE_SUB_EXPRESSION_OF_STRUCT_INIT);
		}
		
		c.setType(new BOOL());
		return c.getType();
	}
	
	/**
	 * Unified check method for function call and inline call. Both of them implement
	 * the Callee interface, which is used to easily retrieve and set data to one of them.
	 */
	public TYPE checkCall(Callee c) throws CTEX_EXC {
		
		/* Check if call is call to super function, if yes, transform call and target */
		CtxCallUtil.transformNestedSuperCall(c, this.getCurrentFunction());
		
		/* Check here to get types for function search */
		List<TYPE> types = new ArrayList();
		for (int i = 0; i < c.getParams().size(); i++) {
			Expression e = c.getParams().get(i);
			
			try {
				types.add(e.check(this).clone());
			} catch (Exception exc) {
				/* 
				 * A temp atom cannot determine its inherited type at this location,
				 * since the function is required for this. But this loop has to be
				 * executed before the function search takes place. When checking the temp atom,
				 * an exception will be thrown. This is not a major problem, since in
				 * the most cases, it is solved later on.
				 */
				if (!(e instanceof TempAtom))
					throw new CTEX_EXC(e.getSource(), Const.FAILED_TO_CHECK_PARAMETER, i + 1, c.getPath().build());
				else
					types.add(null);
			}
		}
		
		/* Call is call to super constructor, replace with explicit call, returns called constructor */
		Function func = CtxCallUtil.transformSuperConstructorCall(c, this.getCurrentFunction());
		
		/* Super constructor was not used, search for function */
		if (func == null) func = this.searchFunction(c, types);
		Function f = func;
		
		if (c.isNestedCall()) {
			/* Call to struct nested function */
			if (c.getParams().get(0).check(this).getCoreType().isStruct()) {
				STRUCT s = (STRUCT) c.getParams().get(0).check(this).getCoreType();
				StructTypedef def = s.getTypedef();
				
				boolean found = false;
				while (def != null) {
					for (Function nested : def.functions) {
						if (nested.equals(f)) {
							found = true;
							break;
						}
					}
					
					def = def.extension;
				}
				
				if (!found)
					throw new CTEX_EXC(Const.FUNCTION_IS_NOT_PART_OF_STRUCT_TYPE, f.path, s.getTypedef().path);
			}
			/* Call to interface via struct pointer instance */
			else {
				INTERFACE s = (INTERFACE) c.getParams().get(0).check(this).getCoreType();
				
				boolean found = false;
				for (Function nested : s.getTypedef().functions) {
					if (nested.equals(f)) {
						nested.wasCalled = true;
						found = true;
						break;
					}
				}
				
				if (!found)
					throw new CTEX_EXC(Const.FUNCTION_IS_NOT_PART_OF_STRUCT_TYPE, f.path, s.getTypedef().path);
			
				/* Use auto provisos for not given proviso types */
				if (c.getProviso().isEmpty()) {
					c.setAutoProviso(true);
					
					/* Attempt to find auto-proviso mapping */
					List<TYPE> iParamTypes = new ArrayList();
					
					for (int a = 0; a < c.getParams().size(); a++) {
						/* Apply parameter type if atom is placeholder */
						if (c.getParams().get(a) instanceof TempAtom) {
							TempAtom atom = (TempAtom) c.getParams().get(a);
							atom.inheritType = f.parameters.get(a).getType();
						}
						
						iParamTypes.add(c.getParams().get(a).check(this));
					}	
					
					List<TYPE> functionTypes = new ArrayList();
					
					for (Declaration d : f.parameters) 
						functionTypes.add(d.getRawType());
					
					/* Replace void* type of implicit self parameter with interface pointer type for auto-proviso */
					functionTypes.set(0, new POINTER(s.getTypedef().self.clone()));
					
					/* Attempt to auto-map proviso types */
					List<TYPE> mapping = this.autoProviso(f.provisoTypes, functionTypes, iParamTypes, c.getCallee().getSource());
					
					/* For the found mapping, wrap the mapped types in the corresponding proviso types */
					for (int i = 0; i < mapping.size(); i++) {
						PROVISO prov0 = (PROVISO) f.provisoTypes.get(i).clone();
						prov0.setContext(mapping.get(i).clone());
						mapping.set(i, prov0);
					}
					
					c.setProviso(mapping);
					f.setContext(c.getProviso());
				}
				
				/* Add proviso mapping to all implementations in the StructTypedefs that extend from this function. */
				for (StructTypedef def : s.getTypedef().implementers) {
					for (Function f0 : def.functions) {
						/* Only do this if mapping is not present already, may cause stack overflow */
						if (!f0.containsMapping(c.getProviso()) && f0.path.getLast().equals(f.path.getLast())) {
							f0.setContext(c.getProviso());
							f0.check(this);
						}
					}
				}
			}
			
			/* Generate warning if instance is not a pointer, but is derefed in call */
			if (c.isNestedDeref() && !c.getParams().get(0).getType().isPointer())
				if (!CompilerDriver.disableWarnings) 
					this.messages.add(new Message(String.format(Const.OPERAND_IS_NOT_A_POINTER, c.getParams().get(0).getSource().getSourceMarker()), LogPoint.Type.WARN, true));
		}
		else {
			/* Found function is nested, should not be able to access it */
			if (this.nestedFunctions.contains(f))
				throw new CTEX_EXC(Const.NESTED_FUNCTION_CANNOT_BE_ACCESSED, f.path);
		}
		
		c.setCalledFunction(f);
		
		if (!this.watchpointStack.isEmpty()) 
			c.setWatchpoint(this.watchpointStack.peek());
		
		if (f != null) {
			/* Inline calls made during global setup may not signal, since exception cannot be watched */
			if (c.getCallee() instanceof InlineCall && f.signals() && this.scopes.size() == 1) {
				throw new CTEX_EXC(Const.CALL_DURING_INIT_CANNOT_SIGNAL, f.path);
			}
			else if (c.getCallee() instanceof FunctionCall && c.getBaseRef() != null) {
				TYPE t = c.getBaseRef().check(this);
				
				if (!t.getCoreType().isStruct()) 
					throw new CTEX_EXC(Const.NESTED_CALL_BASE_IS_NOT_A_STRUCT, t.getCoreType());
				
				STRUCT s = (STRUCT) t.getCoreType();
				
				/* Check if typedef contains a function with the called path */
				boolean found = s.getTypedef().functions.stream().filter(x -> x.path.equals(f.path)).count() > 0;
				if (!found) throw new CTEX_EXC(Const.FUNCTION_IS_NOT_PART_OF_STRUCT_TYPE, f.path, s);
			}
			
			checkModifier(f.modifier, f.path, c.getCallee().getSource());
			
			if (f.signals()) 
				/* Add signaled types if not contained already */
				for (TYPE s : f.signalsTypes) 
					if (!this.signalStackContains(s)) 
						this.signalStack.peek().add(s);
			
			if (!f.provisoTypes.isEmpty()) {
				if (c.getProviso().isEmpty() || c.hasAutoProviso()) {
					c.setAutoProviso(true);
					
					/* Attempt to find auto-proviso mapping */
					List<TYPE> iParamTypes = new ArrayList();
					
					for (int a = 0; a < c.getParams().size(); a++) {
						/* Apply parameter type if atom is placeholder */
						if (c.getParams().get(a) instanceof TempAtom) {
							TempAtom atom = (TempAtom) c.getParams().get(a);
							atom.inheritType = f.parameters.get(a).getType();
						}
						
						iParamTypes.add(c.getParams().get(a).check(this));
					}	
					
					List<TYPE> functionTypes = new ArrayList();
					
					for (Declaration d : f.parameters) 
						functionTypes.add(d.getRawType());
					
					if (c.isNestedCall() && c.getParams().get(0).check(this).getCoreType().isInterface()) {
						INTERFACE s = (INTERFACE) c.getParams().get(0).check(this).getCoreType();
						
						/* Replace void* type of implicit self parameter with interface pointer type for auto-proviso */
						functionTypes.set(0, new POINTER(s.getTypedef().self.clone()));
					}
					
					if (iParamTypes.get(0).isInterface()) {
						/* Must be pointer to interface */
						iParamTypes.set(0, new POINTER(iParamTypes.get(0)));
					}
					
					c.setProviso(this.autoProviso(f.provisoTypes, functionTypes, iParamTypes, c.getCallee().getSource()));
				}
				
				if (c.isNestedCall() && c.getParams().get(0).check(this).getCoreType().isInterface()) {
					INTERFACE s = (INTERFACE) c.getParams().get(0).check(this).getCoreType();
					
					/* Add proviso mapping to all implementations in the StructTypedefs that extend from this function. */
					for (StructTypedef def : s.getTypedef().implementers) {
						for (Function f0 : def.functions) {
							/* Only do this if mapping is not present already, may cause stack overflow */
							if (!f0.containsMapping(c.getProviso()) && f0.path.getLast().equals(f.path.getLast())) {
								f0.setContext(c.getProviso());
								f0.check(this);
							}
						}
					}
				}
				
				if (f.containsMapping(c.getProviso())) {
					/* Mapping already exists, just return return type of this specific mapping */
					f.setContext(c.getProviso());
					c.setType(f.getReturnType());
				}
				else {
					/* Create a new context, check function for this specific context */
					f.setContext(c.getProviso());
					
					this.scopes.push(new Scope(this.scopes.get(0)));
					f.check(this);
					this.scopes.pop();
					c.setType(f.getReturnType());
				}
			}
			else 
				/* 
				 * Add default proviso mapping, so mapping is present,
				 * function was called and will be compiled.
				 */
				f.addProvisoMapping(f.getReturnType(), new ArrayList());
			
			if (c.getParams().size() != f.parameters.size()) 
				throw new CTEX_EXC(Const.MISSMATCHING_ARGUMENT_NUMBER, f.parameters.size(), c.getParams().size());
			
			for (int a = 0; a < f.parameters.size(); a++) {
				TYPE functionParamType = f.parameters.get(a).getType();
				
				/* Apply parameter type if atom is placeholder */
				if (c.getParams().get(a) instanceof TempAtom) {
					TempAtom atom = (TempAtom) c.getParams().get(a);
					atom.inheritType = functionParamType;
				}
				
				TYPE paramType = c.getParams().get(a).check(this);
				
				if (!paramType.isEqual(functionParamType)) {
					if (paramType.isPointer() || functionParamType.isPointer()) 
						CompilerDriver.printProvisoTypes = true;
					
					if (paramType.getCoreType().isStruct()) {
						STRUCT s = (STRUCT) paramType.getCoreType();
						s.checkProvisoPresent(c.getParams().get(a).getSource());
					}
					
					int paramNumber = a + 1;
					if (c.isNestedCall() && a > 0)
						a -= 1;
					
					if (this.checkPolymorphViolation(paramType, functionParamType))
						throw new CTEX_EXC(c.getParams().get(a).getSource(), Const.PARAMETER_TYPE_INDEX_DOES_NOT_MATCH_POLY, paramNumber, paramType.provisoFree(), functionParamType.provisoFree());
					
					throw new CTEX_EXC(c.getParams().get(a).getSource(), Const.PARAMETER_TYPE_INDEX_DOES_NOT_MATCH, paramNumber, paramType.provisoFree(), functionParamType.provisoFree());
				}
			}
			
			if (c.getCallee() instanceof InlineCall) {
				if (f.provisoTypes.isEmpty() || !f.containsMapping(c.getProviso())) 
					c.setType(f.getReturnType().clone());
				
				if (c.getType().isVoid() && !f.hasReturn) 
					throw new CTEX_EXC(Const.EXPECTED_RETURN_VALUE);
			}
		}
		else {
			/* Set void as return type */
			c.setType(new VOID());
			
			for (int a = 0; a < c.getParams().size(); a++) {
				if (c.getCallee() instanceof FunctionCall && c.getParams().get(a) instanceof ArrayInit) 
					throw new CTEX_EXC(Const.STRUCT_INIT_CAN_ONLY_BE_SUB_EXPRESSION_OF_STRUCT_INIT);
				
				c.getParams().get(a).check(this);
			}
		}
		
		/* 
		 * Interface makes an exception to this rule. When the nested 
		 * call chain is transformed during parsig, the information that this is
		 * an interface is not available. An interface is a pointer by itself,
		 * and thus we do not need the extra address reference.
		 */
		if (c.isNestedCall() && c.getParams().get(0).getType().getCoreType().isInterface() && c.getParams().get(0) instanceof AddressOf) {
			AddressOf aof = (AddressOf) c.getParams().get(0);
			c.getParams().set(0, aof.expression);
		}
		
		return (c.getCallee() instanceof FunctionCall)? new VOID() : c.getType();
	}
	
	/**
	 * Checks for:<br>
	 * - Sets the origin of the reference<br>
	 * - Sets the type of the reference
	 */
	public TYPE checkIDRef(IDRef i) throws CTEX_EXC {
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
			
			if (contains && !(i.origin.getType() instanceof FUNC)) 
				/* Set where the last statement accesses the IDRef. */
				i.origin.last = currentStatement;
			else 
				i.origin.last = null;
			
			return i.getType();
		}
		else throw new CTEX_EXC(Const.UNKNOWN_VARIABLE, i.path);
	}
	
	public TYPE checkInlineFunction(InlineFunction i) throws CTEX_EXC {
		i.inlineFunction.check(this);
		i.inlineFunction.wasCalled = true;
		i.inlineFunction.addProvisoMapping(i.inlineFunction.getReturnType(), new ArrayList());
		return new FUNC(i.inlineFunction, new ArrayList());
	}
	
	public TYPE checkFunctionRef(FunctionRef r) throws CTEX_EXC {
		/* If not already linked, find referenced function */
		Function lambda = null;
		if (r.origin != null)
			lambda = r.origin;
		else {
			String prefix = "";
			
			if (r.base != null) {
				TYPE t = r.base.check(this);
				
				/* Extract prefix from ressource */
				if (t.getCoreType().isStruct()) {
					STRUCT s = (STRUCT) t.getCoreType();
					prefix = s.getTypedef().path.getLast();
				}
				else if (t.getCoreType().isInterface()) {
					INTERFACE i = (INTERFACE) t.getCoreType();
					prefix = i.getTypedef().path.getLast();
				}
			}
			
			lambda = this.findFunction(r.path, r.getSource(), true, prefix, null);
			
			if (r.base != null) {
				STRUCT s = (STRUCT) r.base.check(this).getCoreType();
				
				boolean found = false;
				for (Function nested : s.getTypedef().functions) {
					if (nested.equals(lambda)) {
						found = true;
						break;
					}
				}
				
				if (!found)
					throw new CTEX_EXC(Const.FUNCTION_IS_NOT_PART_OF_STRUCT_TYPE, lambda.path, s.getTypedef().path);
			}
			else {
				if (this.nestedFunctions.contains(lambda))
					throw new CTEX_EXC(Const.NESTED_FUNCTION_CANNOT_BE_ACCESSED, lambda.path);
			}
		}
		
		if (lambda == null) 
			throw new CTEX_EXC(Const.UNKNOWN_PREDICATE, r.path);
		
		/* Provided number of provisos does not match number of provisos of lambda */
		if (lambda.provisoTypes.size() != r.proviso.size()) 
			throw new CTEX_EXC(Const.MISSMATCHING_NUMBER_OF_PROVISOS, lambda.provisoTypes.size(), r.proviso.size());
		
		/* A lambda cannot signal exceptions, since it may become anonymous */
		if (lambda.signals()) 
			throw new CTEX_EXC(Const.PREDICATE_CANNOT_SIGNAL);
		
		/* Set context and add mapping */
		if (!this.currentFunction.isEmpty()) 
			ProvisoUtil.mapNToNMaybe(r.proviso, this.currentFunction.peek().provisoTypes);
		
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
	
	public TYPE checkArrayInit(ArrayInit init) throws CTEX_EXC {
		/* Array must at least contain one element */
		if (init.elements.isEmpty()) 
			throw new CTEX_EXC(Const.ARRAY_INIT_MUST_HAVE_ONE_FIELD);
		
		TYPE type0 = init.elements.get(0).check(this);
		
		int dontCareSize = 0;
		
		if (init.elements.size() > 1 || init.dontCareTypes) {
			for (int i = 0; i < init.elements.size(); i++) {
				TYPE typeX = init.elements.get(i).check(this);
				
				if (init.dontCareTypes) 
					dontCareSize += typeX.wordsize();
				else {
					if (!typeX.isEqual(type0)) 
						throw new CTEX_EXC(Const.ARRAY_ELEMENTS_MUST_HAVE_SAME_TYPE, type0.provisoFree(), typeX.provisoFree());
				}
			}
		}
		
		init.setType(new ARRAY((init.dontCareTypes)? new VOID() : type0, (init.dontCareTypes)? dontCareSize : init.elements.size()));
		return init.getType();
	}
	
	public TYPE checkSizeOfType(SizeOfType sot) throws CTEX_EXC {
		if (!this.currentFunction.isEmpty()) 
			ProvisoUtil.mapNTo1(sot.sizeType, this.currentFunction.peek().provisoTypes);
		
		sot.setType(new INT());
		return sot.getType();
	}
	
	public TYPE checkIDOfExpression(IDOfExpression sot) throws CTEX_EXC {
		if (!this.currentFunction.isEmpty()) 
			ProvisoUtil.mapNTo1(sot.type, this.currentFunction.peek().provisoTypes);
		
		if (!(sot.type instanceof STRUCT)) 
			throw new CTEX_EXC(Const.EXPECTED_STRUCT_TYPE, sot.type);
		
		sot.setType(new VOID());
		return sot.getType();
	}
	
	public TYPE checkSizeOfExpression(SizeOfExpression soe) throws CTEX_EXC {
		soe.sizeType = soe.expression.check(this);
		soe.setType(new INT());
		return soe.getType();
	}
	
	public TYPE checkAddressOf(AddressOf aof) throws CTEX_EXC {
		TYPE t = aof.expression.check(this);
		
		if (!(aof.expression instanceof IDRef || aof.expression instanceof IDRefWriteback || aof.expression instanceof ArraySelect || aof.expression instanceof StructSelect || aof.expression instanceof StructureInit)) 
			throw new CTEX_EXC(Const.CAN_ONLY_GET_ADDRESS_OF_VARIABLE_REF_OR_ARRAY_SELECT);
		
		aof.setType(new POINTER(t.getCoreType()));
		
		return aof.getType();
	}
	
	/**
	 * Checks for:<br>
	 * - Operand type is pointer.y
	 */
	public TYPE checkDeref(Deref deref) throws CTEX_EXC {
		TYPE t = deref.expression.check(this);
		
		/* Dereference pointer or primitive type */
		if (t.isPrimitive()) 
			/* Set to core type */
			deref.setType(t.getCoreType());
		else if (t.isPointer()) {
			deref.setType(t.getContainedType());
		}
		else throw new CTEX_EXC(deref.expression.getSource(), Const.CANNOT_DEREF_TYPE, t.provisoFree());
		
		/* Dereferencing a primitive can be a valid statement, but it can be unsafe. A pointer would be safer. */
		if (t.isPrimitive()) {
			if (!CompilerDriver.disableWarnings) 
				this.messages.add(new Message(String.format(Const.OPERAND_IS_NOT_A_POINTER, deref.getSource().getSourceMarker()), LogPoint.Type.WARN, true));
		}
		
		return deref.getType();
	}
	
	public TYPE checkTypeCast(TypeCast tc) throws CTEX_EXC {
		TYPE t = tc.expression.check(this);
		
		if (tc.expression instanceof InlineCall) {
			InlineCall ic = (InlineCall) tc.expression;
			
			/* Anonymous inline call */
			if (ic.calledFunction == null) {
				ic.setType(tc.castType);
				t = tc.castType;
				
				if (!CompilerDriver.disableWarnings) 
					messages.add(new Message(String.format(Const.USING_IMPLICIT_ANONYMOUS_TYPE, tc.castType.provisoFree(), tc.getSource().getSourceMarker()), LogPoint.Type.WARN, true));
			}
		}
		
		/* Allow only casting to equal word sizes or from or to void types */
		if ((t != null && t.wordsize() != tc.castType.wordsize()) && !(tc.castType.getCoreType().isVoid() || t.isVoid())) 
			throw new CTEX_EXC(Const.CANNOT_CAST_TO, t.provisoFree(), tc.castType.provisoFree());
		
		tc.setType(tc.castType);
		return tc.castType;
	}
	
	public TYPE checkIDRefWriteback(IDRefWriteback i) throws CTEX_EXC {
		if (i.getShadowRef() instanceof IDRef) {
			IDRef ref = (IDRef) i.getShadowRef();
			i.idRef = ref;
			
			TYPE t = ref.check(this);
			
			if (!t.isPrimitive()) 
				throw new CTEX_EXC(i.idRef.getSource(), Const.CAN_ONLY_APPLY_TO_PRIMITIVE);
			
			i.setType(t);
		}
		else throw new CTEX_EXC(Const.CAN_ONLY_APPLY_TO_IDREF);
		
		return i.getType();
	}
	
	public TYPE checkStructSelectWriteback(StructSelectWriteback i) throws CTEX_EXC {
		if (i.getShadowSelect() instanceof StructSelect) {
			StructSelect ref = (StructSelect) i.getShadowSelect();
			i.select = ref;
			
			TYPE t = ref.check(this);
			
			if (!t.isPrimitive()) 
				throw new CTEX_EXC(i.select.getSource(), Const.CAN_ONLY_APPLY_TO_PRIMITIVE);
			
			i.setType(t);
		}
		else throw new CTEX_EXC(Const.CAN_ONLY_APPLY_TO_IDREF);
		
		return i.getType();
	}
	
	public TYPE checkAssignWriteback(AssignWriteback i) throws CTEX_EXC {
		if (i.reference instanceof IDRefWriteback || i.reference instanceof StructSelectWriteback) 
			i.reference.check(this);
		else throw new CTEX_EXC(Const.CAN_ONLY_APPLY_TO_IDREF);
		return null;
	}
	
	/**
	 * Checks for:<br>
	 * - At least one selection has to be made<br>
	 * - Checks that the shadow ref is an IDRef<br>
	 * - Checks that the types of the selection are of type int<br>
	 * - Amount of selections does not exceed structure dimension<br>
	 */
	public TYPE checkArraySelect(ArraySelect select) throws CTEX_EXC {
		if (select.selection.isEmpty()) 
			throw new CTEX_EXC(Const.ARRAY_SELECT_MUST_HAVE_SELECTION);
		
		Expression ref = null;
		
		if (select.getShadowRef() instanceof IDRef) {
			ref = (IDRef) select.getShadowRef();
			select.idRef = (IDRef) ref;
		}
		else if (select.getShadowRef() instanceof StructSelect) {
			StructSelect ref0 = (StructSelect) select.getShadowRef();
			select.idRef = (IDRef) ref0.selector;
			ref = ref0;
		}
		else throw new CTEX_EXC(select.getShadowRef().getSource(), Const.CAN_ONLY_SELECT_FROM_VARIABLE_REF);
		
		TYPE type0 = ref.check(this);
		
		/* If pointer, unwrap it */
		TYPE chain = (type0.isPointer())? type0.getContainedType() : type0;
		
		/* Check selection chain */
		for (int i = 0; i < select.selection.size(); i++) {
			TYPE stype = select.selection.get(i).check(this);
			if (!(stype instanceof INT)) 
				throw new CTEX_EXC(select.selection.get(i).getSource(), Const.ARRAY_SELECTION_HAS_TO_BE_OF_TYPE, stype.provisoFree());
			else {
				/* Allow to select from array but only in the first selection, since pointer 'flattens' the array structure */
				if (!(chain.isArray() || (i == 0 && (type0.isPointer() || chain.isVoid())))) 
					throw new CTEX_EXC(select.selection.get(i).getSource(), Const.CANNOT_SELECT_FROM_TYPE, type0.provisoFree());
				else if (chain.isArray()) {
					ARRAY arr = (ARRAY) chain;
					
					if (select.selection.get(i) instanceof Atom) {
						Atom a = (Atom) select.selection.get(i);
						int value = (int) a.getType().getValue();
						if (value < 0 || value >= arr.getLength()) 
							throw new CTEX_EXC(select.selection.get(i).getSource(), Const.ARRAY_OUT_OF_BOUNDS, value, chain.provisoFree());
					}
					
					chain = arr.elementType;
				}
				else {
					if (type0.isPointer()) {
						chain = type0.getContainedType();
					}
					else {
						/* When selecting from void, type will stay void */
					}
					
					if (select.selection.size() > 1) 
						throw new CTEX_EXC(select.getShadowRef().getSource(), Const.CAN_ONLY_SELECT_ONCE_FROM_POINTER_OR_VOID);
				}
			}
		}
		
		if (type0.isPointer()) chain = new POINTER(chain);
		
		select.setType(chain);
		return select.getType();
	}
	
	public TYPE checkAtom(Atom a) throws CTEX_EXC {
		return a.getType();
	}
	
	public TYPE checkTempAtom(TempAtom a) throws CTEX_EXC {
		
		/* Override the type type of the base */
		a.setType(a.inheritType);
		
		if (a.base != null) {
			TYPE t = a.base.check(this);
			
			if (t.wordsize() > a.inheritType.wordsize()) 
				throw new CTEX_EXC(Const.TYPE_CANNOT_BE_ALIGNED_TO, t.provisoFree(), a.inheritType.provisoFree());
		}
		
		return a.getType();
	}
	
	public TYPE checkRegisterAtom(RegisterAtom a) throws CTEX_EXC {
		String reg = a.spelling.toLowerCase();
		
		REG reg0 = RegOp.convertStringToReg(reg);
		
		if (reg0 == null) 
			throw new CTEX_EXC(Const.UNKNOWN_REGISTER, reg);
		else a.reg = reg0;
		
		return a.getType();
	}
	
	public TYPE checkDirectASMStatement(DirectASMStatement d) throws CTEX_EXC {
		for (Pair<Expression, REG> p : d.dataIn) {
			TYPE t = p.first.check(this);
			
			if (t.wordsize() > 1) 
				throw new CTEX_EXC(p.first.getSource(), Const.ONLY_APPLICABLE_FOR_ONE_WORD_TYPE_ACTUAL, t.provisoFree());
		}
		
		for (Pair<Expression, REG> p : d.dataOut) {
			TYPE t = p.first.check(this);
			
			if (!(p.first instanceof IDRef)) 
				throw new CTEX_EXC(p.first.getSource(), Const.EXPECTED_IDREF_ACTUAL, p.first.getClass().getName());
			
			if (t.wordsize() > 1) 
				throw new CTEX_EXC(p.first.getSource(), Const.ONLY_APPLICABLE_FOR_ONE_WORD_TYPE_ACTUAL, t.provisoFree());
		}
		
		if (d.dataOut.isEmpty()) {
			if (!CompilerDriver.disableWarnings) 
				messages.add(new Message(String.format(Const.DIRECT_ASM_HAS_NO_OUTPUTS, d.getSource().getSourceMarker()), LogPoint.Type.WARN, true));
		}
		
		return new VOID();
	}
	
	
			/* ---< HELPER METHODS >--- */
	/**
	 * Attempts to figure out the provided proviso.
	 * 
	 * @param targetProviso The proviso header of the target.
	 * @param expectedTypes The parameter types of the target.
	 * @param providedTypes The provided parameter types.
	 * @return The missing proviso mapping for the target.
	 * @throws CTEX_EXC If the mapping is not clear or a mapping cannot be determined.
	 */
	public List<TYPE> autoProviso(List<TYPE> targetProviso, List<TYPE> expectedTypes, List<TYPE> providedTypes, Source source) throws CTEX_EXC {
		List<TYPE> foundMapping = new ArrayList();
		
		if (expectedTypes.size() != providedTypes.size())
			throw new CTEX_EXC(source, Const.MISSMATCHING_NUMBER_OF_PROVISOS, expectedTypes.size(), providedTypes.size());
			
		for (int i = 0; i < targetProviso.size(); i++) {
			PROVISO prov = (PROVISO) targetProviso.get(i);
			prov.releaseContext();
			
			int ind = -1;
			TYPE mapped = null;
			
			for (int a = 0; a < expectedTypes.size(); a++) {
				/* Found a parameter type that holds the searched proviso */
				TYPE map0 = expectedTypes.get(a).mappable(providedTypes.get(a), prov.placeholderName);
				
				if (map0 != null) {
					if (mapped == null) {
						mapped = map0;
						ind = a;
					}
					else {
						if (!mapped.provisoFree().typeString().equals(map0.provisoFree().typeString())) 
							/* Found two possible types for proviso, abort */
							throw new CTEX_EXC(source, Const.MULTIPLE_AUTO_MAPS_FOR_PROVISO, prov.placeholderName, mapped.provisoFree(), ind + 1, map0.provisoFree(), a + 1);
					}
				}
			}
			
			if (mapped == null) 
				/* None of the types held the searched proviso, proviso cannot be auto-ed, abort. */
				throw new CTEX_EXC(source, Const.CANNOT_AUTO_MAP_PROVISO, prov);
			
			foundMapping.add(mapped.clone());
		}
		
		return foundMapping;
	}
	
	/**
	 * Attempts to find a field in the given struct type where the name matches the name specified by
	 * the IDRef. If such a field is found, the origin of the IDRef is set to the declaration of this
	 * field. Additionally, the type of the IDRef is set to the field type. Then the field type is returned.
	 * 
	 * @param struct The struct type to search in.
	 * @param ref0 The IDRef that specifies the field name.
	 * @return The type of the field that was found.
	 * @throws CTEX_EXC Thrown when the field name does not exist.
	 */
	private TYPE findAndLinkField(STRUCT struct, IDRef ref0) throws CTEX_EXC {
		Declaration field = struct.getField(ref0.path);
		
		/* The ID the current selection targets */
		if (field != null) {
			/* Link manually, identifier is not part of current scope */
			ref0.origin = field;
			ref0.setType(ref0.origin.getType());
			
			/* Next type in chain */
			return ref0.getType();
		}
		else throw new CTEX_EXC(ref0.getSource(), Const.FIELD_NOT_IN_STRUCT, ref0.path, struct.provisoFree());
	}
	
	/**
	 * Checks for a modifier violation. This is done by constructing the path of the current function and
	 * comparing it to the given path. If the modifier is SHARED or STATIC, no checks are required. If it is 
	 * RESTRICTED, check if the path of the current function is contained in the subtree of the given namespace
	 * path. If it is EXCLUSIVE, check for strict equality.
	 * 
	 * @param mod The modifier to check.
	 * @param path The path of the ressource accessed.
	 * @param source The source of the AST node that initiated the check.
	 * @throws CTEX_EXC Thrown if a modifier violation is detected.
	 */
	public void checkModifier(MODIFIER mod, NamespacePath path, Source source) throws CTEX_EXC {
		String currentPath = (this.currentFunction.isEmpty())? "" : this.currentFunction.peek().path.buildPathOnly();
		
		if (mod == MODIFIER.SHARED || mod == MODIFIER.STATIC) return;
		else if (mod == MODIFIER.RESTRICTED) {
			if (!currentPath.startsWith(path.buildPathOnly())) {
				if (CompilerDriver.disableModifiers) {
					if (!CompilerDriver.disableWarnings) 
						this.messages.add(new Message(String.format(Const.MODIFIER_VIOLATION_AT, path, this.currentFunction.peek().path, source.getSourceMarker()), LogPoint.Type.WARN, true));
				}
				else throw new CTEX_EXC(source, Const.MODIFIER_VIOLATION, path, this.currentFunction.peek().path);
			}
		}
		else if (mod == MODIFIER.EXCLUSIVE) {
			if (!currentPath.equals(path.buildPathOnly())) {
				if (CompilerDriver.disableModifiers) {
					if (!CompilerDriver.disableWarnings) 
						this.messages.add(new Message(String.format(Const.MODIFIER_VIOLATION_AT, path, this.currentFunction.peek().path, source.getSourceMarker()), LogPoint.Type.WARN, true));
				}
				else throw new CTEX_EXC(source, Const.MODIFIER_VIOLATION, path, this.currentFunction.peek().path);
			}
		}
	}
	
	/**
	 * Check if given child is a polymorph child of the target. Return true if this is the case,
	 * return false in any other case or if the target is not a struct.
	 * 
	 * @param child The Type that is checked to be a child of target.
	 * @param target The Type that acts as the parent of the child.
	 * @throws CTEX_EXC
	 */
	public boolean checkPolymorphViolation(TYPE child, TYPE target) {
		if (!target.isStruct()) return false;
		
		if (!child.isStruct()) return false;
		
		if (child.getCoreType().isStruct()) {
			if (((STRUCT) child.getCoreType()).isPolymorphTo(target) && 
				!((STRUCT) child).getTypedef().equals(((STRUCT) target).getTypedef())) {
				return true;
			}
		}
		
		return false;
	}
	
	/**
	 * Check if the top layer of the signal stack contains given type.
	 * 
	 * @param newSignal The type to check if it is contained in the signal stack.
	 * @return True if it is contained, false if not.
	 */
	public boolean signalStackContains(TYPE newSignal) {
		return this.signalStack.peek().stream().filter(x -> x.isEqual(newSignal)).count() > 0;
	}
	
	/**
	 * Attempts to find the function that has the given namespace path. Also checks func 
	 * predicate declarations. If more than one match is found, an exception is thrown. If
	 * no match is found, an exception is thrown. If exactly one match is found, the found
	 * function is returned.
	 * 
	 * @param path The namespace path of the searched function.
	 * @param source The location of the AST node that initiated the check.
	 * @param isPredicate Set to true when checking a function ref. Variates the error message.
	 * @return The found function.
	 * @throws CTEX_EXC Thrown if no or multiple matches for the function are found.
	 */
	public Function findFunction(NamespacePath path, Source source, boolean isPredicate, String prefix, List<TYPE> types) throws CTEX_EXC {
		
		/* Collect functions that match this namespace path, start with exact namespace path matches */
		List<Function> funcs = this.functions.stream()
			.filter(x -> x.path.equals(path)).collect(Collectors.toList());
		
		Function f0 = this.filterFromFunctions(funcs, path, prefix, types);
		if (f0 != null) return f0;
		
		funcs.clear();
		
		/* Search through the registered function declarations, but only match the end of the namespace path */
		for (Function f : this.functions) {
			if (f.path.build().endsWith(path.build())) {
				String p0 = f.path.build();
				String p1 = path.build();
				
				if (p0.length() == p1.length() || p0.substring(0, p0.length() - p1.length()).endsWith("."))
					funcs.add(f);
			}
		}
			
		/* Search through predicate declarations */
		if (!this.currentFunction.isEmpty()) for (Declaration d : this.currentFunction.peek().parameters) {
			if (d.getType() instanceof FUNC) {
				FUNC f = (FUNC) d.getType();
				
				if (f.funcHead != null) 
					f.funcHead.lambdaDeclaration = d;
				
				if (d.path.getLast().equals(path.getLast())) 
					funcs.add(f.funcHead);
			}
		}
		
		if (funcs.isEmpty()) 
			/* Return if there is only one result */
			return null;
		else if (funcs.size() == 1) 
			/* Found one match, return this match */
			return funcs.get(0);
		else {
			Function f = this.filterFromFunctions(funcs, path, prefix, types);
			if (f != null) return f;
			
			/* Multiple results, cannot determine correct one, throw an exception */
			String s = funcs.stream().map(x -> x.path.build()).collect(Collectors.joining(", "));
			throw new CTEX_EXC(source, Const.MULTIPLE_MATCHES_FOR_X, ((isPredicate)? "predicate" : "function"), path, s);
		}
	}
	
	public Function filterFromFunctions(List<Function> funcs, NamespacePath path, String prefix, List<TYPE> types) {
		if (funcs.isEmpty()) 
			/* Return if there is only one result */
			return null;
		else if (funcs.size() == 1) 
			/* Found one match, return this match */
			return funcs.get(0);
		
		
		/* Check for match with prefix */
		List<Function> prefixMatchers = new ArrayList();
		for (Function f0 : funcs) 
			if (f0.path.build().endsWith(prefix + "." + path.build()))
				prefixMatchers.add(f0);
		
		/* Only one prefix matcher, found our function */
		if (prefixMatchers.size() == 1) return prefixMatchers.get(0);

		
		/* At this point, functions can only be differentiated by the parameters. All require the UID in the label. */
		funcs.stream().forEach(x -> x.requireUIDInLabel = true);
		
		/* Check for match with parameters */
		if (types != null) {
			List<Function> filtered = new ArrayList();
			for (Function f0 : funcs) {
				if (f0.parameters.size() == types.size()) {
					boolean match = true;
					for (int i = 0; i < types.size(); i++) 
						if (types.get(i) != null) 
							match &= f0.parameters.get(i).getType().isEqual(types.get(i));
					
					if (match)
						filtered.add(f0);
				}
			}
			
			if (filtered.size() == 1) return filtered.get(0);
		}
		

		/* Check for match with parameters and prefix matches */
		if (types != null) {
			List<Function> filtered = new ArrayList();
			for (Function f0 : prefixMatchers) {
				if (f0.parameters.size() == types.size()) {
					boolean match = true;
					for (int i = 0; i < types.size(); i++) 
						if (types.get(i) != null) 
							match &= f0.parameters.get(i).getType().isEqual(types.get(i));
					
					if (match)
						filtered.add(f0);
				}
			}
			
			if (filtered.size() == 1) return filtered.get(0);
		}
		
		/* No match for the given functions, or multiple matches. */
		return null;
	}
	
	/**
	 * Attempts to find the function with the name of the callee namespace path. If the function is not
	 * found, it is searched as a predicate. If the called function is a predicate, and the predicate is
	 * not anonymous, the provisos are overridden by the provisos of the predicate. Also, the anontarget
	 * is set.
	 * 
	 * @param c The callee that called the function.
	 * @return The found function.
	 * @throws CTEX_EXC Thrown if the callee has provisos in a predicate call, or if the function cannot be found.
	 */
	public Function searchFunction(Callee c, List<TYPE> types) throws CTEX_EXC {
		List<TYPE> proviso = c.getProviso();

		/* Extract path from typedef for more extensive function searching */
		String prefix = (c.isNestedCall())? this.getPath(c.getParams().get(0)) : "";
		
		/* Find the called function */
		Function f = this.findFunction(c.getPath(), c.getCallee().getSource(), false, prefix, types);
		
		Declaration anonTarget = null;
		
		/* Function not found, may be a lambda call */
		if (f == null) {
			anonTarget = this.scopes.peek().getFieldNull(c.getPath(), c.getCallee().getSource());
			
			/* Found target as predicate, predicate is not anonymous */
			if (anonTarget != null && anonTarget.getType() instanceof FUNC) {
				FUNC f0 = (FUNC) anonTarget.getType();
				
				/* Provisos of call must be empty in case of predicate. */
				if (!proviso.isEmpty()) 
					throw new CTEX_EXC(c.getCallee().getSource(), Const.PROVISO_ARE_PROVIDED_BY_PREDICATE, anonTarget.path);
				
				/* Proviso types are provided through lambda */
				proviso = f0.proviso;
				
				/* Set found function to function head */
				f = f0.funcHead;
				
				if (f == null) {
					/* Anonymous function head */
					if (!CompilerDriver.disableWarnings) 
						this.messages.add(new Message(String.format(Const.PREDICATE_IS_ANONYMOUS, c.getPath().build(), c.getCallee().getSource().getSourceMarker()), LogPoint.Type.WARN, true));
				}
			}
		}
		
		
		/* Neither regular function or predicate was found, undefined */
		if (f == null && anonTarget == null) 
			throw new CTEX_EXC(c.getCallee().getSource(), Const.UNDEFINED_FUNCTION_OR_PREDICATE, c.getPath().build());
		
		/* Write back anon target and provisos */
		c.setProviso(proviso);
		c.setAnonTarget(anonTarget);
		
		return f;
	}

	/**
	 * Return the path of either the struct or interface typedef.
	 * @param e Expression to extract path from, type of expression must either be STRUCT or INTERFACE.
	 * @return The extracted and built namespace path.
	 */
	public String getPath(Expression e) throws CTEX_EXC {
		if (e.check(this).getCoreType().isStruct()) {
			STRUCT s = (STRUCT) e.check(this).getCoreType();
			return s.getTypedef().path.build();
		}
		else {
			INTERFACE s = (INTERFACE) e.check(this).getCoreType();
			return s.getTypedef().path.build();
		}
	}
	
	public void checkBody(List<Statement> body, boolean pushScope, boolean loopedScope) throws CTEX_EXC {
		if (pushScope) this.scopes.push(new Scope(this.scopes.peek(), loopedScope));
		for (Statement s : body) {
			currentStatement = s;
			s.check(this);
		}
		if (pushScope) this.scopes.pop();
	}
	
	/**
	 * Returns a list of all proviso types that are in the first given list, 
	 * but are not in the second list.
	 * 
	 * @param expectedProviso The proviso types that are required for an operation.
	 * @param providedProviso The proviso types that were provided.
	 * @return
	 */
	public List<TYPE> missingProvisoTypes(List<TYPE> expectedProviso, List<TYPE> providedProviso) {
		List<TYPE> missing = providedProviso.stream().map(TYPE::clone).collect(Collectors.toList());
		
		for (int i = 0; i < missing.size(); i++) {
			for (int a = 0; a < expectedProviso.size(); a++) {
				if (((PROVISO) missing.get(i)).placeholderName.equals(((PROVISO) expectedProviso.get(a)).placeholderName)) {
					missing.remove(i);
					i--;
					break;
				}
			}
		}
		
		return missing;
	}
	
	public void pushTrace(SyntaxElement s) {
		this.stackTrace.push(s);
	}
	
	public void popTrace() {
		this.stackTrace.pop();
	}
	
	/**
	 * Returns either the current function or null
	 * if the currentFunction-stack is empty.
	 */
	public Function getCurrentFunction() {
		if (this.currentFunction.isEmpty()) return null;
		else return this.currentFunction.peek();
	}
	
} 
