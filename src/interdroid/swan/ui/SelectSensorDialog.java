package interdroid.swan.ui;

import interdroid.swan.ExpressionManager;
import interdroid.swan.R;
import interdroid.swan.SensorInfo;

import java.util.List;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.ListView;

public class SelectSensorDialog extends Activity {

	List<SensorInfo> sensors;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setResult(RESULT_CANCELED);

		sensors = ExpressionManager.getSensors(this);

		setContentView(R.layout.expression_builder_select_sensor_dialog);

		((ListView) findViewById(R.id.sensor_list))
				.setAdapter(new ArrayAdapter<SensorInfo>(this,
						android.R.layout.simple_list_item_1, sensors));

		((ListView) findViewById(R.id.sensor_list))
				.setOnItemClickListener(new OnItemClickListener() {

					@Override
					public void onItemClick(AdapterView<?> parent, View view,
							int position, long id) {
						startActivityForResult(sensors.get(position)
								.getConfigurationIntent(), position);
					}
				});
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		if (resultCode == RESULT_OK) {
			Intent result = new Intent();
			result.putExtra("Expression", data.getStringExtra("Expression"));
			setResult(RESULT_OK, result);
			finish();
		}
	}
}
