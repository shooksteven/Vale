package net.verdagon.vale.templar

import net.verdagon.vale.astronomer.IVarNameA
import net.verdagon.vale.scout.RangeS
import net.verdagon.vale.templar.env.{ILocalVariable2, ReferenceLocalVariable2}
import net.verdagon.vale.templar.templata._
import net.verdagon.vale.templar.types._
import net.verdagon.vale.{vassert, vfail, vpass, vwat}

trait IExpressionResult2 extends Queriable2 {
  def expectReference(): ReferenceResult2 = {
    this match {
      case r @ ReferenceResult2(_) => r
      case AddressResult2(_) => vfail("Expected a reference as a result, but got an address!")
    }
  }
  def expectAddress(): AddressResult2 = {
    this match {
      case a @ AddressResult2(_) => a
      case ReferenceResult2(_) => vfail("Expected an address as a result, but got a reference!")
    }
  }
  def underlyingReference: Coord
  def referend: Kind
}
case class AddressResult2(reference: Coord) extends IExpressionResult2 {
  override def underlyingReference: Coord = reference
  override def referend = reference.referend
  def all[T](func: PartialFunction[Queriable2, T]): List[T] = {
    List(this).collect(func) ++ reference.all(func)
  }
}
case class ReferenceResult2(reference: Coord) extends IExpressionResult2 {
  override def underlyingReference: Coord = reference
  override def referend = reference.referend
  def all[T](func: PartialFunction[Queriable2, T]): List[T] = {
    List(this).collect(func) ++ reference.all(func)
  }
}
trait Expression2 extends Queriable2 {
  def resultRegister: IExpressionResult2
  def referend: Kind
}
trait ReferenceExpression2 extends Expression2 {
  override def resultRegister: ReferenceResult2
  override def referend = resultRegister.reference.referend
}
// This is an Expression2 because we sometimes take an address and throw it
// directly into a struct (closures!), which can have addressible members.
trait AddressExpression2 extends Expression2 {
  override def resultRegister: AddressResult2
  override def referend = resultRegister.reference.referend

  def range: RangeS

  // Whether or not we can change where this address points to
  def variability: Variability
}

case class LetAndLend2(
    variable: ILocalVariable2,
    expr: ReferenceExpression2
) extends ReferenceExpression2 {
  vassert(variable.reference == expr.resultRegister.reference)

  override def resultRegister: ReferenceResult2 = {
    val Coord(ownership, permission, kind) = expr.resultRegister.reference
    ReferenceResult2(Coord(if (ownership == Share) Share else Constraint, permission, kind))
  }

  def all[T](func: PartialFunction[Queriable2, T]): List[T] = {
    List(this).collect(func) ++ expr.all(func)
  }
}

case class NarrowPermission2(
    expr: ReferenceExpression2,
    targetPermission: Permission
) extends ReferenceExpression2 {
  expr.resultRegister.reference.ownership match {
    case Own => vfail() // This only works on non owning references
    case Share => vfail() // Share only has readonly
    case Constraint | Weak => // fine
  }
  // Only thing we support so far is Readwrite -> Readonly
  vassert(expr.resultRegister.reference.permission == Readwrite)
  vassert(targetPermission == Readonly)

  override def resultRegister: ReferenceResult2 = {
    val Coord(ownership, permission, kind) = expr.resultRegister.reference
    ReferenceResult2(Coord(ownership, targetPermission, kind))
  }

  def all[T](func: PartialFunction[Queriable2, T]): List[T] = {
    List(this).collect(func) ++ expr.all(func)
  }
}

case class LockWeak2(
  innerExpr: ReferenceExpression2,
  // We could just calculate this, but it feels better to let the StructTemplar
  // make it, so we're sure it's created.
  resultOptBorrowType: Coord,

  // Function to give a borrow ref to to make a Some(borrow ref)
  someConstructor: Prototype2,
  // Function to make a None of the right type
  noneConstructor: Prototype2,
) extends ReferenceExpression2 {
  override def resultRegister: ReferenceResult2 = {
    ReferenceResult2(resultOptBorrowType)
  }

  def all[T](func: PartialFunction[Queriable2, T]): List[T] = {
    List(this).collect(func) ++ resultOptBorrowType.all(func)
  }
}

