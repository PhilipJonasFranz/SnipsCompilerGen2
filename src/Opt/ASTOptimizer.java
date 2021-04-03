package Opt;

import java.util.List;

import Exc.OPT0_EXC;
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
import Imm.AST.Expression.RegisterAtom;
import Imm.AST.Expression.SizeOfExpression;
import Imm.AST.Expression.SizeOfType;
import Imm.AST.Expression.StructSelect;
import Imm.AST.Expression.StructSelectWriteback;
import Imm.AST.Expression.StructureInit;
import Imm.AST.Expression.TempAtom;
import Imm.AST.Expression.TypeCast;
import Imm.AST.Expression.Arith.Add;
import Imm.AST.Expression.Arith.BitAnd;
import Imm.AST.Expression.Arith.BitNot;
import Imm.AST.Expression.Arith.BitOr;
import Imm.AST.Expression.Arith.BitXor;
import Imm.AST.Expression.Arith.Lsl;
import Imm.AST.Expression.Arith.Lsr;
import Imm.AST.Expression.Arith.Mul;
import Imm.AST.Expression.Arith.Sub;
import Imm.AST.Expression.Arith.UnaryMinus;
import Imm.AST.Expression.Boolean.And;
import Imm.AST.Expression.Boolean.Compare;
import Imm.AST.Expression.Boolean.Not;
import Imm.AST.Expression.Boolean.Or;
import Imm.AST.Expression.Boolean.Ternary;
import Imm.AST.Lhs.ArraySelectLhsId;
import Imm.AST.Lhs.LhsId;
import Imm.AST.Lhs.PointerLhsId;
import Imm.AST.Lhs.SimpleLhsId;
import Imm.AST.Lhs.StructSelectLhsId;
import Imm.AST.Statement.AssignWriteback;
import Imm.AST.Statement.Assignment;
import Imm.AST.Statement.BreakStatement;
import Imm.AST.Statement.CaseStatement;
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
import Imm.AST.Typedef.EnumTypedef;
import Imm.AST.Typedef.InterfaceTypedef;
import Imm.AST.Typedef.StructTypedef;

public class ASTOptimizer {

	public Program optProgram(Program AST) {
		for (SyntaxElement s : AST.programElements) {
			try {
				s.opt(this);
			} catch (OPT0_EXC e) {
				e.printStackTrace();
			}
		}
		
		return AST;
	}
	
	public Function optFunction(Function f) throws OPT0_EXC {
		f.body = this.optBody(f.body);
		return f;
	}
	
	public List<Statement> optBody(List<Statement> body) throws OPT0_EXC {
		for (int i = 0; i < body.size(); i++) {
			Statement s = body.get(i);
			Statement s0 = s.opt(this);
			
			if (s0 == null) {
				body.remove(i);
				i--;
			}
			else {
				body.set(body.indexOf(s), s0);
			}
		}
		
		return body;
	}

	public Expression optAddressOf(AddressOf addressOf) throws OPT0_EXC {
		return addressOf;
	}

	public Expression optArrayInit(ArrayInit arrayInit) throws OPT0_EXC {
		return arrayInit;
	}

	public Expression optArraySelect(ArraySelect arraySelect) throws OPT0_EXC {
		return arraySelect;
	}

	public Expression optAtom(Atom atom) throws OPT0_EXC {
		return atom;
	}

	public Expression optDeref(Deref deref) throws OPT0_EXC {
		return deref;
	}

	public Expression optFunctionRef(FunctionRef functionRef) throws OPT0_EXC {
		return functionRef;
	}

	public Expression optIDOfExpression(IDOfExpression idOfExpression) throws OPT0_EXC {
		return idOfExpression;
	}

	public Expression optIDRef(IDRef idRef) throws OPT0_EXC {
		return idRef;
	}

	public Expression optIDRefWriteback(IDRefWriteback idRefWriteback) throws OPT0_EXC {
		return idRefWriteback;
	}

	public Expression optInlineCall(InlineCall inlineCall) throws OPT0_EXC {
		return inlineCall;
	}

