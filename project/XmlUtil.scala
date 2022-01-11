import scala.xml.{ Elem, Node, NodeSeq }
import scala.xml.transform.{ RewriteRule, RuleTransformer }

object XmlUtil {

  def transformPomDependencies(
      tx: Elem => Option[Node]
    ): Node => Node = { node: Node =>
    val tr = new RuleTransformer(new RewriteRule {
      override def transform(node: Node): NodeSeq = node match {
        case e: Elem if e.label == "dependency" =>
          tx(e) match {
            case Some(n) => n
            case _       => NodeSeq.Empty
          }

        case _ => node
      }
    })

    tr.transform(node).headOption match {
      case Some(transformed) => transformed
      case _                 => sys.error("Fails to transform the POM")
    }
  }
}
