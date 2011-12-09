package interdroid.contextdroid.ui;

import interdroid.contextdroid.R;
import interdroid.contextdroid.contextexpressions.ConstantTypedValue;
import interdroid.contextdroid.contextexpressions.TypedValueExpression;
import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;

public class EnterConstantDialog extends Activity {

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setResult(RESULT_CANCELED);

		setContentView(R.layout.expression_builder_enter_constant_dialog);

		findViewById(R.id.ok).setOnClickListener(new View.OnClickListener() {

			@Override
			public void onClick(View v) {
				Intent result = new Intent();
				result.putExtra("Expression", new TypedValueExpression(
						new ConstantTypedValue(
								((EditText) findViewById(R.id.constant))
										.getText().toString())).toParseString());
				setResult(RESULT_OK, result);
				finish();
			}
		});

	}
}
