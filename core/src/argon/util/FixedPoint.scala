package argon.util
case class FixFormat(sign: Boolean, ibits: Int, fbits: Int) {
  lazy val bits = ibits + fbits
  lazy val MAX_FRACTIONAL_VALUE: BigInt = (BigInt(1) << (fbits - 1)) - 1

  lazy val MAX_INTEGRAL_VALUE: BigInt = (if (sign) (BigInt(1) << (ibits-1)) - 1      else (BigInt(1) << ibits) - 1) << fbits
  lazy val MIN_INTEGRAL_VALUE: BigInt = if (sign) -(BigInt(1) << (ibits-1)) << fbits else BigInt(0)

  lazy val MAX_VALUE: BigInt = if (sign) (BigInt(1) << (ibits+fbits-1)) - 1 else (BigInt(1) << (ibits+fbits)) - 1
  lazy val MIN_VALUE: BigInt = if (sign) -(BigInt(1) << (ibits+fbits-1))    else BigInt(0)

  lazy val MAX_INTEGRAL_VALUE_FP: FixedPoint = FixedPoint.clamped(MAX_INTEGRAL_VALUE, valid=true, this)
  lazy val MIN_INTEGRAL_VALUE_FP: FixedPoint = FixedPoint.clamped(MIN_INTEGRAL_VALUE, valid=true, this)
  lazy val MAX_VALUE_FP: FixedPoint = FixedPoint.clamped(MAX_VALUE, valid=true, this)
  lazy val MIN_VALUE_FP: FixedPoint = FixedPoint.clamped(MIN_VALUE, valid=true, this)
}

class FixedPoint(val value: BigInt, val valid: Boolean, val fmt: FixFormat) {
  // All operations assume that both the left and right hand side have the same fixed point format
  def +(that: FixedPoint) = FixedPoint.clamped(this.value + that.value, this.valid && that.valid, fmt)
  def -(that: FixedPoint) = FixedPoint.clamped(this.value - that.value, this.valid && that.valid, fmt)
  def *(that: FixedPoint) = FixedPoint.clamped((this.value * that.value) >> fmt.fbits, this.valid && that.valid, fmt)
  def /(that: FixedPoint) = valueOrX{ FixedPoint.clamped((this.value << fmt.fbits) / that.value, this.valid && that.valid, fmt) }
  def %(that: FixedPoint) = valueOrX{
    val result = this.value % that.value
    val posResult = if (result < 0) result + that.value else result
    FixedPoint.clamped(posResult, this.valid && that.valid, fmt)
  }
  def &(that: FixedPoint) = FixedPoint.clamped(this.value & that.value, this.valid && that.valid, fmt)
  def ^(that: FixedPoint) = FixedPoint.clamped(this.value ^ that.value, this.valid && that.valid, fmt)
  def |(that: FixedPoint) = FixedPoint.clamped(this.value | that.value, this.valid && that.valid, fmt)

  def <(that: FixedPoint)   = Bool(this.value < that.value, this.valid && that.valid)
  def <=(that: FixedPoint)  = Bool(this.value <= that.value, this.valid && that.valid)
  def >(that: FixedPoint)   = Bool(this.value > that.value, this.valid && that.valid)
  def >=(that: FixedPoint)  = Bool(this.value >= that.value, this.valid && that.valid)
  def !==(that: FixedPoint) = Bool(this.value != that.value, this.valid && that.valid)
  def ===(that: FixedPoint) = Bool(this.value == that.value, this.valid && that.valid)

  def bits: Array[Bool] = Array.tabulate(fmt.bits){i => Bool(value.testBit(i)) }

  def <+>(that: FixedPoint) = FixedPoint.saturating(this.value + that.value, this.valid && that.valid, fmt)
  def <->(that: FixedPoint) = FixedPoint.saturating(this.value - that.value, this.valid && that.valid, fmt)
  def <*>(that: FixedPoint) = FixedPoint.saturating((this.value * that.value) >> fmt.bits, this.valid && that.valid, fmt)
  def </>(that: FixedPoint) = FixedPoint.saturating((this.value << fmt.bits) / that.value, this.valid && that.valid, fmt)

  /*
  def *&(that: FixedPoint)  = FixedPoint.unbiased(this.value * that.value, this.valid && that.valid, fmt)
  def /&(that: FixedPoint)  = valueOrX { FixedPoint.unbiased(this.value / that.value, this.valid && that.valid, fmt) }
  def <*&>(that: FixedPoint) = FixedPoint.unbiasedSat(this.value * that.value, this.valid && that.valid, fmt)
  def </&>(that: FixedPoint) = valueOrX { FixedPoint.unbiasedSat(this.value / that.value, this.valid && that.valid, fmt) }*/

