package net.scalytica.symbiotic.postgres.slickext

import net.scalytica.symbiotic.api.types.Path
import slick.ast.ScalaBaseType.booleanType
import slick.ast._
import slick.lifted.{ExtensionMethods, Rep}

import scala.util.matching.Regex

/**
 * Provides extra string extensions to slick.
 */
final class PathExtensionMethods[P1](val c: Rep[P1])
    extends ExtensionMethods[Path, P1] {

  protected[this] implicit def b1Type = implicitly[TypedType[Path]]

  /**
   * Postgres sepcific extension for the {{{~}}} operator allowing regex.
   */
  def regexMatch[R](e: Regex)(implicit om: o#to[Boolean, R]) =
    om.column(ExtOperators.~, n, LiteralNode(e.regex))

  def startsWith[R](p: Path)(implicit om: o#to[Boolean, R]) =
    om.column(Library.StartsWith, n, LiteralNode(p.materialize))

}
