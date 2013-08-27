package interdroid.swan.engine;

import interdroid.swan.R;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import android.app.ListActivity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.graphics.PorterDuff.Mode;
import android.os.Bundle;
import android.os.Parcelable;
import android.support.v4.content.LocalBroadcastManager;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

public class ExpressionViewerActivity extends ListActivity {
	private double mMaxEvalRate = Double.MIN_VALUE;
	private long mMaxAvgEvalDelay = Long.MIN_VALUE;
	private long mMaxAvgEvalTime = Long.MIN_VALUE;
	private long mMaxMinEvalTime = Long.MIN_VALUE;
	private long mMaxMaxEvalTime = Long.MIN_VALUE;
	private boolean mAscending = true;
	private String mSortType = "name";
	private int mSortVisible = R.id.evalRate;
	private int mSortVisibleOld = R.id.evalPercentage;
	private boolean mExpandAll = false;
	private boolean mExpandAllSelected = false;

	private Comparator<Bundle> mComparator = getComparator(mSortType,
			mAscending);
	private List<Bundle> mExpressions = new ArrayList<Bundle>();
	private ExpressionAdapter mAdapter = new ExpressionAdapter();
	private BroadcastReceiver mUpdateReceiver = new BroadcastReceiver() {

		@Override
		public void onReceive(Context context, Intent intent) {
			// get the data stuff it in the base adapter
			Parcelable[] expressions = intent
					.getParcelableArrayExtra("expressions");
			mExpressions.clear();
			for (Parcelable expression : expressions) {
				mExpressions.add((Bundle) expression);
			}
			mMaxEvalRate = Double.MIN_VALUE;
			mMaxAvgEvalDelay = Long.MIN_VALUE;
			mMaxAvgEvalTime = Long.MIN_VALUE;
			mMaxMinEvalTime = Long.MIN_VALUE;
			mMaxMaxEvalTime = Long.MIN_VALUE;

			// get maximum value variables of all expressions (used for Progress
			// Bar)
			for (Bundle expression : mExpressions) {
				mMaxEvalRate = Math.max(
						expression.getDouble("evaluation-rate"), mMaxEvalRate);
				mMaxAvgEvalDelay = Math.max(
						expression.getLong("avg-evaluation-delay"),
						mMaxAvgEvalDelay);
				mMaxAvgEvalTime = Math.max(
						expression.getLong("avg-evaluation-time"),
						mMaxAvgEvalTime);
				mMaxMinEvalTime = Math.max(
						expression.getLong("min-evaluation-time"),
						mMaxMinEvalTime);
				mMaxMaxEvalTime = Math.max(
						expression.getLong("max-evaluation-time"),
						mMaxMaxEvalTime);
			}

			Collections.sort(mExpressions, mComparator);
			mAdapter.notifyDataSetChanged();
		}
	};

	private static Comparator<Bundle> getComparator(final String key,
			final boolean ascending) {
		return new Comparator<Bundle>() {

			@SuppressWarnings({ "unchecked", "rawtypes" })
			@Override
			public int compare(Bundle lhs, Bundle rhs) {
				Bundle first = ascending ? lhs : rhs;
				Bundle last = ascending ? rhs : lhs;
				return ((Comparable) first.get(key))
						.compareTo((Comparable) last.get(key));
			}

		};
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setListAdapter(mAdapter);
	}

	@Override
	protected void onResume() {
		super.onResume();
		runOnUiThread(new Runnable() {
			public void run() {
				Toast.makeText(ExpressionViewerActivity.this,
						"updating with service", Toast.LENGTH_SHORT).show();
			}
		});
		LocalBroadcastManager.getInstance(this).registerReceiver(
				mUpdateReceiver,
				new IntentFilter(EvaluationEngineService.UPDATE_EXPRESSIONS));
		// let the service know that we want to get updates...
		startService(new Intent(EvaluationEngineService.UPDATE_EXPRESSIONS)
				.setClass(this, EvaluationEngineService.class));
	}