	public Expression optInlineFunction(InlineFunction inlineFunction) throws OPT0_EXC {
		return inlineFunction;
	}

	public Expression optRegisterAtom(RegisterAtom registerAtom) throws OPT0_EXC {
		return registerAtom;
	}

	public Expression optSizeOfExpression(SizeOfExpression sizeOfExpression) throws OPT0_EXC {
		return sizeOfExpression;
	}

	public Expression optSizeOfType(SizeOfType sizeOfType) throws OPT0_EXC {
		return sizeOfType;
	}

	public Expression optStructSelect(StructSelect structSelect) throws OPT0_EXC {
		return structSelect;
	}

	public Expression optStructSelectWriteback(StructSelectWriteback structSelectWriteback) throws OPT0_EXC {
		return structSelectWriteback;
	}

	public Expression optStructureInit(StructureInit structureInit) throws OPT0_EXC {
		return structureInit;
	}

	public Expression optTempAtom(TempAtom tempAtom) throws OPT0_EXC {
		tempAtom.base = tempAtom.base.opt(this);
		return tempAtom;
	}

	public Expression optTypeCast(TypeCast typeCast) throws OPT0_EXC {
		typeCast.expression = typeCast.expression.opt(this);
		return typeCast;
	}

	public Expression optAdd(Add add) throws OPT0_EXC {
		add.left = add.left.opt(this);
		add.right = add.right.opt(this);
		return add;
	}

	public Expression optBitAnd(BitAnd bitAnd) throws OPT0_EXC {
		bitAnd.left = bitAnd.left.opt(this);
		bitAnd.right = bitAnd.right.opt(this);
		return bitAnd;
	}

	public Expression optBitNot(BitNot bitNot) throws OPT0_EXC {
		bitNot.operand = bitNot.operand.opt(this);
		return bitNot;
	}

	public Expression optBitOr(BitOr bitOr) throws OPT0_EXC {
		bitOr.left = bitOr.left.opt(this);
		bitOr.right = bitOr.right.opt(this);
		return bitOr;
	}

	public Expression optBitXor(BitXor bitXor) throws OPT0_EXC {
		bitXor.left = bitXor.left.opt(this);
		bitXor.right = bitXor.right.opt(this);
		return bitXor;
	}

	public Expression optLsl(Lsl lsl) throws OPT0_EXC {
		lsl.left = lsl.left.opt(this);
		lsl.right = lsl.right.opt(this);
		return lsl;
	}

	public Expression optLsr(Lsr lsr) throws OPT0_EXC {
		lsr.left = lsr.left.opt(this);
		lsr.right = lsr.right.opt(this);
		return lsr;
	}

	public Expression optMul(Mul mul) throws OPT0_EXC {
		mul.left = mul.left.opt(this);
		mul.right = mul.right.opt(this);
		return mul;
	}
	
	public Expression optSub(Sub s) throws OPT0_EXC {
		s.left = s.left.opt(this);
		s.right = s.right.opt(this);
		
		/* -4 - -5 = 5 - 4 = 1 */
		if (s.left instanceof UnaryMinus && s.right instanceof UnaryMinus) {
			UnaryMinus left = (UnaryMinus) s.left;
			UnaryMinus right = (UnaryMinus) s.right;
			
			s.left = right.getOperand();
			s.right = left.getOperand();
		}
		/* 4 - -5 = 4 + 5 = 9 */
		else if (s.right instanceof UnaryMinus) {
			UnaryMinus right = (UnaryMinus) s.right;
			return new Add(s.left, right.getOperand(), s.getSource());
		}
		
		return s;
	}
	
	public Expression optUnaryMinus(UnaryMinus m) throws OPT0_EXC {
		m.operand = m.operand.opt(this);
		
		if (m.getOperand() instanceof UnaryMinus) {
			UnaryMinus m0 = (UnaryMinus) m.getOperand();
			return m0.getOperand();
		}
		
		return m;
	}

	public Expression optAnd(And and) throws OPT0_EXC {
		and.left = and.left.opt(this);
		and.right = and.right.opt(this);
		return and;
	}

