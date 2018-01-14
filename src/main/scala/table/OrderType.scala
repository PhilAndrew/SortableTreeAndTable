package table

/**
  * Created by lorenzo on 13/07/17.
  */
sealed trait OrderType {
  def next: OrderType = this match {
    case Descending => Ascending
    case Ascending => Descending
    case NoOrder => Descending
  }
}

case object Descending extends OrderType
case object Ascending extends OrderType
case object NoOrder extends OrderType
