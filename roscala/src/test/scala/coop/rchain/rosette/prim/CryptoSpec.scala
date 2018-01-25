package coop.rchain.rosette.prim

import coop.rchain.rosette.prim.Crypto._
import coop.rchain.rosette.{Ctxt, Ob, PC, RblBool, RblString, Tuple}
import org.scalatest._

class CryptoSpec extends FlatSpec with Matchers {
  val ctxt = Ctxt(
    tag = null,
    nargs = 1,
    outstanding = 0,
    pc = PC.PLACEHOLDER,
    rslt = null,
    trgt = null,
    argvec = null,
    env = null,
    code = null,
    ctxt = null,
    self2 = null,
    selfEnv = null,
    rcvr = null,
    monitor = null,
  )

  "sha256" should "correctly hash" in {
    val newCtxt = ctxt.copy(nargs = 1, argvec = Tuple(1, RblString("abc")))
    Sha256.fn(newCtxt) should be(Right(RblString("ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad")))
  }
}