// Turns a constraint ref into a weak ref
// Note that we can also get a weak ref from LocalLoad2'ing a
// constraint ref local into a weak ref.
case class WeakAlias2(
  innerExpr: ReferenceExpression2
) extends ReferenceExpression2 {
  vassert(innerExpr.resultRegister.reference.ownership == Constraint)

  override def resultRegister: ReferenceResult2 = {
    ReferenceResult2(Coord(Weak, innerExpr.resultRegister.reference.permission, innerExpr.referend))
  }

  def all[T](func: PartialFunction[Queriable2, T]): List[T] = {
    List(this).collect(func) ++ innerExpr.all(func)
  }
}

case class LetNormal2(
    variable: ILocalVariable2,
    expr: ReferenceExpression2
) extends ReferenceExpression2 {
  override def resultRegister = ReferenceResult2(Coord(Share, Readonly, Void2()))

  expr match {
    case Return2(_) => vwat()
    case _ =>
  }

  def all[T](func: PartialFunction[Queriable2, T]): List[T] = {
    List(this).collect(func) ++ expr.all(func)
  }
}

// Only ExpressionTemplar.unletLocal should make these
case class Unlet2(variable: ILocalVariable2) extends ReferenceExpression2 {
  override def resultRegister = ReferenceResult2(variable.reference)

  def all[T](func: PartialFunction[Queriable2, T]): List[T] = {
    List(this).collect(func) ++ variable.reference.all(func)
  }
}

// Throws away a reference.
// Unless given to an instruction which consumes it, all borrow and share
// references must eventually hit a Discard2, just like all owning
// references must eventually hit a Destructure2.
// Depending on the backend, it will either be a no-op (like for GC'd backends)
// or a decrement+maybedestruct (like for RC'd backends)
// See DINSIE for why this isnt three instructions, and why we dont have the
// destructor in here for shareds.
case class Discard2(
  expr: ReferenceExpression2
) extends ReferenceExpression2 {
  override def resultRegister = ReferenceResult2(Coord(Share, Readonly, Void2()))

  expr.resultRegister.reference.ownership match {
    case Constraint =>
    case Share =>
    case Weak =>
  }

  expr match {
    case Consecutor2(exprs) => {
      exprs.last match {
        case Discard2(_) => vwat()
        case _ =>
      }
    }
    case _ =>
  }

  def all[T](func: PartialFunction[Queriable2, T]): List[T] = {
    List(this).collect(func) ++ expr.all(func)
  }
}

case class Defer2(
  innerExpr: ReferenceExpression2,
  // Every deferred expression should discard its result, IOW, return Void.
  deferredExpr: ReferenceExpression2
) extends ReferenceExpression2 {

  override def resultRegister = ReferenceResult2(innerExpr.resultRegister.reference)

  vassert(deferredExpr.resultRegister.reference == Coord(Share, Readonly, Void2()))

  def all[T](func: PartialFunction[Queriable2, T]): List[T] = {
    List(this).collect(func) ++ innerExpr.all(func) ++ deferredExpr.all(func)
  }
}


// Eventually, when we want to do if-let, we'll have a different construct
// entirely. See comment below If2.
// These are blocks because we don't want inner locals to escape.
case class If2(
    condition: ReferenceExpression2,
    thenCall: ReferenceExpression2,
    elseCall: ReferenceExpression2) extends ReferenceExpression2 {
  private val conditionResultCoord = condition.resultRegister.reference
  private val thenResultCoord = thenCall.resultRegister.reference
  private val elseResultCoord = elseCall.resultRegister.reference

  vassert(conditionResultCoord == Coord(Share, Readonly, Bool2()))
  vassert(
    thenResultCoord.referend == Never2() ||
      elseResultCoord.referend == Never2() ||
      thenResultCoord == elseResultCoord)

  private val commonSupertype =
    if (thenResultCoord.referend == Never2()) {
      elseResultCoord
    } else {
      thenResultCoord
    }

  override def resultRegister = ReferenceResult2(commonSupertype)

  def all[T](func: PartialFunction[Queriable2, T]): List[T] = {
    List(this).collect(func) ++ condition.all(func) ++ thenCall.all(func) ++ elseCall.all(func)
  }
}

