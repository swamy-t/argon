package argon.lang

import argon._
import argon.nodes._
import forge._

case class String(s: Exp[String]) extends MetaAny[String] {
  override type Internal = java.lang.String
  @api def +(rhs: CString): MString = this + MString(rhs)
  @api def +(rhs: MString): MString = MString(String.concat(this.toText.s, rhs.s))
  @api def +[R](rhs: MetaAny[R]): MString = MString(String.concat(this.s, rhs.toText.s))

  @api def =!=(that: MString): MBoolean = Boolean(String.differ(this.s, that.s))
  @api def ===(that: MString): MBoolean = Boolean(String.equals(this.s, that.s))
  @api def equals(that: MString): MBoolean = Boolean(String.equals(this.s, that.s))
  @api def toText = this
}

object String {
  @api def apply(s: CString): MString = MString(const(s))
  @internal def const(s: CString): Const[MString] = constant[MString](s)

  @internal def ify[T:Type](x: T): MString = MString(sym_tostring(x.s))

  /** Constructors **/
  @internal def sym_tostring[T:Type](x: Exp[T]): Exp[MString] = x match {
    case Const(c: CString) => String.const(c)
    case a if a.tp == StringType => a.asInstanceOf[Exp[MString]]
    case _ => stage(ToString(x))(ctx)
  }
  @internal def concat(x: Exp[MString], y: Exp[MString]): Exp[MString] = (x,y) match {
    case (Const(a), Const(b)) => String.const(a + b)
    case (Const(""), b) => b
    case (a, Const("")) => a
    case _ => stage( StringConcat(x,y) )(ctx)
  }
  @internal def equals(x: Exp[MString], y: Exp[MString]): Exp[MBoolean] = (x,y) match {
    case (Const(a), Const(b)) => Boolean.const(a == b)
    case _ => stage( StringEquals(x,y) )(ctx)
  }
  @internal def differ(x: Exp[MString], y: Exp[MString]): Exp[MBoolean] = (x,y) match {
    case (Const(a), Const(b)) => Boolean.const(a == b)
    case _ => stage( StringDiffer(x,y) )(ctx)
  }
}

trait StringExp {
  /** Static methods **/
  @internal def infix_+[R<:MetaAny[R]](x1: CString, x2: R): MString = MString(x1) + x2.toText

  /** Rewrite Rules **/
  /*@rewrite def Bool$not(x: Exp[Bool])(implicit ctx: SrcCtx): Exp[Bool] = x match {
    case Op(TextEquals(a,b)) => text_differ(a,b)
    case Op(TextDiffer(a,b)) => text_equals(a,b)
  }*/

  /** Type classes **/
  implicit val stringIsStaged: Type[String] = StringType

  /** Lifting **/
  implicit def string2text(x: CString)(implicit ctx: SrcCtx): MString = MMString(x)

  implicit object LiftString extends Lift[CString,MString] {
    @internal def apply(x: CString): MString = MString(x)
  }

  /** Casting **/
  implicit object CastStringLift extends Cast[CString,MString] {
    @internal def apply(x: CString): MString = MString(x)
  }
}