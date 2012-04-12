package interdroid.swan.ui;

import interdroid.swan.R;
import interdroid.swan.contextexpressions.Expression;
import interdroid.swan.contextexpressions.ExpressionParseException;
import interdroid.swan.contextexpressions.MathExpression;
import interdroid.swan.contextexpressions.MathOperator;
import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

public class NewMathExpressionDialog extends Activity {

	public static final int LEFT = 1;
	public static final int RIGHT = 2;
	public static final int OPERATOR = 3;

	boolean leftActive = false;
	boolean rightActive = false;
	boolean operatorActive = false;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setResult(RESULT_CANCELED);

		setContentView(R.layout.expression_builder_new_math_dialog);

		findViewById(R.id.left).setOnClickListener(new View.OnClickListener() {

			@Override
			public void onClick(View v) {
				startActivityForResult(new Intent(getApplicationContext(),
						SelectTypedValueDialog.class), LEFT);
			}
		});

		findViewById(R.id.right).setOnClickListener(new View.OnClickListener() {

			@Override
			public void onClick(View v) {
				startActivityForResult(new Intent(getApplicationContext(),
						SelectTypedValueDialog.class), RIGHT);
			}
		});

		findViewById(R.id.operator).setOnClickListener(
				new View.OnClickListener() {

					@Override
					public void onClick(View v) {
						startActivityForResult(new Intent(
								getApplicationContext(),
								SelectOperatorDialog.class), OPERATOR);
					}
				});

		findViewById(R.id.ok).setOnClickListener(new View.OnClickListener() {

			@Override
			public void onClick(View v) {
				// check whether we have a left and a right
				// value and an operator, create a new
				// comparator expression out of it, then add
				// it to the list of expressions.
				try {
					Expression left = Expression
							.parse(((Button) findViewById(R.id.left)).getText()
									.toString());
					Expression right = Expression
							.parse(((Button) findViewById(R.id.right))
									.getText().toString());
					MathOperator operator = MathOperator
							.parse(((Button) findViewById(R.id.operator))
									.getText().toString());
					Expression newExpression = new MathExpression(left,
							operator, right);
					Intent result = new Intent();
					result.putExtra("Expression", newExpression.toParseString());
					// if (name != null && !name.equals("")) {
					// result.putExtra("name", name);
					// }
					setResult(RESULT_OK, result);
					finish();
				} catch (ExpressionParseException e) {
					// TODO: improve this
					Toast.makeText(getApplicationContext(), "Failed!",
							Toast.LENGTH_LONG).show();
				}

			}
		});

	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		if (resultCode == RESULT_OK) {
			switch (requestCode) {
			case LEFT:
				((Button) findViewById(R.id.left)).setText(data
						.getStringExtra("Expression"));
				leftActive = true;
				break;
			case RIGHT:
				((Button) findViewById(R.id.right)).setText(data
						.getStringExtra("Expression"));
				rightActive = true;
				break;
			case OPERATOR:
				((Button) findViewById(R.id.operator)).setText(data
						.getStringExtra("Operator"));
				operatorActive = true;
				break;
			default:
				break;
			}
			findViewById(R.id.ok).setEnabled(
					leftActive && rightActive && operatorActive);
		}
	}

}
