package srdesign.scoreboardcontrollerv1;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.CountDownTimer;
import android.os.Handler;
import android.provider.Settings;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Method;
import java.util.Set;
import java.util.UUID;

public class MainActivity extends ActionBarActivity {

    final Context context = this;
    private ToggleButton startStopButton;
    private Button resetButton;

    private TextView timerValueView;

    private long previousTimerValue = 00;
    private long timerValue = 00;

    private CountDownTimer countDownTimer;

    private boolean isPaired = false;
    private boolean isConnected = false;

    private BluetoothAdapter bluetoothAdapter;
    private BluetoothDevice bluetoothDevice;

    private ConnectThread connectThread;
    private ConnectedThread connectedThread;

    private BluetoothSocket btSocket;
    private OutputStream btOutputStream;

//    private Button connectionButton;
//    private Button pairingButton;

    private MenuItem connectionMenu;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_scoring);

        initializeAdapter();

        timerValueView = (TextView) findViewById(R.id.timerValue);
        timerValueView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                final Dialog dialog = new Dialog(context);
                dialog.setContentView(R.layout.dialog_timer);
                dialog.setTitle("Set Timer");
                dialog.getWindow().setLayout(425,400);

                Button dialogButton = (Button) dialog.findViewById(R.id.button);
                dialogButton.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        EditText minutesView = (EditText) dialog.findViewById(R.id.minutesField);
                        String minutesString = minutesView.getText().toString();
                        long minutes = 0;
                        if (minutesString.length() > 0) {
                            minutes = Long.parseLong(minutesString);
                        }

                        EditText secondsView = (EditText) dialog.findViewById(R.id.secondsField);
                        String secondsString = secondsView.getText().toString();
                        long seconds = 0;
                        if (secondsString.length() > 0){
                            seconds = Long.parseLong(secondsString);
                        }

                        timerValue = (minutes*60 + seconds)*1000; //Convert to milliseconds
                        previousTimerValue = timerValue;

                        updateCounterView();
                        dialog.dismiss();
                    }
                });
                dialog.show();
            }
        });

        startStopButton = (ToggleButton) findViewById(R.id.startStopButton);
        startStopButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (startStopButton.isChecked()) {
                    //Timer is running
                    initializeCountDownTimer();
                    countDownTimer.start();
                } else {
                    //Timer is paused
                    countDownTimer.cancel();
                }
            }
        });

        resetButton = (Button) findViewById(R.id.resetButton);
        resetButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (startStopButton.isChecked()){
                    //Timer is Running
                    startStopButton.toggle();
                }
                countDownTimer.cancel();
                timerValue = previousTimerValue;
                initializeCountDownTimer();
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();

        initializeAdapter();
    }

    private void clickedBluetoothButton() {
        if (!isPaired) {
            AlertDialog.Builder builder = new AlertDialog.Builder(context);
            builder.setMessage("Please pair with HC-06 Then Return");
            builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    Intent intentBluetooth = new Intent();
                    intentBluetooth.setAction(Settings.ACTION_BLUETOOTH_SETTINGS);
                    startActivityForResult(intentBluetooth, 0);
                    dialog.dismiss();
                }
            });
            builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    dialog.cancel();
                }
            });
            AlertDialog alertDialog = builder.create();

            alertDialog.show();
        } else if (!isConnected) {
            initializeConnection();
        }
    }

    private void initializeAdapter() {
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

        if (!bluetoothAdapter.isEnabled()) {
            Intent enableBluetooth = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBluetooth, 0);
        }

        Set<BluetoothDevice> pairedDevices = bluetoothAdapter.getBondedDevices();

        if (pairedDevices.size() > 0) {
            for (BluetoothDevice device : pairedDevices) {
                if (device.getName().equals("HC-06")) {
                    bluetoothDevice = device;
                    isPaired = true;
                    break;
                }
            }
        }
    }

