package net.verdagon.vale.hammer

import net.verdagon.vale.hammer.ExpressionHammer.translate
import net.verdagon.vale.hinputs.Hinputs
import net.verdagon.vale.metal.{BorrowH, ShareH, Variability => _, Varying => _, _}
import net.verdagon.vale.{metal => m}
import net.verdagon.vale.templar.{types => t}
import net.verdagon.vale.templar._
import net.verdagon.vale.templar.env.{AddressibleLocalVariable2, ReferenceLocalVariable2}
import net.verdagon.vale.templar.templata.FunctionHeader2
import net.verdagon.vale.templar.types._
import net.verdagon.vale.{vassert, vfail}

object LoadHammer {

  def translateLoad(
      hinputs: Hinputs,
      hamuts: HamutsBox,
    currentFunctionHeader: FunctionHeader2,
      locals: LocalsBox,
      load2: SoftLoad2):
  (ExpressionH[ReferendH], List[Expression2]) = {
    val SoftLoad2(sourceExpr2, targetOwnership, targetPermission) = load2

    val (loadedAccessH, sourceDeferreds) =
      sourceExpr2 match {
        case LocalLookup2(_,ReferenceLocalVariable2(varId, variability, reference), varType2, _) => {
          vassert(reference == varType2)
          translateMundaneLocalLoad(hinputs, hamuts, currentFunctionHeader, locals, varId, reference, targetOwnership, targetPermission)
        }
        case LocalLookup2(_,AddressibleLocalVariable2(varId, variability, localReference2), varType2, _) => {
          vassert(localReference2 == varType2)
          translateAddressibleLocalLoad(hinputs, hamuts, currentFunctionHeader, locals, varId, variability, localReference2, targetOwnership, targetPermission)
        }
        case ReferenceMemberLookup2(_,structExpr2, memberName, memberType2, _, _) => {
          translateMundaneMemberLoad(hinputs, hamuts, currentFunctionHeader, locals, structExpr2, memberType2, memberName, targetOwnership, targetPermission)
        }
        case AddressMemberLookup2(_,structExpr2, memberName, memberType2, _) => {
          translateAddressibleMemberLoad(hinputs, hamuts, currentFunctionHeader, locals, structExpr2, memberName, memberType2, targetOwnership, targetPermission)
        }
        case UnknownSizeArrayLookup2(_,arrayExpr2, arrayType, indexExpr2, _) => {
          translateMundaneUnknownSizeArrayLoad(hinputs, hamuts, currentFunctionHeader, locals, arrayExpr2, indexExpr2, targetOwnership, targetPermission)
        }
        case ArraySequenceLookup2(_,arrayExpr2, arrayType, indexExpr2, _) => {
          translateMundaneKnownSizeArrayLoad(hinputs, hamuts, currentFunctionHeader, locals, arrayExpr2, indexExpr2, targetOwnership, targetPermission)
        }
      }

    // Note how we return the deferreds upward, see Hammer doc for why.

    (loadedAccessH, sourceDeferreds)
  }

  private def translateMundaneUnknownSizeArrayLoad(
      hinputs: Hinputs,
      hamuts: HamutsBox,
    currentFunctionHeader: FunctionHeader2,
      locals: LocalsBox,
      arrayExpr2: ReferenceExpression2,
      indexExpr2: ReferenceExpression2,
      targetOwnershipT: t.Ownership,
      targetPermissionT: t.Permission,
  ): (ExpressionH[ReferendH], List[Expression2]) = {
    val targetOwnership = Conversions.evaluateOwnership(targetOwnershipT)
    val targetPermission = Conversions.evaluatePermission(targetPermissionT)

    val (arrayResultLine, arrayDeferreds) =
      translate(hinputs, hamuts, currentFunctionHeader, locals, arrayExpr2);
    val arrayAccess = arrayResultLine.expectUnknownSizeArrayAccess()

    val (indexExprResultLine, indexDeferreds) =
      translate(hinputs, hamuts, currentFunctionHeader, locals, indexExpr2);
    val indexAccess = indexExprResultLine.expectIntAccess()

    vassert(targetOwnership == BorrowH || targetOwnership == ShareH)

    val usa = hamuts.getUnknownSizeArray(arrayAccess.resultType.kind)
    val expectedElementType = usa.rawArray.elementType
    val resultType = {
      val location =
        (targetOwnership, expectedElementType.location) match {
          case (BorrowH, _) => YonderH
          case (OwnH, location) => location
          case (ShareH, location) => location
        }
      ReferenceH(targetOwnership, location, expectedElementType.permission, expectedElementType.kind)
    }

    // We're storing into a regular reference element of an array.
    val loadedNodeH =
        UnknownSizeArrayLoadH(
          arrayAccess,
          indexAccess,
          targetOwnership,
          targetPermission,
          expectedElementType,
          resultType)

    (loadedNodeH, arrayDeferreds ++ indexDeferreds)
  }

