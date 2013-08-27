package interdroid.swan.ui;

import interdroid.swan.R;
import interdroid.swan.swansong.BinaryLogicOperator;
import interdroid.swan.swansong.Expression;
import interdroid.swan.swansong.ExpressionFactory;
import interdroid.swan.swansong.ExpressionParseException;
import interdroid.swan.swansong.LogicExpression;
import interdroid.swan.swansong.TriStateExpression;

import java.util.ArrayList;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.RadioGroup;
import android.widget.Toast;

public class MergeExpressionDialog extends Activity {

	public static final int LEFT = 1;
	public static final int RIGHT = 2;
	public static final int OPERATOR = 3;

	boolean leftActive = false;
	boolean rightActive = false;

	int leftIndex;
	int rightIndex;

	ArrayList<String> expressions;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setResult(RESULT_CANCELED);

		setContentView(R.layout.expression_builder_merge_dialog);

		expressions = getIntent().getStringArrayListExtra("Expressions");

		findViewById(R.id.left).setOnClickListener(new View.OnClickListener() {

			@Override
			public void onClick(View v) {
				startActivityForResult(
						getIntent().setClass(getApplicationContext(),
								SelectExpressionDialog.class), LEFT);
			}
		});

		findViewById(R.id.right).setOnClickListener(new View.OnClickListener() {

			@Override
			public void onClick(View v) {
				startActivityForResult(
						getIntent().setClass(getApplicationContext(),
								SelectExpressionDialog.class), RIGHT);
			}
		});

		findViewById(R.id.ok).setOnClickListener(new View.OnClickListener() {

			@Override
			public void onClick(View v) {
				try {
					Expression left = ExpressionFactory
							.parse(((Button) findViewById(R.id.left)).getText()
									.toString());
					Expression right = ExpressionFactory
							.parse(((Button) findViewById(R.id.right))
									.getText().toString());
					BinaryLogicOperator logicOperator = ((RadioGroup) findViewById(R.id.operator))
							.getCheckedRadioButtonId() == R.id.and ? BinaryLogicOperator.AND
							: BinaryLogicOperator.OR;
					Expression newExpression = new LogicExpression(
							Expression.LOCATION_INFER,
							(TriStateExpression) left, logicOperator,
							(TriStateExpression) right);
					expressions.add(newExpression.toParseString());
					if (!((CheckBox) findViewById(R.id.keep_originals))
							.isChecked()) {
						expressions.remove(Math.max(rightIndex, leftIndex));
						expressions.remove(Math.min(rightIndex, leftIndex));
					}
					Intent result = new Intent();
					result.putStringArrayListExtra("Expressions", expressions);
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
				leftIndex = data.getIntExtra("Position", -1);
				((Button) findViewById(R.id.left)).setText(expressions
						.get(leftIndex));
				leftActive = true;
				break;
			case RIGHT:
				rightIndex = data.getIntExtra("Position", -1);
				((Button) findViewById(R.id.right)).setText(expressions
						.get(rightIndex));
				rightActive = true;
				break;
			default:
				break;
			}
			findViewById(R.id.ok).setEnabled(leftActive && rightActive);
		}
	}

}
