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
import net.osmand.plus.R;
import pinpoint.dash.phone.activities.PhoneActivity;
import pinpoint.dash.phone.fragments.PhoneTabFragment;
import pinpoint.dash.phone.model.Device;

public class PhoneSettingsFragment extends PhoneTabFragment {
    
    private static final int REQUEST_BT_SETTINGS = 1;
    
    private TextView messageLabel;
    private ListView listView;
    private ArrayAdapter<Device> devicesArrayAdapter;

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
        //getPhoneActivity().setupBluetoothAdapter(true /*launchSystemSettings*/);
        showPairedDevices();
    }
    
    @Override
    public int getResource() {
        return R.layout.phone_settings_fragment;
    }

    public void checkSelectedPhone(BluetoothDevice selectedPhone) {
        if (selectedPhone != null) {
            for (int i = 0; i < devicesArrayAdapter.getCount(); i++) {
                Device curr = devicesArrayAdapter.getItem(i);
                if (curr.getAddress() == selectedPhone.getAddress()) {
                    listView.setItemChecked(i, true);
                    return;
                }
            }
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

        Set<BluetoothDevice> bondedDevices = getPhoneActivity().getBondedDevices();
        if (bondedDevices == null || bondedDevices.size() == 0) {
            messageLabel.setText(R.string.bluetooth_no_phones_found);
            return;
        }

        boolean selectedDeviceFound = false;
        int deviceIndex = 0;
        BluetoothDevice selectedDevice = getPhoneActivity().getSelectedDevice();
        for (BluetoothDevice btDevice : bondedDevices) {
            devicesArrayAdapter.add(new Device(btDevice));
            if (!selectedDeviceFound
                    && selectedDevice != null
                    && selectedDevice.getAddress().equalsIgnoreCase(btDevice.getAddress())) {
                selectedDeviceFound = true;
                listView.setItemChecked(deviceIndex, true);
                messageLabel.setText(null);
            }
            deviceIndex++;
        }

        if (!selectedDeviceFound) {
            getPhoneActivity().setSelectedDevice(null);
        }
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
        
        devicesArrayAdapter = new ArrayAdapter<Device>(this.getActivity(),
                android.R.layout.simple_list_item_single_choice);
        listView = (ListView) view.findViewById(R.id.deviceList);
        listView.setAdapter(devicesArrayAdapter);
        listView.setOnItemClickListener(new OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view,
                    int position, long id) {
                Device selectedDevice = devicesArrayAdapter.getItem(position);
                ((PhoneActivity)getActivity()).setSelectedDevice(selectedDevice.getBluetoothDevice());
            }
        });
    }

}