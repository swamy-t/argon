package argon.core

trait StagedTypes { this: Staging =>

  /** Base type class for all staged types **/
  abstract class Staged[T] {
    def wrapped(x: Exp[T]): T
    def unwrapped(x: T): Exp[T]
    def typeArguments: List[Staged[_]] = Nil
    def stagedClass: Class[T]
    def isPrimitive: Boolean

    def <:<(that: Staged[_]) = isSubtype(this.stagedClass, that.stagedClass)
  }

  def typ[T:Staged] = implicitly[Staged[T]]
  def mtyp[A,B](x: Staged[A]): Staged[B] = x.asInstanceOf[Staged[B]]

  def wrap[T:Staged](s: Exp[T]): T = implicitly[Staged[T]].wrapped(s)
  def unwrap[T:Staged](x: T): Exp[T] = implicitly[Staged[T]].unwrapped(x)

  def wrap[T:Staged](xs: List[Exp[T]]): List[T] = xs.map{t => implicitly[Staged[T]].wrapped(t) }
  def unwrap[T:Staged](xs: List[T]): List[Exp[T]] = xs.map{t => implicitly[Staged[T]].unwrapped(t) }
  def wrap[T:Staged](xs: Seq[Exp[T]]): Seq[T] = xs.map{t => implicitly[Staged[T]].wrapped(t) }
  def unwrap[T:Staged](xs: Seq[T]): Seq[Exp[T]] = xs.map{t => implicitly[Staged[T]].unwrapped(t) }

  implicit class StagedTypeOps[T:Staged](x: T) {
    def s: Exp[T] = implicitly[Staged[T]].unwrapped(x)
  }

  /** Stolen from Delite utils **/
  private def isSubtype(x: java.lang.Class[_], cls: java.lang.Class[_]): Boolean = {
    if ((x == cls) || x.getInterfaces.contains(cls)) true
    else if (x.getSuperclass == null && x.getInterfaces.length == 0) false
    else {
      val superIsSub = if (x.getSuperclass != null) isSubtype(x.getSuperclass, cls) else false
      superIsSub || x.getInterfaces.exists(s=>isSubtype(s,cls))
    }
  }
}