// case class IfLet2
// This would check whether:
// - The nullable condition expression evaluates to not null, or
// - The interface condition expression evaluates to the specified sub-citizen
// It would have to use a new chunk of PatternTemplar which produces an
// expression which is a ton of if-statements and try-cast things and assigns
// variables, and puts the given body inside all that.


// The block is expected to return a boolean (false = stop, true = keep going).
// The block will probably contain an If2(the condition, the body, false)
case class While2(block: Block2) extends ReferenceExpression2 {
  override def resultRegister = ReferenceResult2(Coord(Share, Readonly, Void2()))

  def all[T](func: PartialFunction[Queriable2, T]): List[T] = {
    List(this).collect(func) ++ block.all(func)
  }
}

case class Mutate2(
  destinationExpr: AddressExpression2,
  sourceExpr: ReferenceExpression2
) extends ReferenceExpression2 {
  override def resultRegister = ReferenceResult2(destinationExpr.resultRegister.reference)

  def all[T](func: PartialFunction[Queriable2, T]): List[T] = {
    List(this).collect(func) ++ destinationExpr.all(func) ++ sourceExpr.all(func)
  }
}


case class Return2(
  sourceExpr: ReferenceExpression2
) extends ReferenceExpression2 {
  override def resultRegister = ReferenceResult2(Coord(Share, Readonly, Never2()))

  def getFinalExpr(expression2: Expression2): Unit = {
    expression2 match {
      case Block2(exprs) => getFinalExpr(exprs.last)
    }
  }

  def all[T](func: PartialFunction[Queriable2, T]): List[T] = {
    List(this).collect(func) ++ sourceExpr.all(func)
  }
}


//case class CurriedFuncH(closureExpr: ExpressionH, funcName: String) extends ExpressionH

// when we make a closure, we make a struct full of pointers to all our variables
// and the first element is our parent closure
// this can live on the stack, since blocks are limited to this expression
// later we can optimize it to only have the things we use

// Block2 is required to unlet all the variables it introduces.
case class Block2(
    exprs: List[ReferenceExpression2]
) extends ReferenceExpression2 {

  vassert(exprs.last.isInstanceOf[ReferenceExpression2])

  // If there's a Never2() anywhere, then the entire block should end in an unreachable
  // or panic or something.
  if (exprs.exists(_.referend == Never2())) {
    vassert(exprs.last.referend == Never2())
  }

  vassert(exprs.collect({
    case Return2(_) =>
  }).size <= 1)

  def lastReferenceExpr = exprs.last
  override def resultRegister = lastReferenceExpr.resultRegister

  def all[T](func: PartialFunction[Queriable2, T]): List[T] = {
    List(this).collect(func) ++ exprs.flatMap(_.all(func))
  }
}

case class Consecutor2(exprs: List[ReferenceExpression2]) extends ReferenceExpression2 {
  // Everything but the last should result in a Void.
  // The last can be anything, even a Void or a Never.
  exprs.init.foreach(expr => vassert(expr.referend == Void2()))

  def lastReferenceExpr = exprs.last
  override def resultRegister = lastReferenceExpr.resultRegister

  def all[T](func: PartialFunction[Queriable2, T]): List[T] = {
    List(this).collect(func) ++ exprs.flatMap(_.all(func))
  }
}

case class PackE2(
    elements: List[ReferenceExpression2],
    resultReference: Coord,
    packType: PackT2) extends ReferenceExpression2 {
  override def resultRegister = ReferenceResult2(resultReference)
  def all[T](func: PartialFunction[Queriable2, T]): List[T] = {
    List(this).collect(func) ++ elements.flatMap(_.all(func)) ++ packType.all(func)
  }
}

