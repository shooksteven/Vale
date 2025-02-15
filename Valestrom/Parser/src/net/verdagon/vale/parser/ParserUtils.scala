package net.verdagon.vale.parser

import net.verdagon.vale.parser.CombinatorParsers.opt
import org.apache.commons.lang.StringEscapeUtils

import scala.util.parsing.combinator.RegexParsers
import scala.util.parsing.input.{OffsetPosition, Position, Positional}

trait ParserUtils extends RegexParsers {

  case class PosWrapper(u: Unit) extends Positional
  private[parser] def pos: Parser[Int] = {
    positioned(success() ^^ PosWrapper) ^^ (x => x.pos.asInstanceOf[OffsetPosition].offset)
  }

  private[parser] def existsW(str: String): Parser[Option[UnitP]] = {
    opt(pos ~ str ~ pos <~ white) ^^ {
      case None => None
      case Some(begin ~ str ~ end) => Some(UnitP(Range(begin, end)))
    }
  }

  // stands for positioned str
  private[parser] def pstr(str: String): Parser[NameP] = {
    pos ~ str ~ pos ^^ {
      case begin ~ str ~ end => NameP(Range(begin, end), str)
    }
  }

  // MW = maybe white
  private[parser] def existsMW(str: String): Parser[Option[UnitP]] = {
    opt(pos ~ str ~ pos <~ optWhite) ^^ {
      case None => None
      case Some(begin ~ str ~ end) => Some(UnitP(Range(begin, end)))
    }
  }

  private[parser] def white: Parser[Unit] = { "\\s+".r ^^^ () }
  private[parser] def optWhite: Parser[Unit] = { opt(white) ^^^ () }

  // soon, give special treatment to ^
  // we want marine^.item to explode marine and extract its item
  // but, we don't want it to parse it as (marine^).item
  // so, we need to look ahead a bit and see if there's a . after it.
  private[parser] def exprIdentifier: Parser[NameP] = {
    pos ~ """[^\s\.\!\$\&\,\:\(\)\;\[\]\{\}\'\*\^\"\<\>\=\`]+""".r ~ pos ^^ {
      case begin ~ str ~ end => NameP(Range(begin, end), str)
    }
  }

  private[parser] def functionIdentifier: Parser[NameP] = {
    pos ~ """[^\s\.\$\&\,\:\(\)\;\[\]\{\}\'\^\"\<\>\=\`]+""".r ~ pos ^^ {
      case begin ~ str ~ end => NameP(Range(begin, end), str)
    }
  }

  private[parser] def infixFunctionIdentifier: Parser[NameP] = {
    pos ~ """[^\s\.\$\&\,\:\(\)\;\[\]\{\}\'\"\<\>\=\`]+""".r ~ pos ^^ {
      case begin ~ str ~ end => NameP(Range(begin, end), str)
    }
  }

  private[parser] def typeIdentifier: Parser[NameP] = {
    pos ~ """[^\s\.\!\*\?\#\$\&\,\:\|\;\(\)\[\]\{\}=\<\>\`]+""".r ~ pos ^^ {
      case begin ~ str ~ end => NameP(Range(begin, end), str)
    }
  }
//
//  private[parser] def stringOr[T](string: String, parser: Parser[T]): Parser[Option[T]] = {
//    (string ^^ { val x: Option[T] = None; x } | parser ^^ (a => Some(a)))
//  }

  private[parser] def underscoreOr[T](parser: Parser[T]): Parser[Option[T]] = {
    ("_" ^^^ { val x: Option[T] = None; x } | parser ^^ (a => Some(a)))
  }

  private[parser] def int: Parser[Int] = {
    raw"^-?\d+".r ^^ {
      case thingStr => thingStr.toInt
    }
  }

  private[parser] def integer: Parser[IExpressionPE] = {
    pos ~ int ~ pos ^^ { case begin ~ n ~ end => IntLiteralPE(Range(begin, end), n) }
  }

  private[parser] def bool: Parser[IExpressionPE] = {
    pos ~ ("true"|"false") ~ pos ^^ {
      case begin ~ "true" ~ end => BoolLiteralPE(Range(begin, end), true)
      case begin ~ "false" ~ end => BoolLiteralPE(Range(begin, end), false)
    }
  }


  private[parser] def float: Parser[IExpressionPE] = {
    pos ~ raw"^-?\d+\.\d*".r ~ pos ^^ {
      case begin ~ thingStr ~ end => FloatLiteralPE(Range(begin, end), thingStr.toFloat)
    }
  }

  private[parser] def string: Parser[NameP] = {
    pos ~ ("\"" ~> "(?:[^\"\\\\]|\\\\.)*".r <~ "\"") ~ pos ^^ {
      case begin ~ s ~ end => {
        NameP(
          Range(begin, end),
          StringEscapeUtils.unescapeJava(s))
      }
    }
  }

  // ww = with whitespace

  private[parser] def atLeastOneOfWW[A, B](
      parserA: Parser[A],
      parserB: Parser[B]
  ): Parser[(Option[A] ~ Option[B])] = {
    // With A definitely present (or both)
    (parserA ~ opt(optWhite ~> parserB) ^^ { case (a ~ maybeB) =>
      val maybeA: Option[A] = Some(a)
      new ~(maybeA, maybeB)
    }) |
        // With B definitely present
        (parserB ^^ { case b => (new ~(None, Some(b))) })
  }

  private[parser] def atLeastOneOfWW[A, B, C](
      parserA: Parser[A],
      parserB: Parser[B],
      parserC: Parser[C]
  ): Parser[(Option[A] ~ Option[B] ~ Option[C])] = {
    atLeastOneOfWW(atLeastOneOfWW(parserA, parserB), parserC) ^^ {
      case (None ~ c) => (new ~(new ~(None, None), c))
      case (Some((a ~ b)) ~ c) => (new ~(new ~(a, b), c))
    }
  }

  private[parser] def atLeastOneOf[A, B](
      parserA: Parser[A],
      parserB: Parser[B]
  ): Parser[(Option[A] ~ Option[B])] = {
    (parserA ~ opt(parserB) ^^ { case (a ~ maybeB) =>
      val maybeA: Option[A] = Some(a)
      new ~(maybeA, maybeB)
    }) |
      // With B definitely present
      (parserB ^^ { case b => (new ~(None, Some(b))) })
  }

  private[parser] def atLeastOneOf[A, B, C, D](
    parserA: Parser[A],
    parserB: Parser[B],
    parserC: Parser[C],
    parserD: Parser[D]
  ): Parser[(Option[A] ~ Option[B] ~ Option[C] ~ Option[D])] = {
    atLeastOneOfWW(atLeastOneOfWW(parserA, parserB, parserC), parserD) ^^ {
      case (None ~ c) => (new ~(new ~(new ~(None, None), None), c))
      case (Some((a ~ b ~ c)) ~ d) => (new ~(new ~(new ~(a, b), c), d))
    }
  }

  private[parser] def onlyOneOf[A, B](
      parserA: Parser[A],
      parserB: Parser[B]
  ): Parser[(Option[A], Option[B])] = {
    (parserA ^^ { case a =>
      val maybeA: Option[A] = Some(a)
      val maybeB: Option[B] = None
      (maybeA, maybeB)
    }) |
        (parserB ^^ { case a =>
          val maybeA: Option[A] = None
          val maybeB: Option[B] = Some(a)
          (maybeA, maybeB)
        })
  }

}