	@Override
	protected void onPause() {
		LocalBroadcastManager.getInstance(this).unregisterReceiver(
				mUpdateReceiver);
		super.onPause();
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.expressionviewer, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case R.id.action_refresh:
			startService(new Intent(EvaluationEngineService.UPDATE_EXPRESSIONS)
					.setClass(this, EvaluationEngineService.class));
			mExpandAllSelected = false;
			break;
		case R.id.menu_sort:
			return super.onOptionsItemSelected(item);
		case R.id.sort_name:
			mSortType = "name";
			mExpandAllSelected = false;
			break;
		case R.id.sort_eval_per:
			mSortType = "evaluation-percentage";
			mSortVisibleOld = mSortVisible;
			mSortVisible = R.id.evalPercentage;
			mExpandAllSelected = false;
			break;
		case R.id.sort_eval_rate:
			mSortType = "evaluation-rate";
			mSortVisibleOld = mSortVisible;
			mSortVisible = R.id.evalRate;
			mExpandAllSelected = false;
			break;
		case R.id.sort_eval_delay:
			mSortType = "avg-evaluation-delay";
			mSortVisibleOld = mSortVisible;
			mSortVisible = R.id.evalDelay;
			mExpandAllSelected = false;
			break;
		case R.id.sort_eval_avg_time:
			mSortType = "avg-evaluation-time";
			mSortVisibleOld = mSortVisible;
			mSortVisible = R.id.evalTimeAvg;
			mExpandAllSelected = false;
			break;
		case R.id.sort_eval_min_time:
			mSortType = "min-evaluation-time";
			mSortVisibleOld = mSortVisible;
			mSortVisible = R.id.evalTimeMin;
			mExpandAllSelected = false;
			break;
		case R.id.sort_eval_max_time:
			mSortType = "max-evaluation-time";
			mSortVisibleOld = mSortVisible;
			mSortVisible = R.id.evalTimeMax;
			mExpandAllSelected = false;
			break;
		case R.id.sort_order:
			if(mAscending == true){
				mAscending = false;
				item.setIcon(R.drawable.ic_action_descend);
			}else{
				mAscending = true;
				item.setIcon(R.drawable.ic_action_ascend);
			}
			mExpandAllSelected = false;
			break;
		case R.id.expand_all:	
			if(mExpandAll == true){
				mExpandAll = false;
				item.setIcon(R.drawable.expandoff);
			}else{
				mExpandAll = true;
				item.setIcon(R.drawable.expandon);
			}
			mExpandAllSelected = true;
			break;
		case R.id.sensor_activity:
			Intent intent = new Intent(ExpressionViewerActivity.this, SensorViewerActivity.class);
		    ExpressionViewerActivity.this.startActivity(intent);
		    mExpandAllSelected = false;
			break;
		default:
			break;
		}
		mComparator = getComparator(mSortType, mAscending);
		Collections.sort(mExpressions, mComparator);
		mAdapter.notifyDataSetChanged();
		return super.onOptionsItemSelected(item);
	}

	class ExpressionAdapter extends BaseAdapter {

		@Override
		public int getCount() {
			return mExpressions.size();
		}

		@Override
		public Object getItem(int position) {
			return mExpressions.get(position);
		}

		@Override
		public long getItemId(int position) {
			return 0;
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			if (convertView == null) {
				convertView = LayoutInflater
						.from(ExpressionViewerActivity.this).inflate(
								R.layout.expression_viewer, null);
			}
			Bundle expression = mExpressions.get(position);

			// Expression Name
			((TextView) ((LinearLayout) convertView)
					.findViewById(R.id.expressionName)).setText(expression
					.getString("name"));

			// Evaluation Percentage
			((TextView) ((LinearLayout) convertView)
					.findViewById(R.id.evalPercentageName)).setText("Time");
			((ProgressBar) ((LinearLayout) convertView)
					.findViewById(R.id.evalPercentagePB)).setProgress(Math
					.round(expression.getFloat("evaluation-percentage")));
			((TextView) ((LinearLayout) convertView)
					.findViewById(R.id.evalPercentageValue)).setText(String
					.format("%.2f",
							expression.getFloat("evaluation-percentage"))
					+ " %");

			// Evaluation Rate
			((TextView) ((LinearLayout) convertView)
					.findViewById(R.id.evalRateName)).setText("Frequency");

			((ProgressBar) ((LinearLayout) convertView)
					.findViewById(R.id.evalRatePB))
					.setMax((int) (mMaxEvalRate * 1.2));

			((ProgressBar) ((LinearLayout) convertView)
					.findViewById(R.id.evalRatePB))
					.setProgress((int) expression.getDouble("evaluation-rate"));

			((TextView) ((LinearLayout) convertView)
					.findViewById(R.id.evalRateValue)).setText(String.format(
					"%.2f", expression.getDouble("evaluation-rate")) + " Hz");

			// Evaluation Delay
			((TextView) ((LinearLayout) convertView)
					.findViewById(R.id.evalDelayName)).setText("Delay");

			((ProgressBar) ((LinearLayout) convertView)
					.findViewById(R.id.evalDelayPB))
					.setMax((int) (mMaxAvgEvalDelay * 1.2));

			((ProgressBar) ((LinearLayout) convertView)
					.findViewById(R.id.evalDelayPB)).setProgress(Math
					.round(expression.getLong("avg-evaluation-delay")));

			((TextView) ((LinearLayout) convertView)
					.findViewById(R.id.evalDelayValue)).setText(Long
					.toString(expression.getLong("avg-evaluation-delay"))
					+ " ms");

			// Min Evaluation Time
			((TextView) ((LinearLayout) convertView)
					.findViewById(R.id.evalTimeMinName)).setText("Min");

			((ProgressBar) ((LinearLayout) convertView)
					.findViewById(R.id.evalTimeMinPB))
					.setMax((int) (mMaxMinEvalTime * 1.2));

			((ProgressBar) ((LinearLayout) convertView)
					.findViewById(R.id.evalTimeMinPB)).setProgress(Math
					.round(expression.getLong("min-evaluation-time")));

			((TextView) ((LinearLayout) convertView)
					.findViewById(R.id.evalTimeMinValue)).setText(Long
					.toString(expression.getLong("min-evaluation-time"))
					+ " ms");

			// Max Evaluation Time
			((TextView) ((LinearLayout) convertView)
					.findViewById(R.id.evalTimeMaxName)).setText("Max");

			((ProgressBar) ((LinearLayout) convertView)
					.findViewById(R.id.evalTimeMaxPB))
					.setMax((int) (mMaxMaxEvalTime * 1.2));

			((ProgressBar) ((LinearLayout) convertView)
					.findViewById(R.id.evalTimeMaxPB)).setProgress(Math
					.round(expression.getLong("max-evaluation-time")));

			((TextView) ((LinearLayout) convertView)
					.findViewById(R.id.evalTimeMaxValue)).setText(Long
					.toString(expression.getLong("max-evaluation-time"))
					+ " ms");

			// Avg Evaluation Time
			((TextView) ((LinearLayout) convertView)
					.findViewById(R.id.evalTimeAvgName)).setText("Avg");

			((ProgressBar) ((LinearLayout) convertView)
					.findViewById(R.id.evalTimeAvgPB))
					.setMax((int) (mMaxAvgEvalTime * 1.2));

			((ProgressBar) ((LinearLayout) convertView)
					.findViewById(R.id.evalTimeAvgPB)).setProgress(Math
					.round(expression.getLong("avg-evaluation-time")));

			((TextView) ((LinearLayout) convertView)
					.findViewById(R.id.evalTimeAvgValue)).setText(Long
					.toString(expression.getLong("avg-evaluation-time"))
					+ " ms");

			CheckBox expandButton = (CheckBox) convertView
					.findViewById(R.id.expandButton);
			expandButton.setTag(position);

			expandButton
					.setOnCheckedChangeListener(new OnCheckedChangeListener() {

						@Override
						public void onCheckedChanged(CompoundButton buttonView,
								boolean isChecked) {
							LinearLayout parent = ((LinearLayout) buttonView
									.getParent().getParent());
							parent.findViewById(R.id.evalPercentage)
									.setVisibility(
											isChecked ? View.VISIBLE
													: View.GONE);
							parent.findViewById(R.id.evalDelay).setVisibility(
									isChecked ? View.VISIBLE : View.GONE);
							parent.findViewById(R.id.evalRate).setVisibility(
									isChecked ? View.VISIBLE : View.GONE);
							parent.findViewById(R.id.evalTimeMax)
									.setVisibility(
											isChecked ? View.VISIBLE
													: View.GONE);
							parent.findViewById(R.id.evalTimeMin)
									.setVisibility(
											isChecked ? View.VISIBLE
													: View.GONE);
							parent.findViewById(R.id.evalTimeAvg)
									.setVisibility(
											isChecked ? View.VISIBLE
													: View.GONE);
							parent.findViewById(mSortVisible).setVisibility(
									View.VISIBLE);

						}
					});
			
			//if checkbox is not checked but you change sort variable, remove old variable,
			//show new variable
			if (!expandButton.isChecked()) {
				((LinearLayout) convertView).findViewById(mSortVisibleOld)
						.setVisibility(View.GONE);
			}
			((LinearLayout) convertView).findViewById(mSortVisible)
					.setVisibility(View.VISIBLE);
			
			//unfold all values based on mExpandAll value
			if(mExpandAllSelected == true){
				expandButton.setChecked(mExpandAll);
			}
			
			//progress bar color
			ProgressBar mySeekBar;
			mySeekBar = ((ProgressBar) ((LinearLayout) ((LinearLayout) convertView)
					.findViewById(mSortVisibleOld)).getChildAt(1));

			mySeekBar.getProgressDrawable().setColorFilter(
					getResources().getColor(android.R.color.holo_blue_light),
					Mode.SRC_IN);

			mySeekBar = ((ProgressBar) ((LinearLayout) ((LinearLayout) convertView)
					.findViewById(mSortVisible)).getChildAt(1));

			mySeekBar.getProgressDrawable().setColorFilter(Color.RED,
					Mode.SRC_IN);

			return convertView;
		}

	}
}
