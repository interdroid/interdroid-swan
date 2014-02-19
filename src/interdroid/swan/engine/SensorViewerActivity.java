package interdroid.swan.engine;

import interdroid.swan.R;

import java.text.Format;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import android.app.ListActivity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.Parcelable;
import android.support.v4.content.LocalBroadcastManager;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

public class SensorViewerActivity extends ListActivity {

	private List<Bundle> mSensors = new ArrayList<Bundle>();
	private SensorAdapter mAdapter = new SensorAdapter();
	private BroadcastReceiver mUpdateReceiver = new BroadcastReceiver() {

		@Override
		public void onReceive(Context context, Intent intent) {
			// get the data stuff it in the base adapter
			Parcelable[] sensors = intent.getParcelableArrayExtra("sensors");
			mSensors.clear();
			for (Parcelable sensor : sensors) {
				mSensors.add((Bundle) sensor);
			}
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
		runOnUiThread(new Runnable() {
			public void run() {
				Toast.makeText(SensorViewerActivity.this,
						"updating with service", Toast.LENGTH_SHORT).show();
			}
		});
		LocalBroadcastManager.getInstance(this).registerReceiver(
				mUpdateReceiver,
				new IntentFilter(EvaluationEngineService.UPDATE_SENSORS));
		// let the service know that we want to get updates...
		startService(new Intent(EvaluationEngineService.UPDATE_SENSORS)
				.setClass(this, EvaluationEngineService.class));
		super.onResume();
	}

	@Override
	protected void onPause() {
		LocalBroadcastManager.getInstance(this).unregisterReceiver(
				mUpdateReceiver);
		super.onPause();
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.sensorviewer, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case R.id.action_refresh:
			startService(new Intent(EvaluationEngineService.UPDATE_SENSORS)
					.setClass(this, EvaluationEngineService.class));
			break;
		case R.id.expression_viewer:
			this.finish();
			break;
		default:
			break;
		}
		return super.onOptionsItemSelected(item);
	}

	class SensorAdapter extends BaseAdapter {

		@Override
		public int getCount() {
			return mSensors.size();
		}

		@Override
		public Object getItem(int position) {
			return mSensors.get(position);
		}

		@Override
		public long getItemId(int position) {
			return 0;
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			// check als je hier komt of niet, met system.out.println
			if (convertView == null) {
				convertView = LayoutInflater.from(SensorViewerActivity.this)
						.inflate(R.layout.sensor_viewer, null);
			}
			//Sensor name
			((TextView) ((LinearLayout) convertView)
					.findViewById(R.id.sensorName)).setText(mSensors.get(
					position).getString("name"));
			
			//Number of Registered ID using this sensor
			((TextView) ((LinearLayout) convertView)
					.findViewById(R.id.registeredIds)).setText(" ("
					+ mSensors.get(position).getInt("registeredids") + ")");

			//Start time of sensor
		    Date date = new Date(mSensors.get(position).getLong("starttime"));
		    Format format = new SimpleDateFormat("HH:mm:ss dd/MM/yyyy");
			
			((TextView) ((LinearLayout) convertView)
					.findViewById(R.id.startTime)).setText("" + format.format(date).toString());
			
			//Sensing rate
			((TextView) ((LinearLayout) convertView)
					.findViewById(R.id.sensingRate)).setText(String.format(
					"%.2f", mSensors.get(position).getDouble("sensingRate"))
					+ " Hz");
			
			//current milli ampere
			((TextView) ((LinearLayout) convertView)
					.findViewById(R.id.currentMilliAmpere)).setText(mSensors.get(position).getFloat("currentMilliAmpere")
					+ " mA");
			
			//current Watt
			((TextView) ((LinearLayout) convertView)
					.findViewById(R.id.currentWatt)).setText((4 * mSensors.get(position).getFloat("currentMilliAmpere"))
					+ " mW");
			
			//percentage per hour mA
			int batteryMah = 1780;
			((TextView) ((LinearLayout) convertView)
					.findViewById(R.id.percentageHour)).setText(String.format("%.3f", 100 / (batteryMah / mSensors.get(position).getFloat("currentMilliAmpere")))
					+ " %/hr");
			

			return convertView;
		}
	}
}