  private def translateMundaneKnownSizeArrayLoad(
    hinputs: Hinputs,
    hamuts: HamutsBox,
    currentFunctionHeader: FunctionHeader2,
    locals: LocalsBox,
    arrayExpr2: ReferenceExpression2,
    indexExpr2: ReferenceExpression2,
    targetOwnershipT: t.Ownership,
    targetPermissionT: t.Permission,
  ): (ExpressionH[ReferendH], List[Expression2]) = {
    val targetOwnership = Conversions.evaluateOwnership(targetOwnershipT)
    val targetPermission = Conversions.evaluatePermission(targetPermissionT)

    val (arrayResultLine, arrayDeferreds) =
      translate(hinputs, hamuts, currentFunctionHeader, locals, arrayExpr2);
    val arrayAccess = arrayResultLine.expectKnownSizeArrayAccess()

    val (indexExprResultLine, indexDeferreds) =
      translate(hinputs, hamuts, currentFunctionHeader, locals, indexExpr2);
    val indexAccess = indexExprResultLine.expectIntAccess()

    vassert(targetOwnership == m.BorrowH || targetOwnership == m.ShareH)

    val ksa = hamuts.getKnownSizeArray(arrayAccess.resultType.kind)
    val expectedElementType = ksa.rawArray.elementType
    val resultType = {
      val location =
        (targetOwnership, expectedElementType.location) match {
          case (BorrowH, _) => YonderH
          case (OwnH, location) => location
          case (ShareH, location) => location
        }
      ReferenceH(targetOwnership, location, expectedElementType.permission, expectedElementType.kind)
    }

    // We're storing into a regular reference element of an array.
    val loadedNodeH =
        KnownSizeArrayLoadH(
          arrayAccess,
          indexAccess,
          targetOwnership,
          targetPermission,
          expectedElementType,
          ksa.size,
          resultType)

    (loadedNodeH, arrayDeferreds ++ indexDeferreds)
  }

  private def translateAddressibleMemberLoad(
      hinputs: Hinputs,
      hamuts: HamutsBox,
    currentFunctionHeader: FunctionHeader2,
      locals: LocalsBox,
      structExpr2: ReferenceExpression2,
      memberName: FullName2[IVarName2],
      expectedType2: Coord,
      targetOwnershipT: t.Ownership,
      targetPermissionT: t.Permission,
  ): (ExpressionH[ReferendH], List[Expression2]) = {
    val (structResultLine, structDeferreds) =
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

    val boxInStructCoord = ReferenceH(BorrowH, YonderH, ReadwriteH, boxStructRefH)

    // We're storing into a struct's member that is a box. The stack is also
    // pointing at this box. First, get the box, then mutate what's inside.
    var varFullNameH = NameHammer.translateFullName(hinputs, hamuts, memberName)
    val loadBoxNode =
        MemberLoadH(
          structResultLine.expectStructAccess(),
          memberIndex,
          boxInStructCoord,
          boxInStructCoord,
          varFullNameH)

    val targetOwnership = Conversions.evaluateOwnership(targetOwnershipT)
    val targetPermission = Conversions.evaluatePermission(targetPermissionT)
    val loadResultType =
      ReferenceH(
        targetOwnership,
        boxedTypeH.location,
        targetPermission,
        boxedTypeH.kind)
    val loadedNodeH =
        MemberLoadH(
          loadBoxNode.expectStructAccess(),
          StructHammer.BOX_MEMBER_INDEX,
          boxedTypeH,
          loadResultType,
          NameHammer.addStep(hamuts, boxStructRefH.fullName, StructHammer.BOX_MEMBER_NAME))
    (loadedNodeH, structDeferreds)
  }

