package pinpoint.dash.phone.fragments;

import java.util.Set;

import android.app.Activity;
import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import pinpoint.dash.phone.R;
import pinpoint.dash.phone.activities.PhoneActivity;
import pinpoint.dash.phone.fragments.TabFragment;
import pinpoint.dash.phone.model.Phone;

public class PhoneSettingsFragment extends TabFragment {
    
    private static final int REQUEST_BT_SETTINGS = 1;
    
    private TextView messageLabel;
    private ListView listView;
    private ArrayAdapter<Phone> devicesArrayAdapter;

    public PhoneSettingsFragment() {
    	super(PhoneMenuFragment.SETTINGS_TAB);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        View view = inflater.inflate(getResource(), container, false);
        initViews(view);
        return view;
    }
    
    @Override
    public void onStart() {
        super.onStart();
        getPhoneActivity().setupBluetoothAdapter(true /*launchSystemSettings*/);
        showPairedDevices();
    }
    
    /*
    @Override
    public void onResume() {
        super.onResume();
        checkSelectedPhone(getPhoneActivity().getSelectedPhone());
    }
    */

    @Override
    public int getResource() {
        return R.layout.phone_settings_fragment;
    }

    public void checkSelectedPhone(Phone selectedPhone) {
        if (selectedPhone != null) {
            int position = devicesArrayAdapter.getPosition(selectedPhone);
            listView.setItemChecked(position, true);
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        
        if (requestCode == REQUEST_BT_SETTINGS) {
            if (resultCode == Activity.RESULT_OK) {
                getPhoneActivity().setupBluetoothAdapter(false /*launchSystemSettings*/);
                showPairedDevices();
            }
        }
    }
    
    public void showPairedDevices() {
        devicesArrayAdapter.clear();

        Set<BluetoothDevice> pairedDevices = getPhoneActivity().findPairedDevices();
        if (pairedDevices == null) {
            return;
        }

        boolean prevSelectedPhoneFound = false;
        Phone selectedPhone = getPhoneActivity().getSelectedPhone();
        if (pairedDevices.size() == 0) {
            messageLabel.setText(R.string.bluetooth_no_phones_found);
        } else {
            for (BluetoothDevice device : pairedDevices) {
                Phone phone = new Phone(device.getName(), device.getAddress());
                // Add the name and address to an array adapter to show in a ListView
                devicesArrayAdapter.add(phone);
                if (!prevSelectedPhoneFound
                        && selectedPhone != null
                        && selectedPhone.getAddress().equalsIgnoreCase(device.getAddress())) {
                    prevSelectedPhoneFound = true;
                    messageLabel.setText(null);
                }
            }

            // If previously selected phone is no longer paired, clear the setting.
            if (prevSelectedPhoneFound) {
                checkSelectedPhone(selectedPhone);
            } else {
                if (selectedPhone != null) {
                    getPhoneActivity().setSelectedPhone(null);
                }
            }
        }
    }
    
    private PhoneActivity getPhoneActivity() {
        return (PhoneActivity) getActivity();
    }
    
    private void initViews(View view) {
        messageLabel = (TextView) view.findViewById(R.id.phoneSettingsMessage);
        
        Button settingsButton = (Button) view.findViewById(R.id.bluetoothSettingsButton);
        settingsButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intentBluetooth = new Intent();
                intentBluetooth.setAction(android.provider.Settings.ACTION_BLUETOOTH_SETTINGS);
                startActivityForResult(intentBluetooth, REQUEST_BT_SETTINGS); 
            }
        });
        
        devicesArrayAdapter = new ArrayAdapter<Phone>(this.getActivity(),
                android.R.layout.simple_list_item_single_choice);
        listView = (ListView) view.findViewById(R.id.deviceList);
        listView.setAdapter(devicesArrayAdapter);
        listView.setOnItemClickListener(new OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view,
                    int position, long id) {
                Phone selectedPhone = devicesArrayAdapter.getItem(position);
                ((PhoneActivity)getActivity()).setSelectedPhone(selectedPhone);
            }
        });
    }

}