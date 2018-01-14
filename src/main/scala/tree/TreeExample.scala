package tree

import korolev.execution._
import korolev.ApplicationContext
import korolev.Router
import korolev.Router.{Root, /}
import korolev.blazeServer.{KorolevBlazeServer, blazeService}
import korolev.server.{KorolevServiceConfig, ServerRouter, StateStorage}
import levsha.events.EventPhase
import tree.TreeState.RootNode

import scala.concurrent.Future

case class TreeState(tree: RootNode,
                     selected: Option[TreeNode] = None,
                     choosingParent: Boolean = false) {
  def selectedParent: Option[TreeNode] =
    (for {
      tree <- tree
      sel <- selected
    } yield tree.parentOf(sel)).flatten
}
object TreeState {
  type RootNode = Option[TreeNode]

  lazy val demoTree: RootNode =
    Some(
      TreeNode(
        title = "Root",
        children = List(
          TreeNode(title = "Child1",
                   //expanded = false,
                   children = List(TreeNode(title = "Child12"),
                                   TreeNode(title = "Child13"))),
          TreeNode(title = "Child2"),
          TreeNode(title = "Child3")
        )
      ))
}

case class State(treeState: TreeState) {
  def updateIfSelected(f: (State, TreeNode) => State) =
    treeState.selected match {
      case Some(item) =>
        f(this, item)
      case None =>
        this
    }
}

object State {
  val applicationContext = ApplicationContext[Future, State, Any]
}

object TreeExample extends KorolevBlazeServer {
  import State.applicationContext._
  import State.applicationContext.symbolDsl._

  val initialTreeState = TreeState(TreeState.demoTree)

  // Handler to input
  val storage =
    StateStorage.default[Future, State](State(initialTreeState))
  val nameElementId = elementId
  val descriptionElementId = elementId

  def clickExpandCollapseTreeNode(state: State, treeNode: TreeNode): State = {
    state.treeState.tree match {
      case Some(n) =>
        val newTreeNode = treeNode.copy(expanded = !treeNode.expanded)
        State(
          TreeState(Some(n.updated(treeNode, newTreeNode)), Some(newTreeNode)))
      case None =>
        state
    }
  }

  def item(state: State,
           treeNode: TreeNode,
           depth: Int,
           showExpandCollapse: Boolean) = {

    def insertDepth(n: Node*) = {
      (0 until depth).map(d => 'span ('class /= "indent")) ++ n.toSeq
    }

