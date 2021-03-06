package argon.core

import com.typesafe.config.ConfigFactory
import pureconfig._

object Config {

  def init() {

    val default = ConfigFactory.parseString(s"""
argon {
  cwd = $${user.dir}
  verbosity = 1
  unsafe = false
  lib = true
  log = $${argon.cwd}"/logs/"${name}
  out = $${argon.cwd}"/gen/"${name}
  clear-logs = true
  clear-gen = false
  multifile = 4
  detail = 0
  unwrap = true
  emission = 0
  atomicw = true
}
""")

    val mergedConf = ConfigFactory.load().withFallback(default).resolve()

    case class ArgonConf(
      cwd: String,
      verbosity:Int,
      unsafe: Boolean,
      lib: Boolean,
      //name: String,
      log: String,
      out: String,
      clearLogs: Boolean,
      clearGen: Boolean,
      multifile: Int,
      detail: Int, 
      unwrap: Boolean,
      emission: Int,
      atomicw: Boolean
    )

    val conf = loadConfig[ArgonConf](mergedConf, "argon").right.get

    cwd = conf.cwd

    verbosity = conf.verbosity
    showWarn = true
    unsafe = conf.unsafe
    lib    = conf.lib
    //name = conf.name
    logDir = conf.log
    genDir = conf.out
    clearLogs = conf.clearLogs
    clearGen = conf.clearGen
    multifile = conf.multifile
    dotDetail = conf.detail
    unwrapStructs = conf.unwrap
    emitDevel = conf.emission// level of conservativeness and debug printing when emitting nodes
    allowAtomicWrites = conf.atomicw
  }

  def sep = "/"
  var cwd: String = _

  var verbosity: Int = _
  var showWarn: Boolean = true

  var unsafe: Boolean = _
  var lib:  Boolean = _
  var name: String = _
  var logDir: String = _
  var genDir: String = _
  var clearLogs: Boolean = _
  var clearGen: Boolean = _
  var multifile: Int = _
  var dotDetail: Int = _
  var unwrapStructs: Boolean = _
  var emitDevel: Int = _
  var allowAtomicWrites: Boolean = _

  var useAffine: Boolean = false

  //debugger interpreter
  var exit: () => Unit = () => ()
  
}
