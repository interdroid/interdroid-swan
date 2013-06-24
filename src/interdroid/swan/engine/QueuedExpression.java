package interdroid.swan.engine;

import interdroid.swan.swansong.Expression;
import interdroid.swan.swansong.Result;
import interdroid.swan.swansong.TimestampedValue;
import interdroid.swan.swansong.TriStateExpression;
import interdroid.swan.swansong.ValueExpression;

import java.util.Date;

public class QueuedExpression implements Comparable<QueuedExpression> {

	private Expression mExpression;
	private String mId;
	private Result mCurrentResult;
	private long mStartTime;
	private int mEvaluations; // number of evaluations
	private long mTotalEvaluationTime; // total time spent on evaluations so far
	private long mMinEvaluationTime = Long.MAX_VALUE;
	private long mMaxEvaluationTime = Long.MIN_VALUE;
	private long mTotalEvaluationDelay;
	private long mEvaluationsDelay; //number of evaluations with delay

	public QueuedExpression(String id, Expression expression) {
		mId = id;
		mExpression = expression;
		mStartTime = System.currentTimeMillis();
	}

	public int compareTo(QueuedExpression another) {
		return mCurrentResult.compareTo(another.mCurrentResult);
	};

	public Expression getExpression() {
		return mExpression;
	}

	public String getId() {
		return mId;
	}

	/**
	 * update the current result and return whether this update caused a change
	 * 
	 * @param result
	 * @return
	 */
	public boolean update(Result result) {
		if (result == null) {
			// result is null, means reset defer until
			mCurrentResult.setDeferUntil(0);
			// mCurrentResult = null;
			return false;
		}
		if (mExpression instanceof TriStateExpression && mCurrentResult != null
				&& mCurrentResult.getTriState() == result.getTriState()) {
			mCurrentResult = result;
			return false;
		} else if (mExpression instanceof ValueExpression
				&& mCurrentResult != null
				&& !hasChanged(mCurrentResult.getValues(), result.getValues())) {
			mCurrentResult = result;
			return false;
		}
		mCurrentResult = result;
		return true;
	}

	private boolean hasChanged(TimestampedValue[] a, TimestampedValue[] b) {
		if (a == null || b == null) {
			return true;
		}
		if (a.length != b.length) {
			return true;
		}
		for (int i = 0; i < a.length; i++) {
			if (!a[i].equals(b[i])) {
				return true;
			}
		}
		return false;
	}

	public long getDeferUntil() {
		if (mCurrentResult != null) {
			return mCurrentResult.getDeferUntil();
		} else {
			// we don't have a current result yet, so we can't defer
			return 0;
		}

	}

	public String toString() {
		String id = mId;
		if (mId.contains(Expression.SEPARATOR)) {
			id = "<remote> " + mId.split(Expression.SEPARATOR, 2)[1];
		}
		return id
				+ "\n"
				+ mCurrentResult
				+ "\n"
				+ mExpression.toParseString()
				+ "\nStart time: "
				+ new Date(mStartTime)
				+ "\nEvaluation Rate: "
				+ (mEvaluations / ((System.currentTimeMillis() - mStartTime) / 60000.0))
				+ " per minute"
				+ "\nAvg Evaluation Time: "
				+ (mTotalEvaluationTime / Math.max(mEvaluations, 1))
				+ "\nMin Evaluation Time: "
				+ mMinEvaluationTime
				+ "\nMax Evaluation Time: "
				+ mMaxEvaluationTime
				+ "\nEvaluation Percentage: "
				+ ((mTotalEvaluationTime * 100) / (float) (System
						.currentTimeMillis() - mStartTime))
				+ "\nAvg Evaluation Delay: " + (mTotalEvaluationDelay/Math.max(mEvaluationsDelay, 1));
	}

	public void evaluated(long currentEvalutionTime, long evalDelay) {
		mEvaluations += 1;
		mTotalEvaluationTime += currentEvalutionTime;
		mMinEvaluationTime = Math.min(mMinEvaluationTime, currentEvalutionTime);
		mMaxEvaluationTime = Math.max(mMaxEvaluationTime, currentEvalutionTime);
		if(evalDelay != 0 ){
			mTotalEvaluationDelay += evalDelay;
			mEvaluationsDelay += 1;
			System.out.println("Added Delay: " + evalDelay +  " Total Delay: " + mTotalEvaluationDelay);
		}
	}

}
