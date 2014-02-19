package interdroid.swan.ui;

import interdroid.swan.R;
import interdroid.swan.swansong.Expression;
import interdroid.swan.swansong.ExpressionFactory;
import interdroid.swan.swansong.ExpressionParseException;
import interdroid.swan.swansong.LogicExpression;
import interdroid.swan.swansong.TriStateExpression;
import interdroid.swan.swansong.UnaryLogicOperator;

import java.util.ArrayList;
import java.util.List;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

public class ExpressionBuilderActivity extends Activity {

	private final List<Expression> expressions = new ArrayList<Expression>();

	private final BaseAdapter expressionlistAdapter = new BaseAdapter() {

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			if (convertView == null) {
				convertView = new TextView(ExpressionBuilderActivity.this);
				((TextView) convertView).setTextSize(14.0f);
			}
			((TextView) convertView).setText(expressions.get(position)
					.toString());
			if (position == currentPosition) {
				convertView.setBackgroundColor(Color.RED);
			} else {
				convertView.setBackgroundColor(Color.TRANSPARENT);
			}
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

	private final static String TAG = "Expression Builder";

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setResult(RESULT_CANCELED);
		setContentView(R.layout.expression_builder);

		findViewById(R.id.new_expression).setOnClickListener(
				new View.OnClickListener() {

					@Override
					public void onClick(View v) {
						startActivityForResult(new Intent(
								getApplicationContext(),
								NewExpressionDialog.class), 0);
					}
				});

		findViewById(R.id.merge_expression).setOnClickListener(
				new View.OnClickListener() {

					@Override
					public void onClick(View v) {
						if (expressions.size() >= 2) {
							// we need at least two expression to merge
							ArrayList<String> expressionList = new ArrayList<String>();
							for (Expression expression : expressions) {
								expressionList.add(expression.toParseString());
							}
							Intent request = new Intent(
									getApplicationContext(),
									MergeExpressionDialog.class);
							request.putStringArrayListExtra("Expressions",
									expressionList);

							startActivityForResult(request, 0);
						} else {
							Toast.makeText(
									getApplicationContext(),
									"At least 2 expressions needed for merging. Create them with the new button",
									Toast.LENGTH_LONG).show();
						}
					}
				});

		findViewById(R.id.negate_expression).setOnClickListener(
				new View.OnClickListener() {

					@Override
					public void onClick(View v) {
						// showDialog(DIALOG_NEGATE_EXPRESSION);
					}
				});

		((ListView) findViewById(R.id.expression_list))
				.setAdapter(expressionlistAdapter);
		((ListView) findViewById(R.id.expression_list))
				.setOnItemClickListener(new OnItemClickListener() {

					@Override
					public void onItemClick(AdapterView<?> parent, View view,
							int position, long id) {
						currentPosition = position;
						findViewById(R.id.expression_finished).setEnabled(true);
						parent.invalidate();
					}
				});

		registerForContextMenu(findViewById(R.id.expression_list));

		findViewById(R.id.expression_finished).setOnClickListener(
				new View.OnClickListener() {

					@Override
					public void onClick(View v) {
						if (currentPosition >= 0
								&& currentPosition < expressions.size()) {
							String expression = expressions
									.get(currentPosition).toParseString();
							setResult(RESULT_OK, new Intent().putExtra(
									"Expression", expression));
							finish();
						} else {
							Toast.makeText(getApplicationContext(),
									"Please Select an Expression",
									Toast.LENGTH_SHORT).show();
						}

					}
				});

	}

	private int currentPosition = -1;

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		if (resultCode == RESULT_OK) {
			if (data.hasExtra("Expression")) {
				try {
					expressions.add(ExpressionFactory.parse(data
							.getStringExtra("Expression")));
					expressionlistAdapter.notifyDataSetChanged();
				} catch (ExpressionParseException e) {
					// should not happen, already checked for in new expression
					// dialog

					Log.e(TAG, "should not happen: " + e);
				}
			} else if (data.hasExtra("Expressions")) {
				expressions.clear();
				for (String expression : data
						.getStringArrayListExtra("Expressions")) {
					try {
						expressions.add(ExpressionFactory.parse(expression));
					} catch (ExpressionParseException e) {
						// should not happen
						e.printStackTrace();
					}
				}
				expressionlistAdapter.notifyDataSetChanged();
			}
		}
		super.onActivityResult(requestCode, resultCode, data);
	}

	@Override
	public void onCreateContextMenu(ContextMenu menu, View v,
			ContextMenuInfo menuInfo) {
		menu.add("Negate");
		menu.add("Delete");

		super.onCreateContextMenu(menu, v, menuInfo);
	}

	@Override
	public boolean onContextItemSelected(MenuItem item) {
		int position = ((AdapterContextMenuInfo) item.getMenuInfo()).position;
		if (item.getTitle().toString().equals("Negate")) {
			expressions.set(position, new LogicExpression(
					Expression.LOCATION_INFER, UnaryLogicOperator.NOT,
					(TriStateExpression) expressions.get(position)));
			expressionlistAdapter.notifyDataSetChanged();
		} else if (item.getTitle().toString().equals("Delete")) {
			expressions.remove(position);
			expressionlistAdapter.notifyDataSetChanged();
		}
		return super.onContextItemSelected(item);
	}
	/*** OLD STUFF ***/
}
