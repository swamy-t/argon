package argon.nodes

import argon.core._
import argon.compiler._

/** IR Nodes **/
case class IfThenElse[T:Type](cond: Exp[MBoolean], thenp: Block[T], elsep: Block[T]) extends Op[T] {
  def mirror(f:Tx) = IfThenElseOps.ifThenElse[T](f(cond), f(thenp), f(elsep))
  override def aliases = dyns(thenp.result, elsep.result)

  def mT = typ[T]
}
