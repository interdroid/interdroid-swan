package interdroid.swan.util;

import android.content.Context;
import android.preference.DialogPreference;
import android.util.AttributeSet;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.TimePicker;

public class TimePreference extends DialogPreference implements
		TimePicker.OnTimeChangedListener {
	private static final String androidns = "http://schemas.android.com/apk/res/android";

	private TimePicker mTimePicker;
	private TextView mSplashText;
	private Context mContext;

	private String mDialogMessage;

	public TimePreference(Context context, AttributeSet attrs) {
		super(context, attrs);
		mContext = context;

		mDialogMessage = attrs.getAttributeValue(androidns, "dialogMessage");
	}

	@Override
	protected View onCreateDialogView() {
		LinearLayout layout = new LinearLayout(mContext);
		layout.setOrientation(LinearLayout.VERTICAL);
		layout.setPadding(6, 6, 6, 6);

		mSplashText = new TextView(mContext);
		if (mDialogMessage != null)
			mSplashText.setText(mDialogMessage);
		layout.addView(mSplashText);

		mTimePicker = new TimePicker(mContext);
		mTimePicker.setIs24HourView(true);
		mTimePicker.setOnTimeChangedListener(this);
		layout.addView(mTimePicker, new LinearLayout.LayoutParams(
				LinearLayout.LayoutParams.FILL_PARENT,
				LinearLayout.LayoutParams.WRAP_CONTENT));

		return layout;
	}

	@Override
	protected void onBindDialogView(View v) {
		super.onBindDialogView(v);
	}

	@Override
	protected void onSetInitialValue(boolean restore, Object defaultValue) {
		super.onSetInitialValue(restore, defaultValue);
	}

	@Override
	public void onTimeChanged(TimePicker view, int hourOfDay, int minute) {
		String timeString = view.getCurrentHour() + ":"
				+ view.getCurrentMinute();
		if (shouldPersist())
			persistString(timeString);
		callChangeListener(timeString);
	}

}