	public Expression optCompare(Compare compare) throws OPT0_EXC {
		compare.left = compare.left.opt(this);
		compare.right = compare.right.opt(this);
		return compare;
	}

	public Expression optNot(Not not) throws OPT0_EXC {
		not.operand = not.operand.opt(this);		
		return not;
	}

	public Expression optOr(Or or) throws OPT0_EXC {
		or.left = or.left.opt(this);
		or.right = or.right.opt(this);
		return or;
	}

	public Expression optTernary(Ternary ternary) throws OPT0_EXC {
		ternary.condition = ternary.condition.opt(this);
		ternary.left = ternary.left.opt(this);
		ternary.right = ternary.right.opt(this);
		return ternary;
	}

	public LhsId optArraySelectLhsId(ArraySelectLhsId arraySelectLhsId) throws OPT0_EXC {
		return arraySelectLhsId;
	}

	public LhsId optPointerLhsId(PointerLhsId pointerLhsId) throws OPT0_EXC {
		return pointerLhsId;
	}

	public LhsId optSimpleLhsId(SimpleLhsId simpleLhsId) throws OPT0_EXC {
		return simpleLhsId;
	}

	public LhsId optStructSelectLhsId(StructSelectLhsId structSelectLhsId) throws OPT0_EXC {
		return structSelectLhsId;
	}

	public Statement optAssignment(Assignment assignment) throws OPT0_EXC {
		return assignment;
	}

	public Statement optAssignWriteback(AssignWriteback assignWriteback) throws OPT0_EXC {
		return assignWriteback;
	}

	public Statement optBreakStatement(BreakStatement breakStatement) throws OPT0_EXC {
		return breakStatement;
	}

	public Statement optCaseStatement(CaseStatement caseStatement) throws OPT0_EXC {
		return caseStatement;
	}

	public Statement optContinueStatement(ContinueStatement continueStatement) throws OPT0_EXC {
		return continueStatement;
	}

	public Statement optDeclaration(Declaration declaration) throws OPT0_EXC {
		return declaration;
	}

	public Statement optDefaultStatement(DefaultStatement defaultStatement) throws OPT0_EXC {
		return defaultStatement;
	}

	public Statement optDirectASMStatement(DirectASMStatement directASMStatement) throws OPT0_EXC {
		return directASMStatement;
	}

	public Statement optDoWhileStatement(DoWhileStatement doWhileStatement) throws OPT0_EXC {
		return doWhileStatement;
	}

	public Statement optForEachStatement(ForEachStatement forEachStatement) throws OPT0_EXC {
		return forEachStatement;
	}

	public Statement optForStatement(ForStatement forStatement) throws OPT0_EXC {
		return forStatement;
	}

	public Statement optFunctionCall(FunctionCall functionCall) throws OPT0_EXC {
		return functionCall;
	}

	public Statement optIfStatement(IfStatement ifStatement) throws OPT0_EXC {
		return ifStatement;
	}

	public Statement optReturnStatement(ReturnStatement returnStatement) throws OPT0_EXC {
		return returnStatement;
	}

	public Statement optSignalStatement(SignalStatement signalStatement) throws OPT0_EXC {
		return signalStatement;
	}

	public Statement optSwitchStatement(SwitchStatement switchStatement) throws OPT0_EXC {
		return switchStatement;
	}

	public Statement optTryStatement(TryStatement tryStatement) throws OPT0_EXC {
		return tryStatement;
	}

	public Statement optWatchStatement(WatchStatement watchStatement) throws OPT0_EXC {
		return watchStatement;
	}

	public Statement optWhileStatement(WhileStatement whileStatement) throws OPT0_EXC {
		return whileStatement;
	}

	public SyntaxElement optEnumTypedef(EnumTypedef enumTypedef) throws OPT0_EXC {
		return enumTypedef;
	}

	public SyntaxElement optInterfaceTypedef(InterfaceTypedef interfaceTypedef) throws OPT0_EXC {
		return interfaceTypedef;
	}

	public SyntaxElement optStructTypedef(StructTypedef structTypedef) throws OPT0_EXC {
		return structTypedef;
	}
	
}
