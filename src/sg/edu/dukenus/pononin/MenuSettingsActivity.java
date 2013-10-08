package sg.edu.dukenus.pononin;

import org.projectproto.yuscope.R;

import android.os.Bundle;
import android.preference.PreferenceActivity;
import android.util.Log;

public class MenuSettingsActivity extends PreferenceActivity{
	@Override
	protected void onCreate(Bundle savedInstanceState){
		super.onCreate(savedInstanceState);
		addPreferencesFromResource(R.xml.settings);
		Log.v("SharedPreferencesName",this.getPreferenceManager().getSharedPreferencesName());
	}
}
