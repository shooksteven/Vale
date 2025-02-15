package net.verdagon.vale

import net.verdagon.vale.parser.FinalP
import net.verdagon.vale.templar._
import net.verdagon.vale.templar.env.ReferenceLocalVariable2
import net.verdagon.vale.templar.types.{Coord, Final, Int2, Readonly, Share}
import net.verdagon.von.VonInt
import org.scalatest.{FunSuite, Matchers}

class PatternTests extends FunSuite with Matchers {
  // To get something like this to work would be rather involved.
  //test("Test matching a single-member pack") {
  //  val compile = RunCompilation.test( "fn main() int export { [x] = (4); = x; }")
  //  compile.getTemputs()
  //  val main = temputs.lookupFunction("main")
  //  main.header.returnType shouldEqual Coord(Share, Readonly, Int2())
  //  compile.evalForReferend(Vector()) shouldEqual VonInt(4)
  //}

  test("Test matching a multiple-member seq of immutables") {
    // Checks that the 5 made it into y, and it was an int
    val compile = RunCompilation.test( "fn main() int export { (x, y) = [4, 5]; = y; }")
    val temputs = compile.expectTemputs()
    val main = temputs.lookupFunction("main")
    main.header.returnType shouldEqual Coord(Share, Readonly, Int2())
    compile.evalForReferend(Vector()) shouldEqual VonInt(5)
  }

  test("Test matching a multiple-member seq of mutables") {
    // Checks that the 5 made it into y, and it was an int
    val compile = RunCompilation.test(
      """
        |struct Marine { hp int; }
        |fn main() int export { (x, y) = [Marine(6), Marine(8)]; = y.hp; }
      """.stripMargin)
    val temputs = compile.expectTemputs()
    val main = temputs.lookupFunction("main");
    main.header.returnType shouldEqual Coord(Share, Readonly, Int2())
    compile.evalForReferend(Vector()) shouldEqual VonInt(8)
  }

  test("Test matching a multiple-member pack of immutable and own") {
    // Checks that the 5 made it into y, and it was an int
    val compile = RunCompilation.test(
      """
        |struct Marine { hp int; }
        |fn main() int export { (x, y) = [7, Marine(8)]; = y.hp; }
      """.stripMargin)
    val temputs = compile.expectTemputs()
    temputs.functions.head.header.returnType == Coord(Share, Readonly, Int2())
    compile.evalForReferend(Vector()) shouldEqual VonInt(8)
  }

  test("Test matching a multiple-member pack of immutable and borrow") {
    // Checks that the 5 made it into y, and it was an int
    val compile = RunCompilation.test(
      """
        |struct Marine { hp int; }
        |fn main() int export {
        |  m = Marine(8);
        |  (x, y) = [7, &m];
        |  = y.hp;
        |}
      """.stripMargin)
    val temputs = compile.expectTemputs()
    temputs.functions.head.header.returnType == Coord(Share, Readonly, Int2())
    compile.evalForReferend(Vector()) shouldEqual VonInt(8)
  }

  // Intentional failure 2021.02.28, we never implemented pattern destructuring
  test("Test imm struct param destructure") {
    // Checks that the 5 made it into y, and it was an int
    val compile = RunCompilation.test(
      """
        |
        |struct Vec3 { x int; y int; z int; } fn main() { refuelB(Vec3(1, 2, 3), 2); }
        |// Using above Vec3
        |
        |// Without destructuring:
        |fn refuelA(
        |    vec Vec3,
        |    len int) {
        |  Vec3(
        |      vec.x * len,
        |      vec.y * len,
        |      vec.z * len)
        |}
        |
        |// With destructuring:
        |fn refuelB(
        |    Vec3(x, y, z),
        |    len int) {
        |  Vec3(x * len, y * len, z * len)
        |}
        |""".stripMargin)
    val temputs = compile.expectTemputs()
    temputs.functions.head.header.returnType == Coord(Share, Readonly, Int2())
    compile.evalForReferend(Vector()) shouldEqual VonInt(8)
  }
}
