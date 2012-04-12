package interdroid.swan.ui;

import interdroid.swan.R;
import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.ListView;

public class SelectTypedValueDialog extends Activity {

	private static final String[] OPTIONS = new String[] { "Constant",
			"Sensor", "Combined" };

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setResult(RESULT_CANCELED);

		setContentView(R.layout.expression_builder_select_typed_value_dialog);

		((ListView) findViewById(R.id.typed_value_list))
				.setAdapter(new ArrayAdapter<String>(this,
						android.R.layout.simple_list_item_1, OPTIONS));

		((ListView) findViewById(R.id.typed_value_list))
				.setOnItemClickListener(new OnItemClickListener() {

					@Override
					public void onItemClick(AdapterView<?> parent, View view,
							int position, long id) {
						switch (position) {
						case 0: // constant
							startActivityForResult(new Intent(
									getApplicationContext(),
									EnterConstantDialog.class), 0);
							break;
						case 1: // sensor
							startActivityForResult(new Intent(
									getApplicationContext(),
									SelectSensorDialog.class), 0);
							break;
						case 2: // combined (math)
							startActivityForResult(new Intent(
									getApplicationContext(),
									NewMathExpressionDialog.class), 0);

							break;
						default:
							break;
						}
					}
				});
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		if (resultCode == RESULT_OK) {
			setResult(RESULT_OK, data);
			finish();
		}
	}

}