  private def translateMundaneMemberLoad(
      hinputs: Hinputs,
      hamuts: HamutsBox,
    currentFunctionHeader: FunctionHeader2,
      locals: LocalsBox,
      structExpr2: ReferenceExpression2,
      expectedMemberCoord: Coord,
      memberName: FullName2[IVarName2],
//      resultCoord: Coord,
      targetOwnershipT: t.Ownership,
      targetPermissionT: t.Permission,
  ): (ExpressionH[ReferendH], List[Expression2]) = {
    val (structResultLine, structDeferreds) =
      translate(hinputs, hamuts, currentFunctionHeader, locals, structExpr2);

    val (expectedMemberTypeH) =
      TypeHammer.translateReference(hinputs, hamuts, expectedMemberCoord);
//    val (resultTypeH) =
//      TypeHammer.translateReference(hinputs, hamuts, resultCoord);

    val structRef2 =
      structExpr2.resultRegister.reference.referend match {
        case sr @ StructRef2(_) => sr
        case TupleT2(_, sr) => sr
        case PackT2(_, sr) => sr
      }
    val structDef2 = hinputs.lookupStruct(structRef2)
    val memberIndex = structDef2.members.indexWhere(member => structDef2.fullName.addStep(member.name) == memberName)
    vassert(memberIndex >= 0)

    val targetOwnership = Conversions.evaluateOwnership(targetOwnershipT)
    val targetPermission = Conversions.evaluatePermission(targetPermissionT)
    val loadResultType = ReferenceH(targetOwnership, expectedMemberTypeH.location, targetPermission, expectedMemberTypeH.kind)

    // We're loading from a regular reference member of a struct.
    val loadedNode =
        MemberLoadH(
          structResultLine.expectStructAccess(),
          memberIndex,
          expectedMemberTypeH,
          loadResultType,
          NameHammer.translateFullName(hinputs, hamuts, memberName))
    (loadedNode, structDeferreds)
  }

  def translateAddressibleLocalLoad(
      hinputs: Hinputs,
      hamuts: HamutsBox,
    currentFunctionHeader: FunctionHeader2,
      locals: LocalsBox,
      varId: FullName2[IVarName2],
      variability: Variability,
      localReference2: Coord,
      targetOwnershipT: t.Ownership,
      targetPermissionT: t.Permission,
  ): (ExpressionH[ReferendH], List[Expression2]) = {
    val local = locals.get(varId).get
    vassert(!locals.unstackifiedVars.contains(local.id))

    val (localTypeH) =
      TypeHammer.translateReference(hinputs, hamuts, localReference2);
    val (boxStructRefH) =
      StructHammer.makeBox(hinputs, hamuts, variability, localReference2, localTypeH)
    vassert(local.typeH.kind == boxStructRefH)

    // This means we're trying to load from a local variable that holds a box.
    // We need to load the box, then mutate its contents.
    val varNameH = NameHammer.translateFullName(hinputs, hamuts, varId)
    val loadBoxNode =
        LocalLoadH(
          local,
          m.BorrowH,
          // The box should be readwrite, but targetPermission is taken into account below.
          ReadwriteH,
          varNameH)

    val targetOwnership = Conversions.evaluateOwnership(targetOwnershipT)
    val targetPermission = Conversions.evaluatePermission(targetPermissionT)
    val loadResultType = ReferenceH(targetOwnership, localTypeH.location, targetPermission, localTypeH.kind)

    val loadedNode =
        MemberLoadH(
          loadBoxNode.expectStructAccess(),
          StructHammer.BOX_MEMBER_INDEX,
          localTypeH,
          loadResultType,
          NameHammer.addStep(hamuts, boxStructRefH.fullName, StructHammer.BOX_MEMBER_NAME))
    (loadedNode, List())
  }

