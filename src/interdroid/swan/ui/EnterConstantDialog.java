package interdroid.swan.ui;

import interdroid.swan.R;
import interdroid.swan.swansong.ConstantValueExpression;
import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.RadioGroup;

public class EnterConstantDialog extends Activity {

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setResult(RESULT_CANCELED);

		setContentView(R.layout.expression_builder_enter_constant_dialog);

		findViewById(R.id.ok).setOnClickListener(new View.OnClickListener() {

			@Override
			public void onClick(View v) {
				Object constant;
				String constantString = ((EditText) findViewById(R.id.constant))
						.getText().toString();

				int typeId = ((RadioGroup) findViewById(R.id.type))
						.getCheckedRadioButtonId();

				switch (typeId) {
				case R.id.double_type:
					constant = Double.parseDouble(constantString);
					break;
				case R.id.integer_type:
					constant = Integer.parseInt(constantString);
					break;
				case R.id.long_type:
					constant = Long.parseLong(constantString);
					break;
				case R.id.float_type:
					constant = Float.parseFloat(constantString);
					break;
				default:
					constant = constantString;
					break;
				}
				Intent result = new Intent();
				result.putExtra("Expression", new ConstantValueExpression(
						constant).toParseString());
				setResult(RESULT_OK, result);
				finish();
			}
		});

	}
}
