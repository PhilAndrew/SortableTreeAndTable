package tree

case class TreeNode(id: String = java.util.UUID.randomUUID.toString,
                    title: String = "",
                    description: String = "No description",
                    expanded: Boolean = true,
                    children: List[TreeNode] = List()) {
  def + (t: TreeNode): TreeNode = {
    copy(children = children :+ t)
  }

  def - (t: TreeNode): TreeNode = {
    if(children.exists(_.id == t.id)) {
      copy(children = children.filter(_.id != t.id))
    } else {
      val newChildren =
        for {
          child <- children
        } yield child - t
      copy(children = newChildren)
    }
  }

  def updated(toUpdate: TreeNode, newNode: TreeNode): TreeNode = {
    if(this.id == toUpdate.id) newNode
    else {
      val newChildren =
        for {
          child <- children
        } yield child.updated(toUpdate, newNode)
      copy(children = newChildren)
    }
  }

  def moved(node: TreeNode, newParent: TreeNode): TreeNode = {
    (this - node).addChild(newParent, node)
  }

  def addChild(parent: TreeNode, childToAdd: TreeNode): TreeNode = {
    if(this.id == parent.id) this + childToAdd
    else {
      val newChildren =
        for {
          child <- children
        } yield child.addChild(parent, childToAdd)
      copy(children = newChildren)
    }
  }

  def parentOf(node: TreeNode): Option[TreeNode] = {
    if(children.exists(_.id == node.id)) {
      Some(this)
    } else {
      val newChildren =
        for {
          child <- children
        } yield child.parentOf(node)
      newChildren.flatten.headOption
    }
  }

  def parentNodes: List[TreeNode] = {
    def loop(tree: TreeNode): List[TreeNode] = {
      if(tree.children.isEmpty) Nil
      else tree :: tree.children.flatMap(loop)
    }
    loop(this)
  }
}