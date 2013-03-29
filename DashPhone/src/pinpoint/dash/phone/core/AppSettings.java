package pinpoint.dash.phone.core;

import android.app.Application;
import android.content.Context;
import android.content.SharedPreferences;

public class AppSettings {
    
    /**
     * Exposes method to override default value of the preference
     * @author Alexey Pelykh
     *
     * @param <T> Type of preference value
     */
    protected interface AppPreferenceWithOverridableDefault<T> {
            /**
             * Overrides default value with given
             * @param newDefaultValue New default value
             */
            void overrideDefaultValue(T newDefaultValue);
    }
    
    public interface AppPreference<T> extends AppPreferenceWithOverridableDefault<T> {
            T get();
            
            boolean set(T obj);
            
            T getModeValue();
            
            String getId();
            
            void resetToDefault();
    }
    
    // These settings are stored in SharedPreferences
    private static final String SHARED_PREFERENCES_NAME = "pinpoint.dash.phone";
    
    /// Settings variables
    private final Application app;
    private SharedPreferences prefs;
    
    protected AppSettings(PhoneApplication application) {
        this.app = application;
        prefs = app.getSharedPreferences(SHARED_PREFERENCES_NAME, Context.MODE_WORLD_READABLE);
    }
    
    public static String getSharedPreferencesName() {
        return SHARED_PREFERENCES_NAME;
    }

    /*
    // Check internet connection available every 15 seconds
    public boolean isInternetConnectionAvailable(){
            return isInternetConnectionAvailable(false);
    }
    public boolean isInternetConnectionAvailable(boolean update){
            long delta = System.currentTimeMillis() - lastTimeInternetConnectionChecked;
            if(delta < 0 || delta > 15000 || update){
                    ConnectivityManager mgr = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
                    NetworkInfo active = mgr.getActiveNetworkInfo();
                    if(active == null){
                            internetConnectionAvailable = false;
                    } else {
                            NetworkInfo.State state = active.getState();
                            internetConnectionAvailable = state != NetworkInfo.State.DISCONNECTED && state != NetworkInfo.State.DISCONNECTING;
                    }
            }
            return internetConnectionAvailable;
    }
    */
    
    /////////////// PREFERENCES classes ////////////////
    
    public abstract class CommonPreference<T> implements AppPreference<T> {
        private final String id;
        private T cachedValue;
        private SharedPreferences cachedPreference;
        private boolean cache;
        private T defaultValue;
        
        public CommonPreference(String id, T defaultValue){
            this.id = id;
            this.defaultValue = defaultValue; 
        }
        
        public CommonPreference<T> cache(){
            cache = true;
            return this;
        }
        
        protected SharedPreferences getPreferences(){
            return prefs;
        }
        
        public void setModeDefaultValue(T defValue) {
            this.defaultValue = defValue;
        }
        
        protected T getDefaultValue(){
            return defaultValue;
        }
        
        @Override
        public void overrideDefaultValue(T newDefaultValue) {
            this.defaultValue = newDefaultValue;
        }
        
        protected abstract T getValue(SharedPreferences prefs, T defaultValue);
        
        protected abstract boolean setValue(SharedPreferences prefs, T val);
        
        @Override
        public T getModeValue() {
            return get();
        }

        @Override
        public T get() {
            if(cache && cachedValue != null && cachedPreference == getPreferences()){
                return cachedValue;
            }
            cachedPreference = getPreferences();
            cachedValue = getValue(cachedPreference, getDefaultValue());
            return cachedValue;
        }

        @Override
        public String getId() {
            return id;
        }
        
        @Override
        public void resetToDefault(){
            set(getDefaultValue());
        }

        @Override
        public boolean set(T obj) {
            SharedPreferences prefs = getPreferences();
            if(setValue(prefs,obj)){
                cachedValue = obj;
                cachedPreference = prefs;
                return true;
            }
            return false;
        }
    }
    
    private class BooleanPreference extends CommonPreference<Boolean> {
            
        private BooleanPreference(String id, boolean defaultValue) {
                super(id, defaultValue);
        }
        
        @Override
        protected Boolean getValue(SharedPreferences prefs, Boolean defaultValue) {
                return prefs.getBoolean(getId(), defaultValue);
        }

        @Override
        protected boolean setValue(SharedPreferences prefs, Boolean val) {
                return prefs.edit().putBoolean(getId(), val).commit();
        }
    }

    private class IntPreference extends CommonPreference<Integer> {

        private IntPreference(String id, int defaultValue) {
                super(id, defaultValue);
        }
        
        @Override
        protected Integer getValue(SharedPreferences prefs, Integer defaultValue) {
                return prefs.getInt(getId(), defaultValue);
        }

        @Override
        protected boolean setValue(SharedPreferences prefs, Integer val) {
                return prefs.edit().putInt(getId(), val).commit();
        }
    }

    private class FloatPreference extends CommonPreference<Float> {

        private FloatPreference(String id, float defaultValue) {
                super(id, defaultValue);
        }
        
        @Override
        protected Float getValue(SharedPreferences prefs, Float defaultValue) {
                return prefs.getFloat(getId(), defaultValue);
        }

        @Override
        protected boolean setValue(SharedPreferences prefs, Float val) {
                return prefs.edit().putFloat(getId(), val).commit();
        }

    }

    private class StringPreference extends CommonPreference<String> {
    
        private StringPreference(String id, String defaultValue) {
                super(id, defaultValue);
        }

        @Override
        protected String getValue(SharedPreferences prefs, String defaultValue) {
                return prefs.getString(getId(), defaultValue);
        }

        @Override
        protected boolean setValue(SharedPreferences prefs, String val) {
                return prefs.edit().putString(getId(), (val != null) ? val.trim() : val).commit();
        }
    }
    
    public final CommonPreference<String> SELECTED_DEVICE_NAME = new StringPreference("selected_device_name", null);

    public final CommonPreference<String> SELECTED_DEVICE_ADDRESS = new StringPreference("selected_device_address", null);
}
