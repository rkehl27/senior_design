package srdesign.scoreboardcontrollerv1;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.UUID;

/**
 * Created by rebeccakehl on 4/1/15.
 */
class BluetoothService {
    public static final int STATE_NONE = 0;
    public static final int STATE_LISTEN = 1;
    public static final int STATE_CONNECTING = 2;
    public static final int STATE_CONNECTED = 3;
    private final UUID SerialPortServiceClass_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
    private final BluetoothAdapter mAdapter;
    private final Handler mHandler;
    private ConnectThread mConnectThread;
    private ConnectedThread mConnectedThread;
    private int mState;
    private final Context mContext;

    public BluetoothService(Context context, Handler handler) {
        mAdapter = BluetoothAdapter.getDefaultAdapter();
        mContext = context;
        mHandler = handler;
        mState = STATE_NONE;
    }

    private synchronized void setState(int state) {
        mState = state;

        //Pass new state to handler to post message
        //TODO: HANDLE STATE CHANGE MESSAGES
        mHandler.obtainMessage(ScoringActivity.MESSAGE_STATE_CHANGE, state, -1).sendToTarget();
    }

    public synchronized void start() {
        if (mConnectThread != null) {
            mConnectThread.cancel();
            mConnectThread = null;
        }

        if (mConnectedThread != null) {
            mConnectedThread.cancel();
            mConnectedThread = null;
        }

        setState(STATE_NONE);
    }

    public synchronized void connect(BluetoothDevice device) {
        // Cancel any thread attempting to make a connection
        if (mState == STATE_CONNECTING) {
            if (mConnectThread != null) {
                mConnectThread.cancel();
                mConnectThread = null;
            }
        }

        // Cancel any thread currently running a connection
        if (mConnectedThread != null) {
            mConnectedThread.cancel();
            mConnectedThread = null;
        }

        // Start the thread to connect with the given device
        mConnectThread = new ConnectThread(device);
        mConnectThread.start();
        setState(STATE_CONNECTING);
    }

    synchronized void connected(BluetoothSocket socket) {
        if (mConnectThread != null) {
            mConnectThread.cancel();
            mConnectThread = null;
        }

        if (mConnectedThread != null) {
            mConnectedThread.cancel();
            mConnectedThread = null;
        }

        mConnectedThread = new ConnectedThread(socket);
        mConnectedThread.start();

        setState(STATE_CONNECTED);
    }

    public synchronized void stop() {
//        isCancelling = true;
        if (mConnectThread != null) {
            mConnectThread.cancel();
            mConnectThread = null;
        }

        if (mConnectedThread != null) {
            mConnectedThread.cancel();
            mConnectedThread = null;
        }

        setState(STATE_NONE);
    }

    public void write(String out) {
        ConnectedThread thread;

        synchronized (this) {
            if (mState != STATE_CONNECTED) {
            } else {
                thread = mConnectedThread;
                thread.write(out);
            }
        }
    }

//    public String read() {
//        ConnectedThread thread;
//
//        String response = "";
//        synchronized (this) {
//            if (mState != STATE_CONNECTED) {
//                return null;
//            } else {
//                thread = mConnectedThread;
//                thread.read();
//                return response;
//            }
//        }
//    }

    private void connectionFailed() {
        setState(STATE_NONE);

        //TODO: HANDLE CONNECTION FAILED
        Message msg = mHandler.obtainMessage(ScoringActivity.MESSAGE_TOAST);
        Bundle bundle = new Bundle();
        bundle.putString("toast", mContext.getString(R.string.toast_unable_to_connect));
        msg.setData(bundle);
        mHandler.sendMessage(msg);
    }

    private void connectionLost() {
        setState(STATE_NONE);

        Message msg = mHandler.obtainMessage(ScoringActivity.MESSAGE_TOAST);
        Bundle bundle = new Bundle();

        bundle.putString("toast", mContext.getString(R.string.toast_connection_lost));

        msg.setData(bundle);
        mHandler.sendMessage(msg);
    }

    private class ConnectThread extends Thread {
        private final BluetoothSocket mmSocket;

        public ConnectThread(BluetoothDevice device) {
            BluetoothSocket temp = null;

            try {
                temp = device.createRfcommSocketToServiceRecord(SerialPortServiceClass_UUID);
            } catch (IOException e) {
                e.printStackTrace();
            }

            mmSocket = temp;
        }

        public void run() {
            mAdapter.cancelDiscovery();

            try {
                mmSocket.connect();
            } catch (IOException e) {
                connectionFailed();
                try {
                    mmSocket.close();
                } catch (IOException e1) {
                    e1.printStackTrace();
                }
                return;
            }

            synchronized (BluetoothService.this) {
                mConnectThread = null;
            }

            connected(mmSocket);
        }

        public void cancel() {
            try {
                mmSocket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

    }

    private class ConnectedThread extends Thread {
        private final BluetoothSocket mmSocket;
        private final InputStream mmInStream;
        private final OutputStream mmOutStream;

        public ConnectedThread(BluetoothSocket socket) {
            mmSocket = socket;
            InputStream tempIn = null;
            OutputStream tempOut = null;

            try {
                tempIn = socket.getInputStream();
                tempOut = socket.getOutputStream();
            } catch (IOException e) {
                e.printStackTrace();
            }

            mmInStream = tempIn;
            mmOutStream = tempOut;
        }

        public void run() {
            byte[] buffer = new byte[12];
            int bytes;
            while (true) {
                try {
                    bytes = mmInStream.read(buffer);

                    mHandler.obtainMessage(ScoringActivity.MESSAGE_READ, bytes, 0, buffer)
                            .sendToTarget();

                    //TODO: Handle read in bytes
                } catch (IOException e) {
                    e.printStackTrace();
                    connectionLost();
                    break;
                }
            }
        }
//
//        public String read() {
//            InputStreamReader inputStreamReader = new InputStreamReader(mmInStream);
//            BufferedReader reader = new BufferedReader(inputStreamReader);
//            try {
//                String input = reader.readLine();
//                Log.v("Input", input);
//                reader.close();
//                inputStreamReader.close();
//                return input;
//            } catch (IOException e) {
//                e.printStackTrace();
//            }
//            return null;
//        }

        public void write(String string) {
            byte[] bytes = string.getBytes();
            try {
                mmOutStream.write(bytes);
//                mHandler.obtainMessage(ScoringActivity.MESSAGE_WRITE, buffer.length, -1, buffer)
//                        .sendToTarget();
                //TODO: Handle message sent
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        public void cancel() {
            try {
                mmSocket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

}
