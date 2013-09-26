package interdroid.swan.test;

import interdroid.swan.ExpressionListener;
import interdroid.swan.ExpressionManager;
import interdroid.swan.R;
import interdroid.swan.SwanException;
import interdroid.swan.ValueExpressionListener;
import interdroid.swan.swansong.Expression;
import interdroid.swan.swansong.ExpressionFactory;
import interdroid.swan.swansong.ExpressionParseException;
import interdroid.swan.swansong.TimestampedValue;
import interdroid.swan.swansong.TriState;
import interdroid.swan.swansong.ValueExpression;

import java.util.Date;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

public class TestActivity extends Activity {

	private void toast(final String text) {
		runOnUiThread(new Runnable() {
			@Override
			public void run() {
				Toast.makeText(TestActivity.this, text, Toast.LENGTH_LONG)
						.show();
			}
		});
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.test);
		
		Expression proximity;
		
		Expression accelerometer1 = null;
		Expression accelerometer2 = null;
		Expression accelerometer3 = null;
		Expression accelerometer4 = null;
		Expression accelerometer5 = null;
		Expression accelerometer6 = null;
		Expression accelerometer7 = null;
		Expression accelerometer8 = null;
		
		Expression avgAccelerometer = null;
		Expression magnetometer = null;
		
		try {
			// .parse("Roelof@movement:total{MAX,5000}>15.0");
			// .parse("self@wifi:level?bssid='b8:f6:b1:12:9d:77'&discovery_interval=5000{ANY,0}");
			// .parse("self@movement:total?accuracy=0{MAX,5000}>12.0");
			// .parse("self@movement:total?accuracy=3{ANY,0}");
			proximity = ExpressionFactory
					.parse("self@proximity:distance?accuracy=0{ANY,0}");
			
			accelerometer1 = ExpressionFactory
					.parse("self@movement:total?accuracy=0{ANY,0}");
			accelerometer2 = ExpressionFactory
					.parse("self@movement:total?accuracy=0{MAX,0}");
			accelerometer3 = ExpressionFactory
					.parse("self@movement:total?accuracy=0{MIN,0}");
			accelerometer4 = ExpressionFactory
					.parse("self@movement:total?accuracy=0{ALL,0}");
//			accelerometer5 = ExpressionFactory
//					.parse("self@movement:total?accuracy=0{ALL,0}");
//			accelerometer6 = ExpressionFactory
//					.parse("self@movement:total?accuracy=3{ANY,0}");
//			accelerometer7 = ExpressionFactory
//					.parse("self@movement:total?accuracy=2{ANY,0}");
//			accelerometer8 = ExpressionFactory
//					.parse("self@movement:total?accuracy=1{ANY,0}");
			
			
			avgAccelerometer = ExpressionFactory
					.parse("self@movement:total?accuracy=0{MEAN,20000}>12.0");
			
			magnetometer = ExpressionFactory
					.parse("self@magnetometer:total?accuracy=0{ANY,0}");
			

		} catch (ExpressionParseException e1) {
			e1.printStackTrace(System.out);
			finish();
			proximity = null;
		}
		final Expression proximityExpression = proximity;
		
		final Expression accelerometer1Expression = accelerometer1;
		final Expression accelerometer2Expression = accelerometer2;
		final Expression accelerometer3Expression = accelerometer3;
		final Expression accelerometer4Expression = accelerometer4;
		final Expression accelerometer5Expression = accelerometer5;
		final Expression accelerometer6Expression = accelerometer6;
		final Expression accelerometer7Expression = accelerometer7;
		final Expression accelerometer8Expression = accelerometer8;
		
		final Expression avgExpression = avgAccelerometer;
		final Expression magnetometerExpression = magnetometer;
		
		findViewById(R.id.register).setOnClickListener(
				new View.OnClickListener() {

					@Override
					public void onClick(View v) {
//						registerExpressionTest(proximityExpression,"proximty");
						
//						for(int i =0; i<1; i++){
//							String name = "accelerometer" + i;
//							registerExpressionTest(accelerometer1Expression,name);
//						}
//						registerExpressionTest(accelerometer2Expression,"accelerometer2");
//						registerExpressionTest(accelerometer3Expression,"accelerometer3");
//						registerExpressionTest(accelerometer4Expression,"accelerometer4");
//						registerExpressionTest(accelerometer5Expression,"accelerometer5");
//						registerExpressionTest(accelerometer6Expression,"accelerometer6");
//						registerExpressionTest(accelerometer7Expression,"accelerometer7");
//						registerExpressionTest(accelerometer8Expression,"accelerometer8");
						
						registerExpressionTest(avgExpression,"avgAccelerometer");
						
					}
				});

		findViewById(R.id.unregister).setOnClickListener(
				new View.OnClickListener() {

					@Override
					public void onClick(View v) {
//						ExpressionManager.unregisterExpression(
//						TestActivity.this, "proximity");
//						
//						for(int i =0; i<1; i++){
//							String name = "accelerometer" + i;
//							ExpressionManager.unregisterExpression(
//									TestActivity.this, name);
//						}
//						
//						ExpressionManager.unregisterExpression(
//								TestActivity.this, "accelerometer1");
//						ExpressionManager.unregisterExpression(
//								TestActivity.this, "accelerometer2");
//						ExpressionManager.unregisterExpression(
//								TestActivity.this, "accelerometer3");
//						ExpressionManager.unregisterExpression(
//								TestActivity.this, "accelerometer4");
//						ExpressionManager.unregisterExpression(
//								TestActivity.this, "accelerometer5");
//						ExpressionManager.unregisterExpression(
//								TestActivity.this, "accelerometer6");
//						ExpressionManager.unregisterExpression(
//								TestActivity.this, "accelerometer7");
//						ExpressionManager.unregisterExpression(
//								TestActivity.this, "accelerometer8");

						
						ExpressionManager.unregisterExpression(
								TestActivity.this, "avgAccelerometer");
//						ExpressionManager.unregisterExpression(
//								TestActivity.this, "magnetometer");
					}
				});

	}

	void registerExpressionTest(Expression expression, String name){
		try {
			ExpressionManager.registerExpression(
					TestActivity.this, name,
					expression, new ExpressionListener() {

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

							// System.out.println(id
							// + ": "
							// + Arrays.toString(newValues));
						}

					});
		} catch (SwanException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
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
									}
								}

							});
					toast("registered expression!");
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
