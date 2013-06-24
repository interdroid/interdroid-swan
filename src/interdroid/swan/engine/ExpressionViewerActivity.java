package interdroid.swan.engine;

import interdroid.swan.R;
import android.app.ListActivity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.v4.content.LocalBroadcastManager;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.TwoLineListItem;

public class ExpressionViewerActivity extends ListActivity {

	private String[] mExpressions = new String[] {};
	private ExpressionAdapter mAdapter = new ExpressionAdapter();
	private BroadcastReceiver mUpdateReceiver = new BroadcastReceiver() {

		@Override
		public void onReceive(Context context, Intent intent) {
			// get the data stuff it in the base adapter
			mExpressions = intent.getStringArrayExtra("expressions");
			mAdapter.notifyDataSetChanged();
		}
	};

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
			break;
		default:
			break;
		}
		return super.onOptionsItemSelected(item);
	}

	class ExpressionAdapter extends BaseAdapter {

		@Override
		public int getCount() {
			return mExpressions.length;
		}

		@Override
		public Object getItem(int position) {
			return mExpressions[position];
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
								android.R.layout.simple_list_item_2, null);
			}
			((TextView) ((TwoLineListItem) convertView)
					.findViewById(android.R.id.text1))
					.setText(mExpressions[position].split("\n")[0]);
			((TextView) ((TwoLineListItem) convertView)
					.findViewById(android.R.id.text2))
					.setText(mExpressions[position].split("\n", 2)[1]);
			return convertView;
		}

	}

}
