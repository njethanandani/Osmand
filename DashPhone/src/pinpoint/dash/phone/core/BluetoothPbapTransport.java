package pinpoint.dash.phone.core;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import javax.obex.ObexTransport;

import android.bluetooth.BluetoothSocket;

public class BluetoothPbapTransport implements ObexTransport {
    private BluetoothSocket socket;
    
    public BluetoothPbapTransport(BluetoothSocket socket) {
        super();
        this.socket = socket;
    }
    
    @Override
    public void close() throws IOException {
        socket.close();
    }

    @Override
    public void connect() throws IOException {
    }

    @Override
    public void create() throws IOException {
    }

    @Override
    public void disconnect() throws IOException {
    }

    @Override
    public void listen() throws IOException {
    }

    @Override
    public DataInputStream openDataInputStream() throws IOException {
        return new DataInputStream(openInputStream());
    }

    @Override
    public DataOutputStream openDataOutputStream() throws IOException {
        return new DataOutputStream(openOutputStream());
    }

    @Override
    public InputStream openInputStream() throws IOException {
        return socket.getInputStream();
    }

    @Override
    public OutputStream openOutputStream() throws IOException {
        return socket.getOutputStream();
    }

}
