package interdroid.swan.crossdevice;

import interdroid.swan.R;
import interdroid.swan.swansong.Expression;

import java.io.IOException;
import java.util.List;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ListActivity;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.content.res.TypedArray;
import android.nfc.NdefMessage;
import android.nfc.NdefRecord;
import android.nfc.NfcAdapter;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.util.SparseBooleanArray;
import android.view.ActionMode;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView.MultiChoiceModeListener;
import android.widget.BaseAdapter;
import android.widget.CheckedTextView;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.gcm.GoogleCloudMessaging;

public class SwanLakeActivity extends ListActivity {

	private static final String TAG = "SwanLakeActivity";

	private static final int DIALOG_SET_NAME = 1;

	private NfcAdapter mNfcAdapter;
	private RegisteredSWANsAdapter mAdapter;
	private EditText mNameEditText;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		final ListView listView = getListView();
		listView.setChoiceMode(ListView.CHOICE_MODE_MULTIPLE_MODAL);
		listView.setMultiChoiceModeListener(new MultiChoiceModeListener() {

			@Override
			public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
				return false;
			}

			@Override
			public void onDestroyActionMode(ActionMode mode) {
			}

			@Override
			public boolean onCreateActionMode(ActionMode mode, Menu menu) {
				MenuInflater inflater = mode.getMenuInflater();
				inflater.inflate(R.menu.swanlake_cab, menu);
				return true;
			}

			@Override
			public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
				switch (item.getItemId()) {
				case R.id.action_delete:
					SparseBooleanArray array = getListView()
							.getCheckedItemPositions();
					List<String> names = Registry
							.getNames(SwanLakeActivity.this);
					for (int i = 0; i < names.size(); i++) {
						if (array.get(i)) {
							Registry.remove(SwanLakeActivity.this, names.get(i));
						}
					}
					mAdapter.notifyDataSetChanged();
				}
				mode.finish();
				return true;
			}

