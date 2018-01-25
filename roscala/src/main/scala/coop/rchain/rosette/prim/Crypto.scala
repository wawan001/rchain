package coop.rchain.rosette.prim

import coop.rchain.rosette.{Ctxt, Ob, RblString}
import coop.rchain.rosette.macros.{checkArgumentMismatch, checkTypeMismatch}
import coop.rchain.rosette.prim.Prim._

import java.math._

import scorex.crypto.hash.{
  Sha256 => ScryptoSha256,
  Keccak256 => ScryptoKeccak256,
  Blake2b256 => ScryptoBlake2b256
}

object Crypto {
  object Sha256 extends Prim {
    override val name: String = "sha256"
    override val minArgs: Int = 1
    override val maxArgs: Int = 1

    @checkTypeMismatch[RblString]
    @checkArgumentMismatch
    override def fn(ctxt: Ctxt): Either[PrimError, RblString] =
      ctxt.argvec.elem.head.as[RblString] match {
        case None =>
          val typeName = ctxt.argvec.elem.head.getClass.getName
          Left(TypeMismatch(0, typeName))
        case Some(rblString) =>
          val input = rblString.value.getBytes()
          println(String.format("%02x", input))
          val digest = ScryptoSha256.hash(input)
          val output = String.format("%02x", digest)
          Right(RblString(output))
      }
  }

  object Keccak256 extends Prim {
    override val name: String = "keccak256"
    override val minArgs: Int = 1
    override val maxArgs: Int = 1

    @checkTypeMismatch[RblString]
    @checkArgumentMismatch
    override def fn(ctxt: Ctxt): Either[PrimError, RblString] =
      ctxt.argvec.elem.head.as[RblString] match {
        case None =>
          val typeName = ctxt.argvec.elem.head.getClass.getName
          Left(TypeMismatch(0, typeName))
        case Some(rblString) =>
          val input = rblString.value
          val digest = ScryptoKeccak256.hash(input)
          val output = String.format("%032x", new BigInteger(digest))
          Right(RblString(output))
      }
  }

  object Blake2b256 extends Prim {
    override val name: String = "blake2b256"
    override val minArgs: Int = 1
    override val maxArgs: Int = 1

    @checkTypeMismatch[RblString]
    @checkArgumentMismatch
    override def fn(ctxt: Ctxt): Either[PrimError, RblString] =
      ctxt.argvec.elem.head.as[RblString] match {
        case None =>
          val typeName = ctxt.argvec.elem.head.getClass.getName
          Left(TypeMismatch(0, typeName))
        case Some(rblString) =>
          val input = rblString.value
          val digest = ScryptoBlake2b256.hash(input)
          val output = String.format("%032x", new BigInteger(digest))
          Right(RblString(output))
      }
  }
}
