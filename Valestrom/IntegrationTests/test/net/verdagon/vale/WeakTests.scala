package net.verdagon.vale

import net.verdagon.vale.scout.{Environment => _, FunctionEnvironment => _, IEnvironment => _}
import net.verdagon.vale.templar._
import net.verdagon.vale.templar.citizen.WeakableImplingMismatch
import net.verdagon.vale.templar.env.ReferenceLocalVariable2
import net.verdagon.vale.templar.expression.TookWeakRefOfNonWeakableError
import net.verdagon.vale.templar.types._
import net.verdagon.vale.vivem.ConstraintViolatedException
import net.verdagon.von.VonInt
import org.scalatest.{FunSuite, Matchers}

class WeakTests extends FunSuite with Matchers {
  test("Make and lock weak ref then destroy own, with struct") {
    val compile = RunCompilation.test(
        Tests.loadExpected("programs/weaks/lockWhileLiveStruct.vale"))

    val main = compile.expectTemputs().lookupFunction("main")
    main.only({
      case LetNormal2(ReferenceLocalVariable2(FullName2(_,CodeVarName2("weakMuta")),Final,Coord(Weak, Readonly, _)),refExpr) => {
        refExpr.resultRegister.reference match {
          case Coord(Weak, Readonly, StructRef2(simpleName("Muta"))) =>
        }
      }
    })

    compile.evalForReferend(Vector()) shouldEqual VonInt(7)
  }

  test("Destroy own then locking gives none, with struct") {
    val compile = RunCompilation.test(
        Tests.loadExpected("programs/weaks/dropThenLockStruct.vale"))

    compile.evalForReferend(Vector()) shouldEqual VonInt(42)
  }

  test("Drop while locked, with struct") {
    val compile = RunCompilation.test(
        Tests.loadExpected("programs/weaks/dropWhileLockedStruct.vale"))

    try {
      compile.evalForReferend(Vector()) shouldEqual VonInt(42)
      vfail()
    } catch {
      case ConstraintViolatedException(_) =>
      case _ => vfail()
    }
  }

  test("Make and lock weak ref from borrow local then destroy own, with struct") {
    val compile = RunCompilation.test(
      Tests.loadExpected("programs/weaks/weakFromLocalCRefStruct.vale"))

    val main = compile.expectTemputs().lookupFunction("main")

    vassert(main.body.all({ case SoftLoad2(_, Weak, Readonly) => true }).size >= 1)

    compile.evalForReferend(Vector()) shouldEqual VonInt(7)
  }

  test("Make and lock weak ref from borrow then destroy own, with struct") {
    val compile = RunCompilation.test(
        Tests.loadExpected("programs/weaks/weakFromCRefStruct.vale"))

    val main = compile.expectTemputs().lookupFunction("main")

    vassert(main.body.all({ case SoftLoad2(_, Weak, Readonly) => true }).size >= 1)

    compile.evalForReferend(Vector()) shouldEqual VonInt(7)
  }

  test("Make weak ref from temporary") {
    val compile = RunCompilation.test(
        """
          |struct Muta weakable { hp int; }
          |fn getHp(weakMuta &&Muta) int { lock(weakMuta)^.get().hp }
          |fn main() int export { getHp(&&Muta(7)) }
          |""".stripMargin)

    val main = compile.expectTemputs().lookupFunction("main")
    main.body.only({ case WeakAlias2(_) => })
    compile.evalForReferend(Vector()) shouldEqual VonInt(7)
  }

  test("Cant make weak ref to non-weakable") {
    val compile = RunCompilation.test(
        """
          |struct Muta { hp int; }
          |fn getHp(weakMuta &&Muta) { lock(weakMuta)^.get().hp }
          |fn main() int export { getHp(&&Muta(7)) }
          |""".stripMargin)

    try {
      compile.expectTemputs().lookupFunction("main")
      vfail()
    } catch {
      case TookWeakRefOfNonWeakableError() =>
      case _ => vfail()
    }

  }

  test("Cant make weakable extend a non-weakable") {
    val compile = RunCompilation.test(
        """
          |interface IUnit {}
          |struct Muta weakable { hp int; }
          |impl IUnit for Muta;
          |fn main(muta Muta) int  { 7 }
          |""".stripMargin)

    try {
      compile.expectTemputs().lookupFunction("main")
      vfail()
    } catch {
      case WeakableImplingMismatch(true, false) =>
      case _ => vfail()
    }
  }

