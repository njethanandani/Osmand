/*
 * Copyright (C) 2009 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package pinpoint.dash.phone.core;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import javax.obex.ApplicationParameter;
import javax.obex.ClientOperation;
import javax.obex.ClientSession;
import javax.obex.HeaderSet;
import javax.obex.ResponseCodes;

import pinpoint.dash.phone.activities.PhoneActivity;
import pinpoint.dash.phone.model.Contact;
import pinpoint.dash.phone.model.ContactPhoneData;
import pinpoint.dash.phone.model.ContactPhoneType;
import pinpoint.dash.vcard.VCardConfig;
import pinpoint.dash.vcard.VCardEntryCommitter;
import pinpoint.dash.vcard.VCardEntryConstructor;
import pinpoint.dash.vcard.VCardParser_V30;
import pinpoint.dash.vcard.exception.VCardException;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.ParcelUuid;
import android.provider.ContactsContract;
import android.util.Log;


/**
 * TODO(natashaj): Modifying the SDK BluetoothChat example to
 * work for us. Update comments accordingly.
 * 
 * This class does all the work for setting up and managing Bluetooth
 * connections with other devices. It has a thread that listens for
 * incoming connections, a thread for connecting with a device, and a
 * thread for performing data transmissions when connected.
 * 
 * Only instantiated if bluetooth adapter is available and enabled.
 */
public class BluetoothService {

    private static final String PBAP_UUID = "0000112f-0000-1000-8000-00805f9b34fb";
    private static final byte[] PBAP_TARGET = new byte[] { 0x79, 0x61, 0x35,
        (byte) 0xf0, (byte) 0xf0, (byte) 0xc5, 0x11, (byte) 0xd8, 0x09, 0x66,
        0x08, 0x00, 0x20, 0x0c, (byte) 0x9a, 0x66 };
    private static final String PBAP_PHONE_BOOK_PULL = "x-bt/phonebook";
    private static final String PBAP_PHONE_BOOK_NAME = "telecom/pb.vcf";
    private static final int PBAP_PHONE_BOOK_MAX_SIZE = 65535;

    // Member fields
    private final Context context;
    private final BluetoothAdapter btAdapter;
    private final Handler handler;
    private ConnectThread connectThread;
    private ConnectedThread connectedThread;
    private int state;
    private Set<BluetoothDevice> bondedDevices = null;
    private BluetoothDevice selectedDevice = null;
    private boolean contactsSynced = false;

    // Constants that indicate the current connection state
    public static final int STATE_NONE = 0;       // we're doing nothing
    public static final int STATE_CONNECTING = 1; // now initiating an outgoing connection
    public static final int STATE_CONNECTED = 2;  // now connected to a remote device

    private static final String vcardSample = "BEGIN:VCARD\r\n" +
"VERSION:3.0\r\n" +
"N:Doe;John;;;\r\n" +
"FN:John Doe\r\n" +
"EMAIL;type=INTERNET;type=WORK;type=pref:johnDoe@example.org\r\n" +
"TEL;type=WORK;type=pref:+1 617 555 1212\r\n" +
"TEL;type=CELL:+1 781 555 1212\r\n" +
"TEL;type=HOME:+1 202 555 1212\r\n" +
"TEL;type=WORK:+1 (617) 555-1234\r\n" +
"item1.ADR;type=WORK:;;2 Example Avenue;Anytown;NY;01111;USA\r\n" +
"item1.X-ABADR:us\r\n" +
"X-ABUID:5AD380FD-B2DE-4261-BA99-DE1D1DB52FBE\\:ABPerson\r\n" +
"END:VCARD";

    /**
     * Prepares a new bluetooth session.
     * @param context  The UI Activity Context
     * @param handler  A Handler to send messages back to the UI Activity
     */
    public BluetoothService(Context context, Handler handler, String selectedDeviceAddress) {
        this.context = context;
        this.btAdapter = BluetoothAdapter.getDefaultAdapter();
        this.state = STATE_NONE;
        this.handler = handler;
        this.bondedDevices = btAdapter.getBondedDevices();
        setSelectedDevice(findSelectedDevice(selectedDeviceAddress));
    }

