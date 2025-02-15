package net.verdagon.vale.hammer

import net.verdagon.vale.astronomer.{ICompileErrorA, ProgramA}
import net.verdagon.vale.hinputs.Hinputs
import net.verdagon.vale.metal._
import net.verdagon.vale.parser.{FileP, VariabilityP}
import net.verdagon.vale.scout.{ICompileErrorS, ProgramS}
import net.verdagon.vale.templar.{CitizenName2, ExportAs2, ExternFunctionName2, FullName2, FunctionName2, ICompileErrorT, IName2, IVarName2, ImmConcreteDestructorName2, ImmInterfaceDestructorName2, TemplarCompilation, TemplarCompilationOptions, types => t}
import net.verdagon.vale.{Builtins, FileCoordinateMap, INamespaceResolver, IProfiler, NamespaceCoordinate, NamespaceCoordinateMap, NullProfiler, Result, vassert, vfail, vwat}

import scala.collection.immutable.List

case class FunctionRefH(prototype: PrototypeH) {
  //  def functionType = prototype.functionType
  def fullName = prototype.fullName
}

case class LocalsBox(var inner: Locals) {
  def snapshot = inner

  def templarLocals: Map[FullName2[IVarName2], VariableIdH] = inner.templarLocals
  def unstackifiedVars: Set[VariableIdH] = inner.unstackifiedVars
  def locals: Map[VariableIdH, Local] = inner.locals
  def nextLocalIdNumber: Int = inner.nextLocalIdNumber

  def get(id: FullName2[IVarName2]) = inner.get(id)
  def get(id: VariableIdH) = inner.get(id)

  def markUnstackified(varId2: FullName2[IVarName2]): Unit = {
    inner = inner.markUnstackified(varId2)
  }

  def markUnstackified(varIdH: VariableIdH): Unit = {
    inner = inner.markUnstackified(varIdH)
  }
  def setNextLocalIdNumber(nextLocalIdNumber: Int): Unit = {
    inner = inner.copy(nextLocalIdNumber = nextLocalIdNumber)
  }

  def addHammerLocal(
    tyype: ReferenceH[ReferendH],
    variability: Variability):
  Local = {
    val (newInner, local) = inner.addHammerLocal(tyype, variability)
    inner = newInner
    local
  }

  def addTemplarLocal(
    hinputs: Hinputs,
    hamuts: HamutsBox,
    varId2: FullName2[IVarName2],
    variability: Variability,
    tyype: ReferenceH[ReferendH]):
  Local = {
    val (newInner, local) = inner.addTemplarLocal(hinputs, hamuts, varId2, variability, tyype)
    inner = newInner
    local
  }

}

// This represents the locals for the entire function.
// Note, some locals will have the same index, that just means they're in
// different blocks.
case class Locals(
     // This doesn't have all the locals that are in the locals list, this just
     // has any locals added by templar.
     templarLocals: Map[FullName2[IVarName2], VariableIdH],

     unstackifiedVars: Set[VariableIdH],

     // This has all the locals for the function, a superset of templarLocals.
     locals: Map[VariableIdH, Local],

     nextLocalIdNumber: Int) {

  def addTemplarLocal(
    hinputs: Hinputs,
    hamuts: HamutsBox,
    varId2: FullName2[IVarName2],
    variability: Variability,
    tyype: ReferenceH[ReferendH]):
  (Locals, Local) = {
    if (templarLocals.contains(varId2)) {
      vfail("There's already a templar local named: " + varId2)
    }
    val newLocalHeight = locals.size
    val newLocalIdNumber = nextLocalIdNumber
    val varIdNameH = NameHammer.translateFullName(hinputs, hamuts, varId2)
    val newLocalId = VariableIdH(newLocalIdNumber, newLocalHeight, Some(varIdNameH))
    // Temporary until catalyst fills in stuff here
    val keepAlive = newLocalId.name.map(_.readableName).getOrElse("").endsWith("__tether");
    val newLocal = Local(newLocalId, variability, tyype, keepAlive)
    val newLocals =
      Locals(
        templarLocals + (varId2 -> newLocalId),
        unstackifiedVars,
        locals + (newLocalId -> newLocal),
        newLocalIdNumber + 1)
    (newLocals, newLocal)
  }

  def addHammerLocal(
    tyype: ReferenceH[ReferendH],
    variability: Variability):
  (Locals, Local) = {
    val newLocalHeight = locals.size
    val newLocalIdNumber = nextLocalIdNumber
    val newLocalId = VariableIdH(newLocalIdNumber, newLocalHeight, None)
    val newLocal = Local(newLocalId, variability, tyype, false)
    val newLocals =
      Locals(
        templarLocals,
        unstackifiedVars,
        locals + (newLocalId -> newLocal),
        newLocalIdNumber + 1)
    (newLocals, newLocal)
  }

  def markUnstackified(varId2: FullName2[IVarName2]): Locals = {
    markUnstackified(templarLocals(varId2))
  }

  def markUnstackified(varIdH: VariableIdH): Locals = {
    // Make sure it existed and wasnt already unstackified
    vassert(locals.contains(varIdH))
    if (unstackifiedVars.contains(varIdH)) {
      vfail("Already unstackified " + varIdH)
    }
    Locals(templarLocals, unstackifiedVars + varIdH, locals, nextLocalIdNumber)
  }

  def get(varId: FullName2[IVarName2]): Option[Local] = {
    templarLocals.get(varId) match {
      case None => None
      case Some(index) => Some(locals(index))
    }
  }

  def get(varId: VariableIdH): Option[Local] = {
    locals.get(varId)
  }
}

