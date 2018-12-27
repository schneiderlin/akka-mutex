package utils

import mutex.Timestamp
import org.scalacheck.{Arbitrary, Gen}

object TimestampChecks {
  lazy val genTimestamp: Gen[Timestamp] = for {
    num <- Gen.posNum[Int]
    priority <- Gen.posNum[Int]
  } yield Timestamp(num, priority)

  implicit val arbitraryTimestamp: Arbitrary[Timestamp] =
    Arbitrary(genTimestamp)
}
