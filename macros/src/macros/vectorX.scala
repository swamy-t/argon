package macros

import scala.annotation.StaticAnnotation
import scala.meta._



class vectorApiX extends StaticAnnotation {


  inline def apply(defn: Any): Any = meta {


    defn match {
      case q"trait VectorApi extends { ${lit: Lit} }" if lit.value.isInstanceOf[Int] =>
        val X = lit.value.asInstanceOf[Int]
        val typ: Stat = q"type Vector[T] = Vec[T]"
        val types = (Seq(typ) ++ (1 to X).map(x => {
          val vectorX = Type.Name("Vector"+x)
          val vecX = Type.Name("Vec"+x)
          q"type $vectorX[T] = $vecX[T]"
        })).to[List]

        val r = q"""trait VectorApi extends VectorExp with ArrayApi { ..$types }"""
        println(r)
        r
      case _ =>
        abort("vector macro can only be applied to a trait of the form: trait VectorX where X is the maximum size of the vector")
    }
  }

}

class vectorX extends StaticAnnotation {


  inline def apply(defn: Any): Any = meta {


    defn match {
      case q"trait VectorExp extends { ${lit: Lit} }" if lit.value.isInstanceOf[Int] =>
        val X = lit.value.asInstanceOf[Int]

        val vec: Stat = q"sealed trait Vec[T] { def size: Int; def s: Exp[Vec[T]]}"//; def elems: MetaArray[T] }"
        val types = (Seq(vec) ++ (1 to X).map(x => {
          val vecX = Type.Name("Vec"+x)
          q"""case class $vecX[T:Meta](s: Exp[$vecX[T]]) extends MetaAny[$vecX[T]] with Vec[T]{
             val size = $x
             def =!=(x: $vecX[T])(implicit ctx: SrcCtx): Bool = ???
             def ===(x: $vecX[T])(implicit ctx: SrcCtx): Bool = ???
             def toText(implicit ctx: SrcCtx): Text = textify(this)
          }"""
        })).to[List]

        val tcInstance = (1 to X).flatMap(x => {
          val vecX = Type.Name("Vec"+x)
          val vecXt = Term.Name("Vec"+x)
          val vecXtype = Type.Name("Vec"+x+"Type")
          val vecXtypeT = Term.Name("Vec"+x+"Type")
          val stagedVecX = Term.Name("stagedVec"+x)
          Seq(
            q"""
              case class $vecXtype[T](mT: Meta[T]) extends Meta[$vecX[T]] {
                override def wrapped(x: Exp[$vecX[T]]): $vecX[T] = $vecXt[T](x)(mT)
                override def typeArguments = List(mT)
                override def stagedClass = classOf[$vecX[T]]
                override def isPrimitive = false
              }
            """,
            q"""
              implicit def $stagedVecX[T:Meta]: $vecXtype[T] = $vecXtypeT(meta[T])
              """)
        }).to[List]

        val vecCases = (1 to X).map(x => {
          val vecXt = Term.Name("Vec"+x)
          val vecX = Type.Name("Vec"+x)
          p"case $x => $vecXt(constant[$vecX[T]](x))"
        }).to[List]

        val vectorize =
          q"""
              def vectorize[T:Meta](x: Seq[T])(implicit ctx: SrcCtx): Vec[T]  = x.size match { ..case $vecCases }
          """
        val stats = types ++ tcInstance :+ vectorize

        val r =
          q"""
            trait VectorExp extends Staging with ArrayExp { ..$stats }
           """
        println(r)
        r
      case _ =>
        abort("vector macro can only be applied to a trait of the form: trait VectorX where X is the maximum size of the vector")
    }
  }

}

class vectorCompX extends StaticAnnotation {


  inline def apply(defn: Any): Any = meta {


    defn match {
      case q"object Vector extends { ${lit: Lit} }" if lit.value.isInstanceOf[Int] =>
        val X = lit.value.asInstanceOf[Int]
        val applies = (1 to X).map(x => {
          val vecXt = Term.Name("Vec"+x)
          val vecX = Type.Name("Vec"+x)
          val paramss = (1 to x).map(argi => {
            val arg = Term.Name("x"+argi)
            param"$arg: T"
          }).to[List]
          val seqX = (1 to x).map(argi => {
            Term.Name("x" + argi)
          })
          val seq = q"Seq(..$seqX)"
          q"def apply[T:Meta] = $vecXt[T](constant[$vecX[T]]($seq))"
          .copy(paramss = List(paramss, List(param"implicit ctx: SrcCtx")))

        }).to[List]
        val r = q"""object Vector extends VectorExp { ..$applies }"""
        println(r)
        r
      case _ =>
        abort("vector macro can only be applied to a trait of the form: trait VectorX where X is the maximum size of the vector")
    }
  }

}