case class TupleE2(
    elements: List[ReferenceExpression2],
    resultReference: Coord,
    tupleType: TupleT2) extends ReferenceExpression2 {
  override def resultRegister = ReferenceResult2(resultReference)
  def all[T](func: PartialFunction[Queriable2, T]): List[T] = {
    List(this).collect(func) ++ elements.flatMap(_.all(func)) ++ tupleType.all(func)
  }
}

// Discards a reference, whether it be owned or borrow or whatever.
// This is used after panics or other never-returning things, to signal that a certain
// variable should be considered gone. See AUMAP.
// This can also be used if theres anything after a panic in a block, like
//   fn main() int export {
//     __panic();
//     println("hi");
//   }
case class UnreachableMootE2(innerExpr: ReferenceExpression2) extends ReferenceExpression2 {
  override def resultRegister = ReferenceResult2(Coord(Share, Readonly, Never2()))
  def all[T](func: PartialFunction[Queriable2, T]): List[T] = {
    List(this).collect(func) ++ innerExpr.all(func)
  }
}

case class StaticArrayFromValues2(
    elements: List[ReferenceExpression2],
    resultReference: Coord,
    arrayType: KnownSizeArrayT2) extends ReferenceExpression2 {
  override def resultRegister = ReferenceResult2(resultReference)
  def all[T](func: PartialFunction[Queriable2, T]): List[T] = {
    List(this).collect(func) ++ elements.flatMap(_.all(func)) ++ arrayType.all(func)
  }
}

case class ArraySize2(array: ReferenceExpression2) extends ReferenceExpression2 {
  override def resultRegister = ReferenceResult2(Coord(Share, Readonly, Int2()))
  def all[T](func: PartialFunction[Queriable2, T]): List[T] = {
    List(this).collect(func) ++ array.all(func)
  }
}

case class IsSameInstance2(left: ReferenceExpression2, right: ReferenceExpression2) extends ReferenceExpression2 {
  vassert(left.resultRegister.reference == right.resultRegister.reference)

  override def resultRegister = ReferenceResult2(Coord(Share, Readonly, Bool2()))
  def all[T](func: PartialFunction[Queriable2, T]): List[T] = {
    List(this).collect(func) ++ left.all(func) ++ right.all(func)
  }
}

case class AsSubtype2(
    sourceExpr: ReferenceExpression2,
    targetSubtype: Kind,

    // We could just calculate this, but it feels better to let the StructTemplar
    // make it, so we're sure it's created.
    resultOptType: Coord,
    // Function to give a borrow ref to to make a Some(borrow ref)
    someConstructor: Prototype2,
    // Function to make a None of the right type
    noneConstructor: Prototype2,
) extends ReferenceExpression2 {
  override def resultRegister = ReferenceResult2(resultOptType)
  def all[T](func: PartialFunction[Queriable2, T]): List[T] = {
    List(this).collect(func) ++ sourceExpr.all(func) ++ targetSubtype.all(func) ++ resultOptType.all(func) ++ someConstructor.all(func) ++ noneConstructor.all(func)
  }
}

case class VoidLiteral2() extends ReferenceExpression2 {
  override def resultRegister = ReferenceResult2(Coord(Share, Readonly, Void2()))

  def all[T](func: PartialFunction[Queriable2, T]): List[T] = {
    List(this).collect(func)
  }
}

case class IntLiteral2(value: Int) extends ReferenceExpression2 {
  override def resultRegister = ReferenceResult2(Coord(Share, Readonly, Int2()))

  def all[T](func: PartialFunction[Queriable2, T]): List[T] = {
    List(this).collect(func)
  }
}

case class BoolLiteral2(value: Boolean) extends ReferenceExpression2 {
  override def resultRegister = ReferenceResult2(Coord(Share, Readonly, Bool2()))

  def all[T](func: PartialFunction[Queriable2, T]): List[T] = {
    List(this).collect(func)
  }
}

case class StrLiteral2(value: String) extends ReferenceExpression2 {
  override def resultRegister = ReferenceResult2(Coord(Share, Readonly, Str2()))

  def all[T](func: PartialFunction[Queriable2, T]): List[T] = {
    List(this).collect(func)
  }
}

