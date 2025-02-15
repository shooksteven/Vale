package net.verdagon.vale.astronomer.builtins

import net.verdagon.vale.astronomer._
import net.verdagon.vale.parser.{ConstraintP, CaptureP, FinalP, LendConstraintP, OwnP, ReadonlyP, ShareP, UseP}
import net.verdagon.vale.scout.{Environment => _, FunctionEnvironment => _, IEnvironment => _, _}
import net.verdagon.vale.scout.patterns.AtomSP

object RefCounting {
  val checkvarrc =
    FunctionA(
      RangeS.internal(-60),
      FunctionNameA("__checkvarrc", CodeLocationS.internal(-11)),
      List(),
      TemplateTemplataType(List(CoordTemplataType), FunctionTemplataType),
      Set(CodeRuneA("V"), CodeRuneA("I")),
      List(CodeRuneA("T")),
      Set(CodeRuneA("T"), CodeRuneA("TK"), CodeRuneA("V"), CodeRuneA("I")),
      Map(
        CodeRuneA("I") -> CoordTemplataType,
        CodeRuneA("T") -> CoordTemplataType,
        CodeRuneA("V") -> CoordTemplataType,
        CodeRuneA("TK") -> KindTemplataType
      ),
      List(
        ParameterA(AtomAP(RangeS.internal(-117), LocalVariableA(CodeVarNameA("obj"), FinalP, NotUsed, Used, NotUsed, NotUsed, NotUsed, NotUsed), None, CodeRuneA("T"), None)),
        ParameterA(AtomAP(RangeS.internal(-118), LocalVariableA(CodeVarNameA("num"), FinalP, NotUsed, Used, NotUsed, NotUsed, NotUsed, NotUsed), None, CodeRuneA("I"), None))),
      Some(CodeRuneA("V")),
      List(
        EqualsAR(RangeS.internal(-1418),TemplexAR(RuneAT(RangeS.internal(-5650),CodeRuneA("I"), CoordTemplataType)), TemplexAR(NameAT(RangeS.internal(-5651),CodeTypeNameA("int"), CoordTemplataType))),
        EqualsAR(
          RangeS.internal(-1419),
          TemplexAR(RuneAT(RangeS.internal(-5652),CodeRuneA("T"), CoordTemplataType)),
          ComponentsAR(
            RangeS.internal(-79),
            CoordTemplataType,
            List(
              TemplexAR(OwnershipAT(RangeS.internal(-5653),ConstraintP)),
              TemplexAR(PermissionAT(RangeS.internal(-5654),ReadonlyP)),
              TemplexAR(RuneAT(RangeS.internal(-5655),CodeRuneA("TK"), KindTemplataType))))),
        EqualsAR(RangeS.internal(-1420),TemplexAR(RuneAT(RangeS.internal(-5656),CodeRuneA("V"), CoordTemplataType)), TemplexAR(NameAT(RangeS.internal(-5657),CodeTypeNameA("void"), CoordTemplataType)))),
      CodeBodyA(
        BodyAE(
          RangeS.internal(-35),
          List(),
          BlockAE(
            RangeS.internal(-35),
            List(
              CheckRefCountAE(
                RangeS.internal(-35),
                LocalLoadAE(RangeS.internal(-35), CodeVarNameA("obj"), UseP),
                VariableRefCount,
                FunctionCallAE(
                  RangeS.internal(-42),
                  // We add 1 because that "obj" is also a borrow ref
                  OutsideLoadAE(RangeS.internal(-38),"+", LendConstraintP(None)),
                  List(
                    LocalLoadAE(RangeS.internal(-35), CodeVarNameA("num"), UseP),
                    IntLiteralAE(RangeS.internal(-35), 1)))),
              VoidAE(RangeS.internal(-35)))))))

  val checkMemberRcName = FunctionNameA("__checkmemberrc", CodeLocationS.internal(-5))
  val checkmemberrc =
    FunctionA(
      RangeS.internal(-59),
      checkMemberRcName,
      List(),
      TemplateTemplataType(List(CoordTemplataType), FunctionTemplataType),
      Set(CodeRuneA("V"), CodeRuneA("I")),
      List(CodeRuneA("T")),
      Set(CodeRuneA("T"), CodeRuneA("V"), CodeRuneA("I"), CodeRuneA("TK")),
      Map(
        CodeRuneA("I") -> CoordTemplataType,
        CodeRuneA("T") -> CoordTemplataType,
        CodeRuneA("V") -> CoordTemplataType,
        CodeRuneA("TK") -> KindTemplataType),
      List(
        ParameterA(AtomAP(RangeS.internal(-115), LocalVariableA(CodeVarNameA("obj"), FinalP, NotUsed, Used, NotUsed, NotUsed, NotUsed, NotUsed), None, CodeRuneA("T"), None)),
        ParameterA(AtomAP(RangeS.internal(-116), LocalVariableA(CodeVarNameA("num"), FinalP, NotUsed, Used, NotUsed, NotUsed, NotUsed, NotUsed), None, CodeRuneA("I"), None))),
      Some(CodeRuneA("V")),
      List(
        EqualsAR(RangeS.internal(-1421),TemplexAR(RuneAT(RangeS.internal(-5658),CodeRuneA("I"), CoordTemplataType)), TemplexAR(NameAT(RangeS.internal(-5659),CodeTypeNameA("int"), CoordTemplataType))),
        EqualsAR(RangeS.internal(-1422),TemplexAR(RuneAT(RangeS.internal(-5660),CodeRuneA("T"), CoordTemplataType)), ComponentsAR(RangeS.internal(-80), CoordTemplataType, List(TemplexAR(OwnershipAT(RangeS.internal(-5661),ConstraintP)), TemplexAR(RuneAT(RangeS.internal(-5662),CodeRuneA("TK"), KindTemplataType))))),
        EqualsAR(RangeS.internal(-1423),TemplexAR(RuneAT(RangeS.internal(-5663),CodeRuneA("V"), CoordTemplataType)), TemplexAR(NameAT(RangeS.internal(-5664),CodeTypeNameA("void"), CoordTemplataType)))),
      CodeBodyA(
        BodyAE(
          RangeS.internal(-35),
          List(),
          BlockAE(
            RangeS.internal(-35),
            List(
              CheckRefCountAE(RangeS.internal(-35), LocalLoadAE(RangeS.internal(-35), CodeVarNameA("obj"), UseP), MemberRefCount, LocalLoadAE(RangeS.internal(-35), CodeVarNameA("num"), UseP)),
              VoidAE(RangeS.internal(-35)))))))
}
