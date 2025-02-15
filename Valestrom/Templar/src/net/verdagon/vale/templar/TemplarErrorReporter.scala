package net.verdagon.vale.templar

import net.verdagon.vale.astronomer.{GlobalFunctionFamilyNameA, ICompileErrorA, IFunctionDeclarationNameA, IImpreciseNameStepA, IVarNameA, TopLevelCitizenDeclarationNameA}
import net.verdagon.vale.scout.RangeS
import net.verdagon.vale.templar.OverloadTemplar.ScoutExpectedFunctionFailure
import net.verdagon.vale.templar.templata.Signature2
import net.verdagon.vale.templar.types.{CitizenRef2, Coord, InterfaceRef2, Kind, StructRef2}
import net.verdagon.vale.vpass

case class CompileErrorExceptionT(err: ICompileErrorT) extends RuntimeException

sealed trait ICompileErrorT
case class ImmStructCantHaveVaryingMember(range: RangeS, structName: TopLevelCitizenDeclarationNameA, memberName: String) extends ICompileErrorT
case class CantDowncastUnrelatedTypes(range: RangeS, sourceKind: Kind, targetKind: Kind) extends ICompileErrorT
case class CantDowncastToInterface(range: RangeS, targetKind: InterfaceRef2) extends ICompileErrorT
case class CouldntFindTypeT(range: RangeS, name: String) extends ICompileErrorT
case class ArrayElementsHaveDifferentTypes(range: RangeS, types: Set[Coord]) extends ICompileErrorT
case class InitializedWrongNumberOfElements(range: RangeS, expectedNumElements: Int, numElementsInitialized: Int) extends ICompileErrorT
case class CannotSubscriptT(range: RangeS, tyype: Kind) extends ICompileErrorT
case class NonReadonlyReferenceFoundInPureFunctionParameter(range: RangeS, paramName: IVarName2) extends ICompileErrorT
case class CouldntFindIdentifierToLoadT(range: RangeS, name: String) extends ICompileErrorT
case class CouldntFindMemberT(range: RangeS, memberName: String) extends ICompileErrorT
case class BodyResultDoesntMatch(range: RangeS, functionName: IFunctionDeclarationNameA, expectedReturnType: Coord, resultType: Coord) extends ICompileErrorT
case class CouldntConvertForReturnT(range: RangeS, expectedType: Coord, actualType: Coord) extends ICompileErrorT
case class CouldntConvertForMutateT(range: RangeS, expectedType: Coord, actualType: Coord) extends ICompileErrorT
case class CantMoveOutOfMemberT(range: RangeS, name: IVarName2) extends ICompileErrorT
case class CouldntFindFunctionToCallT(range: RangeS, seff: ScoutExpectedFunctionFailure) extends ICompileErrorT
case class CantUseUnstackifiedLocal(range: RangeS, localId: IVarName2) extends ICompileErrorT
case class CantUnstackifyOutsideLocalFromInsideWhile(range: RangeS, localId: IVarName2) extends ICompileErrorT
case class FunctionAlreadyExists(oldFunctionRange: RangeS, newFunctionRange: RangeS, signature: Signature2) extends ICompileErrorT
case class CantMutateFinalLocal(range: RangeS, localName: IVarNameA) extends ICompileErrorT
case class CantMutateFinalMember(range: RangeS, fullName2: FullName2[IName2], memberName: FullName2[IVarName2]) extends ICompileErrorT
//case class CantMutateReadonlyMember(range: RangeS, structRef2: StructRef2, memberName: FullName2[IVarName2]) extends ICompileErrorT
case class CantUseReadonlyReferenceAsReadwrite(range: RangeS) extends ICompileErrorT
case class LambdaReturnDoesntMatchInterfaceConstructor(range: RangeS) extends ICompileErrorT
case class IfConditionIsntBoolean(range: RangeS, actualType: Coord) extends ICompileErrorT
case class WhileConditionIsntBoolean(range: RangeS, actualType: Coord) extends ICompileErrorT
case class CantMoveFromGlobal(range: RangeS, name: String) extends ICompileErrorT
case class InferAstronomerError(err: ICompileErrorA) extends ICompileErrorT
case class CantImplStruct(range: RangeS, parent: StructRef2) extends ICompileErrorT
// REMEMBER: Add any new errors to the "Humanize errors" test

case class RangedInternalErrorT(range: RangeS, message: String) extends ICompileErrorT

object ErrorReporter {
  def report(err: ICompileErrorT): Nothing = {
    throw CompileErrorExceptionT(err)
  }
}
