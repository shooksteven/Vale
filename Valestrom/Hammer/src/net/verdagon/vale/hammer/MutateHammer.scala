package net.verdagon.vale.hammer

import net.verdagon.vale.hammer.ExpressionHammer.{translate, translateDeferreds}
import net.verdagon.vale.hinputs.Hinputs
import net.verdagon.vale.metal.{BorrowH => _, Variability => _, _}
import net.verdagon.vale.{metal => m}
import net.verdagon.vale.templar._
import net.verdagon.vale.templar.env.{AddressibleLocalVariable2, ReferenceLocalVariable2}
import net.verdagon.vale.templar.templata.FunctionHeader2
import net.verdagon.vale.templar.types._
import net.verdagon.vale.vassert

object MutateHammer {

  def translateMutate(
      hinputs: Hinputs,
      hamuts: HamutsBox,
    currentFunctionHeader: FunctionHeader2,
      locals: LocalsBox,
      mutate2: Mutate2):
  (ExpressionH[ReferendH]) = {
    val Mutate2(destinationExpr2, sourceExpr2) = mutate2

    val (sourceExprResultLine, sourceDeferreds) =
      translate(hinputs, hamuts, currentFunctionHeader, locals, sourceExpr2);
    val (sourceResultPointerTypeH) =
      TypeHammer.translateReference(hinputs, hamuts, sourceExpr2.resultRegister.reference)

    val (oldValueAccess, destinationDeferreds) =
      destinationExpr2 match {
        case LocalLookup2(_,ReferenceLocalVariable2(varId, variability, reference), varType2, _) => {
          translateMundaneLocalMutate(hinputs, hamuts, currentFunctionHeader, locals, sourceExprResultLine, varId)
        }
        case LocalLookup2(_,AddressibleLocalVariable2(varId, variability, reference), varType2, _) => {
          translateAddressibleLocalMutate(hinputs, hamuts, currentFunctionHeader, locals, sourceExprResultLine, sourceResultPointerTypeH, varId, variability, reference)
        }
        case ReferenceMemberLookup2(_,structExpr2, memberName, _, _, _) => {
          translateMundaneMemberMutate(hinputs, hamuts, currentFunctionHeader, locals, sourceExprResultLine, structExpr2, memberName)
        }
        case AddressMemberLookup2(_,structExpr2, memberName, memberType2, _) => {
          translateAddressibleMemberMutate(hinputs, hamuts, currentFunctionHeader, locals, sourceExprResultLine, structExpr2, memberName)
        }
        case ArraySequenceLookup2(_,arrayExpr2, arrayType, indexExpr2, _) => {
          translateMundaneKnownSizeArrayMutate(hinputs, hamuts, currentFunctionHeader, locals, sourceExprResultLine, arrayExpr2, indexExpr2)
        }
        case UnknownSizeArrayLookup2(_,arrayExpr2, arrayType, indexExpr2, _) => {
          translateMundaneUnknownSizeArrayMutate(hinputs, hamuts, currentFunctionHeader, locals, sourceExprResultLine, arrayExpr2, indexExpr2)
        }
      }

    translateDeferreds(hinputs, hamuts, currentFunctionHeader, locals, oldValueAccess, sourceDeferreds ++ destinationDeferreds)
  }

  private def translateMundaneUnknownSizeArrayMutate(
    hinputs: Hinputs,
    hamuts: HamutsBox,
    currentFunctionHeader: FunctionHeader2,
    locals: LocalsBox,
    sourceExprResultLine: ExpressionH[ReferendH],
    arrayExpr2: ReferenceExpression2,
    indexExpr2: ReferenceExpression2
  ): (ExpressionH[ReferendH], List[Expression2]) = {
    val (destinationResultLine, destinationDeferreds) =
      translate(hinputs, hamuts, currentFunctionHeader, locals, arrayExpr2);
    val (indexExprResultLine, indexDeferreds) =
      translate(hinputs, hamuts, currentFunctionHeader, locals, indexExpr2);
    val resultType =
      hamuts.getUnknownSizeArray(
        destinationResultLine.expectUnknownSizeArrayAccess().resultType.kind)
        .rawArray.elementType
    // We're storing into a regular reference element of an array.
    val storeNode =
        UnknownSizeArrayStoreH(
          destinationResultLine.expectUnknownSizeArrayAccess(),
          indexExprResultLine.expectIntAccess(),
          sourceExprResultLine,
          resultType)

    (storeNode, destinationDeferreds ++ indexDeferreds)
  }

