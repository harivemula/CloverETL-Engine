/* Generated By:JJTree: Do not edit this line. CLVFPINode.java */

package org.jetel.interpreter.node;
import org.jetel.interpreter.TransformLangParser;
import org.jetel.interpreter.TransformLangParserVisitor;

public class CLVFPINode extends SimpleNode {
  public CLVFPINode(int id) {
    super(id);
  }

  public CLVFPINode(TransformLangParser p, int id) {
    super(p, id);
  }


  /** Accept the visitor. **/
  public Object jjtAccept(TransformLangParserVisitor visitor, Object data) {
    return visitor.visit(this, data);
  }
}
