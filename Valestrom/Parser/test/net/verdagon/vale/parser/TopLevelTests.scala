package net.verdagon.vale.parser

import net.verdagon.vale.{Tests, vassert}
import org.scalatest.{FunSuite, Matchers}



class TopLevelTests extends FunSuite with Matchers with Collector with TestParseUtils {
  test("Function then struct") {
    val program = compileProgram(
      """
        |fn main() int export {}
        |
        |struct mork { }
        |""".stripMargin)
    program.topLevelThings(0) match { case TopLevelFunctionP(_) => }
    program.topLevelThings(1) match { case TopLevelStructP(_) => }
  }


  test("Reports unrecognized at top level") {
    val code =
      """fn main(){}
        |blort
        |""".stripMargin
    val err = compileProgramForError(code)
    err match {
      case UnrecognizedTopLevelThingError(12) =>
    }
  }

  // To support the examples on the site for the syntax highlighter
  test("empty") {
    val program = compileProgram("fn foo() { ... }")
    program.topLevelThings(0) match {
      case TopLevelFunctionP(
      FunctionP(_,
      _,
      Some(BlockPE(_,List(VoidPE(_)))))) =>
    }
  }

  test("exporting int") {
    val program = compileProgram("export int as NumberThing;")
    program.topLevelThings(0) match {
      case TopLevelExportAsP(ExportAsP(_,NameOrRunePT(NameP(_,"int")),NameP(_,"NumberThing"))) =>

    }
  }

  test("exporting array") {
    val program = compileProgram("export Array<mut, int> as IntArray;")
    program.topLevelThings(0) match {
      case TopLevelExportAsP(ExportAsP(_,_,NameP(_,"IntArray"))) =>
    }
  }

  test("import wildcard") {
    val program = compileProgram("import somemodule.*;")
    program.topLevelThings(0) match {
      case TopLevelImportP(ImportP(_, NameP(_, "somemodule"), List(), NameP(_, "*"))) =>
    }
  }

  test("import just module and thing") {
    val program = compileProgram("import somemodule.List;")
    program.topLevelThings(0) match {
      case TopLevelImportP(ImportP(_, NameP(_, "somemodule"), List(), NameP(_, "List"))) =>
    }
  }

  test("full import") {
    val program = compileProgram("import somemodule.subpackage.List;")
    program.topLevelThings(0) match {
      case TopLevelImportP(ImportP(_, NameP(_, "somemodule"), List(NameP(_, "subpackage")), NameP(_, "List"))) =>
    }
  }
}
