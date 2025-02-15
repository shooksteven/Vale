package net.verdagon.vale.parser.patterns

import net.verdagon.vale.{parser, vfail, vimpl}
import net.verdagon.vale.parser.Patterns.{fromEnv, withType}
import net.verdagon.vale.parser.CombinatorParsers._
import net.verdagon.vale.parser._
import org.scalatest.{FunSuite, Matchers}

class TypeTests extends FunSuite with Matchers with Collector {
  private def compile[T](parser: CombinatorParsers.Parser[T], code: String): T = {
    CombinatorParsers.parse(parser, code.toCharArray()) match {
      case CombinatorParsers.NoSuccess(msg, input) => {
        fail(msg + "\n" + input);
      }
      case CombinatorParsers.Success(expr, rest) => {
        if (!rest.atEnd) {
          vfail(rest.pos.longString)
        }
        expr
      }
    }
  }
  private def compile[T](code: String): PatternPP = {
    compile(atomPattern, code)
  }

  private def checkFail[T](parser: CombinatorParsers.Parser[T], code: String) = {
    CombinatorParsers.parse(parser, code) match {
      case CombinatorParsers.NoSuccess(_, _) =>
      case CombinatorParsers.Success(_, rest) => {
        if (!rest.atEnd) {
          // That's good, it didn't parse all of it
        } else {
          fail()
        }
      }
    }
  }

  test("Ignoring name") {
    compile("_ int") shouldHave { case fromEnv("int") => }
  }
//  test("Callable type") {
//    compile("_ fn(T)Void") shouldHave {
//      case withType(
//          FunctionPT(
//            None,
//            List(NameOrRunePT(StringP(_, "T"))),
//            NameOrRunePT(StringP(_, "void")))) =>
//    }
//  }
  test("15a") {
    compile("_ [3 * MutableStruct]") shouldHave {
      case withType(
          RepeaterSequencePT(_,
              MutabilityPT(_,MutableP),
              IntPT(_,3),
              NameOrRunePT(NameP(_, "MutableStruct")))) =>
    }
  }

  test("15b") {
    compile("_ [<imm> 3 * MutableStruct]") shouldHave {
      case withType(
        RepeaterSequencePT(_,
          MutabilityPT(_,ImmutableP),
          IntPT(_,3),
          NameOrRunePT(NameP(_, "MutableStruct")))) =>
    }
  }

  test("Sequence type") {
    compile("_ [int, bool]") shouldHave {
      case withType(
          ManualSequencePT(_,
            List(
              NameOrRunePT(NameP(_, "int")),
              NameOrRunePT(NameP(_, "bool"))))) =>
    }
  }
  test("15") {
    compile("_ &[3 * MutableStruct]") shouldHave {
      case PatternPP(_,_,
        None,
        Some(
          InterpretedPT(_,
            ConstraintP,
            ReadonlyP,
            RepeaterSequencePT(_,
              MutabilityPT(_,MutableP),
              IntPT(_,3),
              NameOrRunePT(NameP(_, "MutableStruct"))))),
        None,
        None) =>
    }
  }
  test("15m") {
    compile("_ &&[<_> 3 * MutableStruct]") shouldHave {
      case PatternPP(_,_,
        None,
        Some(
          InterpretedPT(_,
            WeakP,
            ReadonlyP,
            RepeaterSequencePT(_,
              AnonymousRunePT(_),
              IntPT(_,3),
              NameOrRunePT(NameP(_, "MutableStruct"))))),
        None,
        None) =>
    }
  }
  test("15z") {
    compile("_ MyOption<MyList<int>>") shouldHave {
      case PatternPP(_,_,
        None,
        Some(
          CallPT(
            _,
            NameOrRunePT(NameP(_, "MyOption")),
            List(
              CallPT(_,
                NameOrRunePT(NameP(_, "MyList")),
                List(
                  NameOrRunePT(NameP(_, "int"))))))),
        None,
        None) =>
    }
  }
}
