package net.verdagon.vale

import net.verdagon.vale.templar.{CitizenName2, FullName2, FunctionName2, simpleName}
import net.verdagon.vale.templar.templata.{Abstract2, Signature2}
import net.verdagon.vale.templar.types._
import org.scalatest.{FunSuite, Matchers}
import net.verdagon.vale.vivem.IntV
import net.verdagon.von.VonInt

class VirtualTests extends FunSuite with Matchers {

    test("Simple program containing a virtual function") {
      val compile = RunCompilation.test(
        """
          |interface I {}
          |fn doThing(virtual i I) int {4}
          |fn main(i I) int {
          |  doThing(i)
          |}
        """.stripMargin)
      val temputs = compile.expectTemputs()

      vassert(temputs.getAllUserFunctions.size == 2)
      vassert(temputs.lookupFunction("main").header.returnType == Coord(Share, Readonly, Int2()))

      val doThing =
        vassertSome(
          temputs.lookupFunction(
            Signature2(
              FullName2(
                List(),
                FunctionName2(
                  "doThing",
                  List(),
                  List(
                    Coord(
                      Own,
                      Readwrite,
                      InterfaceRef2(
                        FullName2(List(), CitizenName2("I", List()))))))))))
      vassert(doThing.header.params(0).virtuality.get == Abstract2)
    }

  test("Can call virtual function") {
    val compile = RunCompilation.test(
      """
        |interface I {}
        |fn doThing(virtual i I) int {4}
        |fn main(i I) int {
        |  doThing(i)
        |}
      """.stripMargin)
    val temputs = compile.expectTemputs()

    vassert(temputs.getAllUserFunctions.size == 2)
    vassert(temputs.lookupFunction("main").header.returnType == Coord(Share, Readonly, Int2()))


    val doThing =
      vassertSome(
        temputs.lookupFunction(
          Signature2(
            FullName2(
              List(),
              FunctionName2(
                "doThing",
                List(),
                List(
                  Coord(
                    Own,
                    Readwrite,
                    InterfaceRef2(
                      FullName2(List(), CitizenName2("I", List()))))))))))
    vassert(doThing.header.params(0).virtuality.get == Abstract2)
  }

  test("Can call interface env's function from outside") {
    val compile = RunCompilation.test(
      """
        |interface I {
        |  fn doThing(virtual i I) int;
        |}
        |fn main(i I) int {
        |  doThing(i)
        |}
      """.stripMargin)
    val temputs = compile.expectTemputs()

    vassert(temputs.getAllUserFunctions.size == 1)
    vassert(temputs.lookupFunction("main").header.returnType == Coord(Share, Readonly, Int2()))


    val doThing =
      vassertSome(
        temputs.lookupFunction(
          Signature2(
            FullName2(List(CitizenName2("I",List())),FunctionName2("doThing",List(),List(Coord(Own,Readwrite,InterfaceRef2(FullName2(List(),CitizenName2("I",List()))))))))))
    vassert(doThing.header.params(0).virtuality.get == Abstract2)
  }


  test("Interface with method with param of substruct") {
    val compile = RunCompilation.test(
        """
          |import list.*;
          |interface SectionMember {}
          |struct Header {}
          |impl SectionMember for Header;
          |fn collectHeaders2(header &List<&Header>, virtual this &SectionMember) abstract;
          |fn collectHeaders2(header &List<&Header>, this &Header impl SectionMember) { }
        """.stripMargin)
    val temputs = compile.getHamuts()
  }

  test("Open interface constructors") {
    val compile = RunCompilation.test(
        """
          |interface Bipedal {
          |  fn hop(virtual s &Bipedal) int;
          |  fn skip(virtual s &Bipedal) int;
          |}
          |
          |struct Human {  }
          |fn hop(s &Human impl Bipedal) int { 7 }
          |fn skip(s &Human impl Bipedal) int { 9 }
          |impl Bipedal for Human;
          |
          |fn hopscotch(s &Bipedal) int {
          |  s.hop();
          |  s.skip();
          |  = s.hop();
          |}
          |
          |fn main() export int {
          |   x = Bipedal({ 3 }, { 5 });
          |  // x is an unnamed substruct which implements Bipedal.
          |
          |  = hopscotch(&x);
          |}
        """.stripMargin)
    val temputs = compile.getHamuts()
    compile.evalForReferend(Vector()) shouldEqual VonInt(3)
  }

  test("Successful constraint downcast with as") {
    val compile = RunCompilation.test(
      Tests.loadExpected("programs/downcast/downcastConstraintSuccessful.vale"))
    compile.evalForReferend(Vector()) shouldEqual VonInt(42)
  }

  test("Failed constraint downcast with as") {
    val compile = RunCompilation.test(
      Tests.loadExpected("programs/downcast/downcastConstraintFailed.vale"))
    compile.evalForReferend(Vector()) shouldEqual VonInt(42)
  }
//
//  test("Successful owning downcast with as") {
//    val compile = RunCompilation.test(
//      Tests.loadExpected("programs/downcast/downcastOwningSuccessful.vale"))
//    compile.evalForReferend(Vector()) shouldEqual VonInt(42)
//  }
//
//  test("Failed owning downcast with as") {
////    i think the generated function isnt dropping the owning ref
//    // downcasting an owning ref should give a Result, so the user can drop it.
//    vimpl()
//
//    val compile = RunCompilation.test(
//      Tests.loadExpected("programs/downcast/downcastOwningFailed.vale"))
//    compile.evalForReferend(Vector()) shouldEqual VonInt(42)
//  }

}
