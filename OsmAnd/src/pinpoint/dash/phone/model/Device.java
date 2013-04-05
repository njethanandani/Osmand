package pinpoint.dash.phone.model;

import android.bluetooth.BluetoothDevice;

public class Device {
    private String name;
    private String address;
    private BluetoothDevice btDevice;
    
    public Device(BluetoothDevice btDevice) {
        setName(btDevice.getName());
        setAddress(btDevice.getAddress());
        setBluetoothDevice(btDevice);
    }
    
    public String getName() {
        return name;
    }
    
    public void setName(String name) {
        this.name = name;
    }
    
    public String getAddress() {
        return address;
    }
    
    public void setAddress(String address) {
        this.address = address;
    }
    
    public BluetoothDevice getBluetoothDevice() {
        return btDevice;
    }
    
    public void setBluetoothDevice(BluetoothDevice btDevice) {
        this.btDevice = btDevice;
    }
    
    @Override
    public String toString() {
        return getName() + "\n" + getAddress();
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        
        if (!(o instanceof Device)) {
            return false;
        }
        
        Device other = (Device) o;
        return (getName() == null ? other.getName() == null
                : getName().equals(other.getName())) &&
               (getAddress() == null ? other.getAddress() == null
                : getAddress().equals(other.getAddress()));
    }
}
