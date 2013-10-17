package sg.edu.dukenus.pononin;

//import com.lit.poc.bluepulse.R;

import java.io.IOException;
import java.lang.reflect.Method;
import java.math.BigInteger;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.security.InvalidKeyException;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.RSAPrivateKeySpec;
import java.security.spec.RSAPublicKeySpec;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import com.android.internal.telephony.ITelephony;

import android.app.Activity;
import android.app.PendingIntent;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.AudioManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.PowerManager;
import android.os.Vibrator;
import android.telephony.SmsManager;
import android.telephony.SmsMessage;
import android.telephony.TelephonyManager;
import android.text.Editable;
//import android.text.format.DateFormat;
import java.text.DateFormat;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;

import sg.edu.dukenus.securesms.crypto.MyKeyUtils;
import sg.edu.dukenus.securesms.sms.SmsReceiver;
import sg.edu.dukenus.securesms.sms.SmsSender;
import sg.edu.dukenus.securesms.utils.MyUtils;
import android.util.Base64;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.KeyEvent;
import android.view.MenuInflater;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.view.Window;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.TextView.OnEditorActionListener;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.res.Resources;
import android.preference.PreferenceManager;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;

public class MainActivity extends Activity implements Button.OnClickListener {
	// Debugging
	private static final String TAG = "MainActivity";
	private static final boolean D = true;

	// Run/Pause status
	private boolean bReady = false;

	// Screen density, dimension and scale
	private int screenDensity = 160;// can only of these values: 120, 160, 240,
									// 320 or 480, this is not actually density
									// but density buckets instead
	// Android somehow scale different densities to most appropriate buckets
	private double scale = 1;// screenDensity/160
	private int screenLongDp = 0;// width in pixel * scale
	private int screenLongPx = 0;

	// Message types sent from the BluetoothCommService Handler
	public static final int MESSAGE_STATE_CHANGE = 1;
	public static final int MESSAGE_READ = 2;
	public static final int MESSAGE_WRITE = 3;
	public static final int MESSAGE_DEVICE_NAME = 4;
	public static final int MESSAGE_TOAST = 5;

	// Key names received from the BluetoothCommService Handler
	public static final String DEVICE_NAME = "device_name";
	public static final String TOAST = "toast";

	// Intent request codes
	private static final int REQUEST_CONNECT_DEVICE = 1;
	private static final int REQUEST_ENABLE_BT = 2;

	// dimensions of the waveform chart, affecting the drawing
	private static int waveform_w = 550;
	private static int waveform_h = 350;
	// private static int DATA_START = (waveform_h + 1);
	// private static int DATA_END = (waveform_h + 2);

	/*
	 * private static final byte REQ_DATA = 0x00; private static final byte
	 * ADJ_HORIZONTAL = 0x01; private static final byte ADJ_VERTICAL = 0x02;
	 * private static final byte ADJ_POSITION = 0x03;
	 * 
	 * private static final byte CHANNEL1 = 0x01; private static final byte
	 * CHANNEL2 = 0x02;
	 */

	// Layout Views
	private TextView mBTStatus;
	// private TextView time_per_div;
	// private TextView ch1_scale, ch2_scale;
	// private TextView ch1pos_label, ch2pos_label;
	private TextView pulse_rate, pulse_sat;
	private TextView txtHost, txtPort;
	// private RadioButton rb1, rb2;
	// private Button timebase_inc, timebase_dec;
	// private Button btn_scale_up, btn_scale_down;
	// private Button btn_pos_up, btn_pos_down;
	private Button mConnectButton;
	private Button settingsBtn;
	// private ToggleButton run_buton;

	// Name of the connected device
	// private String mConnectedDeviceName = null;
	private String mConnectedDeviceName = "No Bluetooth Device Found!";
	// Local Bluetooth adapter
	private BluetoothAdapter mBluetoothAdapter = null;
	// Member object for the RFCOMM services
	private BluetoothCommService mRfcommClient = null;

	protected PowerManager.WakeLock mWakeLock;

	public WaveformView mWaveform = null;

	static String[] timebase = { "500ms", "600ms", "700ms", "800ms", "900ms",
			"1s", "1.1s", "1.2s", "1.4s", "1.4s", "1.5s", "1.6s", "1.7s" };
	static String[] ampscale = { "1", "5", "10", "15", "20", "25", "30", "35",
			"40" };
	static byte timebase_index = 5;
	static byte ch1_index = 6, ch2_index = 6;
	static byte ch1_pos = 0, ch2_pos = 0; // 0 to 60
	// What are wavefromArray and ch2_data?
	private int[] wavefromArray = null;
	// private int[] ch2_data = null;

	// private int dataIndex = 0, dataIndex1 = 0, dataIndex2 = 0;
	// private boolean bDataAvailable = false;

	// Tracking frames and packets received
	private int frameCount = 0;
	// What is frameInPacket?
	private int frameInPacket = 0;
	private int packetCount = 0;

	private CheckBox chkboxEnableUDP;
	private Button buttonTestUDP;
	// private ToggleButton buttonCallDoc;
	private ImageButton buttonActivateCall;
	private ImageButton buttonEndCall;
	private ImageButton buttonSms;
	private Button buttonStream2Doc;
	private String destination_host;
	private int destination_port;
	private boolean send_udp = false;
	private DatagramSocket datagramSocket;

	// Main measurements variables: heart-rate and oxygen saturation range
	private int HR = 0;
	private int SPO2 = 0;
	private int MIN_HR = 18;// according to NONIN documentation - minimum pulse
							// rate
	private int MAX_HR = 127;// according to data format #2's byte 4 range
	private int HR_MSB = 0;
	private int HR_LSB = 0;
	private int E_HR_MSB = 0;
	private int E_HR_LSB = 0;
	private int HRD_MSB = 0;
	private int HRD_LSB = 0;
	private int E_HRD_MSB = 0;
	private int E_HRD_LSB = 0;
	private boolean hrMSBReceived = false;
	private boolean hrLSBReceived = false;
	private boolean ehrMSBReceived = false;
	private boolean ehrLSBReceived = false;
	private boolean hrdMSBReceived = false;
	private boolean hrdLSBReceived = false;
	private boolean ehrdMSBReceived = false;
	private boolean ehrdLSBReceived = false;

	// Special information comes with every packet or 3 times a second
	private boolean SPA = false;
	private boolean lowBat = false;

	SharedPreferences bposettings = null;
	public static final String PONONIN_SETTINGS = "PONoninSettings";
	SharedPreferences.Editor bposettingseditor = null;
	public static final String PREF_FILE = "org.projectproto.yuscope_preferences";
	public static final String PREF_PO = "BTPO";
	public static final int PREF_INPUT_SRC_BLUETOOTH = 0;
	public static final int PREF_INPUT_SRC_UDP = 1;
	public static final int PREF_OUTPUT_TXT = 0;
	public static final int PREF_OUTPUT_RAW = 1;
	public static final String PREF_LEGACY_SMS = "CheckboxLegacySMS";
	public static final String PREF_SECURE_SMS = "CheckboxSecureSMS";

	private final String DEFAULT_MAC_ADDR = "00:00:00:00:00:00";
	private final String PREF_MAC_ADDR = "macAddr";
	private final String PREF_DES_NUM = "phone_num";
	private final boolean DEFAULT_LEGACY_SMS = false; // use the new format by
														// default
	private final boolean DEFAULT_SECURE_SMS = true;

	// private final String APP_CODE = "gmstelehealth";

	// Member object for the RFCOMM services
	private UdpCommService mUdpCommClient = null;

	// private ListenSms listenSms;

	// Sending measurement via SMS
	private SmsSender smsSender = null;
	private SmsReceiver mSmsReceiver;
	private boolean pendingSMS = false;
	private String measurementStr = "";

	private final String ACTION = "android.provider.Telephony.SMS_RECEIVED";

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		Log.w(TAG, "onCreate");

		// ???
		Toast.makeText(getApplicationContext(),
				"Connected to " + mConnectedDeviceName, Toast.LENGTH_SHORT)
				.show();

		// Set up the window layout
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		setContentView(R.layout.main);

		determineScreenSize();
		
		
		/*
		 * SMS settings
		 * 1. SMS encryption is enabled by default - applicable to new format 'gmstelehealth @sys=110@...' only
		 * 2. Legacy SMS format ('From [MAC address];...') is disabled by default
		 */
		//PreferenceManager.setDefaultValues(getApplicationContext(), R.xml.settings, false);
		bposettings = PreferenceManager.getDefaultSharedPreferences(this);

		// bposettings = getSharedPreferences(PREF_PO, Context.MODE_PRIVATE);
		// if (D) Log.v("SharedPreferencesName", PreferenceManager.);
		bposettingseditor = bposettings.edit();

		// Get local Bluetooth adapter
		mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

