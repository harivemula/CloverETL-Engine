/* Generated By:JJTree: Do not edit this line. CLVFSqrtNode.java */

package org.jetel.interpreter.node;
import org.jetel.interpreter.TransformLangParser;
import org.jetel.interpreter.TransformLangParserVisitor;

public class CLVFSqrtNode extends SimpleNode {
  public CLVFSqrtNode(int id) {
    super(id);
  }

  public CLVFSqrtNode(TransformLangParser p, int id) {
    super(p, id);
  }


  /** Accept the visitor. **/
  public Object jjtAccept(TransformLangParserVisitor visitor, Object data) {
    return visitor.visit(this, data);
  }
}
