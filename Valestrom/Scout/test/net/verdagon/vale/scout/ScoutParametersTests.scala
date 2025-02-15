package net.verdagon.vale.scout

import net.verdagon.vale.parser._
import net.verdagon.vale.scout.patterns.{AtomSP, CaptureS}
import net.verdagon.vale.scout.rules._
import net.verdagon.vale.{Err, FileCoordinate, Ok, vassert, vfail}
import org.scalatest.{FunSuite, Matchers}

class ScoutParametersTests extends FunSuite with Matchers {

  private def compile(code: String): ProgramS = {
    Parser.runParser(code) match {
      case ParseFailure(err) => fail(err.toString)
      case ParseSuccess(program0) => {
        Scout.scoutProgram(FileCoordinate.test, program0) match {
          case Err(e) => vfail(e.toString)
          case Ok(t) => t
        }
      }
    }
  }

  test("Simple rune rule") {
    val program1 = compile("""fn main<T>(moo T) infer-ret { }""")
    val main = program1.lookupFunction("main")

    val runeInRules =
      main.templateRules match {
        case List(TypedSR(_, rune @ CodeRuneS("T"),CoordTypeSR)) => rune
      }
    RuleSUtils.getDistinctOrderedRunesForRulexes(main.templateRules) match {
      case List(runeFromFunc) => vassert(runeInRules == runeFromFunc)
    }
  }

  test("Borrowed rune") {
    val program1 = compile("""fn main<T>(moo &T) infer-ret { }""")
    val main = program1.lookupFunction("main")
    val List(param) = main.params

    val tCoordRuneFromParams =
      param match {
        case ParameterS(
          AtomSP(_,
            CaptureS(CodeVarNameS("moo"),FinalP),
            None,
            tcr @ ImplicitRuneS(_,_),
            None)) => tcr
      }

    val tCoordRuneFromRules =
      main.templateRules match {
        case List(
          EqualsSR(_,
            TypedSR(_,tcr @ ImplicitRuneS(_,_),CoordTypeSR),
            TemplexSR(InterpretedST(_,ConstraintP,ReadonlyP,RuneST(_,CodeRuneS("T")))))) => tcr
      }

    tCoordRuneFromParams shouldEqual tCoordRuneFromRules
  }

  test("Anonymous typed param") {
    val program1 = compile("""fn main(_ int) infer-ret { }""")
    val main = program1.lookupFunction("main")
    val List(param) = main.params
    val paramRune =
      param match {
        case ParameterS(
          AtomSP(_,
            CaptureS(UnnamedLocalNameS(_),FinalP),
            None,
            pr @ ImplicitRuneS(_, 0),
            None)) => pr
      }

    main.templateRules match {
      case List(
        EqualsSR(_,
          TypedSR(_, pr,CoordTypeSR),
          TemplexSR(NameST(_, CodeTypeNameS("int"))))) => {
        vassert(pr == paramRune)
      }
    }

    RuleSUtils.getDistinctOrderedRunesForRulexes(main.templateRules) shouldEqual
      List(paramRune)
  }

  test("Anonymous untyped param") {
    val program1 = compile("""fn main(_) infer-ret { }""")
    val main = program1.lookupFunction("main")
    val List(param) = main.params
    val paramRune =
      param match {
        case ParameterS(
         AtomSP(_,
          CaptureS(UnnamedLocalNameS(_),FinalP),
          None,
          pr @ ImplicitRuneS(_, 0),
          None)) => pr
      }

    main.templateRules match {
      case List(TypedSR(_, pr,CoordTypeSR)) => {
        vassert(pr == paramRune)
      }
    }

    RuleSUtils.getDistinctOrderedRunesForRulexes(main.templateRules) shouldEqual
      List(paramRune)
  }

  test("Rune destructure") {
    // This is an ambiguous case but we decided it should destructure a struct or sequence, see CSTODTS in docs.
    val program1 = compile("""fn main<T>(moo T(a int)) infer-ret { }""")
    val main = program1.lookupFunction("main")

    val List(param) = main.params

    val (aRune, tRune) =
      param match {
        case ParameterS(
            AtomSP(_,
              CaptureS(CodeVarNameS("moo"),FinalP),
              None,
              tr @ CodeRuneS("T"),
              Some(
                List(
                  AtomSP(_,
                    CaptureS(CodeVarNameS("a"),FinalP),
                    None,
                    ar @ ImplicitRuneS(_, 0),
                    None))))) => (ar, tr)
      }

    main.templateRules match {
      case List(
        TypedSR(_, tr,CoordTypeSR),
        EqualsSR(_,
          TypedSR(_, ar,CoordTypeSR),
          TemplexSR(NameST(_, CodeTypeNameS("int"))))) => {
        vassert(tr == tRune)
        vassert(ar == aRune)
      }
    }

    RuleSUtils.getDistinctOrderedRunesForRulexes(main.templateRules) shouldEqual
      List(tRune, aRune)

    // See CCAUIR.
    main.identifyingRunes shouldEqual List(tRune)
  }
}
