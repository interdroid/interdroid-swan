package interdroid.swan;

import interdroid.swan.swansong.TriStateExpression;
import interdroid.swan.swansong.ValueExpression;

/**
 * Generic listener for both {@link TriStateExpression} expressions and
 * {@link ValueExpression} expressions.
 * 
 * @author rkemp
 * 
 */
public interface ExpressionListener extends TriStateExpressionListener,
		ValueExpressionListener {

}
