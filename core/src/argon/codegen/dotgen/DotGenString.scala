package argon.codegen.dotgen

import argon._
import argon.nodes._
import argon.utils.escapeString

trait DotGenString extends DotCodegen {

  override protected def quoteConst(c: Const[?]): String = c match {
    case Const(c: String) => escapeString(c)
    case _ => super.quoteConst(c)
  }

  override protected def emitNode(lhs: Sym[_], rhs: Op[_]): Unit = rhs match {
    case ToString(x) => 
    case StringConcat(x,y) =>
    case StringEquals(x,y) =>
    case StringDiffer(x,y) =>
    case _ => super.emitNode(lhs, rhs)
  }

}
