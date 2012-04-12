package interdroid.swan.ui;

import interdroid.swan.R;

import java.util.List;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.ListView;

public class SelectExpressionDialog extends Activity {

	List<String> expressions;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setResult(RESULT_CANCELED);

		expressions = getIntent().getStringArrayListExtra("Expressions");

		setContentView(R.layout.expression_builder_select_sensor_dialog);

		((ListView) findViewById(R.id.sensor_list))
				.setAdapter(new ArrayAdapter<String>(this,
						android.R.layout.simple_list_item_1, expressions));

		((ListView) findViewById(R.id.sensor_list))
				.setOnItemClickListener(new OnItemClickListener() {

					@Override
					public void onItemClick(AdapterView<?> parent, View view,
							int position, long id) {
						Intent result = new Intent();
						result.putExtra("Position", position);
						setResult(RESULT_OK, result);
						finish();
					}
				});
	}

}
