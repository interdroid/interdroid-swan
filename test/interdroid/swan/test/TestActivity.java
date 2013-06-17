package interdroid.swan.test;

import interdroid.swan.ExpressionListener;
import interdroid.swan.ExpressionManager;
import interdroid.swan.R;
import interdroid.swan.SwanException;
import interdroid.swan.swansong.Expression;
import interdroid.swan.swansong.ExpressionFactory;
import interdroid.swan.swansong.ExpressionParseException;
import interdroid.swan.swansong.TimestampedValue;
import interdroid.swan.swansong.TriState;

import java.util.Arrays;
import java.util.Date;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

public class TestActivity extends Activity {

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.test);

		Expression parsedExpression;
		try {
			parsedExpression = ExpressionFactory
			// .parse("Roelof@movement:total{MAX,5000}>15.0");
					.parse("self@wifi:level?bssid='b8:f6:b1:12:9d:77'&discovery_interval=5000{ANY,0}");
		} catch (ExpressionParseException e1) {
			e1.printStackTrace(System.out);
			finish();
			parsedExpression = null;
		}
		final Expression expression = parsedExpression;
		findViewById(R.id.register).setOnClickListener(
				new View.OnClickListener() {

					@Override
					public void onClick(View v) {
						try {
							ExpressionManager.registerExpression(
									TestActivity.this, "bla", expression,
									new ExpressionListener() {

										@Override
										public void onNewState(final String id,
												final long timestamp,
												final TriState newState) {
											runOnUiThread(new Runnable() {
												public void run() {
													((TextView) findViewById(R.id.text))
															.setText(new Date(
																	timestamp)
																	+ ": "
																	+ newState);
													Toast.makeText(
															TestActivity.this,
															id + ": "
																	+ newState,
															Toast.LENGTH_LONG)
															.show();
												}
											});
											System.out.println(id + ": "
													+ newState);
										}

										@Override
										public void onNewValues(
												String id,
												final TimestampedValue[] newValues) {
											runOnUiThread(new Runnable() {
												public void run() {
													if (newValues == null
															|| newValues.length == 0) {
														((TextView) findViewById(R.id.text))
																.setText("n.a.");
													} else {
														((TextView) findViewById(R.id.text))
																.setText(new Date(
																		newValues[0]
																				.getTimestamp())
																		+ ": "
																		+ newValues[0]
																				.getValue());
													}
												}
											});

											System.out.println(id
													+ ": "
													+ Arrays.toString(newValues));
										}

									});
						} catch (SwanException e) {
							e.printStackTrace();
						}
					}
				});

		findViewById(R.id.unregister).setOnClickListener(
				new View.OnClickListener() {

					@Override
					public void onClick(View v) {
						ExpressionManager.unregisterExpression(
								TestActivity.this, "bla");
					}
				});

	}

}
