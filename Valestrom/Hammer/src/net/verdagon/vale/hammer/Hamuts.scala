package net.verdagon.vale.hammer

import net.verdagon.vale.metal._
import net.verdagon.vale.templar.{FullName2, IName2}
import net.verdagon.vale.templar.templata.Prototype2
import net.verdagon.vale.templar.types.{InterfaceRef2, PackT2, StructRef2}
import net.verdagon.vale.{vassert, vimpl}
import net.verdagon.von.IVonData


case class HamutsBox(var inner: Hamuts) {

  def exportedNameToFullName: Map[String, FullNameH] = inner.exportedNameToFullName
  def structRefsByRef2: Map[StructRef2, StructRefH] = inner.structRefsByRef2
  def structDefsByRef2: Map[StructRef2, StructDefinitionH] = inner.structDefsByRef2
  def structDefs: List[StructDefinitionH] = inner.structDefs
  def interfaceRefs: Map[InterfaceRef2, InterfaceRefH] = inner.interfaceRefs
  def interfaceDefs: Map[InterfaceRef2, InterfaceDefinitionH] = inner.interfaceDefs
  def functionRefs: Map[Prototype2, FunctionRefH] = inner.functionRefs
  def functionDefs: Map[Prototype2, FunctionH] = inner.functionDefs

  def forwardDeclareStruct(structRef2: StructRef2, structRefH: StructRefH): Unit = {
    inner = inner.forwardDeclareStruct(structRef2, structRefH)
  }

  def addStructOriginatingFromTemplar(structRef2: StructRef2, structDefH: StructDefinitionH): Unit = {
    inner = inner.addStructOriginatingFromTemplar(structRef2, structDefH)
  }

  def addStructOriginatingFromHammer(structDefH: StructDefinitionH): Unit = {
    inner = inner.addStructOriginatingFromHammer(structDefH)
  }

  def forwardDeclareInterface(interfaceRef2: InterfaceRef2, interfaceRefH: InterfaceRefH): Unit = {
    inner = inner.forwardDeclareInterface(interfaceRef2, interfaceRefH)
  }

  def addInterface(interfaceRef2: InterfaceRef2, interfaceDefH: InterfaceDefinitionH): Unit = {
    inner = inner.addInterface(interfaceRef2, interfaceDefH)
  }

  def addKnownSizeArray(knownSizeArrayDefinitionTH: KnownSizeArrayDefinitionTH): Unit = {
    inner = inner.addKnownSizeArray(knownSizeArrayDefinitionTH)
  }

  def addUnknownSizeArray(unknownSizeArrayDefinitionTH: UnknownSizeArrayDefinitionTH): Unit = {
    inner = inner.addUnknownSizeArray(unknownSizeArrayDefinitionTH)
  }

  def forwardDeclareFunction(functionRef2: Prototype2, functionRefH: FunctionRefH): Unit = {
    inner = inner.forwardDeclareFunction(functionRef2, functionRefH)
  }

  def addFunction(functionRef2: Prototype2, functionDefH: FunctionH): Unit = {
    inner = inner.addFunction(functionRef2, functionDefH)
  }

  def addExport(fullNameH: FullNameH, exportedName: String): Unit = {
    inner = inner.addExport(fullNameH, exportedName)
  }

  def getNameId(readableName: String, parts: List[IVonData]): Int = {
    val (newInner, id) = inner.getNameId(readableName, parts)
    inner = newInner
    id
  }

  def getKnownSizeArray(knownSizeArrayTH: KnownSizeArrayTH): KnownSizeArrayDefinitionTH = {
    inner.getKnownSizeArray(knownSizeArrayTH)
  }
  def getUnknownSizeArray(unknownSizeArrayTH: UnknownSizeArrayTH): UnknownSizeArrayDefinitionTH = {
    inner.getUnknownSizeArray(unknownSizeArrayTH)
  }
}