  private def translateMundaneKnownSizeArrayMutate(
                                                    hinputs: Hinputs,
                                                    hamuts: HamutsBox,
    currentFunctionHeader: FunctionHeader2,
                                                    locals: LocalsBox,
                                                    sourceExprResultLine: ExpressionH[ReferendH],
                                                    arrayExpr2: ReferenceExpression2,
                                                    indexExpr2: ReferenceExpression2
  ): (ExpressionH[ReferendH], List[Expression2]) = {
    val (destinationResultLine, destinationDeferreds) =
      translate(hinputs, hamuts, currentFunctionHeader, locals, arrayExpr2);
    val (indexExprResultLine, indexDeferreds) =
      translate(hinputs, hamuts, currentFunctionHeader, locals, indexExpr2);
    val resultType =
      hamuts.getKnownSizeArray(
        destinationResultLine.expectKnownSizeArrayAccess().resultType.kind)
        .rawArray.elementType
    // We're storing into a regular reference element of an array.
    val storeNode =
        KnownSizeArrayStoreH(
          destinationResultLine.expectKnownSizeArrayAccess(),
          indexExprResultLine.expectIntAccess(),
          sourceExprResultLine,
          resultType)

    (storeNode, destinationDeferreds ++ indexDeferreds)
  }

  private def translateAddressibleMemberMutate(
    hinputs: Hinputs,
    hamuts: HamutsBox,
    currentFunctionHeader: FunctionHeader2,
    locals: LocalsBox,
    sourceExprResultLine: ExpressionH[ReferendH],
    structExpr2: ReferenceExpression2,
    memberName: FullName2[IVarName2]
  ): (ExpressionH[ReferendH], List[Expression2]) = {
    val (destinationResultLine, destinationDeferreds) =
      translate(hinputs, hamuts, currentFunctionHeader, locals, structExpr2);

    val structRef2 =
      structExpr2.resultRegister.reference.referend match {
        case sr @ StructRef2(_) => sr
        case TupleT2(_, sr) => sr
        case PackT2(_, sr) => sr
      }
    val structDef2 = hinputs.lookupStruct(structRef2)
    val memberIndex = structDef2.members.indexWhere(member => structDef2.fullName.addStep(member.name) == memberName)
    vassert(memberIndex >= 0)
    val member2 = structDef2.members(memberIndex)

    val variability = member2.variability

    val boxedType2 = member2.tyype.expectAddressMember().reference

    val (boxedTypeH) =
      TypeHammer.translateReference(hinputs, hamuts, boxedType2);

    val (boxStructRefH) =
      StructHammer.makeBox(hinputs, hamuts, variability, boxedType2, boxedTypeH)

    // Remember, structs can never own boxes, they only borrow them
    val expectedStructBoxMemberType = ReferenceH(m.BorrowH, YonderH, ReadwriteH, boxStructRefH)

    // We're storing into a struct's member that is a box. The stack is also
    // pointing at this box. First, get the box, then mutate what's inside.
    val nameH = NameHammer.translateFullName(hinputs, hamuts, memberName)
    val loadResultType =
      ReferenceH(
        m.BorrowH,
        YonderH,
        // The box should be readwrite, but targetPermission is taken into account when we load/mutate from the
        // box's member.
        ReadwriteH,
        boxStructRefH)
    val loadBoxNode =
        MemberLoadH(
          destinationResultLine.expectStructAccess(),
          memberIndex,
          expectedStructBoxMemberType,
          loadResultType,
          nameH)
    val storeNode =
        MemberStoreH(
          boxedTypeH,
          loadBoxNode.expectStructAccess(),
          StructHammer.BOX_MEMBER_INDEX,
          sourceExprResultLine,
          NameHammer.addStep(hamuts, boxStructRefH.fullName, StructHammer.BOX_MEMBER_NAME))
    (storeNode, destinationDeferreds)
  }

