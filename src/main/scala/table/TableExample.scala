package table

import korolev.execution._
import korolev.ApplicationContext
import korolev.blazeServer.{KorolevBlazeServer, blazeService}
import korolev.server.{KorolevServiceConfig, ServerRouter, StateStorage}
import levsha.events.EventPhase
import org.http4s.blaze.http.HttpService

import scala.concurrent.Future

/**
  * Created by lorenzo on 13/07/17.
  */
case class TableData(c1: String, c2: String, c3: String, c4: Int, c5: String)
case class TableOrdering(o1: OrderType = NoOrder,
                         o2: OrderType = NoOrder,
                         o3: OrderType = NoOrder,
                         o4: OrderType = NoOrder,
                         o5: OrderType = NoOrder)

case class TableState(table: List[TableData], tableOrdering: TableOrdering) {
  def newOrdering[T <% Ordered[T]](order: OrderType, tOrd: TableOrdering)(column: TableData => T): TableState =
    order match {
      case Ascending =>
        TableState(table.sortBy(column), tOrd)
      case Descending =>
        TableState(table.sortBy(column)(Ordering[T].reverse),
          tOrd)
      case NoOrder => this
    }

}

case class State(tableState: TableState)

object State {
  val applicationContext = ApplicationContext[Future, State, Any]

  val initialState = State(
    TableState(
      table = List(
        TableData("hi", "vdvs", "asd", 12, "iiuffs"),
        TableData("ghi", "rvdvs", "qasd", 2, "iffs"),
        TableData("khi", "hvdvs", "hasd", 13, "gffsh"),
        TableData("ehi", "mvdvs", "jasd", 122, "gffs")
      ),
      tableOrdering = TableOrdering(
        o3 = Descending
      )
    )
  )
}

object TableExample extends KorolevBlazeServer {
  import State.applicationContext._
  import State.applicationContext.symbolDsl._

  val tableTitles = (
    "First",
    "Second",
    "Third",
    "Fourth",
    "Fifth"
  )

  def icon[T <% Ordered[T]](orderType: OrderType, tOrd: TableOrdering)(column: TableData => T): Node = {
    val ordering: String = orderType match {
      case Descending => "fa-sort-desc"
      case Ascending  => "fa-sort-asc"
      case NoOrder    => "fa-sort"
    }
    'div (
      event('click, EventPhase.Capturing) {
        immediateTransition {
          case state =>
            state.copy(
              tableState = state.tableState.newOrdering(orderType.next, tOrd)(column)
            )
        }
      },
      'class /= "pull-right",
      'i (
        'class /= s"fa fa-fw $ordering"
      )
    )
  }

  def renderTable(state: State): Node = {
    'table (
      'class /= "table table-bordered",
      'thead (
        'tr (
          'th (
            'div (
              icon(state.tableState.tableOrdering.o1, TableOrdering(o1 = state.tableState.tableOrdering.o1.next))(_.c1),
              tableTitles._1
            )
          ),
          'th (icon(state.tableState.tableOrdering.o2, TableOrdering(o2 = state.tableState.tableOrdering.o2.next))(_.c2),
               tableTitles._2),
          'th (icon(state.tableState.tableOrdering.o3, TableOrdering(o3 = state.tableState.tableOrdering.o3.next))(_.c3),
               tableTitles._3),
          'th (icon(state.tableState.tableOrdering.o4, TableOrdering(o4 = state.tableState.tableOrdering.o4.next))(_.c4),
               tableTitles._4),
          'th (icon(state.tableState.tableOrdering.o5, TableOrdering(o5 = state.tableState.tableOrdering.o5.next))(_.c5),
               tableTitles._5)
        )
      ),
      'tbody (
        for (row <- state.tableState.table)
          yield
            'tr (
              'td (row.c1),
              'td (row.c2),
              'td (row.c3),
              'td (row.c4.toString),
              'td (row.c5)
            )
      )
    )
  }

  val config = KorolevServiceConfig[Future, State, Any](
    head = Seq(
      'head (
        'title ("Main Routing Page"),
        'link (
          'href /= "https://maxcdn.bootstrapcdn.com/bootstrap/3.3.7/css/bootstrap.min.css",
          'rel /= "stylesheet",
          'type /= "text/css"),
        'link (
          'href /= "https://maxcdn.bootstrapcdn.com/font-awesome/4.1.0/css/font-awesome.min.css",
          'rel /= "stylesheet")
      )
    ),
    render = {
      case state =>
        'body (
          'div (
            'class /= "container",
            'div (
              'class /= "row",
              'div (
                'class /= "col-sm-12",
                renderTable(state)
              )
            )
          )
        )
    },
    stateStorage = StateStorage.default[Future, State](State.initialState),
    serverRouter = ServerRouter.empty[Future, State]
  )

  override def service: HttpService =
    blazeService[Future, State, Any] from config
}