//    private void initializeConnection() {
//        if (connectThread != null) {
//            connectThread.cancel();
//            connectThread = null;
//        }
//
//        if (connectedThread != null) {
//            connectedThread.cancel();
//            connectedThread = null;
//        }
//
//        connectThread = new ConnectThread(bluetoothDevice);
//        connectThread.start();
//    }

    private void initializeConnection() {
        UUID uuid = UUID.fromString("00001101-0000-1000-8000-00805f9b34fb");
        try {
            btSocket = bluetoothDevice.createRfcommSocketToServiceRecord(uuid);
            btSocket.connect();

            btOutputStream = btSocket.getOutputStream();


        } catch (IOException e) {
            e.printStackTrace();
        }

        if (btOutputStream != null) {
            isConnected = true;
//            connectionButton.setEnabled(false);
            sendMessageToDuino("@");
        }
    }

    private void sendMessageToDuino(String message) {
        byte[] bytes = message.getBytes();

        try {
            btOutputStream.write(bytes);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void updateCounterView() {
        int seconds = (int) (timerValue/1000);
        int minutes = seconds/60;
        seconds = seconds % 60;

        String timerString = "";

        if (seconds < 10) {
            timerValueView.setText("" + minutes + ":0" + seconds);
            if (minutes < 10) {
                timerString = "0" + minutes + "0" + seconds;
            } else {
                timerString = minutes + "0" + seconds;
            }
        } else {
            timerValueView.setText("" + minutes + ":" + seconds);
            if (minutes < 10) {
                timerString = "0" + minutes + seconds;
            } else {
                timerString = "" + minutes + seconds;
            }
        }

        if (isConnected) {
            connectedThread.write("F" + timerString);
        }
    }

    private void initializeCountDownTimer() {
        updateCounterView();
        countDownTimer = new CountDownTimer(timerValue, 500) {
            @Override
            public void onTick(long millisUntilFinished) {
                timerValue = millisUntilFinished;

                updateCounterView();
            }

            @Override
            public void onFinish() {
                timerValueView.setText("0:00");
                startStopButton.toggle();
            }
        };
    }

    Handler connectionHandler = new Handler();
    Runnable connectionFailRunnable = new Runnable() {
        public void run() {
            Toast.makeText(context, "Move Closer To Scoreboard", Toast.LENGTH_LONG).show();
        }
    };

    Runnable connectionSuccessRunnable = new Runnable() {
        @Override
        public void run() {
            Toast.makeText(context, "Success!!", Toast.LENGTH_LONG).show();
            updateConnectionStatus();
        }
    };

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_scoring, menu);
        connectionMenu = menu.findItem(R.id.action_connect);
        updateConnectionStatus();
        return true;
    }

    private void updateConnectionStatus() {
        if (isConnected) {
            connectionMenu.setIcon(R.drawable.ic_action_bluetooth_connected);
        } else if (isPaired) {
            connectionMenu.setIcon(R.drawable.ic_action_bluetooth_searching);
        } else {
            connectionMenu.setIcon(R.drawable.ic_action_bluetooth);
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        if (id == R.id.action_connect) {
            clickedBluetoothButton();
        }

        return super.onOptionsItemSelected(item);
    }

    private class ConnectThread extends Thread {
        private final BluetoothSocket btSocket;
        private final BluetoothDevice btDevice;

        public ConnectThread(BluetoothDevice device) {
            BluetoothSocket tmp = null;
            btDevice = device;

            UUID uuid = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
            try {
//                Method method;
//                method = device.getClass().getMethod("createRfcommSocket", new Class[] { int.class } );
//                tmp = (BluetoothSocket) method.invoke(device, 1);
//
//                if (tmp == null) {
                    tmp = btDevice.createInsecureRfcommSocketToServiceRecord(uuid);
//                }
            } catch (Exception e) {
                Log.e("Fail", "create() failed", e);
            }

            btSocket = tmp;
        }

        public void run() {
            bluetoothAdapter.cancelDiscovery();

            try {
                btSocket.connect();
            } catch (IOException e) {
                Log.e("Error", "Connection Failed");
                try {
                    btSocket.close();
                    Log.v("Socket", "Socket Closed");
                } catch (IOException e1) {
                    Log.e("Error", "unable to close() socket during connection failure", e1);
                    return;
                }
            }

            if (btSocket.isConnected()) {
                manageConnectedSocket(btSocket, btDevice);
            }
        }

        public void cancel() {
            try {
                btSocket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public synchronized void manageConnectedSocket(BluetoothSocket socket, final BluetoothDevice device) {
        if (socket.isConnected()) {

            if (connectThread != null) {
                connectThread.cancel();
                connectThread = null;
            }
            if (connectedThread != null) {
                connectedThread.cancel();
                connectedThread = null;
            }

            connectedThread = new ConnectedThread(socket);
            connectedThread.start();
        } else {
            Log.e("Error", "Socket is not connected");
        }
    }

    private class ConnectedThread extends Thread {
        private final BluetoothSocket btSocket;
        private final InputStream inputStream;
        private final OutputStream outputStream;

        public ConnectedThread(BluetoothSocket socket) {
            btSocket = socket;
            InputStream tempIn = null;
            OutputStream tempOut = null;

            try {
                tempIn = btSocket.getInputStream();
                tempOut = btSocket.getOutputStream();
            } catch (IOException e) {
                e.printStackTrace();
            }

            inputStream = tempIn;
            outputStream = tempOut;
        }

        @Override
        public void run() {
            byte[] buffer = new byte[1024];
            int bytes;

//            while (true) {
//                try {
//                    bytes = inputStream.read(buffer);
//
//                } catch (IOException e) {
//                    e.printStackTrace();
//                }
//
//                //USE HANDLER TO READ MESSAGES
//            }
        }

        public void write(String string) {
            byte[] bytes = string.getBytes();

            try {
                outputStream.write(bytes);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        public void cancel() {
            try {
                btSocket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