object Hammer {
  def translate(hinputs: Hinputs): ProgramH = {
    val Hinputs(
      interfaces,
      structs,
      emptyPackStructRef,
      functions,
      exports,
      externPrototypes2,
      edgeBlueprintsByInterface,
      edges) = hinputs


    val hamuts = HamutsBox(Hamuts(Map(), Map(), Map(), Map(), List(), List(), List(), Map(), Map(), Map(), Map()))
    val emptyPackStructRefH = StructHammer.translateStructRef(hinputs, hamuts, emptyPackStructRef)
    vassert(emptyPackStructRefH == ProgramH.emptyTupleStructRef)

    // We generate the names here first, so that externs get the first chance at having
    // ID 0 for each name, which means we dont need to add _1 _2 etc to the end of them,
    // and they'll match up with the actual outside names.
    val externPrototypesH =
      externPrototypes2.map(prototype2 => {
        val fullNameH = NameHammer.translateFullName(hinputs, hamuts, prototype2.fullName)
        val humanName =
          prototype2.fullName.last match {
            case ExternFunctionName2(humanName, _) => humanName
            case _ => vfail("Only human-named functions can be extern")
          }
        if (fullNameH.readableName != humanName) {
          vfail("Name conflict, two externs with the same name!")
        }
        FunctionHammer.translatePrototype(hinputs, hamuts, prototype2)
      })

    StructHammer.translateInterfaces(hinputs, hamuts);
    StructHammer.translateStructs(hinputs, hamuts)
    val userFunctions = functions.filter(f => f.header.isUserFunction).toList
    val nonUserFunctions = functions.filter(f => !f.header.isUserFunction).toList
    FunctionHammer.translateFunctions(hinputs, hamuts, userFunctions)
    FunctionHammer.translateFunctions(hinputs, hamuts, nonUserFunctions)

    exports.foreach({ case ExportAs2(tyype, exportedName) =>
      val kindH = TypeHammer.translateKind(hinputs, hamuts, tyype)
      val nameH =
        kindH match {
          case UnknownSizeArrayTH(name) => name
          case KnownSizeArrayTH(name) => name
          case StructRefH(name) => name
          case InterfaceRefH(name) => name
        }
      hamuts.addExport(nameH, exportedName)
    })

    val immDestructors2 =
      functions.filter(function => {
        function.header.fullName match {
          case FullName2(List(), ImmConcreteDestructorName2(_)) => true
          case FullName2(List(), ImmInterfaceDestructorName2(_, _)) => true
          case _ => false
        }
      })

    val immDestructorPrototypesH =
      immDestructors2.map(immDestructor2 => {
        val kindH = TypeHammer.translateReference(hinputs, hamuts, immDestructor2.header.params.head.tyype).kind
        val immDestructorPrototypeH = FunctionHammer.translateFunction(hinputs, hamuts, immDestructor2).prototype
        (kindH -> immDestructorPrototypeH)
      }).toMap

    immDestructorPrototypesH.foreach({ case (kindH, immDestructorPrototypeH) => {
      vassert(immDestructorPrototypeH.params.head.kind == kindH)
    }})

    val fullNameToExportedNames = hamuts.exportedNameToFullName.groupBy(_._2).map({ case (k, v) => (k, v.keys.toList) })
//    if (fullNameToExportedNames.size != hamuts.exportedNameToFullName.size) {
//      fullNameToExportedNames.foreach({ case (fullName, exportedName) =>
//        if (hamuts.exportedNameToFullName(exportedName) != fullName) {
//          vfail("Exported name conflict: " + exportedName + ": " + fullName + " and " + hamuts.exportedNameToFullName(exportedName))
//        }
//      })
//
//      // If we get here, something went very wrong
//      hamuts.exportedNameToFullName.foreach({ case (exportedName, fullName) =>
//        println(exportedName + ": " + fullName)
//      })
//      vfail()
//    }

    ProgramH(
      hamuts.interfaceDefs.values.toList,
      hamuts.structDefs,
      externPrototypesH,
      hamuts.functionDefs.values.toList,
      hamuts.inner.knownSizeArrays,
      hamuts.inner.unknownSizeArrays,
      immDestructorPrototypesH,
      fullNameToExportedNames,
      List())
  }

  def exportName(hamuts: HamutsBox, fullName2: FullName2[IName2], fullNameH: FullNameH) = {
    val exportedName =
      fullName2.last match {
        case FunctionName2(humanName, _, _) => humanName
        case CitizenName2(humanName, _) => humanName
        case _ => vfail("Can't export something that doesn't have a human readable name!")
      }
    hamuts.exportedNameToFullName.get(exportedName) match {
      case None =>
      case Some(existingFullName) => {
        vfail("Can't export " + fullNameH + " as " + exportedName + ", that exported name already taken by " + existingFullName)
      }
    }
    hamuts.addExport(fullNameH, exportedName)
  }
}

