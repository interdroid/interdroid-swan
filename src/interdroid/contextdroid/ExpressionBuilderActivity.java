package interdroid.contextdroid;

import interdroid.contextdroid.contextexpressions.ConstantTypedValue;
import interdroid.contextdroid.contextexpressions.Expression;
import interdroid.contextdroid.contextexpressions.TypedValue;

import java.util.ArrayList;
import java.util.List;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

public class ExpressionBuilderActivity extends Activity {

	private static final int DIALOG_CHOOSE_TYPEDVALUE_LEFT = 0;
	private static final int DIALOG_CHOOSE_TYPEDVALUE_RIGHT = 1;
	private static final int DIALOG_CHOOSE_COMPARATOR = 2;
	private static final int DIALOG_ENTER_CONSTANT_LEFT = 3;
	private static final int DIALOG_ENTER_CONSTANT_RIGHT = 4;

	private static final CharSequence[] COMPARATORS = new CharSequence[] { "<",
			"<=", "==", "!=", ">", ">=", "contains", "regexp" };

	private static final int TYPEDVALUE_OPTION_CONSTANT = 0;
	private static final int TYPEDVALUE_OPTION_CONTEXT = 1;
	private static final int TYPEDVALUE_OPTION_COMBINED = 2;

	private static final CharSequence[] TYPEDVALUE_OPTIONS = new CharSequence[] {
			"constant", "context", "combined" };

	private TypedValue left;
	private TypedValue right;

	private List<Expression> expressions = new ArrayList<Expression>();

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.expression_builder);

		final BaseAdapter expressionlistAdapter = new BaseAdapter() {

			@Override
			public View getView(int position, View convertView, ViewGroup parent) {
				if (convertView == null) {
					convertView = new TextView(ExpressionBuilderActivity.this);
				}
				((TextView) convertView).setText(expressions.get(position)
						.toString());
				return convertView;
			}

			@Override
			public long getItemId(int position) {
				return 0;
			}

			@Override
			public Object getItem(int position) {
				return expressions.get(position);
			}

			@Override
			public int getCount() {
				return expressions.size();
			}
		};

		findViewById(R.id.typedvalue_left).setOnClickListener(
				new View.OnClickListener() {

					@Override
					public void onClick(View v) {
						showDialog(DIALOG_CHOOSE_TYPEDVALUE_LEFT);
					}
				});

		findViewById(R.id.typedvalue_right).setOnClickListener(
				new View.OnClickListener() {

					@Override
					public void onClick(View v) {
						showDialog(DIALOG_CHOOSE_TYPEDVALUE_RIGHT);
					}
				});

		findViewById(R.id.typedvalue_comparator).setOnClickListener(
				new View.OnClickListener() {

					@Override
					public void onClick(View v) {
						showDialog(DIALOG_CHOOSE_COMPARATOR);
					}
				});

		findViewById(R.id.typedvalue_ok).setOnClickListener(
				new View.OnClickListener() {

					@Override
					public void onClick(View v) {
						if (left == null) {
							Toast.makeText(
									getApplicationContext(),
									"Unable to create expression, please provide a value for left member",
									Toast.LENGTH_SHORT).show();
							return;
						}
						if (right == null) {
							Toast.makeText(
									getApplicationContext(),
									"Unable to create expression, please provide a value for right member",
									Toast.LENGTH_SHORT).show();
							return;
						}
						String comparator = ((Button) findViewById(R.id.typedvalue_comparator))
								.getText().toString();
						if (" ? ".equals(comparator)) {
							Toast.makeText(
									getApplicationContext(),
									"Unable to create expression, please provide a comparator",
									Toast.LENGTH_SHORT).show();
							return;
						}
						expressions
								.add(new Expression(left, comparator, right));
						expressionlistAdapter.notifyDataSetChanged();
						System.out.println("#expressions now: "
								+ expressions.size());
					}
				});
		((ListView) findViewById(R.id.expression_list))
				.setAdapter(expressionlistAdapter);
		((ListView) findViewById(R.id.expression_list))
				.setOnItemClickListener(new AdapterView.OnItemClickListener() {

					@Override
					public void onItemClick(AdapterView<?> parent, View view,
							int position, long id) {
						Expression expression = expressions.remove(position);
						left = expression.getTypedValue(true);
						((Button) findViewById(R.id.typedvalue_left))
								.setText(left.toString());
						right = expression.getTypedValue(false);
						((Button) findViewById(R.id.typedvalue_right))
								.setText(right.toString());
						((Button) findViewById(R.id.typedvalue_comparator))
								.setText(expression.getComparator());

						expressionlistAdapter.notifyDataSetChanged();
					}
				});

	}

	@Override
	protected Dialog onCreateDialog(int id) {
		boolean isLeft = false;
		switch (id) {
		case DIALOG_CHOOSE_TYPEDVALUE_LEFT:
			isLeft = true;
		case DIALOG_CHOOSE_TYPEDVALUE_RIGHT:
			final boolean isRightChoose = !isLeft;
			return new AlertDialog.Builder(this)
					.setTitle("Choose Typed Value")
					.setItems(TYPEDVALUE_OPTIONS,
							new DialogInterface.OnClickListener() {

								@Override
								public void onClick(DialogInterface dialog,
										int which) {
									switch (which) {
									case TYPEDVALUE_OPTION_CONSTANT:
										if (isRightChoose) {
											showDialog(DIALOG_ENTER_CONSTANT_RIGHT);
										} else {
											showDialog(DIALOG_ENTER_CONSTANT_LEFT);
										}
										break;
									case TYPEDVALUE_OPTION_COMBINED:
										break;
									case TYPEDVALUE_OPTION_CONTEXT:
										break;

									default:
										break;
									}

								}
							}).create();

		case DIALOG_CHOOSE_COMPARATOR:
			return new AlertDialog.Builder(this)
					.setTitle("Choose Comparator")
					.setItems(COMPARATORS,
							new DialogInterface.OnClickListener() {

								@Override
								public void onClick(DialogInterface dialog,
										int which) {
									((Button) findViewById(R.id.typedvalue_comparator))
											.setText(COMPARATORS[which]);

								}
							}).create();
		case DIALOG_ENTER_CONSTANT_LEFT:
			isLeft = true;
		case DIALOG_ENTER_CONSTANT_RIGHT:
			final boolean isRightEnter = !isLeft;
			final View view = LayoutInflater.from(this).inflate(
					R.layout.expression_builder_constant_dialog, null);

			return new AlertDialog.Builder(this)
					.setTitle("Enter Constant")
					.setView(view)
					.setPositiveButton("OK",
							new DialogInterface.OnClickListener() {

								@Override
								public void onClick(DialogInterface dialog,
										int which) {
									String constant = ((EditText) view
											.findViewById(R.id.value))
											.getText().toString();
									if (isRightEnter) {
										right = new ConstantTypedValue(constant);
										((Button) findViewById(R.id.typedvalue_right))
												.setText(constant);
									} else {
										left = new ConstantTypedValue(constant);
										((Button) findViewById(R.id.typedvalue_left))
												.setText(constant);
									}
								}
							}).create();
		default:
			break;
		}
		return super.onCreateDialog(id);
	}
}
