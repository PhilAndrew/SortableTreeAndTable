/*import korolev._
import korolev.blazeServer._
import korolev.execution._
import korolev.server._

import scala.concurrent.Future

case class ItemInList(name: String)
case class StateList(list: List[ItemInList],
                     selected: Option[ItemInList])

object StateList {
  def apply(items: List[ItemInList]): StateList =
    new StateList(items, None)

  def demoList(): StateList =
    StateList(
      List(ItemInList("first"),
          ItemInList("second"),
           ItemInList("third")))
  val applicationContext = ApplicationContext[Future, StateList, Any]
}

object ListExample extends KorolevBlazeServer {
  import StateList.applicationContext._
  import StateList.applicationContext.symbolDsl._

  // Handler to input
  val storage =
    StateStorage.default[Future, StateList](StateList.demoList())
  val input = elementId

  def listItems(state: StateList) =
    'ul (
      'class /= "list-group",
      for (item <- state.list) yield {
        'li (
          'a (
            item.name,
            event('click) {
              immediateTransition {
                case s =>
                  s.copy(selected = Some(item))
              }
            }
          )
        )
      },
      formField("new value",
                input,
                "input",
                state.selected.map(_.name).getOrElse("")),
      'button (
        "Update selected",
        eventWithAccess('click) { access =>
          deferredTransition {
            for {
              newStr <- access.property[String](input).get('value)
            } yield
              transition {
                case s: StateList =>
                  s.selected match {
                    case Some(item) =>
                      s.copy(
                        list = s.list
                          .takeWhile(_ != item) ++ (ItemInList(
                          newStr) :: s.list
                          .dropWhile(_ != item)
                          .tail))
                    case None =>
                      s
                  }
              }
          }
        }
      ),
      'button (
        "Remove selected",
        eventWithAccess('click) { access =>
          deferredTransition {
            for {
              newStr <- access.property[String](input).get('value)
            } yield
              transition {
                case s: StateList =>
                  s.selected match {
                    case Some(item) =>
                      StateList(s.list.takeWhile(_ != item) ++ s.list
                                  .dropWhile(_ != item)
                                  .tail,
                                selected = None)
                    case None =>
                      s
                  }
              }
          }
        }
      ),
      'button (
        "Add",
        eventWithAccess('click) { access =>
          deferredTransition {
            for {
              newStr <- access.property[String](input).get('value)
            } yield
              transition {
                case s: StateList =>
                  if (newStr != "")
                    s.copy(list = s.list :+ ItemInList(newStr))
                  else
                    s
              }
          }
        }
      )
    )

  def formField(title: String,
                elId: ElementId,
                name: String,
                value: String) = {
    'div (
      'class /= "form-group",
      'label ('class /= "col-sm-3 control-label", title),
      'div ('class /= "col-sm-9",
            'div ('class /= "input-group",
                  'input (elId,
                          'name /= name,
                          'type /= "text",
                          'class /= "form-control",
                          'placeholder /= title,
                          'value := value)))
    )
  }

  val service = blazeService[Future, StateList, Any] from KorolevServiceConfig[
    Future,
    StateList,
    Any](
    //serverRouter = ServerRouter.empty[Future, State],
    stateStorage = storage,
    head = Seq(
      'head (
        'title ("Main Routing Page"),
        'link (
          'href /= "https://maxcdn.bootstrapcdn.com/bootstrap/3.3.7/css/bootstrap.min.css",
          'rel /= "stylesheet",
          'type /= "text/css"),
        'link ('href /= "/treeview.css",
               'rel /= "stylesheet",
               'type /= "text/css")
      )
    ),
    render = {
      case state =>
        'body (
          'div ('class /= "container",
                'div ('class /= "row",
                      'div ('class /= "col-sm-4",
                            'div (
                              'id /= "treeview1",
                              'class /= "treeview",
                              listItems(state)
                            ))))
        )
    },
    serverRouter = {
      ServerRouter(
        dynamic = (_, _) =>
          Router(
            fromState = {
              case s: StateList =>
                Root
            },
            toState = {
              case (s, Root) =>
                //val u = s.copy(template = "summary1")
                Future.successful(s)
              case (s, Root / name) =>
                Future.successful(s)
            }
        ),
        static = (deviceId) =>
          Router(
            toState = {
              case (_, Root) =>
                storage.initial(deviceId)
              case (_, Root / name) =>
                storage.initial(deviceId) map { s =>
                  s
                }
            }
        )
      )
    }
  )
}
*/