    'li (
      event('click, EventPhase.AtTarget) {
        immediateTransition {
          case s =>
            State(s.treeState.copy(selected = Some(treeNode)))
        }
      },
      'class /= "list-group-item node-treeview1",
      'style /= {
        if (state.treeState.selected.contains(treeNode))
          "background-color:#428bca"
        else ""
      },
      insertDepth(
        'span (
          event('click, EventPhase.AtTarget) {
            immediateTransition {
              case s =>
                clickExpandCollapseTreeNode(state, treeNode)
            }
          },
          'class /= "icon expand-icon glyphicon" +
            (if (showExpandCollapse) {
               if (treeNode.expanded)
                 " glyphicon-minus"
               else
                 " glyphicon-plus"
             } else "")
        ),
        treeNode.title
      )
    )
  }

  def treeItemsRec(state: State, tree: TreeNode, depth: Int): Seq[Node] = {
    val noChildren = tree.children.isEmpty
    val expanded = tree.expanded

    val c =
      item(state, tree, depth, !noChildren)
    if (noChildren || !expanded) Seq(c)
    else {
      val result = for (c <- tree.children) yield {
        treeItemsRec(state, c, depth + 1)
      }
      c +: result.flatten
    }
  }

  def listParentNodes(state: State) = {
    state.treeState.tree.map(_.parentNodes).getOrElse(Nil).map { node =>
      'li (
        'a (
          event('click, EventPhase.AtTarget) {
            immediateTransition {
              case s =>
                val treeState = s.treeState
                val newTree: RootNode = for {
                  t <- treeState.tree
                  selected <- treeState.selected
                } yield t.moved(selected, node)
                State(TreeState(newTree, None))
            }
          },
          node.title
        )
      )
    }
  }

  def treeItems(state: State) = {
    val treeItems: Node = state.treeState.tree match {
      case Some(t) => treeItemsRec(state, t, 0)
      case None    => 'div () //TODO substitute with empty element
    }

    'div (
      treeItems
    )
  }

  def parentDropdown(state: State, parent: TreeNode) =
    if (state.treeState.choosingParent)
      'div (
        'class /= s"dropdown open",
        'button (
          event('click, EventPhase.AtTarget) {
            immediateTransition {
              case state =>
                val choosingParent = state.treeState.choosingParent
                State(state.treeState.copy(choosingParent = !choosingParent))
            }
          },
          'class /= "btn btn-primary dropdown-toggle",
          'type /= "button",
          'dataToggle /= "dropdown",
          'span ('class /= "caret"),
          parent.title,
          'ul (
            'class /= "dropdown-menu",
            listParentNodes(state)
          )
        )
      )

    else 'div (
      'class /= s"dropdown hover",
      'button (
        event('click, EventPhase.AtTarget) {
          immediateTransition {
            case state =>
              val choosingParent = state.treeState.choosingParent
              State(state.treeState.copy(choosingParent = !choosingParent))
          }
        },
        'class /= "btn btn-primary dropdown-toggle",
        'type /= "button",
        'dataToggle /= "dropdown",
        'span ('class /= "caret"),
        parent.title,
        'ul (
          'class /= "dropdown-menu",
          listParentNodes(state)
        )
      )
    )
  def parentFolder(state: State) = state.treeState.selectedParent match {
    case Some(parent) =>
      'div (
        'class /= "row",
        'label ('class /= "col-sm-3 control-label", "Parent folder"),
        parentDropdown(state, parent)
      )
    case None => 'div ()
  }

  def editTree(state: State) = {
    'div (
      parentFolder(state),
      formField(
        "Name",
        nameElementId,
        "Select a node to edit it",
        state.treeState.selected.map(_.title).getOrElse("")
      ),
      textArea(
        "Description",
        descriptionElementId,
        "Insert the description of the node",
        state.treeState.selected.map(_.description).getOrElse("")
      ),
      'button (
        "Save",
        'class /= "btn btn-primary dropdown-toggle pull-right",
        eventWithAccess('click, EventPhase.AtTarget) { access =>
          deferredTransition {
            for {
              newName <- access.property[String](nameElementId).get('value)
              newDescription <- access
                .property[String](descriptionElementId)
                .get('value)
            } yield
              transition {
                case state: State =>
                  state.updateIfSelected {
                    (s, selected) =>
                      val newSelected = selected.copy(title = newName,
                                                      description =
                                                        newDescription)
                      State(
                        TreeState(tree = s.treeState.tree
                                    .map(_.updated(selected, newSelected)),
                                  selected = Some(newSelected)))
                    //TODO choose update policy for example TreeNode(title = newStr)
                  }
              }
          }
        }
      ),
      'button (
        eventWithAccess('click, EventPhase.AtTarget) { access =>
          immediateTransition {
            case s: State =>
              access.property[String](nameElementId).set('value, "")
              access.property[String](descriptionElementId).set('value, "")
              State(s.treeState.copy(selected = None))
          }
        },
        'class /= "btn btn-primary dropdown-toggle pull-right",
        "Cancel"
      )
    )
  }

  def formField(title: String,
                elId: ElementId,
                placeholder: String,
                //name: String,
                value: String): Node = {
    'div (
      'class /= "form-group",
      'label ('class /= "col-sm-3 control-label", title),
      'div (
        'class /= "col-sm-9",
        'div ('class /= "input-group",
              'input (elId,
                      //'name /= name,
                      'type /= "text",
                      'class /= "form-control",
                      'placeholder /= placeholder,
                      'value := value))
      )
    )
  }

  def textArea(title: String,
               elId: ElementId,
               placeholder: String,
               //name: String,
               value: String): Node = {
    'div (
      'class /= "form-group",
      'label ('class /= "col-sm-3 control-label", title),
      'textarea (
        elId,
        //'name /= name,
        'type /= "text",
        'class /= "col-sm-9 input-group form-controls",
        'style /= "resize:none",
        'rows /= "5",
        'placeholder /= placeholder,
        'value := value
      )
    )
  }

  val service = blazeService[Future, State, Any] from
    KorolevServiceConfig[Future, State, Any](
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
            'div (
              'class /= "container",
              'div (
                'class /= "row",
                'div (
                  'class /= "col-sm-4",
                  'h3 ("Properties Tree"),
                  'button (
                    eventWithAccess('click, EventPhase.AtTarget) {
                      access =>
                        deferredTransition {
                          for {
                            newStr <- access
                              .property[String](nameElementId)
                              .get('value)
                          } yield
                            transition {
                              case s: State =>
                                if (newStr != "") {
                                  val newNode: TreeNode =
                                    TreeNode(title = newStr)
                                  state.updateIfSelected { (s, selected) =>
                                    State(TreeState(s.treeState.tree match {
                                      case Some(n) =>
                                        Some(n.updated(selected,
                                                       selected + newNode))
                                      case None => Some(newNode)
                                    }, selected = Some(newNode)))
                                  }
                                } else s
                            }
                        }
                    },
                    'type /= "button",
                    'class /= "btn btn-circle btn-default glyphicon glyphicon-plus pull-right"
                  ),
                  'button (
                    eventWithAccess('click, EventPhase.AtTarget) {
                      access =>
                        deferredTransition {
                          for {
                            newStr <- access
                              .property[String](nameElementId)
                              .get('value)
                          } yield
                            transition {
                              case s: State =>
                                if (newStr != "") {
                                  val newNode: TreeNode =
                                    TreeNode(title = newStr)
                                  state.updateIfSelected { (s, selected) =>
                                    val someNewNode = Some(newNode)
                                    State(TreeState(s.treeState.tree match {
                                      case Some(n) =>
                                        val parent = n.parentOf(selected)
                                        parent match {
                                          case Some(p) =>
                                            Some(n.updated(p, p + newNode))
                                          case None => Some(n)
                                        }
                                       case None => someNewNode
                                    }, selected = someNewNode))
                                  }
                                } else s
                            }
                        }
                    },
                    'type /= "button",
                    'class /= "btn btn-circle btn-default glyphicon glyphicon-plus pull-right"
                  ),
                  'button (
                    event('click, EventPhase.AtTarget) {
                      immediateTransition {
                        case state: State =>
                          state.updateIfSelected { (s, selected) =>
                            State(
                              TreeState(
                                s.treeState.tree.map(_ - selected),
                                selected = None
                              ))
                          }
                      }
                    },
                    'type /= "button",
                    'class /= "btn btn-circle btn-default glyphicon glyphicon-minus pull-right"
                  )
                ),
                'div (
                  'class /= "col-sm-8",
                  'h3 ("Edit tree")
                )
              ),
              'div ('class /= "row",
                    'div (
                      'class /= "col-sm-4",
                      'div (
                        'id /= "treeview1",
                        'class /= "treeview",
                        treeItems(state)
                      )
                    ),
                    'div (
                      'class /= "col-sm-8",
                      'div (
                        editTree(state)
                      )
                    ))
            )
          )
      },
      serverRouter = {
        ServerRouter(
          dynamic = (_, _) =>
            Router(
              fromState = {
                case s: State =>
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