case class FloatLiteral2(value: Double) extends ReferenceExpression2 {
  override def resultRegister = ReferenceResult2(Coord(Share, Readonly, Float2()))

  def all[T](func: PartialFunction[Queriable2, T]): List[T] = {
    List(this).collect(func)
  }
}

case class LocalLookup2(
  range: RangeS,
  localVariable: ILocalVariable2,
  reference: Coord,
  variability: Variability
) extends AddressExpression2 {
  override def resultRegister = AddressResult2(reference)

  def all[T](func: PartialFunction[Queriable2, T]): List[T] = {
    List(this).collect(func) ++ reference.all(func)
  }
}

case class ArgLookup2(
    paramIndex: Int,
    reference: Coord
) extends ReferenceExpression2 {
  override def resultRegister = ReferenceResult2(reference)

  def all[T](func: PartialFunction[Queriable2, T]): List[T] = {
    List(this).collect(func) ++ reference.all(func)
  }
}

//case class PackLookup2(packExpr: Expression2, index: Int) extends Expression2 {
//  override def resultType: BaseType2 = {
//    // A pack can never be in a changeable variable, and so can't be an addressible, so will always
//    // be a pointer.
//    // (it can be in a final variable, when its spawned by pattern matching)
//    TypeUtils.softDecay(packExpr.resultType).innerType match {
//      case PackT2(memberTypes, underlyingStructRef) => memberTypes(index)
//    }
//  }
//
//  def all[T](func: PartialFunction[Ast2, T]): List[T] = {
//    List(this).collect(func) ++ packExpr.all(func)
//  }
//}

case class ArraySequenceLookup2(
  range: RangeS,
    arrayExpr: ReferenceExpression2,
    arrayType: KnownSizeArrayT2,
    indexExpr: ReferenceExpression2,
    variability: Variability) extends AddressExpression2 {
  vassert(arrayExpr.resultRegister.reference.referend == arrayType)

  override def resultRegister = AddressResult2(arrayType.array.memberType)

  def all[T](func: PartialFunction[Queriable2, T]): List[T] = {
    List(this).collect(func) ++ arrayExpr.all(func) ++ indexExpr.all(func) ++ arrayType.all(func)
  }
}

case class UnknownSizeArrayLookup2(
  range: RangeS,
    arrayExpr: ReferenceExpression2,
    arrayType: UnknownSizeArrayT2,
    indexExpr: ReferenceExpression2,
  variability: Variability) extends AddressExpression2 {
  vassert(arrayExpr.resultRegister.reference.referend == arrayType)

  override def resultRegister = AddressResult2(arrayType.array.memberType)

  def all[T](func: PartialFunction[Queriable2, T]): List[T] = {
    List(this).collect(func) ++ arrayExpr.all(func) ++ indexExpr.all(func) ++ arrayType.all(func)
  }
}

case class ArrayLength2(arrayExpr: ReferenceExpression2) extends ReferenceExpression2 {
  override def resultRegister = ReferenceResult2(Coord(Share, Readonly, Int2()))
  def all[T](func: PartialFunction[Queriable2, T]): List[T] = {
    List(this).collect(func) ++ arrayExpr.all(func)
  }
}

case class ReferenceMemberLookup2(
    range: RangeS,
    structExpr: ReferenceExpression2,
    memberName: FullName2[IVarName2],
    memberReference: Coord,
    // See RMLRMO for why we dont have a targetOwnership field here.
    // See RMLHTP why we can have this here.
    targetPermission: Permission,
    variability: Variability) extends AddressExpression2 {
  override def resultRegister = {
    if (structExpr.resultRegister.reference.permission == Readonly) {
      vassert(targetPermission == Readonly)
    }
    if (targetPermission == Readwrite) {
      vassert(structExpr.resultRegister.reference.permission == Readwrite)
    }
    // See RMLRMO why we just return the member type.
    AddressResult2(memberReference.copy(permission = targetPermission))
  }

  def all[T](func: PartialFunction[Queriable2, T]): List[T] = {
    List(this).collect(func) ++ structExpr.all(func) ++ memberName.all(func) ++ memberReference.all(func)
  }
}
case class AddressMemberLookup2(
    range: RangeS,
    structExpr: ReferenceExpression2,
    memberName: FullName2[IVarName2],
    resultType2: Coord,
    variability: Variability) extends AddressExpression2 {
  override def resultRegister = AddressResult2(resultType2)

  def all[T](func: PartialFunction[Queriable2, T]): List[T] = {
    List(this).collect(func) ++ structExpr.all(func) ++ resultType2.all(func)
  }
}