  def valueOrX(x: => FixedPoint): FixedPoint = {
    try { x } catch { case _: Throwable => FixedPoint.invalid(fmt) }
  }
  override def toString: String = if (valid) {
    if (fmt.fbits > 0) {
      (BigDecimal(this.value) / BigDecimal(BigInt(1) << fmt.fbits)).bigDecimal.toPlainString
    }
    else {
      value.toString
    }
  } else "X"
}

object FixedPoint {
  def apply(x: Byte, fmt: FixFormat): FixedPoint = FixedPoint.clamped(BigInt(x) << fmt.fbits, valid=true, fmt)
  def apply(x: Short, fmt: FixFormat): FixedPoint = FixedPoint.clamped(BigInt(x) << fmt.fbits, valid=true, fmt)
  def apply(x: Int, fmt: FixFormat): FixedPoint = FixedPoint.clamped(BigInt(x) << fmt.fbits, valid=true, fmt)
  def apply(x: Long, fmt: FixFormat): FixedPoint = FixedPoint.clamped(BigInt(x) << fmt.fbits, valid=true, fmt)
  def apply(x: BigInt, fmt: FixFormat): FixedPoint = FixedPoint.clamped(x << fmt.fbits, valid=true, fmt)

  def apply(x: Float, fmt: FixFormat): FixedPoint = FixedPoint.clamped(BigDecimal(x.toDouble) * Math.pow(2,fmt.fbits), valid=true, fmt)
  def apply(x: Double, fmt: FixFormat): FixedPoint = FixedPoint.clamped(BigDecimal(x) * Math.pow(2,fmt.fbits), valid=true, fmt)
  def apply(x: BigDecimal, fmt: FixFormat): FixedPoint = FixedPoint.clamped(x, valid=true, fmt)

  def invalid(fmt: FixFormat) = new FixedPoint(-1, valid=false, fmt)
  def clamped(value: BigDecimal, valid: Boolean, fmt: FixFormat): FixedPoint = clamped(value.toBigInt, valid, fmt)

  def fromBits(bits: BigInt, valid: Boolean, fmt: FixFormat): FixedPoint = clamped(bits, valid, fmt)

  /**
    * Creates a fixed point value from the given array of bits, wrapping the result upon overflow/underflow
    * @param bits Big-endian (LSB at index 0, MSB at last index) bits
    * @param fmt The fixed point format to be used by this number
    */
  def fromBits(bits: Array[Bool], fmt: FixFormat): FixedPoint = {
    // Will be negative?
    if (fmt.sign && bits.length >= fmt.bits && bits(fmt.bits-1).value) {
      var x = BigInt(-1)
      bits.take(fmt.bits).zipWithIndex.foreach{case (bit, i) => if (!bit.value) x = x.clearBit(i) }
      new FixedPoint(x, bits.forall(_.valid), fmt)
    }
    else {
      var x = BigInt(0)
      bits.take(fmt.bits).zipWithIndex.foreach{case (bit, i) => if (bit.value) x = x.setBit(i) }
      new FixedPoint(x, bits.forall(_.valid), fmt)
    }
  }

  /**
    * Create a new fixed point number, wrapping on overflow or underflow
    * @param bits Value's bits (with both integer and fractional components)
    * @param valid Defines whether this value is valid or not
    * @param fmt The fixed point format used by this number
    */
  def clamped(bits: BigInt, valid: Boolean, fmt: FixFormat): FixedPoint = {
    val clampedValue = if (fmt.sign && bits.testBit(fmt.bits-1)) {
      bits | fmt.MIN_VALUE
    }
    else bits & fmt.MAX_VALUE
    new FixedPoint(clampedValue, valid, fmt)
  }

  /**
    * Create a new fixed point number, saturating on overflow or underflow
    * at the format's max or min values, respectively
    * @param bits Value's bits (with both integer and fractional components)
    * @param valid Defines whether this value is valid or not
    * @param fmt The fixed point format used by this number
    */
  def saturating(bits: BigInt, valid: Boolean, fmt: FixFormat): FixedPoint = {
    if (bits < fmt.MIN_VALUE) fmt.MIN_VALUE_FP
    else if (bits > fmt.MAX_VALUE) fmt.MAX_VALUE_FP
    else new FixedPoint(bits, valid, fmt)
  }

  /**
    * Create a new fixed point number, rounding to the closest representable number
    * using unbiased rounding
    * @param bits Value's bits, with double the number of fractional bits (beyond normal format representation)
    * @param valid Defines whether this value is valid or not
    * @param fmt The fixed point format used by this number
    */
  def unbiased(bits: BigInt, valid: Boolean, fmt: FixFormat): FixedPoint = {
    val biased = bits >> fmt.fbits
    // TODO
    new FixedPoint(biased, valid, fmt)
    /*val remainder = bits & (fmt.)
    if (biased >= 0) {

    }
    else {

    }*/
  }

}

