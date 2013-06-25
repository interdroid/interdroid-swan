package interdroid.swan.test;

import interdroid.swan.ExpressionManager;
import interdroid.swan.R;
import interdroid.swan.SensorInfo;
import interdroid.swan.SwanException;
import interdroid.swan.ValueExpressionListener;
import interdroid.swan.swansong.Expression;
import interdroid.swan.swansong.ExpressionFactory;
import interdroid.swan.swansong.ExpressionParseException;
import interdroid.swan.swansong.TimestampedValue;
import interdroid.swan.swansong.ValueExpression;

import java.util.List;

import android.app.Activity;
import android.content.Intent;
import android.location.Location;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Toast;

public class TestActivity extends Activity {

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.test);
		findViewById(R.id.register).setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				List<SensorInfo> sensors = ExpressionManager
						.getSensors(TestActivity.this);
				for (SensorInfo sensor : sensors) {
					if (sensor.getEntity().equals("location")) {
						startActivityForResult(sensor.getConfigurationIntent(),
								1234);
						break;
					}
				}
			}
		});
		findViewById(R.id.unregister).setOnClickListener(
				new View.OnClickListener() {

					@Override
					public void onClick(View v) {
						ExpressionManager.unregisterExpression(
								TestActivity.this, "test");
					}
				});

	}

	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		if (requestCode == 1234) {
			if (resultCode == RESULT_OK) {
				try {
					Expression expression = ExpressionFactory.parse(data
							.getStringExtra("Expression"));
					ExpressionManager.registerValueExpression(
							TestActivity.this, "test",
							(ValueExpression) expression,
							new ValueExpressionListener() {

								@Override
								public void onNewValues(String id,
										TimestampedValue[] newValues) {
									if (newValues.length > 0) {
										System.out.println("got new values: "
												+ newValues[0]);
										if (newValues[0].getValue() instanceof Location) {
											System.out
													.println("It's a location object!");
											runOnUiThread(new Runnable() {
												public void run() {
													Toast.makeText(
															TestActivity.this,
															"LOCATION!",
															Toast.LENGTH_LONG)
															.show();
												}
											});
										}
									}
								}

							});
				} catch (ExpressionParseException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (SwanException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}

		}
	}
}
