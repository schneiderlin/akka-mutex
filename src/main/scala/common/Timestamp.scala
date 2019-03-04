package common



case class Timestamp(num: Int, processPriority: Int) {
  def +(n: Int): Timestamp = Timestamp(num + n, processPriority)

  //  def +(other: common.Timestamp) = common.Timestamp(num + other.num)
  def >(other: Timestamp): Boolean = num > other.num

  def <(other: Timestamp): Boolean = num < other.num

  def ==>(other: Timestamp): Boolean =
    !this.<==(other)

  // total order of timestamp
  def <==(other: Timestamp): Boolean =
    if (num < other.num) true
    else if (num == other.num) processPriority < other.processPriority
    else false
}

object Timestamp {
  val delta = 1

  implicit val totalOrdering: Ordering[Timestamp] = Ordering.fromLessThan(_ <== _)
}
