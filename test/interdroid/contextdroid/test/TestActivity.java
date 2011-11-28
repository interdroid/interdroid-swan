package interdroid.contextdroid.test;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import interdroid.contextdroid.ContextDroidException;
import interdroid.contextdroid.ContextManager;
import interdroid.contextdroid.R;
import interdroid.contextdroid.contextexpressions.ContextTypedValue;
import android.app.Activity;
import android.os.Bundle;
import android.view.View;

public class TestActivity extends Activity {
	/**
	 * Access to logger.
	 */
	private static final Logger LOG = LoggerFactory
			.getLogger(TestActivity.class);

	private ContextManager contextManager;

	@Override
	protected void onResume() {
		contextManager.start();
		super.onResume();
	}

	@Override
	protected void onPause() {
		LOG.debug("unbind context service from app: " + getClass());
		contextManager.stop();
		super.onPause();
	}

	@Override
	protected void onDestroy() {
		try {
			contextManager.destroy();
		} catch (ContextDroidException e) {
			e.printStackTrace();
		}
		super.onDestroy();
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.test);

		final ContextTypedValue left = new ContextTypedValue(
				"smart_location:vicinity?latitude=52.152962&longitude=5.367988&provider=gps");
		// "cuckootrain/departure_time?from_station=Amsterdam+Zuid&to_station=Amersfoort&departure_time=17:28");
		final String valueName = "custom_value";
		contextManager = new ContextManager(TestActivity.this);

		findViewById(R.id.register).setOnClickListener(
				new View.OnClickListener() {

					@Override
					public void onClick(View v) {
						LOG.debug("registering expression");
						try {
							contextManager.registerContextTypedValue(valueName,
									left, null);
						} catch (ContextDroidException e) {
							e.printStackTrace();
						}
					}
				});

		findViewById(R.id.unregister).setOnClickListener(
				new View.OnClickListener() {

					@Override
					public void onClick(View v) {
						LOG.debug("unregistering expression");
						try {
							contextManager
									.unregisterContextTypedValue(valueName);
						} catch (ContextDroidException e) {
							e.printStackTrace();
						}
					}
				});
		findViewById(R.id.shutdown).setOnClickListener(
				new View.OnClickListener() {

					@Override
					public void onClick(View v) {
						contextManager.shutdown();
					}
				});

	}

	// public static void main(String[] args) throws ContextDroidException {
	//
	// // ContextManager cm = new ContextManager(null /* context */);
	// // cm.start();
	// //
	// // // several expressions and how you write them down
	// //
	// // // simple: MAX(sound.level, 1000) > 5.0
	// // TypedValue left = new ContextTypedValue("sound/level",
	// // HistoryReductionMode.MAX, 1000);
	// // TypedValue right = new ConstantTypedValue(5.0);
	// // Expression simpleExpression = new Expression(left, ">", right);
	// //
	// // cm.registerContextExpression("simple", simpleExpression,
	// // expressionListener);
	// // cm.unregisterContextExpression("simple");
	// //
	// // // multiple expressions: (MAX(sound.level, 1000) > 5.0) &&
	// // // (time.dayofweek == "sunday")
	// // TypedValue soundLeft = new ContextTypedValue("sound.level",
	// // HistoryReductionMode.MAX, 1000);
	// // TypedValue soundRight = new ConstantTypedValue(5.0);
	// // Expression soundExpression = new Expression(soundLeft, ">",
	// // soundRight);
	// //
	// // TypedValue timeLeft = new ContextTypedValue("time.dayofweek");
	// // TypedValue timeRight = new ConstantTypedValue("sunday");
	// // Expression timeExpression = new Expression(timeLeft, "==",
	// // timeRight);
	// //
	// // Expression totalExpression = new Expression(soundExpression, "&&",
	// // timeExpression);
	// //
	// // // complex expressions: (MIN(sound.level, 1000) + ((MAX(sound.level,
	// // // 1000) - MIN(sound.level, 1000)) / 2)) > (MEAN(sound.level, 1000)
	// // TypedValue minSound = new ContextTypedValue("sound.level",
	// // HistoryReductionMode.MIN, 1000);
	// // TypedValue maxSound = new ContextTypedValue("sound.level",
	// // HistoryReductionMode.MAX, 1000);
	// // TypedValue meanSound = new ContextTypedValue("sound.level",
	// // HistoryReductionMode.MEAN, 1000);
	// //
	// // Expression complexExpression = new Expression(
	// // new CombinedTypedValue(minSound, "+", new CombinedTypedValue(
	// // maxSound, "-", minSound)), ">", meanSound);
	// //
	// // // look whether there exists a situation where all the readings of
	// // the
	// // // soundlevel of the last half a second are greater than the mean of
	// // the
	// // // readings of the last whole second
	// //
	// // // something with the ALL, ANY modifiers: NONE(sound.level, 500)
	// // // >(ALL) MEAN(sound.level, 1000)
	// //
	// // TypedValue sound = new ContextTypedValue(
	// // "sound.level?timespan=500&history_mode=none",
	// // HistoryReductionMode.NONE, 500);
	// // // use the mean sound from previous example
	// // Expression allExpression = new Expression(sound,
	// // ExpressionComparators.ALL_GREATER_THAN, meanSound);
	// //
	// // Bundle configuration = new Bundle();
	// // configuration.putString("source", "Amsterdam+Centraal");
	// // configuration.putString("destination", "Utrecht+Centraal");
	// // configuration.putString("time", "21:30");
	// // // TypedValue actualDepartureTime = new ContextTypedValue("train",
	// // // configuration, HistoryReductionMode.NONE, 0);
	// // // TypedValue regularDepartureTime = new ConstantTypedValue(new
	// // // DateTime(
	// // // "21:30"));
	// // // TypedValue maxDelay = new ConstantTypedValue(15);
	// // //
	// // // Expression trainExpression = new Expression(new
	// // // CombinedTypedValue<T>(
	// // // actualDepartureTime, "-", regularDepartureTime), ">=", maxDelay);
	// //
	// // // Is my train going to be more than 15 minutes late.
	// // //
	// //
	// train/departure?source=Amsterdam+Central&destination=Utrecht+Centraal&departure_time=21:30
	// // // - 21:30 >= 15
	// //
	// // // Is my train delayed
	// // //
	// //
	// train/delay?source=Amsterdam+Central&destination=Utrecht+Centraal&time=21:30
	// // // >= 15
	// //
	// // // Alert me when I need to leave in order to arrive by 21:30
	// // //
	// //
	// train/departure?source=Amsterdam+Central&destination=Utrecht+Centraal&arrival_time=21:30
	// // // - 5 > currentTime?
	// //
	// // // WIFI Problem?
	// //
	// // // Have I seen the same network for 5 minutes
	// // // wifi/?SSID=MyNetwork == true && wifi/?SSID=AnotherNetwork == true
	// //
	// // // Do I see a single bluetooth device with name=x and address=y
	// // // bluetooth/name == x && bluetooth/address = y
	// //
	// // // bluetooth/name?address=y == x
	// // // bluetooth/address?name=x == y
	//
	// }
}
