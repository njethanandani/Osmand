package pinpoint.dash.phone.core;

import android.app.Application;

public class PhoneApplication extends Application {
    
    private static final org.apache.commons.logging.Log LOG = LogUtil.getLog(PhoneApplication.class);

    AppSettings appSettings = null;

    @Override
    public void onCreate() {
        super.onCreate();
        appSettings = createAppSettings();
    }
    
    private AppSettings createAppSettings() {
        return new AppSettings(this);
    }

    public AppSettings getSettings() {
        if (appSettings == null) {
            LOG.error("Trying to access settings before they were created");
        }
        return appSettings;
    }
}