    /**
     * Set the current state of the chat connection
     * @param state  An integer defining the current connection state
     */
    private synchronized void setState(int state) {
        Log.d(LogUtil.TAG, "setState() " + state + " -> " + state);
        this.state = state;

        // Give the new state to the Handler so the UI Activity can update
        handler.obtainMessage(PhoneActivity.MESSAGE_STATE_CHANGE, state, -1).sendToTarget();
    }

    /**
     * Return the current connection state. */
    public synchronized int getState() {
        return state;
    }

    /**
     * @return the current bonded (paired) devices.
     */
    public Set<BluetoothDevice> getBondedDevices() {
        return bondedDevices;
    }

    /**
     * @return the currently selected device.
     */
    public BluetoothDevice getSelectedDevice() {
        return selectedDevice;
    }

    /**
     * Set the currently selected device
     * @param device Currently connected device
     */
    public void setSelectedDevice(BluetoothDevice device) {
        this.selectedDevice = device;
        if (device != null) {
            // Attempt to connect to the device
            connect(device, false /*secure*/);
        }
        contactsSynced = false;
    }

    /*
    public List<Contact> getContacts() throws IOException {
        if (getSelectedDevice() == null) {
            return null;
        }
        
        transport = new BluetoothPbapTransport(socket);
        clientSession = new ClientSession(transport);

        HeaderSet requestConnection = new HeaderSet();
        requestConnection.setHeader(HeaderSet.TARGET, PBAP_UUID);
        HeaderSet returnHeader = clientSession.connect(requestConnection);
        // Check return header
        
        contactsSynced = true;
        return null;
    }
    */

    /**
     * Start the service. Specifically start AcceptThread to begin a
     * session in listening (server) mode. Called by the Activity onResume() */
    public synchronized void start() {
        Log.d(LogUtil.TAG, "start");

        // Cancel any thread attempting to make a connection
        if (connectThread != null) {connectThread.cancel(); connectThread = null;}

        // Cancel any thread currently running a connection
        if (connectedThread != null) {connectedThread.cancel(); connectedThread = null;}

        //setState(STATE_LISTEN);
    }

    /**
     * Start the ConnectThread to initiate a connection to a remote device.
     * @param device  The BluetoothDevice to connect
     * @param secure Socket Security type - Secure (true) , Insecure (false)
     */
    public synchronized void connect(BluetoothDevice device, boolean secure) {
        Log.d(LogUtil.TAG, "connect to: " + device);

        // Cancel any thread attempting to make a connection
        if (state == STATE_CONNECTING) {
            if (connectThread != null) {connectThread.cancel(); connectThread = null;}
        }

        // Cancel any thread currently running a connection
        if (connectedThread != null) {connectedThread.cancel(); connectedThread = null;}

        // Start the thread to connect with the given device
        connectThread = new ConnectThread(device, secure);
        connectThread.start();
        setState(STATE_CONNECTING);
    }

    /**
     * Start the ConnectedThread to begin managing a Bluetooth connection
     * @param socket  The BluetoothSocket on which the connection was made
     * @param device  The BluetoothDevice that has been connected
     */
    public synchronized void connected(BluetoothSocket socket, BluetoothDevice
            device, final String socketType) {
        Log.d(LogUtil.TAG, "connected, Socket Type:" + socketType);

        // Cancel the thread that completed the connection
        if (connectThread != null) {connectThread.cancel(); connectThread = null;}

        // Cancel any thread currently running a connection
        if (connectedThread != null) {connectedThread.cancel(); connectedThread = null;}

        // Start the thread to manage the connection and perform transmissions
        connectedThread = new ConnectedThread(socket, socketType);
        connectedThread.start();

        // Send the name of the connected device back to the UI Activity
        Message msg = handler.obtainMessage(PhoneActivity.MESSAGE_DEVICE_NAME);
        Bundle bundle = new Bundle();
        bundle.putString(PhoneActivity.DEVICE_NAME, device.getName());
        msg.setData(bundle);
        handler.sendMessage(msg);

        setState(STATE_CONNECTED);
    }

