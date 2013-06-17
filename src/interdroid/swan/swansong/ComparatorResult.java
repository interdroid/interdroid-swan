package interdroid.swan.swansong;


public class ComparatorResult extends Result {

	/**
	 * 
	 */
	private static final long serialVersionUID = 7226955492296550441L;
	private HistoryReductionMode mLeft;
	private HistoryReductionMode mRight;

	public ComparatorResult(long timestamp, HistoryReductionMode left,
			HistoryReductionMode right) {
		super(timestamp, TriState.UNDEFINED);
		this.mLeft = left;
		this.mRight = right;
	}

	public void startOuterLoop() {
		// left == ANY -> start with FALSE, stop with positive counter example
		// left == ALL -> start with TRUE, stop with negative counter example
		if (mLeft == HistoryReductionMode.ANY) {
			mTriState = TriState.FALSE;
		} else if (mLeft == HistoryReductionMode.ALL) {
			mTriState = TriState.TRUE;
		}
	}

	public void startInnerLoop() {
		if (mRight == HistoryReductionMode.ANY) {
			mTriState = TriState.FALSE;
		} else if (mRight == HistoryReductionMode.ALL) {
			mTriState = TriState.TRUE;
		}
	}

	public boolean innerResult(TriState result) {
		if (mRight == HistoryReductionMode.ANY) {
			if (result == TriState.TRUE) {
				mTriState = TriState.TRUE;
				return true;
			}
		} else if (mRight == HistoryReductionMode.ALL) {
			if (result == TriState.FALSE) {
				mTriState = TriState.FALSE;
				return true;
			}
		}
		return false;
	}

	public boolean outerResult() {
		if (mLeft == HistoryReductionMode.ANY) {
			if (mTriState == TriState.TRUE) {
				return true;
			}
		} else if (mLeft == HistoryReductionMode.ALL) {
			if (mTriState == TriState.FALSE) {
				return true;
			}
		}
		return false;
	}

}
