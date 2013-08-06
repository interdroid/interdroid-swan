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
		
		//BatteryUtil.readPowerProfile(this);


		Expression proximity;
		Expression accelerometer = null;
		Expression maxAccelerometer = null;
		
		try {
			proximity = ExpressionFactory
			// .parse("Roelof@movement:total{MAX,5000}>15.0");
//			 .parse("self@wifi:level?bssid='b8:f6:b1:12:9d:77'&discovery_interval=5000{ANY,0}");
			//.parse("self@movement:total?accuracy=0{MAX,5000}>12.0");
			//.parse("self@movement:total?accuracy=3{ANY,0}");
			 .parse("self@proximity:distance?accuracy=0{ANY,0}");
			
			accelerometer = ExpressionFactory
					.parse("self@movement:total?accuracy=0{ANY,0}");
			
			maxAccelerometer = ExpressionFactory
					.parse("self@movement:total?accuracy=0{MEAN,300000}>12.0");
			
		} catch (ExpressionParseException e1) {
			e1.printStackTrace(System.out);
			finish();
			proximity = null;
		}
		final Expression proximityExpression = proximity;
		final Expression accelerometerExpression = accelerometer;
		final Expression maxExpression = maxAccelerometer;
		
		findViewById(R.id.register).setOnClickListener(
				new View.OnClickListener() {



					@Override
					public void onClick(View v) {
						try {
							ExpressionManager.registerExpression(
									TestActivity.this, "proximity", proximityExpression,
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

											// System.out.println(id
											// + ": "
											// + Arrays.toString(newValues));
										}

									});
							
							ExpressionManager.registerExpression(
									TestActivity.this, "accelerometer", accelerometerExpression,
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

											// System.out.println(id
											// + ": "
											// + Arrays.toString(newValues));
										}

									});
							
							
							ExpressionManager.registerExpression(
									TestActivity.this, "accelerometerMax", maxExpression,
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

											// System.out.println(id
											// + ": "
											// + Arrays.toString(newValues));
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
						ExpressionManager.unregisterExpression(
								TestActivity.this, "ala");
						ExpressionManager.unregisterExpression(
								TestActivity.this, "cla");

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
