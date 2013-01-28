package pinpoint.dash.phone.activities;

import java.util.Set;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.os.Bundle;
import android.view.Window;
import pinpoint.dash.phone.R;
import pinpoint.dash.phone.fragments.PhoneSettingsFragment;
import pinpoint.dash.phone.fragments.TabFragment;
import pinpoint.dash.phone.model.Phone;

// TODO(natashaj): Change to derive from net.osmand.access.AccessibleActivity
public class PhoneActivity extends Activity {

    private static final int REQUEST_ENABLE_BT = 1;
    
    private Phone selectedPhone = null;
    private BluetoothAdapter btAdapter = null;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.phone_activity);
        
        setupBluetoothAdapter(false /*launchSystemSettings*/);
    }

    public void setupBluetoothAdapter(boolean launchSystemSettings) {
        if (btAdapter == null) {
            btAdapter = BluetoothAdapter.getDefaultAdapter();
        }

        if (btAdapter != null && !btAdapter.isEnabled() && launchSystemSettings) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        
        if (requestCode == REQUEST_ENABLE_BT) {
            if (resultCode == Activity.RESULT_OK) {
                TabFragment tab = (TabFragment)
                        getFragmentManager().findFragmentById(R.id.Details);
                if (tab.getResource() == R.layout.phone_settings_fragment) {
                    ((PhoneSettingsFragment)tab).showPairedDevices();
                }
            }
        }
    }
    
    public Set<BluetoothDevice> findPairedDevices() {
        if (btAdapter == null || !btAdapter.isEnabled()) {
            return null;
        } else {
            return btAdapter.getBondedDevices();
        }
    }

    public Phone getSelectedPhone() {
        return selectedPhone;
    }
    
    public void setSelectedPhone(Phone phone) {
        this.selectedPhone = phone;
    }
    
}