    /**
     * Stop all threads
     */
    public synchronized void stop() {
        Log.d(LogUtil.TAG, "stop");

        if (connectThread != null) {
            connectThread.cancel();
            connectThread = null;
        }

        if (connectedThread != null) {
            connectedThread.cancel();
            connectedThread = null;
        }

        setState(STATE_NONE);
    }

    /**
     * Write to the ConnectedThread in an unsynchronized manner
     * @param out The bytes to write
     * @see ConnectedThread#write(byte[])
     */
    public void write(byte[] out) {
        // Create temporary object
        ConnectedThread r;
        // Synchronize a copy of the ConnectedThread
        synchronized (this) {
            if (state != STATE_CONNECTED) return;
            r = connectedThread;
        }
        // Perform the write unsynchronized
        //r.write(out);
    }

    /**
     * Write to the ConnectedThread in an unsynchronized manner
     * @param out The bytes to write
     * @see ConnectedThread#write(byte[])
     */
    public List<Contact> getContacts() {
        ConnectedThread r;
        // Synchronize a copy of the ConnectedThread
        synchronized (this) {
            if (state != STATE_CONNECTED) return null;
            r = connectedThread;
        }
        return r.getContacts();
    }

    /**
     * Indicate that the connection attempt failed and notify the UI Activity.
     */
    private void connectionFailed() {
        // Send a failure message back to the Activity
        Message msg = handler.obtainMessage(PhoneActivity.MESSAGE_TOAST);
        Bundle bundle = new Bundle();
        bundle.putString(PhoneActivity.TOAST, "Unable to connect device");
        msg.setData(bundle);
        handler.sendMessage(msg);

        // Start the service over to restart listening mode
        BluetoothService.this.start();
    }

    /**
     * Indicate that the connection was lost and notify the UI Activity.
     */
    private void connectionLost() {
        // Send a failure message back to the Activity
        Message msg = handler.obtainMessage(PhoneActivity.MESSAGE_TOAST);
        Bundle bundle = new Bundle();
        bundle.putString(PhoneActivity.TOAST, "Device connection was lost");
        msg.setData(bundle);
        handler.sendMessage(msg);

        // Start the service over to restart listening mode
        BluetoothService.this.start();
    }

    private BluetoothDevice findSelectedDevice(String selectedDeviceAddress) {
        if (selectedDeviceAddress == null) {
            return null;
        }
        
        Set<BluetoothDevice> pairedDevices = getBondedDevices();
        if (pairedDevices == null) {
            return null;
        }

        for (BluetoothDevice btDevice : pairedDevices) {
            if (selectedDeviceAddress.equalsIgnoreCase(btDevice.getAddress())) {
                return btDevice;
            }
        }

        return null;
    }
    
    /**
     * This thread runs while attempting to make an outgoing connection
     * with a device. It runs straight through; the connection either
     * succeeds or fails.
     */
    private class ConnectThread extends Thread {
        private final BluetoothSocket mmSocket;
        private final BluetoothDevice mmDevice;
        private String mSocketType;

        public ConnectThread(BluetoothDevice device, boolean secure) {
            mmDevice = device;
            BluetoothSocket tmp = null;
            mSocketType = secure ? "Secure" : "Insecure";

            // Get a BluetoothSocket for a connection with the BluetoothDevice
            try {
                if (secure) {
                    tmp = device.createRfcommSocketToServiceRecord(ParcelUuid.fromString(PBAP_UUID).getUuid());
                } else {
                    tmp = device.createInsecureRfcommSocketToServiceRecord(ParcelUuid.fromString(PBAP_UUID).getUuid());
                }
            } catch (IOException e) {
                Log.e(LogUtil.TAG, "Socket Type: " + mSocketType + "create() failed", e);
            }
            mmSocket = tmp;
        }

