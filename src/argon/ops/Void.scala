package argon.ops

import argon.core.Staging
import forge._

trait VoidApi extends VoidExp {
  self: TextExp =>
  type Unit = Void
}

trait VoidExp extends Staging {
  self: TextExp =>

  case class Void(s: Exp[Void]) extends MetaAny[Void] {
    @api def ===(that: Void) = true
    @api def =!=(that: Void) = false
    @api def toText = textify(this)
  }

  /** Type classes **/
  // --- Staged
  implicit object VoidType extends Meta[Void] {
    override def wrapped(x: Exp[Void]) = Void(x)
    override def stagedClass = classOf[Void]
    override def isPrimitive = true
  }

  // --- Lift
  implicit object LiftUnit2Void extends Lift[Unit,Void] {
    override def apply(x: Unit)(implicit ctx: SrcCtx) = Void(void())
  }
  implicit object CastUnit2Void extends Cast[Unit,Void] {
    override def apply(x: Unit)(implicit ctx: SrcCtx) = Void(void())
  }

  /** Constant lifting **/
  implicit def unit2void(x: Unit)(implicit ctx: SrcCtx): Void = lift(x)
  def void()(implicit ctx: SrcCtx): Exp[Void] = constant[Void](())
}


