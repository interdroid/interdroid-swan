package interdroid.contextdroid.ui;

import interdroid.contextdroid.ContextManager;
import interdroid.contextdroid.R;
import interdroid.contextdroid.SensorServiceInfo;
import interdroid.contextdroid.contextexpressions.Comparator;
import interdroid.contextdroid.contextexpressions.ConstantTypedValue;
import interdroid.contextdroid.contextexpressions.ContextTypedValue;
import interdroid.contextdroid.contextexpressions.Expression;
import interdroid.contextdroid.contextexpressions.LogicExpression;
import interdroid.contextdroid.contextexpressions.LogicOperator;
import interdroid.contextdroid.contextexpressions.TypedValue;
import interdroid.contextdroid.contextexpressions.ComparisonExpression;
import interdroid.contextdroid.contextservice.SensorManager;

import java.util.ArrayList;
import java.util.List;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;

public class ExpressionBuilderActivity extends Activity {

	private static final int DIALOG_CHOOSE_TYPEDVALUE_LEFT = 0;
	private static final int DIALOG_CHOOSE_TYPEDVALUE_RIGHT = 1;
	private static final int DIALOG_CHOOSE_COMPARATOR = 2;
	private static final int DIALOG_ENTER_CONSTANT_LEFT = 3;
	private static final int DIALOG_ENTER_CONSTANT_RIGHT = 4;
	private static final int DIALOG_CHOOSE_EXPRESSION_LEFT = 5;
	private static final int DIALOG_CHOOSE_EXPRESSION_RIGHT = 6;
	private static final int DIALOG_CHOOSE_OPERATOR = 7;
	private static final int DIALOG_RENAME_EXPRESSION = 8;
	private static final int DIALOG_CHOOSE_SENSOR_LEFT = 9;
	private static final int DIALOG_CHOOSE_SENSOR_RIGHT = 10;

	private static final int REQUEST_CODE_CONFIGURE_SENSOR_LEFT = 0;
	private static final int REQUEST_CODE_CONFIGURE_SENSOR_RIGHT = 1;

	private static final CharSequence[] COMPARATORS = new CharSequence[] { "<",
			"<=", "==", "!=", ">", ">=", "contains", "regexp" };
	private static final CharSequence[] OPERATORS = new CharSequence[] { "&&",
			"||" };

	private static final int TYPEDVALUE_OPTION_CONSTANT = 0;
	private static final int TYPEDVALUE_OPTION_CONTEXT = 1;
	private static final int TYPEDVALUE_OPTION_COMBINED = 2;

	private static final CharSequence[] TYPEDVALUE_OPTIONS = new CharSequence[] {
			"constant", "context", "combined" };

	private boolean not = false;

	private TypedValue leftValue;
	private TypedValue rightValue;

	private Expression leftExpression;
	private Expression rightExpression;

	private int currentPosition;

	private final List<Expression> expressions = new ArrayList<Expression>();