        @Override
        public void run() {
            Log.d(LogUtil.TAG, "BEGIN mConnectThread SocketType:" + mSocketType);
            setName("ConnectThread" + mSocketType);

            // Always cancel discovery because it will slow down a connection
            btAdapter.cancelDiscovery();

            // Make a connection to the BluetoothSocket
            try {
                // This is a blocking call and will only return on a
                // successful connection or an exception
                mmSocket.connect();
            } catch (IOException e) {
                // Close the socket
                try {
                    mmSocket.close();
                } catch (IOException e2) {
                    Log.e(LogUtil.TAG, "unable to close() " + mSocketType +
                            " socket during connection failure", e2);
                }
                connectionFailed();
                return;
            }

            // Reset the ConnectThread because we're done
            synchronized (BluetoothService.this) {
                connectThread = null;
            }

            // Start the connected thread
            connected(mmSocket, mmDevice, mSocketType);
        }

        public void cancel() {
            try {
                mmSocket.close();
            } catch (IOException e) {
                Log.e(LogUtil.TAG, "close() of connect " + mSocketType + " socket failed", e);
            }
        }
    }

    /**
     * This thread runs during a connection with a remote device.
     * It handles all incoming and outgoing transmissions.
     */
    private class ConnectedThread extends Thread {
        private final BluetoothSocket socket;
        private InputStream inStream;
        private BluetoothPbapTransport transport = null;
        private ClientSession clientSession = null;

        /**
         * Headers Connection ID           Connection Identifier
         *         Name                    Object name (*.vcf)
         *         Type                    x-bt/phonebook
         *         Application parameters  Filter
         *                                 Format 
         *                                 MaxListCount 
         *                                 ListStartOffset
         */
        public ConnectedThread(BluetoothSocket socket, String socketType) {
            Log.d(LogUtil.TAG, "create ConnectedThread: " + socketType);
            this.socket = socket;
        }

        /**
         * Look up the device contact store to return the contacts already there.
         */
        // TODO(natashaj): Figure out how to sync contacts instead of adding new contacts every time
        // TODO(natashaj): Should contact sync be automatic? Or opt-in? Or a prompt?
        public List<Contact> getContacts() {
            List<Contact> contacts = new ArrayList<Contact>();

            Uri uri = ContactsContract.CommonDataKinds.Phone.CONTENT_URI;
            String[] projection    = new String[] {ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
                            ContactsContract.CommonDataKinds.Phone.NUMBER};

            Cursor people = context.getContentResolver().query(uri, projection, null, null, null);

            int indexName = people.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME);
            int indexNumber = people.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER);

            people.moveToFirst();
            do {
                Contact contact = new Contact(people.getString(indexName));
                List<ContactPhoneData> phones = new ArrayList<ContactPhoneData>();
                contact.setPhones(phones);
                ContactPhoneData phone = new ContactPhoneData();
                phone.setType(ContactPhoneType.MOBILE);
                phone.setNumber(people.getString(indexNumber));
                phones.add(phone);
                contacts.add(contact);
            } while (people.moveToNext());
            
