package cloudchen.com.sunshine.sync;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;

/**
 * Created by Cloud on 8/16/14.
 */
public class SunshineAuthenticatorService extends Service {

    SunshineAuthenticator sunshineAuthenticator;

    public SunshineAuthenticatorService() {
        sunshineAuthenticator = new SunshineAuthenticator(this);
    }

    @Override
    public IBinder onBind(Intent intent) {
        return sunshineAuthenticator.getIBinder();
    }
}
