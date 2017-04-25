package argon.codegen.chiselgen

import argon.core.Staging
import argon.ops.{FixPtExp, FltPtExp}

trait ChiselGenFixPt extends ChiselCodegen {
  val IR: FixPtExp with FltPtExp with Staging
  import IR._

  override protected def remap(tp: Type[_]): String = tp match {
    case IntType() => "Int"
    case LongType() => "Long"
    case _ => super.remap(tp)
  }

  override protected def bitWidth(tp: Type[_]): Int = tp match {
      case IntType()  => 32
      case LongType() => 32 // or 64?
      case FixPtType(s,d,f) => d+f
      case _ => super.bitWidth(tp)
  }

  override protected def needsFPType(tp: Type[_]): Boolean = tp match {
      case FixPtType(s,d,f) => if (s) true else if (f == 0) false else true
      case IntType()  => false
      case LongType() => false
      case _ => super.needsFPType(tp)
  }

  override protected def quoteConst(c: Const[_]): String = (c.tp, c) match {
    case (FixPtType(s,d,f), Const(cc: BigDecimal)) => 
      if (s) {
        cc.toInt.toString + src".FP(true, $d, $f)"
      } else {
        cc.toInt.toString + ".U(32.W)"        
      }
    case (IntType(), Const(cc: BigDecimal)) => 
      if (cc >= 0) {
        cc.toInt.toString + ".U(32.W)"  
      } else {
        cc.toInt.toString + ".S(32.W).asUInt"
      }
      
    case (LongType(), Const(cc: BigDecimal)) => cc.toLong.toString + ".L"
    case (FixPtType(s,d,f), Const(cc: BigDecimal)) => 
      if (needsFPType(c.tp)) {s"Utils.FixedPoint($s,$d,$f,$cc)"} else {
        if (cc >= 0) cc.toInt.toString + ".U(32.W)" else cc.toInt.toString + ".S(32.W).asUInt"
      }
    case _ => super.quoteConst(c)
  }

  override protected def emitNode(lhs: Sym[_], rhs: Op[_]): Unit = rhs match {
    case FixInv(x)   => emit(src"val $lhs = ~$x")
    case FixNeg(x)   => emit(src"val $lhs = -$x")
    case FixAdd(x,y) => emit(src"val $lhs = $x + $y")
    case FixSub(x,y) => emit(src"val $lhs = $x - $y")
    case FixMul(x,y) => alphaconv_register(src"$lhs"); emit(src"val $lhs = $x * $y")
    case FixDiv(x,y) => emit(src"val $lhs = $x / $y")
    case FixAnd(x,y) => emit(src"val $lhs = $x & $y")
    case FixOr(x,y)  => emit(src"val $lhs = $x | $y")
    case FixXor(x,y)  => emit(src"val $lhs = $x ^ $y")
    case FixLt(x,y)  => alphaconv_register(src"$lhs"); emit(src"val $lhs = $x < $y")
    case FixLeq(x,y) => alphaconv_register(src"$lhs"); emit(src"val $lhs = $x <= $y")
    case FixNeq(x,y) => alphaconv_register(src"$lhs"); emit(src"val $lhs = $x =/= $y")
    case FixEql(x,y) => alphaconv_register(src"$lhs"); emit(src"val $lhs = $x === $y")
    case FixMod(x,y) => emit(src"val $lhs = $x % $y")
    case UnbMul(x,y) => emit(src"val $lhs = $x *& $y")
    case UnbDiv(x,y) => emit(src"val $lhs = $x /& $y")
    case SatAdd(x,y) => emit(src"val $lhs = $x <+> $y")
    case SatSub(x,y) => emit(src"val $lhs = $x <-> $y")
    case SatMul(x,y) => emit(src"val $lhs = $x <*> $y")
    case SatDiv(x,y) => emit(src"val $lhs = $x </> $y")
    case UnbSatMul(x,y) => emit(src"val $lhs = $x <*&> $y")
    case UnbSatDiv(x,y) => emit(src"val $lhs = $x </&> $y")
    case FixRandom(x) => lhs.tp match {
      case IntType()  => emit(src"val $lhs = chisel.util.Random.nextInt()")
      case LongType() => emit(src"val $lhs = chisel.util.Random.nextLong()")
    }
    case FixConvert(x) => lhs.tp match {
      case IntType()  => 
        emitGlobalWire(src"val $lhs = Wire(new FixedPoint(true, 32, 0))")
        emit(src"${lhs}.r := ${x}.r")
      case LongType() => 
        val pad = bitWidth(lhs.tp) - bitWidth(x.tp)
        emitGlobalWire(src"val $lhs = Wire(new FixedPoint(true, 64, 0))")
        if (pad > 0) {
          emit(src"${lhs}.r := Utils.Cat(0.U(${pad}.W), ${x}.r)")
        } else {
          emit(src"${lhs}.r := ${x}.r.apply(${bitWidth(lhs.tp)-1}, 0)")
        }
      case FixPtType(s,d,f) => 
        emit(src"val $lhs = ${x}.r.FP($s, $d, $f)")
    }
    case FixPtToFltPt(x) => lhs.tp match {
      case DoubleType() => emit(src"val $lhs = $x.toDouble")
      case FloatType()  => emit(src"val $lhs = $x.toFloat")
    }
    case StringToFixPt(x) => lhs.tp match {
      case IntType()  => emit(src"val $lhs = $x.toInt")
      case LongType() => emit(src"val $lhs = $x.toLong")
      case _ => emit(src"val $lhs = $x // No rule for this")
    }
    case _ => super.emitNode(lhs, rhs)
  }
}