            return contacts;
        }

        /*
         * Sync with the phone to update contacts. 
         * TODO: should this kick off a separate thread to sync with the phone in the background?
         * TODO: should this delete all local contacts and then update with phone contacts or
         *       do a smarter sync somehow?
         * TODO: should this be automatic? Currently plan on an opt-in option on the interface.
         */
        public List<Contact> syncContacts() {
            try {
                transport = new BluetoothPbapTransport(socket);
                clientSession = new ClientSession(transport);

                HeaderSet requestHeader = new HeaderSet();
                requestHeader.setHeader(HeaderSet.TARGET, PBAP_TARGET);

                clientSession.connect(requestHeader);
                Log.d(LogUtil.TAG, "OBEX session connected");
            } catch (IOException e) {
                Log.e(LogUtil.TAG, "OBEX session connection error", e);
                return null;
            }

            ClientOperation op = null;
            try {
                HeaderSet requestHeader = createPhoneBookPullHeader();
                op = (ClientOperation) clientSession.get(requestHeader);
                inStream = op.openInputStream(); // GET request is sent here
                if (op.getResponseCode() == ResponseCodes.OBEX_HTTP_OK) {
                    
                    parseContacts(inStream);
                    return getContacts();
                    /*
                    // Share the sent message back to the UI Activity
                    //mHandler.obtainMessage(PhoneActivity.MESSAGE_WRITE, -1, -1, buffer)
                    //        .sendToTarget();
                     * 
                     */
                } else {
                    Log.e(LogUtil.TAG, "Request failed. Response code: " + op.getResponseCode());
                    inStream = new ByteArrayInputStream(vcardSample.getBytes(Charset.defaultCharset()));
                    boolean success = parseContacts(inStream);
                    if (success) {
                        return getContacts();
                    }
                }
            } catch (IOException e) {
                Log.e(LogUtil.TAG, "Exception during read ", e);
                connectionLost();
            } finally {
                try {
                    if (op != null) {
                        op.close();
                    }
                    if (inStream != null) {
                        inStream.close();
                    }
                } catch (IOException e) {
                    Log.e(LogUtil.TAG, "Exception while closing client operation ", e);
                }
            }
            return null;
        }

        public void cancel() {
            try {
                socket.close();
            } catch (IOException e) {
                Log.e(LogUtil.TAG, "close() of connect socket failed", e);
            }
        }
        
        private byte[] getBytes(int value) {
            byte[] bytes = new byte[2];
            bytes[0] = (byte)((value / 256) & 0xff); // High value
            bytes[1] = (byte)((value % 256) & 0xff); // High value
            return bytes;
        }
        
        private HeaderSet createPhoneBookPullHeader() {
            HeaderSet requestHeader = new HeaderSet();
            requestHeader.setHeader(HeaderSet.TARGET, PBAP_TARGET);
            requestHeader.setHeader(HeaderSet.NAME, PBAP_PHONE_BOOK_NAME);
            requestHeader.setHeader(HeaderSet.TYPE, PBAP_PHONE_BOOK_PULL);

            ApplicationParameter appParams = new ApplicationParameter();
            appParams.addAPPHeader(
                    ApplicationParameter.TRIPLET_TAGID.PHONEBOOKSIZE_TAGID,
                    ApplicationParameter.TRIPLET_LENGTH.PHONEBOOKSIZE_LENGTH,
                    getBytes(PBAP_PHONE_BOOK_MAX_SIZE));
            appParams.addAPPHeader(
                    ApplicationParameter.TRIPLET_TAGID.FORMAT_TAGID,
                    ApplicationParameter.TRIPLET_LENGTH.FORMAT_LENGTH,
                    new byte[] { ApplicationParameter.TRIPLET_VALUE.FORMAT.VCARD_VERSION_30 });
            requestHeader.setHeader(HeaderSet.APPLICATION_PARAMETER, appParams.getAPPparam());

            return requestHeader;
        }

        private boolean parseContacts(InputStream is) {
            try {
                VCardParser_V30 parser = new VCardParser_V30();
                VCardEntryConstructor interpreter = new VCardEntryConstructor(VCardConfig.VCARD_TYPE_V30_GENERIC);
                VCardEntryCommitter committer = new VCardEntryCommitter(context.getContentResolver()); 
                interpreter.addEntryHandler(committer);
                parser.addInterpreter(interpreter);
                parser.parse(is);
                return true;
            } catch (VCardException ve) {
                Log.e(LogUtil.TAG, "VCardException while parsing vcards ", ve);
                return false;
            } catch (IOException ie) {
                Log.e(LogUtil.TAG, "IOException while parsing vcards ", ie);
                return false;
            } catch (Exception e) {
                Log.e(LogUtil.TAG, "Exception while parsing vcards ", e);
                return false;                
            }
        }
    }
}