	private final BaseAdapter expressionlistAdapter = new BaseAdapter() {

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			if (convertView == null) {
				convertView = new TextView(ExpressionBuilderActivity.this);
				((TextView) convertView).setTextSize(22.0f);
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

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.expression_builder);

		((CheckBox) findViewById(R.id.typedvalue_not))
				.setOnCheckedChangeListener(new OnCheckedChangeListener() {

					@Override
					public void onCheckedChanged(CompoundButton buttonView,
							boolean isChecked) {
						not = isChecked;
					}
				});

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
						String comparator = ((Button) findViewById(R.id.typedvalue_comparator))
								.getText().toString();
						Expression expression = new ComparisonExpression(leftValue,
								Comparator.parse(comparator), rightValue);
						if (not) {
							expressions.add(new LogicExpression(LogicOperator.NOT, expression));
						} else {
							expressions.add(expression);
						}
						// reset the buttons
						((Button) findViewById(R.id.typedvalue_right))
								.setText(" . ");
						((Button) findViewById(R.id.typedvalue_left))
								.setText(" . ");
						((Button) findViewById(R.id.typedvalue_comparator))
								.setText(" ? ");
						((CheckBox) findViewById(R.id.typedvalue_not))
								.setChecked(false);
						// reset all intermediate values
						leftValue = rightValue = null;
						checkTypedValueOkEnabled();
						checkExpressionEnabled();
						expressionlistAdapter.notifyDataSetChanged();
					}
				});
		((ListView) findViewById(R.id.expression_list))
				.setAdapter(expressionlistAdapter);
		((ListView) findViewById(R.id.expression_list))
				.setOnItemClickListener(new AdapterView.OnItemClickListener() {

					@Override
					public void onItemClick(AdapterView<?> parent, View view,
							int position, long id) {
						// Expression expression = expressions.get(position);
					}
				});
		registerForContextMenu(findViewById(R.id.expression_list));

		findViewById(R.id.expression_left).setOnClickListener(
				new View.OnClickListener() {

					@Override
					public void onClick(View v) {
						showDialog(DIALOG_CHOOSE_EXPRESSION_LEFT);
					}
				});

		findViewById(R.id.expression_right).setOnClickListener(
				new View.OnClickListener() {

					@Override
					public void onClick(View v) {
						showDialog(DIALOG_CHOOSE_EXPRESSION_RIGHT);
					}
				});

		findViewById(R.id.expression_operator).setOnClickListener(
				new View.OnClickListener() {

					@Override
					public void onClick(View v) {
						showDialog(DIALOG_CHOOSE_OPERATOR);
					}
				});
		findViewById(R.id.expression_ok).setOnClickListener(
				new View.OnClickListener() {

					@Override
					public void onClick(View v) {
						String operator = ((Button) findViewById(R.id.expression_operator))
								.getText().toString();
						expressions.add(new LogicExpression(leftExpression,
								LogicOperator.parse(operator), rightExpression));
						checkExpressionEnabled();
						expressionlistAdapter.notifyDataSetChanged();
					}
				});

	}

	private void checkTypedValueOkEnabled() {
		findViewById(R.id.typedvalue_ok)
				.setEnabled(
						(leftValue != null && rightValue != null && !" . "
								.equals(((Button) findViewById(R.id.typedvalue_comparator))
										.getText().toString())));
	}

	private void checkExpressionEnabled() {
		findViewById(R.id.expression_left).setEnabled(expressions.size() >= 2);
		findViewById(R.id.expression_right).setEnabled(expressions.size() >= 2);
		findViewById(R.id.expression_operator).setEnabled(
				expressions.size() >= 2);
	}

	private void checkExpressionOkEnabled() {
		findViewById(R.id.expression_ok)
				.setEnabled(
						(leftExpression != null && rightExpression != null && !" . "
								.equals(((Button) findViewById(R.id.expression_operator))
										.getText().toString())));
	}

	/*********************** DIALOG STUFF ***********************/

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
										if (isRightChoose) {
											showDialog(DIALOG_CHOOSE_SENSOR_RIGHT);
										} else {
											showDialog(DIALOG_CHOOSE_SENSOR_LEFT);
										}
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
									checkTypedValueOkEnabled();
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
										rightValue = new ConstantTypedValue(
												constant);
										((Button) findViewById(R.id.typedvalue_right))
												.setText(constant);
									} else {
										leftValue = new ConstantTypedValue(
												constant);
										((Button) findViewById(R.id.typedvalue_left))
												.setText(constant);
									}
									checkTypedValueOkEnabled();
								}
							}).create();
		case DIALOG_CHOOSE_OPERATOR:
			return new AlertDialog.Builder(this).setTitle("Choose Operator")
					.setItems(OPERATORS, new DialogInterface.OnClickListener() {

						@Override
						public void onClick(DialogInterface dialog, int which) {
							((Button) findViewById(R.id.expression_operator))
									.setText(OPERATORS[which]);
							checkExpressionOkEnabled();

						}
					}).create();
		case DIALOG_CHOOSE_EXPRESSION_LEFT:
			isLeft = true;
		case DIALOG_CHOOSE_EXPRESSION_RIGHT:
			final boolean isRightExpression = !isLeft;

			return new AlertDialog.Builder(this)
					.setTitle("Select Expression")
					.setItems(new CharSequence[] { "test" },
							new DialogInterface.OnClickListener() {

								@Override
								public void onClick(DialogInterface dialog,
										int which) {
									if (isRightExpression) {
										rightExpression = expressions
												.get(which);
										((Button) findViewById(R.id.expression_right))
												.setText(rightExpression
														.toString());
									} else {
										leftExpression = expressions.get(which);
										((Button) findViewById(R.id.expression_left))
												.setText(leftExpression
														.toString());
									}
									checkExpressionOkEnabled();
								}
							}).create();
		case DIALOG_RENAME_EXPRESSION:
			final View renameView = LayoutInflater.from(this).inflate(
					R.layout.expression_builder_rename_dialog, null);
			return new AlertDialog.Builder(this)
					.setTitle("Rename Expression")
					.setView(renameView)
					.setPositiveButton("OK",
							new DialogInterface.OnClickListener() {

								@Override
								public void onClick(DialogInterface dialog,
										int which) {
									String renamed = ((EditText) renameView
											.findViewById(R.id.value))
											.getText().toString();
									expressions.get(currentPosition).setId(
											renamed);
									expressionlistAdapter
											.notifyDataSetChanged();
								}
							}).create();
		case DIALOG_CHOOSE_SENSOR_LEFT:
			isLeft = true;
		case DIALOG_CHOOSE_SENSOR_RIGHT:
			final boolean isRightSensor = !isLeft;
			final List<SensorServiceInfo> sensors = ContextManager
					.getSensors(this);
			String[] sensorNames = new String[sensors.size()];
			for (int i = 0; i < sensorNames.length; i++) {
				sensorNames[i] = sensors.get(i).getEntity();
			}

			return new AlertDialog.Builder(this)
					.setTitle("Choose Sensor")
					.setItems(sensorNames,
							new DialogInterface.OnClickListener() {

								@Override
								public void onClick(DialogInterface dialog,
										int which) {
									startActivityForResult(
											sensors.get(which)
													.getConfigurationIntent()
													.putExtra(
															"entityId",
															sensors.get(which)
																	.getEntity()),
											isRightSensor ? REQUEST_CODE_CONFIGURE_SENSOR_RIGHT
													: REQUEST_CODE_CONFIGURE_SENSOR_LEFT);
								}
							}).create();

		default:
			break;
		}
		return super.onCreateDialog(id);
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		if (resultCode == RESULT_OK) {
			switch (requestCode) {
			case REQUEST_CODE_CONFIGURE_SENSOR_LEFT:
				leftValue = new ContextTypedValue(
						data.getStringExtra("entityId") +
						ContextTypedValue.ENTITY_VALUE_PATH_SEPARATOR
								+ data.getStringExtra("configuration"));
				((Button) findViewById(R.id.typedvalue_left)).setText(leftValue
						.toString());
				checkTypedValueOkEnabled();
				break;
			case REQUEST_CODE_CONFIGURE_SENSOR_RIGHT:
				rightValue = new ContextTypedValue(
						data.getStringExtra("entityId") +
						ContextTypedValue.ENTITY_VALUE_PATH_SEPARATOR
								+ data.getStringExtra("configuration"));
				((Button) findViewById(R.id.typedvalue_right))
						.setText(rightValue.toString());
				checkTypedValueOkEnabled();
				break;
			default:
				break;
			}
		}
		super.onActivityResult(requestCode, resultCode, data);
	}

	@Override
	protected void onPrepareDialog(int id, Dialog dialog) {
		switch (id) {
		case DIALOG_CHOOSE_EXPRESSION_RIGHT:
		case DIALOG_CHOOSE_EXPRESSION_LEFT:
			// Create new adapter
			ArrayAdapter<CharSequence> adapter = new ArrayAdapter<CharSequence>(
					this, android.R.layout.select_dialog_singlechoice);
			for (int i = 0; i < expressions.size(); i++) {
				adapter.add(expressions.get(i).toString());
			}

			// Use the new adapter
			AlertDialog alert = (AlertDialog) dialog;
			alert.getListView().setAdapter(adapter);

			break;
		case DIALOG_RENAME_EXPRESSION:
			((EditText) dialog.findViewById(R.id.value)).setText(expressions
					.get(currentPosition).toString());
			break;
		default:
			break;
		}

		// TODO Auto-generated method stub
		super.onPrepareDialog(id, dialog);
	}

	@Override
	public void onCreateContextMenu(ContextMenu menu, View v,
			ContextMenuInfo menuInfo) {
		menu.add("use");
		menu.add("delete");
		menu.add("rename");
		menu.add("edit");

		super.onCreateContextMenu(menu, v, menuInfo);
	}

	@Override
	public boolean onContextItemSelected(MenuItem item) {
		if (item.getTitle().toString().equals("rename")) {
			currentPosition = ((AdapterContextMenuInfo) item.getMenuInfo()).position;
			showDialog(DIALOG_RENAME_EXPRESSION);
		} else if (item.getTitle().toString().equals("delete")) {
			expressions
					.remove(((AdapterContextMenuInfo) item.getMenuInfo()).position);
			expressionlistAdapter.notifyDataSetChanged();
		}
		return super.onContextItemSelected(item);
	}

}