  test("Cant make non-weakable extend a weakable") {
    val compile = RunCompilation.test(
        """
          |interface IUnit weakable {}
          |struct Muta { hp int; }
          |impl IUnit for Muta;
          |fn main(muta Muta) int  { 7 }
          |""".stripMargin)

    try {
      compile.expectTemputs().lookupFunction("main")
      vfail()
    } catch {
      case WeakableImplingMismatch(false, true) =>
      case other => {
        other.printStackTrace()
        vfail()
      }
    }
  }


  test("Make and lock weak ref then destroy own, with interface") {
    val compile = RunCompilation.test(
        Tests.loadExpected("programs/weaks/lockWhileLiveInterface.vale"))

    val main = compile.expectTemputs().lookupFunction("main")
    main.only({
      case LetNormal2(ReferenceLocalVariable2(FullName2(_,CodeVarName2("weakUnit")),Final,Coord(Weak, _, _)),refExpr) => {
        refExpr.resultRegister.reference match {
          case Coord(Weak, Readonly, InterfaceRef2(simpleName("IUnit"))) =>
        }
      }
    })

    compile.evalForReferend(Vector()) shouldEqual VonInt(7)
  }

  test("Destroy own then locking gives none, with interface") {
    val compile = RunCompilation.test(
        Tests.loadExpected("programs/weaks/dropThenLockInterface.vale"))

    compile.evalForReferend(Vector()) shouldEqual VonInt(42)
  }

  test("Drop while locked, with interface") {
    val compile = RunCompilation.test(
        Tests.loadExpected("programs/weaks/dropWhileLockedInterface.vale"))

    try {
      compile.evalForReferend(Vector()) shouldEqual VonInt(42)
      vfail()
    } catch {
      case ConstraintViolatedException(_) =>
      case _ => vfail()
    }
  }

  test("Make and lock weak ref from borrow local then destroy own, with interface") {
    val compile = RunCompilation.test(
        Tests.loadExpected("programs/weaks/weakFromLocalCRefInterface.vale"))

    val main = compile.expectTemputs().lookupFunction("main")

    vassert(main.body.all({ case SoftLoad2(_, Weak, Readonly) => true }).size >= 1)

    compile.evalForReferend(Vector()) shouldEqual VonInt(7)
  }

  test("Make and lock weak ref from borrow then destroy own, with interface") {
    val compile = RunCompilation.test(
        Tests.loadExpected("programs/weaks/weakFromCRefInterface.vale"))

    val main = compile.expectTemputs().lookupFunction("main")

    vassert(main.body.all({ case SoftLoad2(_, Weak, Readonly) => true }).size >= 1)

    compile.evalForReferend(Vector()) shouldEqual VonInt(7)
  }

  test("Call weak-self method, after drop") {
    val compile = RunCompilation.test(
        Tests.loadExpected("programs/weaks/callWeakSelfMethodAfterDrop.vale"))

    val main = compile.expectTemputs().lookupFunction("main")

    vassert(main.body.all({ case SoftLoad2(_, Weak, Readonly) => true }).size >= 1)

    val hamuts = compile.getHamuts()

    compile.evalForReferend(Vector()) shouldEqual VonInt(0)
  }

  test("Call weak-self method, while alive") {
    val compile = RunCompilation.test(
        Tests.loadExpected("programs/weaks/callWeakSelfMethodWhileLive.vale"))

    val main = compile.expectTemputs().lookupFunction("main")

    vassert(main.body.all({ case SoftLoad2(_, Weak, Readonly) => true }).size >= 1)

    val hamuts = compile.getHamuts()

    compile.evalForReferend(Vector()) shouldEqual VonInt(42)
  }

  test("Weak yonder member") {
    val compile = RunCompilation.test(
        """
          |struct Base {
          |  hp int;
          |}
          |struct Spaceship {
          |  origin &&Base;
          |}
          |fn main() int export {
          |  base = Base(73);
          |  ship = Spaceship(&&base);
          |
          |  base^.drop(); // Destroys base.
          |
          |  maybeOrigin = lock(ship.origin); «14»«15»
          |  if (not maybeOrigin.isEmpty()) { «16»
          |    o = maybeOrigin.get();
          |    ret o.hp;
          |  } else {
          |    ret 42;
          |  }
          |}
          |""".stripMargin)

    val main = compile.expectTemputs().lookupFunction("main")

    val hamuts = compile.getHamuts()

    compile.evalForReferend(Vector()) shouldEqual VonInt(42)
  }


}