  private def translateMundaneMemberMutate(
    hinputs: Hinputs,
    hamuts: HamutsBox,
    currentFunctionHeader: FunctionHeader2,
    locals: LocalsBox,
    sourceExprResultLine: ExpressionH[ReferendH],
    structExpr2: ReferenceExpression2,
    memberName: FullName2[IVarName2]
  ): (ExpressionH[ReferendH], List[Expression2]) = {
    val (destinationResultLine, destinationDeferreds) =
      translate(hinputs, hamuts, currentFunctionHeader, locals, structExpr2);

    val structRef2 =
      structExpr2.resultRegister.reference.referend match {
        case sr @ StructRef2(_) => sr
      }
    val structDef2 = hinputs.lookupStruct(structRef2)
    val memberIndex =
      structDef2.members
        .indexWhere(member => structDef2.fullName.addStep(member.name) == memberName)
    vassert(memberIndex >= 0)

    val structDefH = hamuts.structDefsByRef2(structRef2)

    // We're storing into a regular reference member of a struct.
    val storeNode =
        MemberStoreH(
          structDefH.members(memberIndex).tyype,
          destinationResultLine.expectStructAccess(),
          memberIndex,
          sourceExprResultLine,
          NameHammer.translateFullName(hinputs, hamuts, memberName))
    (storeNode, destinationDeferreds)
  }

  private def translateAddressibleLocalMutate(
    hinputs: Hinputs,
    hamuts: HamutsBox,
    currentFunctionHeader: FunctionHeader2,
    locals: LocalsBox,
    sourceExprResultLine: ExpressionH[ReferendH],
    sourceResultPointerTypeH: ReferenceH[ReferendH],
    varId: FullName2[IVarName2],
    variability: Variability,
    reference: Coord
  ): (ExpressionH[ReferendH], List[Expression2]) = {
    val local = locals.get(varId).get
    val (boxStructRefH) =
      StructHammer.makeBox(hinputs, hamuts, variability, reference, sourceResultPointerTypeH)

    val structDefH = hamuts.structDefs.find(_.getRef == boxStructRefH).get

    // This means we're trying to mutate a local variable that holds a box.
    // We need to load the box, then mutate its contents.
    val nameH = NameHammer.translateFullName(hinputs, hamuts, varId)
    val loadBoxNode =
      LocalLoadH(
        local,
        m.BorrowH,
        // The box should be readwrite, but targetPermission is taken into account when we load from its member.
        ReadwriteH,
        nameH)
    val storeNode =
        MemberStoreH(
          structDefH.members.head.tyype,
          loadBoxNode.expectStructAccess(),
          StructHammer.BOX_MEMBER_INDEX,
          sourceExprResultLine,
          NameHammer.addStep(hamuts, boxStructRefH.fullName, StructHammer.BOX_MEMBER_NAME))
    (storeNode, List())
  }

  private def translateMundaneLocalMutate(
                                           hinputs: Hinputs,
                                           hamuts: HamutsBox,
    currentFunctionHeader: FunctionHeader2,
                                           locals: LocalsBox,
                                           sourceExprResultLine: ExpressionH[ReferendH],
                                           varId: FullName2[IVarName2]
  ): (ExpressionH[ReferendH], List[Expression2]) = {
    val local = locals.get(varId).get
    vassert(!locals.unstackifiedVars.contains(local.id))
    val newStoreNode =
        LocalStoreH(
          local,
          sourceExprResultLine,
          NameHammer.translateFullName(hinputs, hamuts, varId))
    (newStoreNode, List())
  }
}
