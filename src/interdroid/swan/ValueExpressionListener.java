package interdroid.swan;

import interdroid.swan.swansong.HistoryReductionMode;
import interdroid.swan.swansong.TimestampedValue;
import interdroid.swan.swansong.ValueExpression;

public interface ValueExpressionListener {

	/**
	 * This method will be invoked when a {@link ValueExpression} produces new
	 * values. Depending on the {@link HistoryReductionMode} the array with new
	 * values can have a single value or multiple values.
	 * 
	 * @param id
	 * @param newValues
	 */
	public void onNewValues(String id, TimestampedValue[] newValues);

}
