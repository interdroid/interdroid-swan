package interdroid.contextdroid.contextexpressions;

import interdroid.contextdroid.ContextDroidException;
import interdroid.contextdroid.ContextManager;
import interdroid.contextdroid.contextservice.SensorConfigurationException;
import interdroid.contextdroid.contextservice.SensorManager;
import interdroid.contextdroid.contextservice.SensorSetupFailedException;

import java.io.Serializable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import android.os.Parcel;
import android.os.Parcelable;

/**
 * A class which represents an Expression in ContextDroid.
 * 
 * @author roelof &lt;rkemp@cs.vu.nl&gt;
 * @author nick &lt;palmer@cs.vu.nl&gt;
 * 
 */
public abstract class Expression implements Parseable<Expression>, Parcelable,
		Serializable, Comparable<Expression> {
	/**
	 * Access to logger.
	 */
	private static final Logger LOG = LoggerFactory.getLogger(Expression.class);

	/**
	 *
	 */
	private static final long serialVersionUID = 1056115630721395151L;

	/**
	 * Subtype ID for a Value Expression.
	 */
	protected static final int VALUE_EXPRESSION_TYPE = 0;

	/**
	 * Subtype ID for a Logic Expression.
	 */
	protected static final int LOGIC_EXPRESSION_TYPE = 1;

	/**
	 * Time until we need to reevaluate this expression.
	 */
	private long mDeferUntil = -1;

	/**
	 * The result of the expression.
	 */
	private int mResult = ContextManager.UNDEFINED;

	/**
	 * The id for this expression.
	 */
	private String mId;

	/**
	 * Construct an expression.
	 */
	protected Expression() {
		// No argument constructor
	}

	/**
	 * Constructs an expression from a parcel.
	 * 
	 * @param in
	 *            the parcel to read from.
	 */
	protected Expression(final Parcel in) {
		readFromParcel(in);
	}

	/**
	 * The CREATOR used to construct from a parcel.
	 */
	public static final Parcelable.Creator<Expression> CREATOR = new Parcelable.Creator<Expression>() {
		@Override
		public Expression createFromParcel(final Parcel in) {
			Expression result;
			// Ugh. I kind of hate this since we have to know
			// all subclass types and include a flag for it
			// but using subclasses is much cleaner code separation
			// and makes it easier to see what we are constructing.
			int type = in.readInt();
			switch (type) {
			case VALUE_EXPRESSION_TYPE:
				result = new ValueExpression(in);
				break;
			case LOGIC_EXPRESSION_TYPE:
				result = new LogicExpression(in);
				break;
			default:
				throw new RuntimeException("Unknown subtype: " + type);
			}
			return result;
		}

		@Override
		public Expression[] newArray(final int size) {
			return new Expression[size];
		}
	};

	/**
	 * @return the ID for this expression.
	 */
	public final String getId() {
		return mId;
	}

	/**
	 * Sets the ID for this expression.
	 * 
	 * @param id
	 *            the id to set to
	 */
	public final void setId(final String id) {
		this.mId = id;
	}

	/**
	 * @return the time at which this expression should be re-evaluated.
	 */
	public final long getDeferUntil() {
		return mDeferUntil;
	}

	/**
	 * Sets the time at which this expression should be re-evaluated.
	 * 
	 * @param newDefer
	 *            the time to re-evaluate at.
	 */
	public void setDeferUntil(final long newDefer) {
		mDeferUntil = newDefer;
	}

	/**
	 * @return true if the result of the expression changed
	 * @throws ContextDroidException
	 *             if something goes wrong.
	 */
	public final boolean evaluate() throws ContextDroidException {
		int previousResult = mResult;
		evaluate(System.currentTimeMillis());
		return previousResult != mResult;
	}

	/**
	 * Evaluates this expression given the requested time.
	 * 
	 * @param now
	 *            the epoch time to evaluate the expression at in milliseconds
	 * @throws ContextDroidException
	 *             if something goes wrong.
	 */
	protected final void evaluate(final long now) throws ContextDroidException {
		if (now < mDeferUntil) {
			LOG.debug("deffered until: {} not evaluating", mDeferUntil);
			return;
		}
		try {
			evaluateImpl(now);
		} catch (NoValuesInIntervalException e) {
			setResult(ContextManager.UNDEFINED);
		}
		setDeferUntil(getDeferUntilImpl());
	}

	/**
	 * Sets the result of the last evaluation.
	 * 
	 * @param newResult
	 *            the result to set to.
	 */
	protected final void setResult(final int newResult) {
		mResult = newResult;
	}

	/**
	 * @return the result of the last evaluation.
	 */
	public final int getResult() {
		return mResult;
	}

	@Override
	public final int describeContents() {
		return 0;
	}

	@Override
	public final void writeToParcel(final Parcel dest, final int flags) {
		dest.writeInt(getSubtypeId());
		dest.writeString(mId);
		dest.writeInt(mResult);
		dest.writeLong(mDeferUntil);
		writeToParcelImpl(dest, flags);
	}

	/**
	 * @return a unique id for the subtype
	 */
	protected abstract int getSubtypeId();

	/**
	 * Read from parcel.
	 * 
	 * @param in
	 *            the in
	 */
	private void readFromParcel(final Parcel in) {
		mId = in.readString();
		mResult = in.readInt();
		mDeferUntil = in.readLong();
	}

	/**
	 * @return the next time this expression should be evaluated
	 */
	public final long getNextEvaluationTime() {
		return mDeferUntil;
	}

	@Override
	public final int compareTo(final Expression another) {
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

	@Override
	public final String toString() {
		if (mId != null) {
			return mId + toStringImpl();
		}
		return toStringImpl();
	}

	/**
	 * @return A parseable version of the expression.
	 */
	@Override
	public final String toParseString() {
		String result = "(" + this.toParseStringImpl() + ")";
		return result;
	}

	/* =-=-=-=- Subclass methods -=-=-=-= */

	/**
	 * Initializes this expression tree with the sensor manager.
	 * 
	 * @param id
	 *            the id of the expression
	 * @param sensorManager
	 *            the sensor manager to initialize with
	 * @throws SensorConfigurationException
	 *             if the sensor does not accept the configuration for the
	 *             expression.
	 * @throws SensorSetupFailedException
	 *             if initializing fails.
	 */
	public abstract void initialize(final String id,
			final SensorManager sensorManager)
			throws SensorConfigurationException, SensorSetupFailedException;

	/**
	 * Destroys this expression with the sensor manager.
	 * 
	 * @param id
	 *            the id for this expression.
	 * @param sensorManager
	 *            the sensor manager to destroy with
	 * @throws ContextDroidException
	 *             if something goes wrong.
	 */
	public abstract void destroy(final String id,
			final SensorManager sensorManager) throws ContextDroidException;

	/**
	 * Subclass toString implementation.
	 * 
	 * @return the subclass portion of the string
	 */
	protected abstract String toStringImpl();

	/**
	 * Subclass toParseString implementation.
	 * 
	 * @return the subclass portion of the parse string
	 */
	protected abstract String toParseStringImpl();

	/**
	 * The subclass implementation of evalute.
	 * 
	 * @param now
	 *            the timestamp for the evaluation
	 * @throws ContextDroidException
	 *             if something goes wrong.
	 */
	protected abstract void evaluateImpl(final long now)
			throws ContextDroidException;

	/**
	 * Subclass implementation of writeToParcel.
	 * 
	 * @param dest
	 *            the parcel to write to
	 * @param flags
	 *            the flags for parceling
	 */
	protected abstract void writeToParcelImpl(Parcel dest, int flags);

	/**
	 * @return the time until this expression should be defered.
	 */
	protected abstract long getDeferUntilImpl();

	/**
	 * Parses a string into an expression.
	 * 
	 * @param expression
	 *            the string to parse
	 * @return An expression version of the string
	 * @throws ExpressionParseException
	 *             if the expression has a problem
	 */
	public static final Expression parse(final String expression)
			throws ExpressionParseException {
		return ContextExpressionParser.parseExpression(expression);
	}

	public abstract void sleepAndBeReadyAt(long deferUntil);

	public abstract long getTimespan();

}
