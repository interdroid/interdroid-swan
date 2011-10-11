package interdroid.contextdroid.contextexpressions;

import interdroid.contextdroid.ContextDroidException;
import interdroid.contextdroid.ContextManager;
import interdroid.contextdroid.contextservice.SensorConfigurationException;
import interdroid.contextdroid.contextservice.SensorInitializationFailedException;
import interdroid.contextdroid.contextservice.SensorManager;

import java.io.Serializable;
import java.util.Scanner;

import android.os.Parcel;
import android.os.Parcelable;

public class Expression implements Parcelable, Serializable,
		Comparable<Expression> {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1056115630721395151L;

	private static final String COMPARE_STRATEGY_ALL = "ALL";
	private static final String COMPARE_STRATEGY_ANY = "ANY";

	boolean leafNode;
	long deferUntil;

	Expression leftExpression;
	Expression rightExpression;

	String operator;

	TypedValue leftValue;
	TypedValue rightValue;

	String comparator;
	String compareStrategy = COMPARE_STRATEGY_ALL;

	int result = ContextManager.UNDEFINED;

	String id;

	private Expression(Parcel in) {
		readFromParcel(in);
	}

	public Expression(Expression left, String operator, Expression right) {
		this.leafNode = false;
		this.leftExpression = left;
		this.rightExpression = right;
		this.operator = operator;
	}

	public Expression(String operator, Expression expression) {
		this(expression, operator, null);
	}

	public Expression(TypedValue left, String comparator, TypedValue right) {
		this.leafNode = true;
		this.leftValue = left;
		this.rightValue = right;
		Scanner scanner = new Scanner(comparator);
		this.comparator = scanner.next();
		if (scanner.hasNext()) {
			String strategy = scanner.next();
			if (COMPARE_STRATEGY_ALL.equals(strategy)) {
				compareStrategy = COMPARE_STRATEGY_ALL;
			} else if (COMPARE_STRATEGY_ANY.equals(strategy)) {
				compareStrategy = COMPARE_STRATEGY_ANY;
			} else {
				throw new AssertionError("Invalid compare strategy: '"
						+ strategy + "', can only be " + COMPARE_STRATEGY_ALL
						+ " or " + COMPARE_STRATEGY_ANY);
			}
		}

	}

	public String getId() {
		return id;
	}

	public void initialize(String id, SensorManager sensorManager)
			throws SensorConfigurationException,
			SensorInitializationFailedException {
		this.id = id;
		if (leafNode) {
			leftValue.initialize(id + ".L", sensorManager);
			if (rightValue != null) {
				rightValue.initialize(id + ".R", sensorManager);
			}
		} else {
			leftExpression.initialize(id + ".L", sensorManager);
			if (rightExpression != null) {
				rightExpression.initialize(id + ".R", sensorManager);
			}
		}
	}

	public void destroy(String id, SensorManager sensorManager)
			throws ContextDroidException {
		if (leafNode) {
			leftValue.destroy(id + ".L", sensorManager);
			if (rightValue != null) {
				rightValue.destroy(id + ".R", sensorManager);
			}
		} else {
			leftExpression.destroy(id + ".L", sensorManager);
			if (rightExpression != null) {
				rightExpression.destroy(id + ".R", sensorManager);
			}
		}
	}

	public boolean evaluate() throws ContextDroidException {
		int previousResult = result;
		evaluate(System.currentTimeMillis());
		return previousResult != result;
	}

	// TODO: shortcircuit some non leaf node evaluations
	// left = TRUE, comparator = &&
	void evaluate(long now) throws ContextDroidException {
		if (now < deferUntil) {
			System.out.println("not evaluating");
			return;
		}
		if (leafNode) {
			try {
				evaluateLeafNode(now);
			} catch (NoValuesInIntervalException e) {
				setResult(ContextManager.UNDEFINED);
			}
		} else {
			leftExpression.evaluate(now);
			if (rightExpression != null) {
				rightExpression.evaluate(now);
				deferUntil = Math.min(leftExpression.deferUntil,
						rightExpression.deferUntil);
			} else {
				deferUntil = leftExpression.deferUntil;
			}
			evaluateNonLeafNode();
		}
	}

	private void evaluateNonLeafNode() {
		int leftResult = leftExpression.getResult();
		int rightResult = ContextManager.UNDEFINED;
		if (rightExpression != null) {
			rightExpression.getResult();
		}
		if ("!".equals(operator)) {
			if (leftResult == ContextManager.UNDEFINED) {
				setResult(ContextManager.UNDEFINED);
			} else if (leftResult == ContextManager.TRUE) {
				setResult(ContextManager.FALSE);
			} else if (leftResult == ContextManager.FALSE) {
				setResult(ContextManager.TRUE);
			}
		} else if ("&&".equals(operator)) {
			if (leftResult == ContextManager.UNDEFINED
					|| rightResult == ContextManager.UNDEFINED) {
				setResult(ContextManager.UNDEFINED);
			} else if (leftResult == ContextManager.TRUE
					&& rightResult == ContextManager.TRUE) {
				setResult(ContextManager.TRUE);
			} else {
				setResult(ContextManager.FALSE);
			}
		} else if ("||".equals(operator)) {
			if (leftResult == ContextManager.UNDEFINED
					&& rightResult == ContextManager.UNDEFINED) {
				setResult(ContextManager.UNDEFINED);
			} else if (leftResult == ContextManager.TRUE
					|| rightResult == ContextManager.TRUE) {
				setResult(ContextManager.TRUE);
			} else {
				setResult(ContextManager.FALSE);
			}
		}
	}

	private void evaluateLeafNode(long now) throws ContextDroidException,
			NoValuesInIntervalException {
		TimestampedValue[] left = leftValue.getValues(id + ".L", now);
		TimestampedValue[] right = rightValue.getValues(id + ".R", now);

		int endResult = (compareStrategy.equals(COMPARE_STRATEGY_ALL)) ? ContextManager.TRUE
				: ContextManager.FALSE;

		for (TimestampedValue leftItem : left) {
			for (TimestampedValue rightItem : right) {
				int tempResult = evaluateLeafItem(leftItem.value,
						rightItem.value);
				if (compareStrategy.equals(COMPARE_STRATEGY_ALL)) {
					if (tempResult == ContextManager.FALSE) {
						setResult(now, ContextManager.FALSE, left, right);
						return;
					} else if (tempResult == ContextManager.UNDEFINED) {
						endResult = ContextManager.UNDEFINED;
					}
				} else {
					if (tempResult == ContextManager.TRUE) {
						setResult(now, ContextManager.TRUE, left, right);
						return;
					} else if (tempResult == ContextManager.UNDEFINED) {
						endResult = ContextManager.UNDEFINED;
					}
				}

			}
		}
		setResult(now, endResult, left, right);
	}

	private void setResult(long now, int result, Object[] left, Object[] right) {
		if (result == ContextManager.UNDEFINED) {
			setResult(result);
			return;
		}
		// TODO: cache these values?
		if (!leftValue.hasCurrentTime() && !rightValue.hasCurrentTime()) {
			deferUntil = Math.min(leftValue.deferUntil(),
					rightValue.deferUntil());
			setResult(result);
			return;
		}

		// TODO: think about when left or right is an array
		long leftTime = (Long) ((TimestampedValue) left[0]).value;
		long rightTime = (Long) ((TimestampedValue) right[0]).value;

		// we're dealing with current time, now do something smart
		if (("<".equals(comparator) || "<=".equals(comparator))) {
			if (leftValue.hasCurrentTime()) {
				if (result == ContextManager.TRUE) {
					// set defer until to right - left
					deferUntil = now + rightTime - leftTime;
				} else {
					// it's FALSE and won't be TRUE anymore
					deferUntil = Long.MAX_VALUE;
				}
			} else {
				// right has current time
				if (result == ContextManager.TRUE) {
					// it's TRUE and won't be FALSE anymore
					deferUntil = Long.MAX_VALUE;
				} else {
					// set defer until to left - right
					deferUntil = now + leftTime - rightTime;
				}
			}
		} else if ((">".equals(comparator) || ">=".equals(comparator))) {
			if (leftValue.hasCurrentTime()) {
				if (result == ContextManager.TRUE) {
					// it's TRUE and won't be FALSE anymore
					deferUntil = Long.MAX_VALUE;
				} else {
					// set defer until to right - left
					deferUntil = now + rightTime - leftTime;
				}
			} else {
				if (result == ContextManager.TRUE) {
					// set defer until to left - right
					deferUntil = now + leftTime - rightTime;
				} else {
					// it's FALSE and won't be TRUE anymore
					deferUntil = Long.MAX_VALUE;
				}
			}
		}
	}

	private int evaluateLeafItem(Object left, Object right) {
		if ("<".equals(comparator)) {
			int result = ((Comparable) left).compareTo(((Comparable) right));
			return (result < 0) ? ContextManager.TRUE : ContextManager.FALSE;
		} else if ("<=".equals(comparator)) {
			int result = ((Comparable) left).compareTo(((Comparable) right));
			return (result <= 0) ? ContextManager.TRUE : ContextManager.FALSE;
		} else if (">".equals(comparator)) {
			int result = ((Comparable) left).compareTo(((Comparable) right));
			return (result > 0) ? ContextManager.TRUE : ContextManager.FALSE;
		} else if (">=".equals(comparator)) {
			int result = ((Comparable) left).compareTo(((Comparable) right));
			return (result >= 0) ? ContextManager.TRUE : ContextManager.FALSE;
		} else if ("==".equals(comparator)) {
			int result = ((Comparable) left).compareTo(((Comparable) right));
			return (result == 0) ? ContextManager.TRUE : ContextManager.FALSE;
		} else if ("!=".equals(comparator)) {
			int result = ((Comparable) left).compareTo(((Comparable) right));
			return (result != 0) ? ContextManager.TRUE : ContextManager.FALSE;
		} else if ("regexp".equals(comparator)) {
			return ((String) left).matches((String) right) ? ContextManager.TRUE
					: ContextManager.FALSE;
		} else if ("contains".equals(comparator)) {
			return ((String) left).contains((String) right) ? ContextManager.TRUE
					: ContextManager.FALSE;
		}
		throw new AssertionError("Unknown comparator '" + comparator
				+ "'. Should not happen");
	}

	public void setResult(int result) {
		this.result = result;
	}

	public int getResult() {
		return result;
	}

	@Override
	public int describeContents() {
		return 0;
	}

	@Override
	public void writeToParcel(Parcel dest, int flags) {
		dest.writeInt(leafNode ? 1 : 0);
		if (leafNode) {
			dest.writeParcelable(leftValue, 0);
			dest.writeParcelable(rightValue, 0);
			dest.writeString(comparator);
			dest.writeString(compareStrategy);
		} else {
			dest.writeParcelable(leftExpression, 0);
			dest.writeParcelable(rightExpression, 0);
			dest.writeString(operator);
		}
	}

	/**
	 * Read from parcel.
	 * 
	 * @param in
	 *            the in
	 */
	public void readFromParcel(Parcel in) {
		leafNode = (in.readInt() == 1);
		if (leafNode) {
			leftValue = in.readParcelable(this.getClass().getClassLoader());
			rightValue = in.readParcelable(this.getClass().getClassLoader());
			comparator = in.readString();
			compareStrategy = in.readString();
		} else {
			leftExpression = in
					.readParcelable(this.getClass().getClassLoader());
			rightExpression = in.readParcelable(this.getClass()
					.getClassLoader());
			operator = in.readString();
		}
	}

	/** The CREATOR. */
	public static Expression.Creator<Expression> CREATOR = new Expression.Creator<Expression>() {

		@Override
		public Expression createFromParcel(Parcel source) {
			return new Expression(source);
		}

		@Override
		public Expression[] newArray(int size) {
			return new Expression[size];
		}
	};

	public void setNextEvaluationTime(long evaluateAt) {
		deferUntil = evaluateAt;
	}

	public long getNextEvaluationTime() {
		return deferUntil;
	}

	@Override
	public int compareTo(Expression another) {
		long difference = getNextEvaluationTime()
				- another.getNextEvaluationTime();
		if (difference == 0) {
			return 0;
		} else if (difference < 0) {
			return -1;
		} else {
			return 1;
		}
	}

	public String toString() {
		if (leafNode) {
			return leftValue + " " + comparator + " " + rightValue;
		} else {
			return leftExpression + " " + operator + " " + rightExpression;
		}
	}

	public TypedValue getTypedValue(boolean left) {
		if (left) {
			return leftValue;
		} else {
			return rightValue;
		}
	}

	public String getComparator() {
		return comparator;
	}

}
