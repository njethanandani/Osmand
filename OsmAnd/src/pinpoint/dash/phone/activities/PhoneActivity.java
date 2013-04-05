package pinpoint.dash.phone.activities;

import java.util.List;
import java.util.Set;

import android.app.ActionBar;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.Window;
import android.widget.Toast;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.OsmandSettings;
import net.osmand.plus.R;
//import pinpoint.dash.phone.core.AppSettings;
import pinpoint.dash.phone.core.BluetoothService;
import pinpoint.dash.phone.core.LogUtil;
//import pinpoint.dash.phone.core.PhoneApplication;
import pinpoint.dash.phone.model.Contact;

// TODO(natashaj): Change to derive from net.osmand.access.AccessibleActivity
public class PhoneActivity extends Activity {

    private static final int REQUEST_ENABLE_BT = 1;
    
    // Message types sent from the BluetoothService Handler
    public static final int MESSAGE_STATE_CHANGE = 1;
    public static final int MESSAGE_READ = 2;
    public static final int MESSAGE_WRITE = 3;
    public static final int MESSAGE_DEVICE_NAME = 4;
    public static final int MESSAGE_TOAST = 5;

    // Key names received from the BluetoothChatService Handler
    public static final String DEVICE_NAME = "device_name";
    public static final String TOAST = "toast";

    private OsmandSettings settings = null;
    private BluetoothAdapter btAdapter = null;
    private BluetoothService btService = null;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        settings = ((OsmandApplication)getApplication()).getSettings();
        
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.phone_activity);

        btAdapter = BluetoothAdapter.getDefaultAdapter();

        // If the adapter is null, then Bluetooth is not supported and
        // activity is terminated.
        if (btAdapter == null) {
            Toast.makeText(this, R.string.bluetooth_not_supported, Toast.LENGTH_LONG).show();
            finish();
            return;
        }
    }

    @Override
    public void onStart() {
        super.onStart();
        Log.d(LogUtil.TAG, "ON START");

        setupBluetoothAdapter(true /*launchSystemSettings*/);
    }

    public void setupBluetoothAdapter(boolean launchSystemSettings) {
        if (!btAdapter.isEnabled()) {
            if (launchSystemSettings) {
                Intent enableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                startActivityForResult(enableIntent, REQUEST_ENABLE_BT);
            }
        } else {
            if (btService == null) setupBluetoothService();
        }
    }
    
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        
        if (requestCode == REQUEST_ENABLE_BT) {
            if (resultCode == Activity.RESULT_OK) {
                if (resultCode == Activity.RESULT_OK) {
                    setupBluetoothService();
                } else {
                    Log.d(LogUtil.TAG, "Bluetooth was not enabled");
                    Toast.makeText(this, R.string.bluetooth_not_enabled_leaving, Toast.LENGTH_SHORT).show();
                    finish();
                }
            }
        }
    }

    public Set<BluetoothDevice> getBondedDevices() {
        return btService.getBondedDevices();
    }
    
    public BluetoothDevice getSelectedDevice() {
        return btService.getSelectedDevice();
    }
    
    public void setSelectedDevice(BluetoothDevice device) {
        btService.setSelectedDevice(device);
        if (device == null) {
            settings.SELECTED_DEVICE_ADDRESS.set(null);
            settings.SELECTED_DEVICE_NAME.set(null);
        } else {
            settings.SELECTED_DEVICE_NAME.set(device.getName());
            settings.SELECTED_DEVICE_ADDRESS.set(device.getAddress());
        }
    }
    
    private void setupBluetoothService() {
        btService = new BluetoothService(this, mHandler, settings.SELECTED_DEVICE_ADDRESS.get());

        //findBondedDevices();
        //findSelectedDevice();

        // Populate fragment details depending on which tab the user is in
        //PhoneTabFragment tab = (PhoneTabFragment)
        //        getFragmentManager().findFragmentById(R.id.Details);
        //if (tab.getResource() == R.layout.phone_settings_fragment) {
        //    ((PhoneSettingsFragment)tab).showPairedDevices();
        //}
    }

    private final void setStatus(int resId) {
        final ActionBar actionBar = getActionBar();
        if (actionBar != null) {
            actionBar.setSubtitle(resId);
        }
    }

    private final void setStatus(CharSequence subTitle) {
        final ActionBar actionBar = getActionBar();
        if (actionBar != null) {
            actionBar.setSubtitle(subTitle);
        }
    }

    /*
    private Set<BluetoothDevice> findBondedDevices() {
        if (!btAdapter.isEnabled()) {
            return null;
        } else {
            bondedDevices = btAdapter.getBondedDevices();
            return bondedDevices;
        }
    }

    private Device findSelectedDevice() {
        String selectedPhoneAddress = settings.SELECTED_DEVICE_ADDRESS.get();
        if (selectedPhoneAddress == null) {
            return null;
        }
        
        Set<BluetoothDevice> pairedDevices = btService.getBondedDevices();
        if (pairedDevices == null) {
            return null;
        }

        for (BluetoothDevice btDevice : pairedDevices) {
            if (selectedPhoneAddress.equalsIgnoreCase(btDevice.getAddress())) {
                Device deviceFound = new Device(btDevice);
                setSelectedDevice(deviceFound);
                return deviceFound;
            }
        }

        return null;
    }
    */

    public List<Contact> getContacts() {
        return btService.getContacts();
        
        //contactsSynced = true;
        //return null;
    }

    // Handler that gets information back from the BluetoothService
    private final Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
            case MESSAGE_STATE_CHANGE:
                Log.d(LogUtil.TAG, "MESSAGE_STATE_CHANGE: " + msg.arg1);
                switch (msg.arg1) {
                case BluetoothService.STATE_CONNECTED:
                    setStatus(getString(R.string.title_connected_to, getSelectedDevice().getName()));
                    //mConversationArrayAdapter.clear();
                    break;
                case BluetoothService.STATE_CONNECTING:
                    setStatus(R.string.title_connecting);
                    break;
                //case BluetoothService.STATE_LISTEN:
                case BluetoothService.STATE_NONE:
                    setStatus(R.string.title_not_connected);
                    break;
                }
                break;
            /*
            case MESSAGE_WRITE:
                byte[] writeBuf = (byte[]) msg.obj;
                // construct a string from the buffer
                String writeMessage = new String(writeBuf);
                mConversationArrayAdapter.add("Me:  " + writeMessage);
                break;
            case MESSAGE_READ:
                byte[] readBuf = (byte[]) msg.obj;
                // construct a string from the valid bytes in the buffer
                String readMessage = new String(readBuf, 0, msg.arg1);
                mConversationArrayAdapter.add(mConnectedDeviceName+":  " + readMessage);
                break;
            case MESSAGE_DEVICE_NAME:
                // save the connected device's name
                mConnectedDeviceName = msg.getData().getString(DEVICE_NAME);
                Toast.makeText(getApplicationContext(), "Connected to "
                               + mConnectedDeviceName, Toast.LENGTH_SHORT).show();
                break;
            */
            case MESSAGE_TOAST:
                Toast.makeText(getApplicationContext(), msg.getData().getString(TOAST),
                               Toast.LENGTH_SHORT).show();
                break;
            }
        }
    };
}