		// ----------
		// Default input source to Bluetooth
		bposettingseditor.putString("selected_input_source",
				String.valueOf(PREF_INPUT_SRC_BLUETOOTH));
		bposettingseditor.commit();
		// ----------
		if (D)
			Log.i(TAG,
					"the chosen input source is "
							+ Integer.parseInt(bposettings.getString(
									"selected_input_source", "1")));
		// If the adapter is null, then Bluetooth is not supported
		if (Integer.parseInt(bposettings
				.getString("selected_input_source", "1")) == PREF_INPUT_SRC_BLUETOOTH) {
			if (mBluetoothAdapter == null) {
				Toast.makeText(this, "Bluetooth is not available",
						Toast.LENGTH_LONG).show();
				// bposettingseditor.putString("selected_input_source", "1");
				bposettingseditor.putString("selected_input_source",
						String.valueOf(PREF_INPUT_SRC_BLUETOOTH));// why do we
																	// need to
																	// update
																	// the
																	// shared
																	// preference
																	// to the
																	// same
																	// value (0)
																	// here?
				bposettingseditor.commit();
				// finish();
				// return;
			}
		}
		// Prevent phone from sleeping - might require the DEVICE_POWER
		// permisson which doesn't work with Android 4.0+
		PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
		this.mWakeLock = pm.newWakeLock(PowerManager.SCREEN_BRIGHT_WAKE_LOCK,
				"My Tag");
		this.mWakeLock.acquire();

		// Start SMS Listener Service - what does it do?
		if (bposettings.getBoolean("enable_sms_listener", true)) {
			Intent smsListener = new Intent(getApplicationContext(),
					ListenSms.class);
			startService(smsListener);
		}

		// Bundle extras = getIntent().getExtras();
		// if(extras != null){
		// String remote_ip = extras.getString("remote_ip");
		// bposettingseditor.putString("destination_host", remote_ip);
		// bposettingseditor.putBoolean("enable_udp_stream", true);
		// bposettingseditor.commit();
		// }

		// Initialize views
		// ch1_scale = (TextView) findViewById(R.id.txt_ch1_scale);
		// ch1_scale.setText(ampscale[ch1_index]);

		initialiseViews();

		SetListeners();

		// SmsReceiver that handle key exchanges messages
		registerReceivers();

		mSmsReceiver = new SmsReceiver();

		// String message =
		// "gmstelehealth @systolic=100@ @diastolic=70@ @hr=70@";
		// sendEncryptedMessage(message);
		// TODO Check keys in SharedPreferences for server's number +6584781395
		// checkServerKey();