case class Hamuts(
    idByFullNameByHumanName: Map[String, Map[String, Int]],
    exportedNameToFullName: Map[String, FullNameH],
    structRefsByRef2: Map[StructRef2, StructRefH],
    structDefsByRef2: Map[StructRef2, StructDefinitionH],
    structDefs: List[StructDefinitionH],
    knownSizeArrays: List[KnownSizeArrayDefinitionTH],
    unknownSizeArrays: List[UnknownSizeArrayDefinitionTH],
    interfaceRefs: Map[InterfaceRef2, InterfaceRefH],
    interfaceDefs: Map[InterfaceRef2, InterfaceDefinitionH],
    functionRefs: Map[Prototype2, FunctionRefH],
    functionDefs: Map[Prototype2, FunctionH]) {
  def forwardDeclareStruct(structRef2: StructRef2, structRefH: StructRefH): Hamuts = {
    Hamuts(
      idByFullNameByHumanName,
      exportedNameToFullName,
      structRefsByRef2 + (structRef2 -> structRefH),
      structDefsByRef2,
      structDefs,
      knownSizeArrays,
      unknownSizeArrays,
      interfaceRefs,
      interfaceDefs,
      functionRefs,
      functionDefs)
  }

  def addStructOriginatingFromTemplar(structRef2: StructRef2, structDefH: StructDefinitionH): Hamuts = {
    vassert(structRefsByRef2.contains(structRef2))
    Hamuts(
      idByFullNameByHumanName,
      exportedNameToFullName,
      structRefsByRef2,
      structDefsByRef2 + (structRef2 -> structDefH),
      structDefs :+ structDefH,
      knownSizeArrays,
      unknownSizeArrays,
      interfaceRefs,
      interfaceDefs,
      functionRefs,
      functionDefs)
  }

  def addStructOriginatingFromHammer(structDefH: StructDefinitionH): Hamuts = {
    Hamuts(
      idByFullNameByHumanName,
      exportedNameToFullName,
      structRefsByRef2,
      structDefsByRef2,
      structDefs :+ structDefH,
      knownSizeArrays,
      unknownSizeArrays,
      interfaceRefs,
      interfaceDefs,
      functionRefs,
      functionDefs)
  }

  def forwardDeclareInterface(interfaceRef2: InterfaceRef2, interfaceRefH: InterfaceRefH): Hamuts = {
    Hamuts(
      idByFullNameByHumanName,
      exportedNameToFullName,
      structRefsByRef2,
      structDefsByRef2,
      structDefs,
      knownSizeArrays,
      unknownSizeArrays,
      interfaceRefs + (interfaceRef2 -> interfaceRefH),
      interfaceDefs,
      functionRefs,
      functionDefs)
  }

  def addInterface(interfaceRef2: InterfaceRef2, interfaceDefH: InterfaceDefinitionH): Hamuts = {
    vassert(interfaceRefs.contains(interfaceRef2))
    Hamuts(
      idByFullNameByHumanName,
      exportedNameToFullName,
      structRefsByRef2,
      structDefsByRef2,
      structDefs,
      knownSizeArrays,
      unknownSizeArrays,
      interfaceRefs,
      interfaceDefs + (interfaceRef2 -> interfaceDefH),
      functionRefs,
      functionDefs)
  }

  def forwardDeclareFunction(functionRef2: Prototype2, functionRefH: FunctionRefH): Hamuts = {
    Hamuts(
      idByFullNameByHumanName,
      exportedNameToFullName,
      structRefsByRef2,
      structDefsByRef2,
      structDefs,
      knownSizeArrays,
      unknownSizeArrays,
      interfaceRefs,
      interfaceDefs,
      functionRefs + (functionRef2 -> functionRefH),
      functionDefs)
  }

  def addFunction(functionRef2: Prototype2, functionDefH: FunctionH): Hamuts = {
    vassert(functionRefs.contains(functionRef2))

    Hamuts(
      idByFullNameByHumanName,
      exportedNameToFullName,
      structRefsByRef2,
      structDefsByRef2,
      structDefs,
      knownSizeArrays,
      unknownSizeArrays,
      interfaceRefs,
      interfaceDefs,
      functionRefs,
      functionDefs + (functionRef2 -> functionDefH))
  }

  def addExport(fullNameH: FullNameH, exportedName: String): Hamuts = {
    vassert(!exportedNameToFullName.contains(exportedName))

    Hamuts(
      idByFullNameByHumanName,
      exportedNameToFullName + (exportedName -> fullNameH),
      structRefsByRef2,
      structDefsByRef2,
      structDefs,
      knownSizeArrays,
      unknownSizeArrays,
      interfaceRefs,
      interfaceDefs,
      functionRefs,
      functionDefs)
  }

  def addKnownSizeArray(knownSizeArrayDefinitionTH: KnownSizeArrayDefinitionTH): Hamuts = {
    Hamuts(
      idByFullNameByHumanName,
      exportedNameToFullName,
      structRefsByRef2,
      structDefsByRef2,
      structDefs,
      knownSizeArrays :+ knownSizeArrayDefinitionTH,
      unknownSizeArrays,
      interfaceRefs,
      interfaceDefs,
      functionRefs,
      functionDefs)
  }

  def addUnknownSizeArray(unknownSizeArrayDefinitionTH: UnknownSizeArrayDefinitionTH): Hamuts = {
    Hamuts(
      idByFullNameByHumanName,
      exportedNameToFullName,
      structRefsByRef2,
      structDefsByRef2,
      structDefs,
      knownSizeArrays,
      unknownSizeArrays :+ unknownSizeArrayDefinitionTH,
      interfaceRefs,
      interfaceDefs,
      functionRefs,
      functionDefs)
  }

  // This returns a unique ID for that specific human name.
  // Two things with two different human names could result in the same ID here.
  // This ID is meant to be concatenated onto the human name.
  def getNameId(readableName: String, parts: List[IVonData]): (Hamuts, Int) = {
    val namePartsString = FullNameH.namePartsToString(parts)
    val idByFullNameForHumanName =
      idByFullNameByHumanName.get(readableName) match {
        case None => Map[String, Int]()
        case Some(x) => x
      }
    val id =
      idByFullNameForHumanName.get(namePartsString) match {
        case None => idByFullNameForHumanName.size
        case Some(i) => i
      }
    val idByFullNameForHumanNameNew = idByFullNameForHumanName + (namePartsString -> id)
    val idByFullNameByHumanNameNew = idByFullNameByHumanName + (readableName -> idByFullNameForHumanNameNew)
    val newHamuts =
      Hamuts(
        idByFullNameByHumanNameNew,
        exportedNameToFullName,
        structRefsByRef2,
        structDefsByRef2,
        structDefs,
        knownSizeArrays,
        unknownSizeArrays,
        interfaceRefs,
        interfaceDefs,
        functionRefs,
        functionDefs)
    (newHamuts, id)
  }

  def getKnownSizeArray(knownSizeArrayTH: KnownSizeArrayTH): KnownSizeArrayDefinitionTH = {
    knownSizeArrays.find(_.name == knownSizeArrayTH.name).get
  }
  def getUnknownSizeArray(unknownSizeArrayTH: UnknownSizeArrayTH): UnknownSizeArrayDefinitionTH = {
    unknownSizeArrays.find(_.name == unknownSizeArrayTH.name).get
  }

}