//
//case class FunctionLookup2(prototype: Prototype2) extends ReferenceExpression2 {
//  override def resultRegister: ReferenceRegister2 =
//    ReferenceRegister2(Coord(Raw, prototype.functionType))
//
//  def all[T](func: PartialFunction[Queriable2, T]): List[T] = {
//    List(this).collect(func) ++ prototype.all(func)
//  }
//}

case class InterfaceFunctionCall2(
    superFunctionHeader: FunctionHeader2,
    resultReference: Coord,
    args: List[ReferenceExpression2]) extends ReferenceExpression2 {
  override def resultRegister: ReferenceResult2 =
    ReferenceResult2(resultReference)

  def all[T](func: PartialFunction[Queriable2, T]): List[T] = {
    List(this).collect(func) ++ superFunctionHeader.all(func) ++ resultReference.all(func) ++ args.flatMap(_.all(func))
  }
}

case class ExternFunctionCall2(
    prototype2: Prototype2,
    args: List[ReferenceExpression2]) extends ReferenceExpression2 {
  // We dont:
  //   vassert(prototype2.fullName.last.templateArgs.isEmpty)
  // because we totally can have extern templates.
  // Will one day be useful for plugins, and we already use it for
  // lock<T>, which is generated by the backend.

  prototype2.fullName.last match {
    case ExternFunctionName2(_, _) =>
    case _ => vwat()
  }

  def all[T](func: PartialFunction[Queriable2, T]): List[T] = {
    List(this).collect(func) ++ args.flatMap(_.all(func))
  }

  override def resultRegister = ReferenceResult2(prototype2.returnType)
}

case class FunctionCall2(
    callable: Prototype2,
    args: List[ReferenceExpression2]) extends ReferenceExpression2 {

  vassert(callable.paramTypes.size == args.size)
  vassert(callable.paramTypes == args.map(_.resultRegister.reference))

  override def resultRegister: ReferenceResult2 = {
    ReferenceResult2(callable.returnType)
  }

  def all[T](func: PartialFunction[Queriable2, T]): List[T] = {
    List(this).collect(func) ++ callable.all(func) ++ args.flatMap(_.all(func))
  }
}
case class Tuple2(
    elements: List[ReferenceExpression2],
    tupleReference: Coord) extends ReferenceExpression2 {
  override def resultRegister = ReferenceResult2(tupleReference)

  def all[T](func: PartialFunction[Queriable2, T]): List[T] = {
    List(this).collect(func) ++ elements.flatMap(_.all(func)) ++ tupleReference.all(func)
  }
}

// A templar reinterpret is interpreting a type as a different one which is hammer-equivalent.
// For example, a pack and a struct are the same thing to hammer.
// Also, a closure and a struct are the same thing to hammer.
// But, Templar attaches different meanings to these things. The templar is free to reinterpret
// between hammer-equivalent things as it wants.
case class TemplarReinterpret2(
    expr: ReferenceExpression2,
    resultReference: Coord) extends ReferenceExpression2 {
  vassert(expr.resultRegister.reference != resultReference)

  override def resultRegister = ReferenceResult2(resultReference)

  // Unless it's a Never...
  if (expr.resultRegister.reference.referend != Never2()) {
    if (resultReference.ownership != expr.resultRegister.reference.ownership) {
      // Cant reinterpret to a different ownership!
      vfail("wat");
    }
  }

  def all[T](func: PartialFunction[Queriable2, T]): List[T] = {
    List(this).collect(func) ++ expr.all(func) ++ resultReference.all(func)
  }
}

