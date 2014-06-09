package se.sics.ah3.splash;

import se.sics.ah3.AHState;
import se.sics.ah3.AffectiveHealthActivity;
import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;

/**
 * Splash screen activity
 * TODO: Move initialitation of time consuming procedures here
 * @version 1
 * @author mareri
 *
 */

public class Splash extends Activity {
	// time set low for development purposes
	private static int SPLASH_DISPLAY_TIME = 10;
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
        setContentView(se.sics.ah3.R.layout.splash);
        
        // spawn the launcher view after splash display
        new Handler().postDelayed(new Runnable() {
			public void run() {
//		        AHState state = AHState.getInstance();
//		        state.init(Splash.this);

				Intent intent = new Intent(Splash.this,
						AffectiveHealthActivity.class);
				Splash.this.startActivity(intent);
				Splash.this.finish();
			}
		}, SPLASH_DISPLAY_TIME);
	}
	
}
