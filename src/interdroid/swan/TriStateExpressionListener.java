package interdroid.swan;

import interdroid.swan.swansong.Expression;
import interdroid.swan.swansong.TriState;

public interface TriStateExpressionListener {

	/**
	 * This method will be invoked when the {@link Expression} with the given id
	 * changes state to a new {@link TriState}.
	 * 
	 * @param id
	 *            the id of the {@link Expression}.
	 * @param timestamp
	 *            the timestamp when the change happened.
	 * @param newState
	 *            the new state.
	 */
	public void onNewState(String id, long timestamp, TriState newState);

}