case class Construct2(
    structRef: StructRef2,
    resultReference: Coord,
    args: List[Expression2]) extends ReferenceExpression2 {
  vpass()

  override def resultRegister = ReferenceResult2(resultReference)

  def all[T](func: PartialFunction[Queriable2, T]): List[T] = {
    List(this).collect(func) ++ structRef.all(func) ++ args.flatMap(_.all(func))
  }
}

// Note: the functionpointercall's last argument is a Placeholder2,
// it's up to later stages to replace that with an actual index
case class ConstructArray2(
    arrayType: UnknownSizeArrayT2,
    sizeExpr: ReferenceExpression2,
    generator: ReferenceExpression2,
    generatorMethod: Prototype2
) extends ReferenceExpression2 {
  override def resultRegister: ReferenceResult2 = {
    ReferenceResult2(
      Coord(
        if (arrayType.array.mutability == Mutable) Own else Share,
        if (arrayType.array.mutability == Mutable) Readwrite else Readonly,
        arrayType))
  }

  def all[T](func: PartialFunction[Queriable2, T]): List[T] = {
    List(this).collect(func) ++ arrayType.all(func) ++ sizeExpr.all(func) ++ generator.all(func)
  }
}

case class StaticArrayFromCallable2(
  arrayType: KnownSizeArrayT2,
  generator: ReferenceExpression2,
  generatorMethod: Prototype2
) extends ReferenceExpression2 {
  override def resultRegister: ReferenceResult2 = {
    ReferenceResult2(
      Coord(
        if (arrayType.array.mutability == Mutable) Own else Share,
        if (arrayType.array.mutability == Mutable) Readwrite else Readonly,
        arrayType))
  }

  def all[T](func: PartialFunction[Queriable2, T]): List[T] = {
    List(this).collect(func) ++ arrayType.all(func) ++ generator.all(func) ++ generatorMethod.all(func)
  }
}

// Note: the functionpointercall's last argument is a Placeholder2,
// it's up to later stages to replace that with an actual index
// This returns nothing, as opposed to DrainArraySequence2 which returns a
// sequence of results from the call.
case class DestroyArraySequenceIntoFunction2(
    arrayExpr: ReferenceExpression2,
    arrayType: KnownSizeArrayT2,
    consumer: ReferenceExpression2,
    consumerMethod: Prototype2) extends ReferenceExpression2 {
  override def resultRegister: ReferenceResult2 = ReferenceResult2(Coord(Share, Readonly, Void2()))

  def all[T](func: PartialFunction[Queriable2, T]): List[T] = {
    List(this).collect(func) ++ arrayType.all(func) ++ arrayExpr.all(func) ++ consumer.all(func)
  }
}

// We destroy both Share and Own things
// If the struct contains any addressibles, those die immediately and aren't stored
// in the destination variables, which is why it's a list of ReferenceLocalVariable2.
case class DestroyArraySequenceIntoLocals2(
  expr: ReferenceExpression2,
  arraySeq: KnownSizeArrayT2,
  destinationReferenceVariables: List[ReferenceLocalVariable2]
) extends ReferenceExpression2 {
  override def resultRegister: ReferenceResult2 = ReferenceResult2(Coord(Share, Readonly, Void2()))

  vassert(expr.referend == arraySeq)
  if (expr.resultRegister.reference.ownership == Constraint) {
    vfail("wot")
  }

  def all[T](func: PartialFunction[Queriable2, T]): List[T] = {
    List(this).collect(func) ++ expr.all(func)
  }
}

case class DestroyUnknownSizeArray2(
    arrayExpr: ReferenceExpression2,
    arrayType: UnknownSizeArrayT2,
    consumer: ReferenceExpression2,
    consumerMethod: Prototype2
) extends ReferenceExpression2 {
  override def resultRegister: ReferenceResult2 = ReferenceResult2(Coord(Share, Readonly, Void2()))

  def all[T](func: PartialFunction[Queriable2, T]): List[T] = {
    List(this).collect(func) ++ arrayType.all(func) ++ arrayExpr.all(func) ++ consumer.all(func)
  }
}