  def translateMundaneLocalLoad(
      hinputs: Hinputs,
      hamuts: HamutsBox,
    currentFunctionHeader: FunctionHeader2,
      locals: LocalsBox,
      varId: FullName2[IVarName2],
      expectedType2: Coord,
      targetOwnershipT: t.Ownership,
      targetPermissionT: t.Permission,
  ): (ExpressionH[ReferendH], List[Expression2]) = {
    val targetOwnership = Conversions.evaluateOwnership(targetOwnershipT)
    val targetPermission = Conversions.evaluatePermission(targetPermissionT)


    val local = locals.get(varId) match {
      case Some(x) => x
      case None => {
        vfail("wot")
      }
    }
    vassert(!locals.unstackifiedVars.contains(local.id))

    val (expectedTypeH) =
      TypeHammer.translateReference(hinputs, hamuts, expectedType2);
    vassert(expectedTypeH == local.typeH)

    val loadedNode =
        LocalLoadH(
          local,
          targetOwnership,
          targetPermission,
          NameHammer.translateFullName(hinputs, hamuts, varId))
    (loadedNode, List())
  }

  def translateLocalAddress(
      hinputs: Hinputs,
      hamuts: HamutsBox,
    currentFunctionHeader: FunctionHeader2,
      locals: LocalsBox,
      lookup2: LocalLookup2):
  (ExpressionH[ReferendH]) = {
    val LocalLookup2(_,localVar, type2, _) = lookup2;
    vassert(type2 == localVar.reference)

    val local = locals.get(localVar.id).get
    vassert(!locals.unstackifiedVars.contains(local.id))
    val (boxStructRefH) =
      StructHammer.makeBox(hinputs, hamuts, localVar.variability, localVar.reference, local.typeH)

    // This means we're trying to load from a local variable that holds a box.
    // We need to load the box, then mutate its contents.
    val loadBoxNode =
      LocalLoadH(
        local,
        m.BorrowH,
        // The box should be readwrite, but we'll load from it in a way that takes into account
        // the user's desired permission.
        ReadwriteH,
        NameHammer.translateFullName(hinputs, hamuts, localVar.id))
    loadBoxNode
  }

  // In this, we're basically taking an addressible lookup, in other words,
  // a reference to a box.
  def translateMemberAddress(
      hinputs: Hinputs,
      hamuts: HamutsBox,
    currentFunctionHeader: FunctionHeader2,
      locals: LocalsBox,
      lookup2: AddressMemberLookup2):
  (ExpressionH[ReferendH], List[Expression2]) = {
    val AddressMemberLookup2(_,structExpr2, memberName, resultType2, _) = lookup2;

    val (structResultLine, structDeferreds) =
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
    vassert(variability == Varying, "Expected varying for member " + memberName) // curious

    val boxedType2 = member2.tyype.expectAddressMember().reference

    val (boxedTypeH) =
      TypeHammer.translateReference(hinputs, hamuts, boxedType2);

    val (boxStructRefH) =
      StructHammer.makeBox(hinputs, hamuts, variability, boxedType2, boxedTypeH)

    // We expect a borrow because structs never own boxes, they only borrow them
    val expectedStructBoxMemberType = ReferenceH(m.BorrowH, YonderH, ReadwriteH, boxStructRefH)

    val loadResultType =
      ReferenceH(
        // Boxes are either owned or borrowed. We only own boxes from locals,
        // and we're loading from a struct here, so we're getting a borrow to the
        // box from the struct.
        BorrowH,
        YonderH,
        // The box should be readwrite, but we'll load from it in a way that takes into account
        // the user's desired permission.
        ReadwriteH,
        boxStructRefH)

    // We're storing into a struct's member that is a box. The stack is also
    // pointing at this box. First, get the box, then mutate what's inside.
    val loadBoxNode =
      MemberLoadH(
        structResultLine.expectStructAccess(),
        memberIndex,
        expectedStructBoxMemberType,
        loadResultType,
        NameHammer.translateFullName(hinputs, hamuts, memberName))

    (loadBoxNode, structDeferreds)
  }

  def getBorrowedLocation(memberType: ReferenceH[ReferendH]) = {
    (memberType.ownership, memberType.location) match {
      case (OwnH, _) => YonderH
      case (BorrowH, _) => YonderH
      case (ShareH, location) => location
    }
  }
}