			@Override
			public void onItemCheckedStateChanged(ActionMode mode,
					int position, long id, boolean checked) {
				mode.setTitle(getListView().getCheckedItemCount() + " selected");
				System.out.println("clicked on item");
			}
		});
		mNfcAdapter = NfcAdapter.getDefaultAdapter(this);
		mAdapter = new RegisteredSWANsAdapter();
		onNewIntent(getIntent());
		setListAdapter(mAdapter);

	}

	@Override
	protected Dialog onCreateDialog(int id) {
		mNameEditText = new EditText(this);
		mNameEditText.setPadding(10, 10, 10, 10);
		mNameEditText.setText(PreferenceManager.getDefaultSharedPreferences(
				SwanLakeActivity.this).getString("name",
				"SWAN-" + System.currentTimeMillis()));
		return new AlertDialog.Builder(this)
				.setTitle("Choose a name for your device")
				.setView(mNameEditText)
				.setPositiveButton(android.R.string.ok, new OnClickListener() {

					@Override
					public void onClick(DialogInterface dialog, int which) {
						if (mNameEditText.getText().toString().contains(":")) {
							runOnUiThread(new Runnable() {
								public void run() {
									Toast.makeText(
											SwanLakeActivity.this,
											"Character ':' is not allowed in the name, pick another name.",
											Toast.LENGTH_LONG).show();
								}
							});
							return;
						}
						PreferenceManager
								.getDefaultSharedPreferences(
										SwanLakeActivity.this)
								.edit()
								.putString("name",
										mNameEditText.getText().toString())
								.commit();
						updateNFC();
					}
				}).create();
	}

	@Override
	protected void onPause() {
		// mNfcAdapter.disableForegroundDispatch(this);
		mNfcAdapter.disableForegroundNdefPush(this);
		super.onPause();
	}

	@Override
	protected void onNewIntent(Intent intent) {
		// NDEF exchange mode
		if (NfcAdapter.ACTION_NDEF_DISCOVERED.equals(intent.getAction())) {
			// just get the data from the intent
			String authority = intent.getData().getAuthority();
			final String name = authority.split(":")[0];
			String regId = authority.split(":", 2)[1];
			if (!Registry.add(this, name, regId)) {
				// pop up duplicate dialog
				runOnUiThread(new Runnable() {

					@Override
					public void run() {
						Toast.makeText(SwanLakeActivity.this,
								"Duplicate '" + name + "', to be implemented",
								Toast.LENGTH_LONG).show();
					}
				});
			} else {
				mAdapter.notifyDataSetChanged();
			}
		}
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.swanlake, menu);
		((Switch) (menu.findItem(R.id.action_enable).getActionView()))
				.setOnCheckedChangeListener(new OnCheckedChangeListener() {

					@Override
					public void onCheckedChanged(CompoundButton buttonView,
							boolean isChecked) {
						if (isChecked
								&& Registry.get(SwanLakeActivity.this,
										Expression.LOCATION_SELF) == null) {
							// first time, we should register with gcm in the
							// background now.
							registerBackground((Switch) buttonView);
						} else {
							PreferenceManager
									.getDefaultSharedPreferences(
											SwanLakeActivity.this).edit()
									.putBoolean("enabled", isChecked).commit();
						}
					}
				});

		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case R.id.action_share:
			updateNFC();
			break;
		case R.id.action_set_name:
			showDialog(DIALOG_SET_NAME);
			break;
		default:
			break;
		}
		return super.onOptionsItemSelected(item);
	}

	private void updateNFC() {
		String regId = Registry.get(this, Expression.LOCATION_SELF);
		if (regId == null) {
			Log.d(TAG,
					"Not registered with Google Cloud Messaging, cannot share");
			runOnUiThread(new Runnable() {
				public void run() {
					Toast.makeText(
							SwanLakeActivity.this,
							"Not registered with Google Cloud Messaging, cannot share",
							Toast.LENGTH_LONG).show();
				}
			});
			return;
		}
		if (mNfcAdapter != null) {
			String userFriendlyName = PreferenceManager
					.getDefaultSharedPreferences(SwanLakeActivity.this)
					.getString("name", null);
			if (userFriendlyName == null) {
				runOnUiThread(new Runnable() {

					@Override
					public void run() {
						Toast.makeText(SwanLakeActivity.this,
								"Please set a name for your device",
								Toast.LENGTH_SHORT).show();
						showDialog(DIALOG_SET_NAME);
					}
				});
				return;
			}

			NdefRecord data = NdefRecord.createUri("swan://" + userFriendlyName
					+ ":" + regId);
			NdefMessage message = new NdefMessage(new NdefRecord[] { data });
			mNfcAdapter.enableForegroundNdefPush(this, message);
			runOnUiThread(new Runnable() {
				public void run() {
					Toast.makeText(SwanLakeActivity.this,
							"Ready for NFC sharing", Toast.LENGTH_LONG).show();
				}
			});
		}
	}

	@Override
	public boolean onPrepareOptionsMenu(Menu menu) {
		((Switch) (menu.findItem(R.id.action_enable).getActionView()))
				.setChecked(PreferenceManager.getDefaultSharedPreferences(this)
						.getBoolean("enabled", false));
		return super.onPrepareOptionsMenu(menu);
	}

	private void registerBackground(final Switch switchWidget) {
		switchWidget.setEnabled(false);
		new Thread() {
			public void run() {
				try {
					Registry.add(SwanLakeActivity.this,
							Expression.LOCATION_SELF, GoogleCloudMessaging
									.getInstance(SwanLakeActivity.this)
									.register(SwanGCMConstants.SENDER_ID));
					PreferenceManager
							.getDefaultSharedPreferences(SwanLakeActivity.this)
							.edit().putBoolean("enabled", true).commit();
					runOnUiThread(new Runnable() {

						@Override
						public void run() {
							Toast.makeText(
									SwanLakeActivity.this,
									"Got a registration ID: "
											+ Registry.get(
													SwanLakeActivity.this,
													Expression.LOCATION_SELF),
									Toast.LENGTH_LONG).show();
						}
					});
				} catch (IOException e) {
					Log.d(TAG,
							"Failed to register with Google Cloud Messaging", e);
					runOnUiThread(new Runnable() {

						@Override
						public void run() {
							switchWidget.setChecked(false);
							Toast.makeText(
									SwanLakeActivity.this,
									"Failed to register with Google Cloud Messaging",
									Toast.LENGTH_LONG).show();
						}
					});
				}
				runOnUiThread(new Runnable() {

					@Override
					public void run() {
						switchWidget.setEnabled(true);
					}
				});
			}
		}.start();
	}

	class RegisteredSWANsAdapter extends BaseAdapter {

		@Override
		public int getCount() {
			return Registry.getNames(SwanLakeActivity.this).size();
		}

		@Override
		public Object getItem(int position) {
			return Registry.getNames(SwanLakeActivity.this).get(position);
		}

		@Override
		public long getItemId(int position) {
			return 0;
		}

		@Override
		public View getView(final int position, View convertView,
				ViewGroup parent) {
			if (convertView == null) {
				convertView = LayoutInflater
						.from(SwanLakeActivity.this)
						.inflate(
								android.R.layout.simple_list_item_multiple_choice,
								null);
			}
			TypedArray ta = SwanLakeActivity.this
					.obtainStyledAttributes(new int[] { android.R.attr.activatedBackgroundIndicator });
			convertView.setBackgroundDrawable(ta.getDrawable(0));
			ta.recycle();

			((CheckedTextView) (convertView.findViewById(android.R.id.text1)))
					.setOnClickListener(new View.OnClickListener() {

						@Override
						public void onClick(View v) {
							getListView().setItemChecked(position,
									!((CheckedTextView) v).isChecked());
						}
					});
			((TextView) (convertView.findViewById(android.R.id.text1)))
					.setText(getItem(position).toString());
			((TextView) (convertView.findViewById(android.R.id.text1)))
					.setPadding(20, 20, 20, 20);
			return convertView;
		}
	}
}