		// MyKeyUtils.checkKeys(PONONIN_SETTINGS, "phone_num",
		// getApplicationContext());
		

	}

	@Override
	public void onStart() {
		super.onStart();
		Log.w(TAG, "onStart");

		Log.w(TAG,
				"legacy SMS format is enabled? "
						+ bposettings.getBoolean(PREF_LEGACY_SMS, false));
		Log.w(TAG, "SMS encryption is enabled? "+bposettings.getBoolean(PREF_SECURE_SMS, true));

		// If BT is not on, request that it be enabled.
		// setupOscilloscope() will then be called during onActivityResult
		Bundle extras = getIntent().getExtras();
		if (extras != null) {
			String remote_ip = extras.getString("remote_ip");
			if (remote_ip != null) {
				if (D)
					Log.v("BluetoothOscilloscope",
							"receive message from service: remote_ip="
									+ remote_ip);
				bposettingseditor.putString("destination_host", remote_ip);
				bposettingseditor.putBoolean("enable_udp_stream", true);
				bposettingseditor.commit();
			}
			String input_source = extras.getString("input_source_pref");
			if (input_source != null) {
				if (D)
					Log.v("BluetoothOscilloscope",
							"receive message from service: input_source="
									+ input_source);
				bposettingseditor.putString("selected_input_source",
						input_source);
				bposettingseditor.putBoolean("enable_udp_stream", false);
				bposettingseditor.commit();
			}
		}

		if (Integer.parseInt(bposettings
				.getString("selected_input_source", "1")) == PREF_INPUT_SRC_BLUETOOTH) {
			if (!mBluetoothAdapter.isEnabled()) {
				Intent enableIntent = new Intent(
						BluetoothAdapter.ACTION_REQUEST_ENABLE);
				startActivityForResult(enableIntent, REQUEST_ENABLE_BT);
				// Otherwise, setup the Oscillosope session
			} else {
				if (mRfcommClient == null)
					setupOscilloscope();
			}
		} else {
			if (mUdpCommClient == null)
				setupOscilloscope();
		}

		RefreshSettings();

		// MyKeyUtils.checkKeys(PONONIN_SETTINGS, "phone_num",
		// getApplicationContext());
		String serverNum = bposettings.getString("phone_num", "");
		checkServerKey(serverNum);
		
		/*
		 * Clear the server's key for testing purpose
		 * When user clicks "Send SMS" there will definitely be no keys
		 */
		/*SharedPreferences prefs = getSharedPreferences(serverNum, Context.MODE_PRIVATE);
		SharedPreferences.Editor prefsEditor = prefs.edit();
		prefsEditor.clear();
		prefsEditor.commit();*/
	}

	@Override
	public void onWindowFocusChanged(boolean hasFocus) {
		super.onWindowFocusChanged(hasFocus);
		if (hasFocus == true) {
			// Hide the test textview
			// TextView testView = (TextView) findViewById(R.id.testTextview);
			// testView.setVisibility(View.INVISIBLE);

			int waveformW = screenLongPx * 2 / 3;
			int waveformH = waveformW / 2;
			if (D)
				Log.i(TAG, "waveform relative layout dimension is " + waveformW
						+ " x " + waveformH);
			// waveformW = 550;
			// waveformH = 350;

			RelativeLayout waveformLayout = (RelativeLayout) findViewById(R.id.Waveform);
			RelativeLayout.LayoutParams adaptLayout = new RelativeLayout.LayoutParams(
					waveformW, waveformH);// the parameters are in pixel not dp
			adaptLayout.addRule(RelativeLayout.BELOW, R.id.test_layout);
			waveformLayout.setLayoutParams(adaptLayout);

			// Only 1 of the folloiwng 2 lines is necessary
			mWaveform.setLayoutParams(new RelativeLayout.LayoutParams(
					waveformW, waveformH));// I have to use
											// RelativeLayout.LayoutParams here
											// because RelativeLayout is the
											// parent view of this custom view;
											// not working w Froyo Andriod 2.2
			// mWaveform.measure(ViewGroup.LayoutParams.MATCH_PARENT,
			// ViewGroup.LayoutParams.MATCH_PARENT);//for some strange reason,
			// this is not setting the dimension of the custom view right w
			// Froyo Andriod 2.2

			mWaveform.setSize(waveformW, waveformH);
			waveform_w = waveformW;
			waveform_h = waveformH;

			if (D)
				if (D)
					Log.i(TAG,
							mWaveform.getWidth() + " - "
									+ mWaveform.getHeight());

			// Initiate waveform data array
			wavefromArray = new int[waveform_w];
		}
	}

	@Override
	public synchronized void onResume() {
		super.onResume();
		Log.w(TAG, "onResume");

		// Performing this check in onResume() covers the case in which BT was
		// not enabled during onStart(), so we were paused to enable it...
		// onResume() will be called when ACTION_REQUEST_ENABLE activity
		// returns.
		if (Integer.parseInt(bposettings
				.getString("selected_input_source", "1")) == PREF_INPUT_SRC_BLUETOOTH) {
			if (mRfcommClient != null) {
				// Only if the state is STATE_NONE, do we know that we haven't
				// started already
				if (mRfcommClient.getState() == BluetoothCommService.STATE_NONE) {
					// Start the Bluetooth RFCOMM services
					mRfcommClient.start();
				}
			}
		} else {
			if (mUdpCommClient.getState() == UdpCommService.STATE_NONE) {
				mUdpCommClient.start();
			}
		}
		frameCount = 0;
		packetCount = 0;
		RefreshSettings();

		/*
		 * receiver that receives SMSs
		 */
		IntentFilter iff = new IntentFilter();
		iff.addAction(ACTION);
		this.registerReceiver(mSmsReceiver, iff);
	}

	@Override
	public void onPause() {
		super.onPause();
		Log.w(TAG, "onPause");
		this.unregisterReceiver(mSmsReceiver);
	}

	@Override
	public void onStop() {
		super.onStop();
		Log.w(TAG, "onStop");
	}

	private void setupOscilloscope() {

		// Initialize the BluetoothCommService to perform bluetooth connections
		if (Integer.parseInt(bposettings
				.getString("selected_input_source", "1")) == PREF_INPUT_SRC_BLUETOOTH) {
			if (mRfcommClient == null) {
				mRfcommClient = new BluetoothCommService(this, mHandler);
			}
		} else {
			if (mUdpCommClient == null) {
				mUdpCommClient = new UdpCommService(this, mHandler);
			}
		}
		// mWaveform = (WaveformView)findViewById(R.id.WaveformArea);

		frameCount = 0;
		packetCount = 0;

		// Why do we need to do this?
		/*
		 * for(int i=0; i<waveform_w; i++){ wavefromArray[i] = 0; ch2_data[i] =
		 * 0; }
		 */

		RefreshSettings();

	}

	// Settings are stored in the application's shared preferences
	private void RefreshSettings() {
		if (!bposettings.contains("enable_udp_stream")) {
			if (D)
				Log.v("setupOscilloscope",
						"PREF_FILE does not contain 'enable_udp_stream' key");
			bposettingseditor.putBoolean("enable_udp_stream", false);
			bposettingseditor.commit();
		}
		send_udp = bposettings.getBoolean("enable_udp_stream", false);
		chkboxEnableUDP.setChecked(send_udp);
		if (!bposettings.contains("destination_host")) {
			if (D)
				Log.v("setupOscilloscope",
						"PREF_FILE does not contain 'destination_host' key");
			bposettingseditor.putString("destination_host", "127.0.0.1");
			bposettingseditor.commit();
		}
		destination_host = bposettings.getString("destination_host",
				"127.0.0.1");
		txtHost.setText(destination_host);
		// editTextHost.setText(destination_host);
		if (!bposettings.contains("destination_port")) {
			if (D)
				Log.v("setupOscilloscope",
						"PREF_FILE does not contain 'destination_port' key");
			bposettingseditor.putString("destination_port", "12345");
			bposettingseditor.commit();
		}
		destination_port = Integer.parseInt(bposettings.getString(
				"destination_port", "12345"));
		txtPort.setText(bposettings.getString("destination_port", "12345"));
		// editTextPort.setText(bposettings.getString("destination_port",
		// "12345"));
		if (!bposettings.contains("enable_sms_listener")) {
			if (D)
				Log.v("setupOscilloscope",
						"PREF_FILE does not contain 'enable_sms_listener' key");
			bposettingseditor.putBoolean("enable_sms_listener", true);
			bposettingseditor.commit();
		}
		ChangeDestination();
		if (D)
			Log.v("SharedPreferences",
					"selected_input_source="
							+ bposettings.getString("selected_input_source",
									"1"));
		if (D)
			Log.v("SharedPreferences",
					"selected_output_format="
							+ bposettings.getString("selected_output_format",
									"0"));
	}

	private void SetListeners() {
		chkboxEnableUDP
				.setOnCheckedChangeListener(new OnCheckedChangeListener() {
					public void onCheckedChanged(CompoundButton buttonView,
							boolean isChecked) {
						send_udp = isChecked;
						bposettingseditor.putBoolean("enable_udp_stream",
								isChecked);
						bposettingseditor.commit();
					}
				});
		buttonTestUDP.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				SendLiteralByUdp();
				if (D)
					Log.v("TestUdp#onClick", "ButtonSendDebugMessage");
			}
		});
		buttonActivateCall.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				ActivateCall();
				if (D)
					Log.v("TestCallDoc#onClick", "ButtonActivateCall");
			}
		});
		buttonEndCall.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				EndCall();
				if (D)
					Log.v("TestCallDoc#onClick", "ButtonEndCall");
			}
		});
		buttonSms.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				// SmsHrSpo2Values();
				// String macAddr = DEFAULT_MAC_ADDR;

				sendMeasurement();

				// if (D) Log.v("TestCallDoc#onClick", "ButtonEndCall");
			}
		});
		buttonStream2Doc.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				Stream2Doc();
				if (D)
					Log.v("TestStream2Doc#onClick", "ButtonStream2Doc");
			}
		});
		mConnectButton.setOnClickListener(new OnClickListener() {
			public void onClick(View arg0) {
				if (D)
					Log.d(TAG, "Connect BT button is clicked");
				BTConnect();
			}
		});

	}

	void ActivateCall() {
		String phoneNo = bposettings.getString(PREF_DES_NUM, "");
		AudioManager mAudioManager = (AudioManager) getSystemService(AUDIO_SERVICE);

		if (phoneNo.length() > 0) {
			Intent callIntent = new Intent(Intent.ACTION_CALL);
			// callIntent.addFlags(Intent.FLAG_ACTIVITY_TASK_ON_HOME);
			callIntent.setData(Uri.parse("tel:" + phoneNo));
			startActivity(callIntent);
			mAudioManager.setSpeakerphoneOn(true);
		} else {
			Toast.makeText(getBaseContext(),
					"Please enter doctor's phone number in 'Settings' menu.",
					Toast.LENGTH_SHORT).show();
		}

	}

	void EndCall() {
		TelephonyManager tm = (TelephonyManager) getBaseContext()
				.getSystemService(Context.TELEPHONY_SERVICE);
		try {
			// Java reflection to gain access to TelephonyManager's
			// ITelephony getter
			Class c = Class.forName(tm.getClass().getName());
			Method m = c.getDeclaredMethod("getITelephony");
			m.setAccessible(true);
			com.android.internal.telephony.ITelephony telephonyService = (ITelephony) m
					.invoke(tm);
			telephonyService.endCall();
		} catch (Exception e) {
			e.printStackTrace();
		}
		Toast.makeText(getBaseContext(), "Call is ended", Toast.LENGTH_SHORT)
				.show();
	}

	/*
	 * void SmsHrSpo2Values() {
	 * 
	 * String macAddr = DEFAULT_MAC_ADDR; if (bposettings != null) { macAddr =
	 * bposettings.getString(PREF_MAC_ADDR, DEFAULT_MAC_ADDR); }
	 * 
	 * String phoneNo = bposettings.getString(PREF_DES_NUM, ""); //
	 * construcSMS(int weight, int systolic, int diastolic, int pulse, int //
	 * spo2, String measurementDate) // passing -1 as value for weight, systolic
	 * and diastolic // passing null as value for measurementDate String msg =
	 * constructMeasurementStr(PREF_PO, -1, -1, -1, HR, SPO2, null); if
	 * (phoneNo.length() > 0) { sendSMS(phoneNo, msg); } else {
	 * Toast.makeText(getBaseContext(),
	 * "Please enter doctor's phone number in 'Settings' menu.",
	 * Toast.LENGTH_SHORT).show(); } }
	 */

	// TODO: to pass the preference name e.g. "Bluetooth PO"
	// Format of the SMS:
	// "@MAC=[mac address of the pulse oximeter]@ @datetime=[yyyy-mm-dd HH:mm:ss]@ @systolic=[systolic]@ @diastolic=[diastolic]@ @weight=[weight]@ @hr=[hr]@ @spo2=[spo2]@"
	private String constructMeasurementStr(String pref, int weight,
			int systolic, int diastolic, int pulse, int spo2,
			String measurementDate) {
		// Code for this app
		String str = "";

		// MAC address of the health device
		String macAddr = DEFAULT_MAC_ADDR;

		// SharedPreferences tmp = getSharedPreferences(pref,
		// Context.MODE_PRIVATE);
		// temporarily ignoring the SharedPreference passed as parameter, use
		// the defaulsharedpreference for consistency
		SharedPreferences tmp = PreferenceManager
				.getDefaultSharedPreferences(this);
		if (tmp != null) {
			macAddr = tmp.getString(PREF_MAC_ADDR, DEFAULT_MAC_ADDR);
			if (D)
				Log.w(TAG, "mac address of the pulse oximeter is " + macAddr);
		}
		str = str + "@mac=" + macAddr + "@ ";

		// Date and time of the measurement
		String dt = new String();
		if (measurementDate == null) {
			// Grab the Android system date and time
			DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
			Date date = new Date();
			dt = dateFormat.format(date);
		} else
			dt = measurementDate;
		str = str + "@datetime=" + dt + "@ ";

		// Measurement data e.g. weight, systolic, pulse
		if (weight >= 0)
			str = str + "@weight=" + weight + "@ ";
		if (systolic >= 0)
			str = str + "@systolic=" + systolic + "@ ";
		if (diastolic >= 0)
			str = str + "@diastolic=" + diastolic + "@ ";
		if (pulse >= 0)
			str = str + "@hr=" + pulse + "@ ";
		if (spo2 >= 0)
			str = str + "@spo2=" + spo2 + "@ ";

		// msg = "gmstelehealth @MAC="+ macAddr
		// +"@ @datetime="+dt+"@ @HR="+HR+"@ @spO2="+SPO2+"@";
		str = str.trim();

		if (D)
			Log.w(TAG, "The exact final measurement string is: '" + str + "'");
		return str;
	}

	// From MAC; @HR@ = 90; @spO2@ = 97;
	private String constructLegacyMeasurementStr(String pref, int weight, int systolic,
			int diastolic, int pulse, int spo2, String measurementDate) {
		String msg = "";

		// MAC address of the health device
		String macAddr = DEFAULT_MAC_ADDR;

		SharedPreferences tmp = getSharedPreferences(pref, Context.MODE_PRIVATE);
		if (tmp != null) {
			macAddr = tmp.getString(PREF_MAC_ADDR, DEFAULT_MAC_ADDR);
			if (D)
				Log.w(TAG, "mac address of the health device is " + macAddr);
		}
		msg = msg + macAddr + "; ";

		// Measurement data e.g. weight, systolic, pulse
		// if (weight>=0) msg = msg + "@weight="+weight+"@ ";
		if (systolic >= 0)
			msg = msg + "@systolic@ = " + systolic + "; ";
		if (diastolic >= 0)
			msg = msg + "@diastolic@ = " + diastolic + "; ";
		if (pulse >= 0)
			msg = msg + "@HR@ = " + pulse + "; ";
		if (spo2 >= 0)
			msg = msg + "@spO2@ = " + spo2 + "; ";

		// msg = "gmstelehealth @MAC="+ macAddr
		// +"@ @datetime="+dt+"@ @HR="+HR+"@ @spO2="+SPO2+"@";
		msg = msg.trim();

		if (D)
			Log.w(TAG, "The final measurement string is: '" + msg + "'");

		return msg;
	}

	void Stream2Doc() {
		String phoneNo = bposettings.getString(PREF_DES_NUM, "");
		if (phoneNo.length() > 0) {
			sendSMS(phoneNo, "wakeup");
		} else {
			Toast.makeText(getBaseContext(),
					"Please enter doctor's phone number in 'Settings' menu.",
					Toast.LENGTH_SHORT).show();
		}
	}

	private void sendSMS(String phoneNumber, String message) {

		String SENT = "SMS_SENT";
		String DELIVERED = "SMS_DELIVERED";

		PendingIntent sentPI = PendingIntent.getBroadcast(this, 0, new Intent(
				SENT), 0);

		PendingIntent deliveredPI = PendingIntent.getBroadcast(this, 0,
				new Intent(DELIVERED), 0);

		SmsManager sms = SmsManager.getDefault();
		sms.sendTextMessage(phoneNumber, null, message, sentPI, deliveredPI);
	}

	void ChangeDestination() {
		try {
			datagramSocket = null;
			datagramSocket = new DatagramSocket();
		} catch (SocketException e) {
			if (D)
				Log.e(TAG, e.toString());
		}
	}

	@Override
	public void onClick(View v) {

	}

	private void BTConnect() {
		// Once in here, it's already trying to establish Bluetooth connection,
		// so why the if?
		// if (Integer.parseInt(bposettings.getString("selected_input_source",
		// "1")) == PREF_INPUT_SRC_BLUETOOTH){
		if (D)
			Log.d(TAG, "opening deviceactivity");
		Intent serverIntent = new Intent(this, DeviceListActivity.class);
		startActivityForResult(serverIntent, REQUEST_CONNECT_DEVICE);
		// }
	}

	private int toScreenPos(byte position) {
		return ((int) waveform_h - (int) position * 7 - 8);
	}

	@Override
	public void onDestroy() {
		Log.w(TAG, "onDestroy");
		// Stop the Bluetooth RFCOMM services
		if (mRfcommClient != null)
			mRfcommClient.stop();
		if (mUdpCommClient != null)
			mUdpCommClient.stop();
		// release screen being on
		if (mWakeLock.isHeld()) {
			mWakeLock.release();
		}
		// unregisterReceiver(smsReceiver);

		super.onDestroy();
	}

	/**
	 * Sends a message.
	 * 
	 * @param message
	 *            A string of text to send.
	 */

	private void SendMessageByUdp(String string_to_be_sent) {
		try {
			byte[] byte_array = string_to_be_sent.getBytes();
			InetAddress inet_address = InetAddress.getByName(destination_host);
			DatagramPacket datagram_packet = new DatagramPacket(byte_array,
					byte_array.length, inet_address, destination_port);
			if (datagramSocket == null) {
				datagramSocket = new DatagramSocket();
			}
			datagramSocket.send(datagram_packet);
		} catch (IOException io_exception) {
			datagramSocket = null;
			if (D)
				Log.e(TAG, io_exception.toString());
		}
	}

	private void SendLiteralByUdp() {
		SendMessageByUdp("[" + destination_host + ":" + destination_port + "] "
				+ mConnectedDeviceName + "\n");
	}

	private void SendBytesByUdp(byte[] byte_array) {
		try {
			InetAddress inet_address = InetAddress.getByName(destination_host);
			DatagramPacket datagram_packet = new DatagramPacket(byte_array,
					byte_array.length, inet_address, destination_port);
			if (datagramSocket == null) {
				datagramSocket = new DatagramSocket();
			}
			datagramSocket.send(datagram_packet);
		} catch (IOException io_exception) {
			datagramSocket = null;
			if (D)
				Log.e(TAG, io_exception.toString());
		}
	}

	// The Handler that gets information back from the BluetoothCommService
	private final Handler mHandler = new Handler() {

		@Override
		public void handleMessage(Message msg) {
			switch (msg.what) {
			case MESSAGE_STATE_CHANGE:
				switch (msg.arg1) {
				case BluetoothCommService.STATE_CONNECTED:
					mBTStatus.setText(R.string.title_connected_to);
					mBTStatus.append("\n" + mConnectedDeviceName);
					break;
				case BluetoothCommService.STATE_CONNECTING:
					mBTStatus.setText(R.string.title_connecting);
					break;
				case BluetoothCommService.STATE_NONE:
					mBTStatus.setText(R.string.title_not_connected);
					break;
				}
				break;
			case MESSAGE_WRITE:
				break;
			case MESSAGE_READ:

				int raw,
				data_length,
				x;// what is x? the iterator for the array of data for the
					// waveformview
				byte[] readBuf = (byte[]) msg.obj;
				data_length = msg.arg1;

				x = frameCount % waveform_w;// framecount is 0 isn't it?
				// raw = (int) readBuf[2];//UByte(readBuf[2]);//This is the
				// frame's 3rd byte containing the waveform data - is it for HR
				// or SPO2 or both somehow?
				// Byte in Java is -128 to 127 while the intended figure here is
				// 0 to 255 so conversion is necessary
				raw = UByte(readBuf[2]);
				wavefromArray[x] = raw;
				// if (D) Log.d(TAG, "waveform data " + raw);
				mWaveform.setData(wavefromArray);

				// Retrieve pulse rate and SpO2 and display on top right corner
				// of the app's main screen
				getMeasurements(readBuf);
				frameCount++;
				break;
			case MESSAGE_DEVICE_NAME:
				// save the connected device's name
				mConnectedDeviceName = msg.getData().getString(DEVICE_NAME);
				Toast.makeText(getApplicationContext(),
						"Connected to " + mConnectedDeviceName,
						Toast.LENGTH_SHORT).show();
				break;
			case MESSAGE_TOAST:
				Toast.makeText(getApplicationContext(),
						msg.getData().getString(TOAST), Toast.LENGTH_SHORT)
						.show();
				break;
			}
			// if (D) Log.d(TAG, " framecount is "+frameCount);
		}

	};

	public void getMeasurements(byte[] frame) {
		byte statusB = frame[1];
		int test = frame[1];
		if ((statusB & 0x01) == 1) {// frame[1] is Byte 2 or the STATUS byte
									// whose bits are like 0??????[SYNC Bit]
			// but other
			// if (D) Log.w(TAG,
			// "Frame Sync encountered! Status is "+UByte(statusB)+" - "+
			// Integer.toBinaryString(statusB));
			frameInPacket = 1;// Frame Sync
			packetCount++;// tracking packets based on Frame Sync instead of
							// frameCount/25 since there might be frames lost
							// during transmission
		} else if (frameInPacket > 0 && frameInPacket < 25) {// if Frame Sync is
																// not found
																// first,
																// frameInPacket
																// should not be
																// increased
			frameInPacket++;
		} else if (frameInPacket == 25) {
			// reset frameInPacket at the end of the packet
			frameInPacket = 0;
		}
		if ((statusB & 0x1D) == 2) {
			if (D)
				Toast.makeText(this, "reliable measurement", Toast.LENGTH_SHORT)
						.show();
		}
		// if (D) Log.w(TAG, "Status is "+test+" - "+ String.format("%8s",
		// Integer.toBinaryString(statusB & 0xFF)).replace(' ', '0') + " - "+
		// Integer.toBinaryString(statusB));
		// if (D) Log.w(TAG, "Status is "+UByte(statusB)+" - "+
		// Integer.toBinaryString((statusB+256)%256) + " - "+
		// Integer.toBinaryString(statusB));

		// if (D) Log.i(TAG,
		// "Packet No."+packetCount+" frame No."+frameInPacket);
		// both HR and SPO2 are stored in the 4th byte which is frame[3]
		byte b = frame[3];
		int integer1 = frame[3];
		// String bString = String.format("%8s", Integer.toBinaryString(b &
		// 0xFF)).replace(' ', '0');
		String bString = String.format("%8s", Integer.toBinaryString(b))
				.replace(' ', '0');
		String s2 = String.format("%8s", Integer.toBinaryString(integer1))
				.replace(' ', '0');

		switch (frameInPacket) {

		// reading the information on certain frames
		case 1:
			HR_MSB = integer1;
			hrMSBReceived = true;
			break;
		case 2:
			HR_LSB = integer1;
			hrLSBReceived = true;
			break;
		case 8:
			// Checking this STAT2 frame's Bit 5 for high quality smartpoint
			// measurement and low battery
			if ((b & 0x20) > 0)
				SPA = true;
			else
				SPA = false;
			if (D)
				Log.d(TAG, "STAT2 is " + bString);

			if ((b & 0x01) > 0)
				lowBat = true;
			break;
		case 14:
			E_HR_MSB = integer1;
			ehrMSBReceived = true;
			break;
		case 15:
			E_HR_LSB = integer1;
			ehrLSBReceived = true;
			break;
		case 20:// if (MIN_HR<=UByte(frame[3])&&UByte(frame[3])<=MAX_HR) {

			// pulse_rate.setText(""+UByte(frame[3])+" "+frame[3]);
			// HR = UByte(frame[3]);
			// HRD_MSB = integer1;
			if (SPA) {
				HRD_MSB = b;
				// if (D) Log.i(TAG, frameCount + ") HR-D MSB "+ UByte(frame[3])
				// + " before conversion " + HRD_MSB+ " - third byte "+ bString
				// + " - "+s2);
				hrdMSBReceived = true;
			}

			break;
		// }
		case 21: // if (MIN_HR<=UByte(frame[3])&&UByte(frame[3])<=MAX_HR) {

			// pulse_rate.setText(""+UByte(frame[3])+" "+frame[3]);
			// HR = UByte(frame[3]);
			// HRD_LSB = integer1;
			if (SPA) {
				HRD_LSB = b;
				hrdLSBReceived = true;
				// if (D) Log.i(TAG, frameCount+") HR-D LSB "+ UByte(frame[3]) +
				// " before conversion " + HRD_LSB + " - third byte "+ bString +
				// " - "+s2);
			}

			break;
		// }
		case 22:// if (MIN_HR<=UByte(frame[3])&&UByte(frame[3])<=MAX_HR) {

			// pulse_rate.setText(""+UByte(frame[3])+" "+frame[3]);
			// HR = UByte(frame[3]);
			E_HRD_MSB = integer1;
			ehrdMSBReceived = true;
			// if (D) Log.i(TAG, frameCount+") E-HR-D MSB "+ UByte(frame[3]) +
			// " before conversion " + E_HRD_MSB + " - third byte "+
			// bString+" - "+s2);
			break;
		// }
		case 23:// if (MIN_HR<=UByte(frame[3])&&UByte(frame[3])<=MAX_HR) {
			// if (18<UByte(frame[3])&&UByte(frame[3])<321) {Log.i(TAG,
			// frameCount+") E-HR-D LSB " + frame[3]);}

			// pulse_rate.setText(""+UByte(frame[3])+" "+frame[3]);
			// HR = UByte(frame[3]);
			E_HRD_LSB = integer1;
			ehrdLSBReceived = true;
			// if (D) Log.i(TAG, frameCount+") E-HR-D LSB "+ UByte(frame[3]) +
			// " before conversion " + E_HRD_LSB + " - third byte "+
			// bString+" - "+s2);
			break;
		// }
		case 3:
			// case 9:
			// case 16:
			// case 17:
			pulse_sat.setText("" + UByte(frame[3]));
			SPO2 = UByte(frame[3]);
			break;
		}
		// if (D) Log.i(TAG, "High quality measurement yes? "+SPA);

		String hrdMSB = "HR D MSB:"
				+ HRD_MSB
				+ "-"
				+ String.format("%8s", Integer.toBinaryString(HRD_MSB))
						.replace(' ', '0');
		String hrdLSB = "HR D LSB:"
				+ HRD_LSB
				+ "-"
				+ String.format("%8s", Integer.toBinaryString(HRD_LSB))
						.replace(' ', '0');
		String ehrdMSB = "E HR D MSB:"
				+ E_HRD_MSB
				+ "-"
				+ String.format("%8s", Integer.toBinaryString(E_HRD_MSB))
						.replace(' ', '0');
		String ehrdLSB = "E HR D LSB:"
				+ E_HRD_LSB
				+ "-"
				+ String.format("%8s", Integer.toBinaryString(E_HRD_LSB))
						.replace(' ', '0');
		// if (D) Log.i(TAG, hrdMSB +" ; "+ hrdLSB +" ; "+ ehrdMSB +" ; "+
		// ehrdLSB);

		// Combining the Most Significant Byte and Least Significant Byte to get
		// pulse rate
		/*
		 * if (hrMSBReceived&&hrLSBReceived&&SPA) { HR = ((HR_MSB & 0x03) << 7)
		 * | (HR_LSB);
		 * 
		 * if (HR > 0) { pulse_rate.setText(""+HR); //String result =
		 * "HR:"+HR+" - "+String.format("%16s",
		 * Integer.toBinaryString(HR)).replace(' ', '0'); //if (D) Log.i(TAG,
		 * hrdMSB + " "+hrdLSB+" "+result);
		 * 
		 * HR_MSB = 0; HR_LSB = 0; hrMSBReceived = false; hrLSBReceived = false;
		 * 
		 * } else { if (D) Log.d(TAG, "HR is 0 huh? "); } }
		 * 
		 * if (ehrMSBReceived&&ehrLSBReceived&&SPA) { HR = ((E_HR_MSB & 0x03) <<
		 * 7) | (E_HR_LSB);
		 * 
		 * if (HR > 0) { pulse_rate.setText(""+HR+" extended"); //String result
		 * = "HR:"+HR+" - "+String.format("%16s",
		 * Integer.toBinaryString(HR)).replace(' ', '0'); //if (D) Log.i(TAG,
		 * hrdMSB + " "+hrdLSB+" "+result);
		 * 
		 * E_HR_MSB = 0; E_HR_LSB = 0; ehrMSBReceived = false; ehrLSBReceived =
		 * false;
		 * 
		 * } else { if (D) Log.d(TAG, "HR is 0 huh? "); } }
		 */

		if (hrdMSBReceived && hrdLSBReceived && SPA) {
			HR = ((HRD_MSB & 0x03) << 7) | (HRD_LSB);
			// HR = HRD_LSB+(HRD_MSB*128);

			if (HR >= 18 && HR <= 321) {
				// pulse_rate.setText(""+HR+" D");
				pulse_rate.setText("" + HR);
				String result = "HR:"
						+ HR
						+ " - "
						+ String.format("%16s", Integer.toBinaryString(HR))
								.replace(' ', '0');
				// if (D) Log.i(TAG, hrdMSB + " "+hrdLSB+" "+result);

				HRD_MSB = 0;
				HRD_LSB = 0;
				hrdMSBReceived = false;
				hrdLSBReceived = false;
				SPA = false;

			} else {
				if (D)
					Log.e(TAG, "HR is 0 huh? " + hrdMSB + " " + hrdLSB);
			}
		}

		/*
		 * if (ehrdMSBReceived&&ehrdLSBReceived&&SPA) { HR = ((E_HRD_MSB & 0x03)
		 * << 7) | (E_HRD_LSB); if (HR >0) {
		 * 
		 * pulse_rate.setText(""+HR+" D extended"); //String result =
		 * "HR:"+HR+" - "+String.format("%16s",
		 * Integer.toBinaryString(HR)).replace(' ', '0'); //if (D) Log.i(TAG,
		 * ehrdMSB + " "+ehrdLSB+" "+result);
		 * 
		 * E_HRD_MSB = 0; E_HRD_LSB = 0; ehrdMSBReceived = false;
		 * ehrdLSBReceived = false;
		 * 
		 * } else { if (D) Log.d(TAG, "HR is 0 huh? "+ehrdMSB + " "+ehrdLSB); }
		 * }
		 */

		// if (D) Log.d(TAG,
		// frameCount+") - packetpulse rate is "+HR+" oxigen saturation is "+SPO2);

		if (send_udp) {
			switch (Integer.parseInt(bposettings.getString(
					"selected_output_format", "0"))) {
			case PREF_OUTPUT_TXT:
				Date date = new Date();
				// String udpMessage = String.format("%d, %d, %d, %d, %d",
				// UByte(frame[0]), UByte(frame[1]),
				// UByte(frame[2]), UByte(frame[3]), UByte(frame[4]));
				String udpMessage = String.format("%d, %d, %d",
						UByte(frame[2]), HR, SPO2);
				SendMessageByUdp("OX, " + frameCount + ", " + date.getTime()
						+ ", " + udpMessage + "\n");
				break;
			case PREF_OUTPUT_RAW:
				SendBytesByUdp(frame);
				break;
			}
		}

	}

	// Converting byte to integer for the negative numbers
	// For example, status's range is 128-255 but they are read as byte ==> -127
	// to 0(or -1?)
	private int UByte(byte b) {
		if (b < 0) {
			// if negative {
			// return (int) ((b&0x7F) + 128 );
			int a = (int) b;
			return (a + 256);
		} else
			return (int) b;
	}

	// Hirwan's UByte method
	/*
	 * private int UByte(byte b){ if(b<0) // if negative return (int)( (b&0x7F)
	 * + 128 ); else return (int)b; }
	 */

	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		// if (D) Log.i(TAG,
		// "request code was "+requestCode+" and result code was "+resultCode);
		switch (requestCode) {
		case REQUEST_CONNECT_DEVICE:
			// Again, there is no point having the following if because it is
			// handling Bluetooth connection here
			// if
			// (Integer.parseInt(bposettings.getString("selected_input_source",
			// "1")) == PREF_INPUT_SRC_BLUETOOTH){
			// When DeviceListActivity returns with a device to connect
			if (resultCode == Activity.RESULT_OK) {// 1 device is discovered -
													// pair or not pair?
				// Get the device MAC address
				String address = data.getExtras().getString(
						DeviceListActivity.EXTRA_DEVICE_ADDRESS);
				if (D)
					Log.i(TAG, "MAC address of the PO is " + address
							+ " as received by BluetoothPulsOximer activity");

				// Store the MAC address in application's shared preference
				bposettingseditor.putString(PREF_MAC_ADDR, address);
				bposettingseditor.commit();
				Log.w(TAG,
						"mac address stored in app's shared preferences correctly "
								+ bposettings.getString(PREF_MAC_ADDR,
										DEFAULT_MAC_ADDR));
				// Get the BLuetoothDevice object
				BluetoothDevice device = mBluetoothAdapter
						.getRemoteDevice(address);
				// Attempt to connect to the device
				mRfcommClient.connect(device);
			}
			// }
			break;
		case REQUEST_ENABLE_BT:
			// if
			// (Integer.parseInt(bposettings.getString("selected_input_source",
			// "1")) == PREF_INPUT_SRC_BLUETOOTH){
			// When the request to enable Bluetooth returns
			if (resultCode == Activity.RESULT_OK) {
				// Bluetooth is now enabled, so set up the oscilloscope
				setupOscilloscope();
			} else {
				// User did not enable Bluetooth or an error occured
				Toast.makeText(this, R.string.bt_not_enabled_leaving,
						Toast.LENGTH_SHORT).show();
				finish();
			}
			// }
		}
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.mainmenu, menu);
		// if (D) Log.v("BluetoothOscilloscope", "onCreateOptionsMenu");
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// if (D) Log.v("BluetoothOscilloscope#MenuItem", "" +
		// item.getItemId());
		// if (D) Log.v("BluetoothOscilloscope#R.id.settings", "" +
		// R.id.settings);
		if (item.getItemId() == R.id.settings) {
			Intent intent = new Intent().setClass(this,
					MenuSettingsActivity.class);
			this.startActivityForResult(intent, 0);
		}
		return true;
	}

	// getting messages from where?
	/*
	 * private BroadcastReceiver smsReceiver = new BroadcastReceiver() {
	 * 
	 * @Override public void onReceive(Context context, Intent intent) { if
	 * (!intent.getAction().equals( "android.provider.Telephony.SMS_RECEIVED"))
	 * { return; } SmsMessage msg[] = getMessagesFromIntent(intent);
	 * 
	 * for (int i = 0; i < msg.length; i++) { String message =
	 * msg[i].getDisplayMessageBody(); if (message != null && message.length() >
	 * 0) { if (D) Log.i("MessageListener:", message); // to check sms keyword..
	 * need to define the keyword if (message.startsWith("startip")) { // String
	 * senderphone = msg[i].getOriginatingAddress(); String remote_ip =
	 * message.replaceFirst( "(?i)(startip)(.+?)(stopip)", "$2");
	 * bposettingseditor.putString("destination_host", remote_ip);
	 * bposettingseditor.putBoolean("enable_udp_stream", true);
	 * bposettingseditor.putString("selected_output_format", "1");
	 * bposettingseditor.commit(); destination_host = bposettings.getString(
	 * "destination_host", "127.0.0.1"); txtHost.setText(destination_host);
	 * send_udp = bposettings.getBoolean("enable_udp_stream", false);
	 * chkboxEnableUDP.setChecked(send_udp); } } } }
	 * 
	 * private SmsMessage[] getMessagesFromIntent(Intent intent) { SmsMessage
	 * retMsgs[] = null; Bundle bdl = intent.getExtras(); try { Object pdus[] =
	 * (Object[]) bdl.get("pdus"); retMsgs = new SmsMessage[pdus.length]; for
	 * (int n = 0; n < pdus.length; n++) { byte[] byteData = (byte[]) pdus[n];
	 * retMsgs[n] = SmsMessage.createFromPdu(byteData); }
	 * 
	 * } catch (Exception e) { if (D) Log.e("GetMessages", "fail", e); } return
	 * retMsgs; }
	 * 
	 * };
	 */

	/*
	 * Receivers that listen to the sent and delivered events of SMSs
	 */

	private void registerReceivers() {
		String SENT = "SMS_SENT";
		String DELIVERED = "SMS_DELIVERED";
		// ---when the SMS has been sent---
		registerReceiver(new BroadcastReceiver() {
			@Override
			public void onReceive(Context arg0, Intent arg1) {
				switch (getResultCode()) {
				case Activity.RESULT_OK:
					Toast.makeText(getBaseContext(), "SMS sent",
							Toast.LENGTH_SHORT).show();
					Log.i(TAG, "SMS sent");

					break;
				case SmsManager.RESULT_ERROR_GENERIC_FAILURE:
					Toast.makeText(getBaseContext(), "Generic failure",
							Toast.LENGTH_SHORT).show();
					Log.w(TAG, "Generic failure");
					break;
				case SmsManager.RESULT_ERROR_NO_SERVICE:
					Toast.makeText(getBaseContext(), "No service",
							Toast.LENGTH_SHORT).show();
					Log.w(TAG, "No service");
					break;
				case SmsManager.RESULT_ERROR_NULL_PDU:
					Toast.makeText(getBaseContext(), "Null PDU",
							Toast.LENGTH_SHORT).show();
					Log.w(TAG, "Null PDU");
					break;
				case SmsManager.RESULT_ERROR_RADIO_OFF:
					Toast.makeText(getBaseContext(), "Radio off",
							Toast.LENGTH_SHORT).show();
					Log.w(TAG, "Radio off");
					break;
				}
			}
		}, new IntentFilter(SENT));

		// ---when the SMS has been delivered---
		registerReceiver(new BroadcastReceiver() {
			@Override
			public void onReceive(Context arg0, Intent arg1) {
				switch (getResultCode()) {
				case Activity.RESULT_OK:
					Toast.makeText(getBaseContext(), "SMS delivered",
							Toast.LENGTH_SHORT).show();
					Log.i(TAG, "SMS delivered");

					break;
				case Activity.RESULT_CANCELED:
					Toast.makeText(getBaseContext(), "SMS not delivered",
							Toast.LENGTH_SHORT).show();
					Log.w(TAG, "SMS not delivered");
					break;
				}
			}
		}, new IntentFilter(DELIVERED));
	}

	private void initialiseViews() {
		mBTStatus = (TextView) findViewById(R.id.txt_btstatus);

		mWaveform = (WaveformView) findViewById(R.id.WaveformArea);

		pulse_rate = (TextView) findViewById(R.id.txt_hr_value);
		pulse_sat = (TextView) findViewById(R.id.txt_spo_value);
		txtHost = (TextView) findViewById(R.id.txt_host_value);
		txtPort = (TextView) findViewById(R.id.txt_port_value);
		chkboxEnableUDP = (CheckBox) this.findViewById(R.id.chkbox_enable_udp);
		buttonTestUDP = (Button) this.findViewById(R.id.button_udp);
		// buttonCallDoc = (ToggleButton) this.findViewById(R.id.button_call);
		buttonActivateCall = (ImageButton) findViewById(R.id.btn_r_activatecall);
		buttonEndCall = (ImageButton) findViewById(R.id.btn_r_endcall);
		buttonSms = (ImageButton) findViewById(R.id.btn_sms);
		buttonStream2Doc = (Button) this.findViewById(R.id.button_data);
		mConnectButton = (Button) findViewById(R.id.button_connect);
		settingsBtn = (Button) findViewById(R.id.Btn_Settings);

		boolean menuKey = ViewConfiguration.get(getApplicationContext())
				.hasPermanentMenuKey();
		// if (D) Log.w(TAG, "device has a permanent menu key "+menuKey);
		// if (D) Toast.makeText(getApplicationContext(),
		// "Device has a permanent meny key "+menuKey,
		// Toast.LENGTH_SHORT).show();
		if (menuKey)
			settingsBtn.setVisibility(View.GONE);
		else
			settingsBtn.setOnClickListener(new OnClickListener() {
				public void onClick(View arg0) {
					// TODO: redirect user to the settings view
					Intent intent = new Intent().setClass(MainActivity.this,
							MenuSettingsActivity.class);
					MainActivity.this.startActivityForResult(intent, 0);
				}
			});
	}

	private boolean checkServerKey(String contactNum) {
		// String contactNum = bposettings.getString(PREF_DES_NUM, "");
		// Log.w(TAG, "stored server num is " + contactNum);
		if (contactNum.isEmpty()) {
			// Toast.makeText(getApplicationContext(),
			// "Please enter server's number!", Toast.LENGTH_LONG);
			MyUtils.alert("Please enter server's number", MainActivity.this);
			return false;
		}

		RSAPublicKeySpec publicKeySpec = MyKeyUtils.getRecipientsPublicKey(
				contactNum, getApplicationContext());

		if (publicKeySpec == null) {
			Log.e(TAG, "server's key not found, requesting for it");
			Toast.makeText(getApplicationContext(),
					"server's key not found, requesting for it",
					Toast.LENGTH_SHORT).show();

			// TODO request server's key
			MyUtils.RequestKeyTask task = new MyUtils.RequestKeyTask(
					contactNum, getApplicationContext());
			task.execute();
			return false;
		} else {
			Log.w(TAG, "server's key found, ready to send secure message");
			Toast.makeText(getApplicationContext(),
					"Server's key found, ready to send secure message",
					Toast.LENGTH_SHORT).show();
			return true;
		}

	}

	private void determineScreenSize() {
		// Checking the density of the screen
		DisplayMetrics metrics = new DisplayMetrics();
		getWindowManager().getDefaultDisplay().getMetrics(metrics);
		screenDensity = metrics.densityDpi;
		int w = metrics.widthPixels;
		int h = metrics.heightPixels;
		double x = metrics.xdpi;
		double y = metrics.ydpi;

		if (w > h)
			screenLongPx = w;
		else
			screenLongPx = h;
		scale = screenDensity / 160;
		screenLongDp = w * 160 / screenDensity;

		if (D)
			Log.i(TAG, "the phone's screen density is " + screenDensity
					+ " dpi " + w + " " + h + " " + x + " " + y
					+ " screen width in dp is " + screenLongDp);
	}

	private void sendMeasurement() {
		if (bposettings != null) {
			// macAddr = bposettings.getString(PREF_MAC_ADDR, DEFAULT_MAC_ADDR);
			boolean legacySMS = bposettings.getBoolean(PREF_LEGACY_SMS,DEFAULT_LEGACY_SMS);
			boolean secureSMS = bposettings.getBoolean(PREF_SECURE_SMS, DEFAULT_SECURE_SMS);
			Log.w(TAG, "sending measurement, legacy sms format is enabled? "
					+ legacySMS);
			Log.w(TAG, "sending measurement, secure sms is enabled? "
					+ secureSMS);
			
			
			String phoneNo = bposettings.getString(PREF_DES_NUM, "");
			if (phoneNo.isEmpty()) {
				MyUtils.alert("Please enter server's number", MainActivity.this);
				return;
			}

			smsSender = new SmsSender(phoneNo);

			

				// String measurementStr = "";
				// construcSMS(int weight, int systolic, int diastolic, int
				// pulse,
				// int spo2, String measurementDate)
				// passing -1 as value for weight, systolic and diastolic
				// passing null as value for measurementDate
				// this does not include the code "gmstelehealth"
				// format of the measurement string is:
				// "@MAC=[mac address of the pulse oximeter]@ @datetime=[yyyy-mm-dd HH:mm:ss]@ @hr=[hr]@ @spo2=[spo2]@"
				if (legacySMS) {
					Log.w(TAG,
							"constructing the measurement based on Frank's format - ");
					measurementStr = constructLegacyMeasurementStr(PREF_PO, -1, -1, -1,
							HR, SPO2, null);
				} else {
					Log.w(TAG,
							"constructing the measurement based on new format by Kaung");
					measurementStr = constructMeasurementStr(PREF_PO, -1, -1,
							-1, HR, SPO2, null);
				}
				
				/*
				 * To send secure SMS or plain text
				 */
				if (secureSMS) {
					if (legacySMS) {
						Toast.makeText(getApplicationContext(), "Encryption not defined for legacy SMS format", Toast.LENGTH_SHORT);
						Log.e(TAG, "Encryption not defined for legacy SMS format");
						return;
					}

				// sendSMS(phoneNo, measurementStr);
				RSAPublicKeySpec publicKeySpec = MyKeyUtils
						.getRecipientsPublicKey(phoneNo,
								getApplicationContext());
				if (publicKeySpec == null) {
					// TODO: store the measurement pending to be sent and send it when key becomes available 
					pendingSMS = true;
					
					MyUtils.missingKeyAlert("Missing server's key, request now?", phoneNo , getApplicationContext());
					return;
				}
				smsSender
						.sendSecureSMS(getApplicationContext(), measurementStr); // add
																					// "gmstelehealth"
																					// to
																					// new
																					// format
																					// but
																					// have
																					// to
																					// check
																					// what
																					// to
																					// do
																					// with
																					// old
																					// format
				} else {
					if (legacySMS) {
						smsSender.sendPlainText(getApplicationContext(), measurementStr, "From ");
					} else {
						smsSender.sendPlainText(getApplicationContext(), measurementStr, "gmstelehealth ");
					}
				}
			}
		

	}

	public void onPublicKeyReceived(Intent i, String contactNum) {
		// SmsReceiver will try to trigger this
		Log.w(TAG, "server's key received");
		
		/*SharedPreferences prefs = getSharedPreferences(contactNum,
				Context.MODE_PRIVATE);
		String pubMod = prefs.getString(MyKeyUtils.PREF_PUBLIC_MOD,
				MyKeyUtils.DEFAULT_PREF);

		Log.w(TAG, "public modulus updated to " + pubMod);
		Toast.makeText(getApplicationContext(),
				"Ready to send secure messages to " + contactNum,
				Toast.LENGTH_LONG).show();*/

		// TODO check if there is a message pending to be sent and send it
		if (pendingSMS && !measurementStr.isEmpty() && smsSender != null) {
			smsSender.sendSecureSMS(getApplicationContext(), measurementStr);
		}
	}

	/*
	 * private BroadcastReceiver mSmsReceiver = new BroadcastReceiver() { //
	 * SharedPreferences private final String PREFS = "MyKeys"; private final
	 * String PREF_PUBLIC_MOD = "PublicModulus"; private final String
	 * PREF_PUBLIC_EXP = "PublicExponent"; private final String PREF_PRIVATE_MOD
	 * = "PrivateModulus"; private final String PREF_PRIVATE_EXP =
	 * "PrivateExponent";
	 * 
	 * // private final String PREF_PHONE_NUMBER = "PhoneNumber"; // private
	 * final String PREF_RECIPIENT_NUM = "PhoneNumber";
	 * 
	 * private final String DEFAULT_PREF = "";
	 * 
	 * // sms codes private final String KEY_EXCHANGE_CODE = "keyx"; private
	 * final String HEALTH_SMS = "gmstelehealth";
	 * 
	 * @Override public void onReceive(Context context, Intent intent) {
	 * //intent.putExtra(INTENT_SOURCE, "this comes from the sms receiver");
	 * 
	 * 
	 * // updating a sharedpreferences boolean value, hopefully the // activity
	 * can see the updated value after that SharedPreferences prefs =
	 * getSharedPreferences("prefs", Context.MODE_PRIVATE);
	 * SharedPreferences.Editor prefseditor = prefs.edit();
	 * prefseditor.putBoolean("receivedsms", true); prefseditor.commit();
	 * 
	 * //MainActivity.this.receivedBroadcast(intent);
	 * 
	 * Map<String, String> msg = retrieveMessages(intent);
	 * 
	 * Log.i(TAG, "we received " + msg.size() + " messages in total"); if (msg
	 * != null) { for (String sender : msg.keySet()) { String message =
	 * msg.get(sender);
	 * 
	 * Log.i(TAG, "message received is " + message);
	 * 
	 * handleMessage(message, sender, context, intent); } } }
	 * 
	 * private void handleMessage(String message, String sender, Context
	 * context, Intent i) { if (message.startsWith(KEY_EXCHANGE_CODE)) {
	 * Log.i(TAG, "message received is a key exchange message");
	 * handleKeyExchangeMsg(message, sender, context, i); } else if
	 * (message.startsWith(HEALTH_SMS)) { Log.i(TAG,
	 * "received a secure text message"); // TODO handle secure text message
	 * handleEncryptedMsg(message, sender, context); } else { Log.i(TAG,
	 * "Message not recognised, not doing anything"); } }
	 * 
	 * 
	 * the sender here is actually the recipient of future encrypted text
	 * messages the recipient's public key will be used to encrypt the future
	 * text messages so that the recipient can use his/ her private key to
	 * decrypt the messages upon receiving them
	 * 
	 * private void handleKeyExchangeMsg(String message, String sender, Context
	 * context, Intent i) { Toast.makeText(context,
	 * "got a key exchange message", Toast.LENGTH_LONG).show(); // call
	 * MainActivitiy //MainActivity.this.receivedBroadcast(i);
	 * 
	 * 
	 * // TODO get the modulus and exponent of the public key of the sender & //
	 * reconstruct the public key String contactNum = sender; String[] parts =
	 * message.split(" "); // expected structure of the key exchange message:
	 * "keyx modBase64Encoded expBase64Encoded" if (parts.length == 3) { String
	 * recipientPubModBase64Str = parts[1]; String recipientPubExpBase64Str =
	 * parts[2];
	 * 
	 * 
	 * ================================ for testing only - to be removed later
	 * 
	 * //
	 * verifyRecipientsPublicKey(recipientPubModBase64Str,recipientPubExpBase64Str
	 * , // context);
	 * 
	 * ================================
	 * 
	 * 
	 * byte[] recipientPubModBA = Base64.decode(recipientPubModBase64Str,
	 * Base64.DEFAULT); // TODO to decide whether to use NO_WRAP or NO_PADDING
	 * here byte[] recipientPubExpBA = Base64.decode(recipientPubExpBase64Str,
	 * Base64.DEFAULT); BigInteger recipientPubMod = new
	 * BigInteger(recipientPubModBA); BigInteger recipientPubExp = new
	 * BigInteger(recipientPubExpBA);
	 * 
	 * Log.i(TAG, "the recipient's public key modulus is " + recipientPubMod +
	 * " and exponent is " + recipientPubExp);
	 * 
	 * // TODO store the intended recipient's public key in the app's //
	 * SharedPreferences SharedPreferences prefs =
	 * context.getSharedPreferences(contactNum, Context.MODE_PRIVATE);
	 * SharedPreferences.Editor prefsEditor = prefs.edit();
	 * 
	 * prefsEditor.putString(PREF_PUBLIC_MOD, recipientPubModBase64Str);
	 * prefsEditor.putString(PREF_PUBLIC_EXP, recipientPubExpBase64Str); //
	 * prefsEditor.putString(PREF_PHONE_NUMBER, recipient);
	 * prefsEditor.commit();
	 * 
	 * Log.i(TAG, "successfully remembered the contact " + contactNum +
	 * " and its public key module " + prefs.getString(PREF_PUBLIC_MOD,
	 * DEFAULT_PREF) + " and exponent " + prefs.getString(PREF_PUBLIC_EXP,
	 * PREF_PUBLIC_EXP)); Toast.makeText(context,
	 * "Got public key for "+contactNum, Toast.LENGTH_LONG).show();
	 * 
	 * 
	 * // TODO inform the UI Activity that public key is received
	 * MainActivity.this.onPublicKeyReceived(i, contactNum);
	 * 
	 * // TODO reload MainActivity so that it can read updated sharedpreferences
	 * Log.w(TAG, "restarting MainActivity"); Intent intent = new Intent();
	 * intent.setClassName("sg.edu.dukenus.securesms",
	 * "sg.edu.dukenus.securesms.MainActivity");
	 * intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
	 * context.startActivity(intent);
	 * 
	 * // TODO handle a pending list of message to be sent securely due to lack
	 * of key
	 * 
	 * } else { Log.e(TAG,
	 * "something is wrong with the key exchange message, it's supposed to have 3 parts: the code 'keyx', the modulus and the exponent"
	 * ); }
	 * 
	 * }
	 * 
	 * private void handleEncryptedMsg(String message, String sender, Context
	 * context) { String contactNum = sender; String[] parts =
	 * message.split(" "); if (parts.length == 2) {
	 * 
	 * // TODO get the private key of the intended recipient SharedPreferences
	 * prefs = context.getSharedPreferences( PREFS, Context.MODE_PRIVATE);
	 * 
	 * String privateMod = prefs.getString(PREF_PRIVATE_MOD, DEFAULT_PREF);
	 * String priavteExp = prefs.getString(PREF_PRIVATE_EXP, DEFAULT_PREF); //
	 * String recipient = prefs.getString(PREF_RECIPIENT_NUM, // DEFAULT_PREF);
	 * if (!privateMod.equals(DEFAULT_PREF) && !priavteExp.equals(DEFAULT_PREF))
	 * { byte[] recipientPrivateModBA = Base64.decode(privateMod,
	 * Base64.DEFAULT); byte[] recipientPrivateExpBA = Base64.decode(priavteExp,
	 * Base64.DEFAULT); BigInteger recipientPrivateMod = new BigInteger(
	 * recipientPrivateModBA); BigInteger recipientPrivateExp = new BigInteger(
	 * recipientPrivateExpBA); RSAPrivateKeySpec recipientPrivateKeySpec = new
	 * RSAPrivateKeySpec( recipientPrivateMod, recipientPrivateExp);
	 * 
	 * // TODO decrypt the encrypted message decryptMsg(parts[1],
	 * recipientPrivateKeySpec); } else { Log.e(TAG,
	 * "private key could not be retrieved"); } } else { Log.e(TAG,
	 * "message has incorrect format, it's suppose to be 'gmstelehealth [measurements]'"
	 * ); } }
	 * 
	 * private void decryptMsg(String msg, RSAPrivateKeySpec privateKey) { try {
	 * KeyFactory fact = KeyFactory.getInstance("RSA");
	 * 
	 * PrivateKey privKey = fact.generatePrivate(privateKey);
	 * 
	 * // TODO encrypt the message and send it // first decode the Base64
	 * encoded string to get the encrypted // message byte[] encryptedMsg =
	 * Base64.decode(msg, Base64.DEFAULT); Log.i(TAG, "We got a message: " + msg
	 * + " and after decode we got the encrypted message : " + new
	 * String(encryptedMsg));
	 * 
	 * Cipher cipher = Cipher.getInstance("RSA");
	 * cipher.init(Cipher.DECRYPT_MODE, privKey); // byte[] msgByteArray =
	 * msg.getBytes();
	 * 
	 * byte[] cipherData = cipher.doFinal(encryptedMsg);
	 * 
	 * String decryptedMsg = new String(cipherData); Log.i(TAG,
	 * "After decryption, we got the original message '" + decryptedMsg + "'");
	 * 
	 * } catch (NoSuchAlgorithmException e) { Log.e(TAG,
	 * "RSA algorithm not available", e); } catch (InvalidKeySpecException e) {
	 * Log.e(TAG, "", e); } catch (NoSuchPaddingException e) { Log.e(TAG, "",
	 * e); } catch (InvalidKeyException e) { Log.e(TAG, "", e); } catch
	 * (BadPaddingException e) { Log.e(TAG, "", e); } catch
	 * (IllegalBlockSizeException e) { Log.e(TAG, "", e); } }
	 * 
	 * private Map<String, String> retrieveMessages(Intent intent) { Map<String,
	 * String> msg = null; SmsMessage[] msgs = null; Bundle bundle =
	 * intent.getExtras();
	 * 
	 * if (bundle != null && bundle.containsKey("pdus")) { Object[] pdus =
	 * (Object[]) bundle.get("pdus");
	 * 
	 * if (pdus != null) { int nbrOfpdus = pdus.length; msg = new
	 * HashMap<String, String>(nbrOfpdus); msgs = new SmsMessage[nbrOfpdus];
	 * 
	 * // There can be multiple SMS from multiple senders, there can be // a
	 * maximum of nbrOfpdus different senders // However, send long SMS of same
	 * sender in one message for (int i = 0; i < nbrOfpdus; i++) { msgs[i] =
	 * SmsMessage.createFromPdu((byte[]) pdus[i]);
	 * 
	 * String originatinAddress = msgs[i].getOriginatingAddress();
	 * 
	 * // Check if index with number exists if
	 * (!msg.containsKey(originatinAddress)) { // Index with number doesn't
	 * exist // Save string into associative array with sender number // as
	 * index msg.put(msgs[i].getOriginatingAddress(), msgs[i].getMessageBody());
	 * 
	 * } else { // Number has been there, add content but consider that //
	 * msg.get(originatinAddress) already contains // sms:sndrNbr:previousparts
	 * of SMS, // so just add the part of the current PDU String previousparts =
	 * msg.get(originatinAddress); String msgString = previousparts +
	 * msgs[i].getMessageBody(); msg.put(originatinAddress, msgString); } } } }
	 * 
	 * return msg; } };
	 */

}
