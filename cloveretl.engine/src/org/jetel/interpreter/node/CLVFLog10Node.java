/* Generated By:JJTree: Do not edit this line. CLVFLog10Node.java */

package org.jetel.interpreter.node;
import org.jetel.interpreter.TransformLangParser;
import org.jetel.interpreter.TransformLangParserVisitor;

public class CLVFLog10Node extends SimpleNode {
  public CLVFLog10Node(int id) {
    super(id);
  }

  public CLVFLog10Node(TransformLangParser p, int id) {
    super(p, id);
  }


  /** Accept the visitor. **/
  public Object jjtAccept(TransformLangParserVisitor visitor, Object data) {
    return visitor.visit(this, data);
  }
}
