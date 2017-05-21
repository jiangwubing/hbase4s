package hbase4s.filter

import hbase4s.filter.FilterParser._
import org.parboiled2._

import scala.util.{Failure, Success}

/** xxx
  * Created by Volodymyr.Glushak on 11/05/2017.
  */
class FilterParser(val input: ParserInput) extends Parser {

  def InputLine: Rule1[Expr] = rule {
    MultiExpr ~ EOI
  }

  private[this] def MultiExpr: Rule1[Expr] = rule {
    Factor ~ zeroOrMore(
      ignoreCase("or") ~ Factor ~> Or
        | ignoreCase("and") ~ Factor ~> And
    )
  }

  private[this] def Factor: Rule1[Expr] = rule {
    SingleExprWrapped | KeywordBased | SingleExpr | MultiExprWrapped
  }

  private[this] def MultiExprWrapped = rule {
    openBracket ~ MultiExpr ~ closeBracket
  }

  private[this] def SingleExprWrapped: Rule1[Expr] = rule {
    openBracket ~ SingleExpr ~ closeBracket
  }

  private[this] def SingleExpr: Rule1[SingleColVal] = rule {
    FieldName ~ ws ~ OpExpr ~ ws ~ QuotedText ~> ((a: Column, b: CompareOp, c: String) => SingleColVal(a, b, c))
  }

  private[this] def KeywordBased: Rule1[Expr] = rule {
    (Keywords.Key ~ push(KeyOnly)) |
      (Keywords.FirstKey ~ push(FirstKeyOnly)) |
      Keywords.RowPrefix ~ eq ~ CapturedKeyword ~> RowPrefix |
      Keywords.ColumnPrefix ~ eq ~ ComaSeparatedKeywords ~> MultipleColumnPrefix |
      Keywords.ColumnPrefix ~ eq ~ CapturedKeyword ~> ColumnPrefix |
      Keywords.ColumnLimit ~ eq ~ capture(oneOrMore(CharPredicate.Digit)) ~> (x => ColumnCountGet(x.toInt)) |
      Keywords.PageCount ~ eq ~ capture(oneOrMore(CharPredicate.Digit)) ~> (y => Page(y.toInt)) |
      Keywords.StopRow ~ eq ~ CapturedKeyword ~> InclusiveStop |
      Keywords.ColName ~ ws ~ OpExpr ~ ws ~ FieldName ~> Qualifier |
      Keywords.ColValue ~ ws ~ OpExpr ~ ws ~ (QuotedText | CapturedKeyword) ~> Value
  }



  private[this] def eq = rule {
    ws ~ oneOrMore(anyOf("=")) ~ ws
  }

  private[this] def ComaSeparatedKeywords = rule {
    openBracket ~ oneOrMore(CapturedKeyword ~ zeroOrMore(',')) ~ closeBracket
  }

  private[this] def OpExpr: Rule1[CompareOp] = rule {
    capture(oneOrMore(anyOf("=<>!"))) ~> compareOp _
  }

  private[this] def FieldName: Rule1[Column] = rule {
    CapturedKeyword ~ ":" ~ CapturedKeyword ~> Column
  }

  private[this] def CapturedKeyword: Rule1[String] = rule {
    zeroOrMore(Quote) ~ capture(oneOrMore(CharPredicate.AlphaNum | anyOf("_"))) ~ zeroOrMore(Quote)
  }

  private[this] def QuotedText: Rule1[String] = rule {
    Quote ~ capture(oneOrMore(CharPredicate.All -- '"' | EscapedQuote)) ~ Quote | CapturedKeyword
  }

  private[this] def EscapedQuote = "\\\""

  private[this] def Quote = rule {
    "\""
  }

  private[this] def openBracket = rule {
    ws ~ "(" ~ ws
  }

  private[this] def closeBracket = rule {
    ws ~ ")" ~ ws
  }

  private[this] def ws = rule {
    zeroOrMore(WhiteSpaceChar)
  }
}

object FilterParser {

  protected val WhiteSpaceChar = CharPredicate(" \n\r\t\f")
  protected val QuoteBackslash = CharPredicate("\"\\")

  protected def compareOp(s: String): CompareOp = s match {
    case "=" | "==" => Eq
    case ">" => Greater
    case ">=" => GreaterOrEq
    case "<" => Less
    case "<=" => LessOrEq
    case "!=" | "<>" => NonEq
    case x => sys.error(s"Unsupported operation: $x")
  }

  def parse(in: String): Expr = {
    val parser = new FilterParser(in)
    parser.InputLine.run() match {
      case Success(s) => s
      case Failure(x) => x match {
        case pe: ParseError => sys.error(parser.formatError(pe))
        case z => throw z
      }
    }
  }

  object Keywords {
    val Key = "key"
    val FirstKey = "first_key"
    val RowPrefix = "row_prefix"
    val ColumnPrefix = "column_prefix"
    val ColumnLimit = "column_limit"
    val RowLimit = "row_limit"
    val PageCount = "page_count"
    val StopRow = "stop_row"
    val ColName = "column_name"
    val ColValue = "column_value"
  }

}
