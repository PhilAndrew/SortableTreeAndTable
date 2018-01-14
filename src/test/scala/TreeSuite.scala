import org.scalatest.FunSuite
import tree.TreeNode

class TreeSuite extends FunSuite {


  test("remove a direct child from a tree") {
    val t3 = TreeNode(
      id = "3",
      children = List()
    )
    val t1 =
      TreeNode(
        id = "1",
        children = List(
          TreeNode(
            id = "2",
            children = List()
          ),
          t3
        )
      )

    val removed = t1 - t3
    val manualRemoved =
      TreeNode(
        id = "1",
        children = List(
          TreeNode(
            id = "2",
            children = List()
          )
        )
      )

    assert( removed === manualRemoved )
  }


  test("remove a indirect child on a tree") {
    val t3 = TreeNode(
      id = "3",
      children = List()
    )
    val t1 =
      TreeNode(
        id = "1",
        children = List(
          TreeNode(
            id = "2",
            children = List()
          ),
          TreeNode(
            id = "4",
            children = List(
              TreeNode(
                id = "5",
                children = List(t3)
              )
            )
          )
        )
      )

    val removed = t1 - t3
    val manualRemoved =       TreeNode(
      id = "1",
      children = List(
        TreeNode(
          id = "2",
          children = List()
        ),
        TreeNode(
          id = "4",
          children = List(
            TreeNode(
              id = "5",
              children = List()
            )
          )
        )
      )
    )
    assert( removed === manualRemoved )
  }


  test("updated tree") {
    val toUpdate = TreeNode(
      id = "3",
      children = List()
    )

    val newNode = TreeNode(
      id = "ciao",
      children = List()
    )



    val t1 =
      TreeNode(
        id = "1",
        children = List(
          TreeNode(
            id = "2",
            children = List()
          ),
          TreeNode(
            id = "4",
            children = List(
              TreeNode(
                id = "5",
                children = List(toUpdate)
              )
            )
          )
        )
      )

    val updated = t1.updated(toUpdate, newNode)

    val manuallyUpdated =
      TreeNode(
        id = "1",
        children = List(
          TreeNode(
            id = "2",
            children = List()
          ),
          TreeNode(
            id = "4",
            children = List(
              TreeNode(
                id = "5",
                children = List(newNode)
              )
            )
          )
        )
      )

    assert(updated === manuallyUpdated)
  }

  test("moved") {
    val toMove = TreeNode(
      id = "3",
      children = List()
    )



    val t1 =
      TreeNode(
        id = "1",
        children = List(
          TreeNode(
            id = "2",
            children = List()
          ),
          TreeNode(
            id = "4",
            children = List(
              TreeNode(
                id = "5",
                children = List(toMove)
              )
            )
          )
        )
      )

    val moved = t1.moved(toMove, t1)

    val manuallyMoved =
      TreeNode(
        id = "1",
        children = List(
          TreeNode(
            id = "2",
            children = List()
          ),
          TreeNode(
            id = "4",
            children = List(
              TreeNode(
                id = "5",
                children = List()
              )
            )
          ),
          toMove
        )
      )

    assert(moved === manuallyMoved)
  }
}