case class InterfaceToInterfaceUpcast2(
    innerExpr: ReferenceExpression2,
    targetInterfaceRef: InterfaceRef2) extends ReferenceExpression2 {
  def resultRegister: ReferenceResult2 = {
    ReferenceResult2(
      Coord(
        innerExpr.resultRegister.reference.ownership,
        innerExpr.resultRegister.reference.permission,
        targetInterfaceRef))
  }

  def all[T](func: PartialFunction[Queriable2, T]): List[T] = {
    List(this).collect(func) ++ innerExpr.all(func) ++ targetInterfaceRef.all(func)
  }
}

case class StructToInterfaceUpcast2(innerExpr: ReferenceExpression2, targetInterfaceRef: InterfaceRef2) extends ReferenceExpression2 {
  def resultRegister: ReferenceResult2 = {
    ReferenceResult2(
      Coord(
        innerExpr.resultRegister.reference.ownership,
        innerExpr.resultRegister.reference.permission,
        targetInterfaceRef))
  }

  def all[T](func: PartialFunction[Queriable2, T]): List[T] = {
    List(this).collect(func) ++ innerExpr.all(func) ++ targetInterfaceRef.all(func)
  }
}

// A soft load is one that turns an int** into an int*. a hard load turns an int* into an int.
// Turns an Addressible(Pointer) into an OwningPointer. Makes the source owning pointer into null

// If the source was an own and target is borrow, that's a lend

case class SoftLoad2(
    expr: AddressExpression2, targetOwnership: Ownership, targetPermission: Permission) extends ReferenceExpression2 {

  vassert((targetOwnership == Share) == (expr.resultRegister.reference.ownership == Share))
  vassert(targetOwnership != Own) // need to unstackify or destroy to get an owning reference
  // This is just here to try the asserts inside Coord's constructor
  Coord(targetOwnership, targetPermission, expr.resultRegister.reference.referend)

  (expr.resultRegister.reference.permission, targetPermission) match {
    case (Readonly, Readonly) =>
    case (Readwrite, Readonly) =>
    case (Readwrite, Readwrite) =>
    case (Readonly, Readwrite) =>
    case _ => vwat()
  }

  override def resultRegister: ReferenceResult2 = {
    ReferenceResult2(Coord(targetOwnership, targetPermission, expr.resultRegister.reference.referend))
  }

  def all[T](func: PartialFunction[Queriable2, T]): List[T] = {
    List(this).collect(func) ++ expr.all(func)
  }
}

// Destroy an object.
// If the struct contains any addressibles, those die immediately and aren't stored
// in the destination variables, which is why it's a list of ReferenceLocalVariable2.
//
// We also destroy shared things with this, see DDSOT.
case class Destroy2(
    expr: ReferenceExpression2,
    structRef2: StructRef2,
    destinationReferenceVariables: List[ReferenceLocalVariable2]
) extends ReferenceExpression2 {
  override def resultRegister: ReferenceResult2 = ReferenceResult2(Coord(Share, Readonly, Void2()))

  if (expr.resultRegister.reference.ownership == Constraint) {
    vfail("wot")
  }

  def all[T](func: PartialFunction[Queriable2, T]): List[T] = {
    List(this).collect(func) ++ expr.all(func)
  }
}

//// If source was an own and target is borrow, that's a lend
//// (thats the main purpose of this)
//case class Alias2(expr: ReferenceExpression2, targetOwnership: Ownership) extends ReferenceExpression2 {
//  override def resultRegister: ReferenceRegister2 = {
//    expr.resultRegister.reference match {
//      case Coord(_, innerType) => ReferenceRegister2(Coord(targetOwnership, innerType))
//    }
//  }
//
//  def all[T](func: PartialFunction[Queriable2, T]): List[T] = {
//    List(this).collect(func) ++ expr.all(func)
//  }
//}

case class CheckRefCount2(
    refExpr: ReferenceExpression2,
    category: types.RefCountCategory,
    numExpr: ReferenceExpression2) extends ReferenceExpression2 {
  override def resultRegister = ReferenceResult2(Coord(Share, Readonly, Void2()))

  def all[T](func: PartialFunction[Queriable2, T]): List[T] = {
    List(this).collect(func) ++ refExpr.all(func) ++ numExpr.all(func)
  }
}