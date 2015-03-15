package srdesign.scoreboardcontrollerv1;

import android.app.AlertDialog;
import android.app.Dialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
import android.provider.Settings;
import android.support.v7.app.ActionBarActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.GridView;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Set;
import java.util.UUID;

public class ScoringActivity extends ActionBarActivity{

    final Context context = this;
    Handler connectionHandler = new Handler();
    Runnable connectionFailRunnable = new Runnable() {
        public void run() {
            Toast.makeText(context, "Connection Failed", Toast.LENGTH_SHORT).show();
        }
    };
    Runnable connectionSuccessRunnable = new Runnable() {
        @Override
        public void run() {
            Toast.makeText(context, "Connected!", Toast.LENGTH_SHORT).show();
        }
    };
    Runnable unlockSuccessRunnable = new Runnable() {
        @Override
        public void run() {
            Toast.makeText(context, "Unlocked Scoreboard", Toast.LENGTH_SHORT).show();
        }
    };

    Runnable connectionDroppedRunnable = new Runnable() {
        @Override
        public void run() {
            Toast.makeText(context, "Connection Dropped", Toast.LENGTH_SHORT).show();
        }
    };

    Runnable notConnectedRunnable = new Runnable() {
        @Override
        public void run() {
            Toast.makeText(context, "No Connection", Toast.LENGTH_SHORT).show();
        }
    };    private ToggleButton startStopButton;
//    private TextView timerValueView;
    private Button minutesButton;
    private Button secondsButton;
    private long previousTimerValue = 00;
    private long timerValue = 00;
    private CountDownTimer scoreboardTimer;
    private boolean isPaired = false;
    private boolean isConnected = false;
    private boolean isUnlocked = false;
    private BluetoothAdapter bluetoothAdapter;
    private BluetoothDevice bluetoothDevice;
    private ConnectThread connectThread;
    private ConnectedThread connectedThread;
    private String macAddress = "112233445566";
    private boolean hasError = false;
    private String homeScoreColor = "1";
    private String awayScoreColor = "1";
    private String buzzerEnableString = "0";
    private boolean isCounting = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_scoring);

        WifiManager wifiManager = (WifiManager) getSystemService(Context.WIFI_SERVICE);
        WifiInfo wInfo = wifiManager.getConnectionInfo();
        String macAddress1 = wInfo.getMacAddress();
        if (macAddress1 != null) {
            macAddress = macAddress1.replaceAll(":", "");
        } else {
            String uuid = UUID.randomUUID().toString();
            uuid = uuid.replace("-", "");
            macAddress = uuid.substring(0, 12);
        }

        int homeRadio = 0;
        int awayRadio = 0;
        boolean buzzerEnable = false;
        Sport selectedSport = null;

        Bundle extras = getIntent().getExtras();
        if (extras != null) {
            homeRadio = extras.getInt("homeRadio");
            awayRadio = extras.getInt("awayRadio");
            buzzerEnable = extras.getBoolean("buzzerEnable");
            selectedSport = (Sport) extras.getParcelable("sport");
        }

        if (buzzerEnable) {
            buzzerEnableString = "1";
        } else {
            buzzerEnableString = "0";
        }

        setTitle(selectedSport.name);

        timerValue = selectedSport.minutesPerPeriod * 60 * 1000;
        previousTimerValue = timerValue;

        minutesButton = (Button) findViewById(R.id.minutesButton);
        secondsButton = (Button) findViewById(R.id.secondsButton);

        minutesButton.setOnClickListener(new NumberClickListener(minutesButton));
        secondsButton.setOnClickListener(new NumberClickListener(secondsButton));

        startStopButton = (ToggleButton) findViewById(R.id.startStopButton);
        startStopButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (startStopButton.isChecked()) {
                    //Timer is running
                    initializeCountDownTimer();
                    //countDownTimer.start();
                    scoreboardTimer.start();
                    isCounting = true;
                } else {
                    //countDownTimer.cancel();
                    isCounting = false;
                    scoreboardTimer.cancel();
                }
            }
        });

        Button resetButton = (Button) findViewById(R.id.resetButton);
        resetButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (startStopButton.isChecked()) {
                    //Timer is Running
                    startStopButton.toggle();
                }
                //countDownTimer.cancel();
                isCounting = false;
                scoreboardTimer.cancel();
                timerValue = previousTimerValue + 100;
                initializeCountDownTimer();
                try {
                    Thread.sleep(50, 00);
                    updateTimerValue();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        });

        final Button homeScoreButton = (Button) findViewById(R.id.homeScoreButton);
        if (homeRadio == 0) {
            homeScoreButton.setTextColor(Color.RED);
            homeScoreColor = "1";
        } else {
            homeScoreButton.setTextColor(Color.BLUE);
            homeScoreColor = "0";
        }
        homeScoreButton.setOnClickListener(new NumberClickListener(homeScoreButton));

        final Button awayScoreButton = (Button) findViewById(R.id.awayScoreButton);
        if (awayRadio == 0) {
            awayScoreButton.setTextColor(Color.RED);
            awayScoreColor = "1";
        } else {
            awayScoreButton.setTextColor(Color.BLUE);
            awayScoreColor = "0";
        }
        awayScoreButton.setOnClickListener(new NumberClickListener(awayScoreButton));

        final Button periodButton = (Button) findViewById(R.id.periodButton);
        periodButton.setOnClickListener(new NumberClickListener(periodButton));

//        GridView scoringView = (GridView) findViewById(R.id.scoringGridView);
//        ArrayList<Button> scoreButtons = new ArrayList<>();
//
//        String[] scoreParts = selectedSport.scoreString.split(",");
//
//        for (String part : scoreParts) {
//            Button homePosScore = new Button(context);
//            homePosScore.setText("+ " + part);
//            homePosScore.setTextColor(Color.WHITE);
//            Button homeNegScore = new Button(context);
//            homeNegScore.setText("- " + part);
//            homeNegScore.setTextColor(Color.WHITE);
//            Button awayPosScore = new Button(context);
//            awayPosScore.setText("+ " + part);
//            Button awayNegScore = new Button(context);
//            awayNegScore.setText("- " + part);
//        }


        updateCounterView();
    }

    @Override
    protected void onPause() {
        super.onPause();

        if (isConnected) {
            connectedThread.cancel();
            connectedThread = null;
            isConnected = false;
            isUnlocked = false;
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (isConnected) {
            connectedThread.cancel();
            connectedThread = null;
            isConnected = false;
            isUnlocked = false;
        }
    }

    private void syncToScoreboard() {
        updateTimerValue();

        try {
            Thread.sleep(50, 0);

            if (isConnected && isUnlocked) {
                connectedThread.write("V" + macAddress + "1" + homeScoreColor);
                Thread.sleep(50, 0);
                connectedThread.write("W" + macAddress + "1" + awayScoreColor);
                Thread.sleep(50, 0);
                connectedThread.write("b" + macAddress + "1" + buzzerEnableString);

            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    private void updateTimerValue() {
        long minutesValue = Long.parseLong(minutesButton.getText().toString());
        long secondsValue = Long.parseLong(secondsButton.getText().toString());

        timerValue = (minutesValue * 60 + secondsValue) * 1000;
        previousTimerValue = timerValue;

        String timerString = minutesButton.getText().toString() + secondsButton.getText().toString();

        if (isConnected && isUnlocked) {
            connectedThread.write("F" + macAddress + "4" + timerString);
        } else {
            Toast.makeText(context, "Not Connected", Toast.LENGTH_SHORT).show();
        }
    }

    private void updateCounterView() {
        long counterVal = timerValue;

        if (isCounting) {
            counterVal = counterVal - 100;
        }
        int seconds = (int) (counterVal / 1000);
        int minutes = seconds / 60;
        seconds = seconds % 60;

        if (seconds < 10 && minutes < 10) {
            minutesButton.setText("0" + minutes);
            secondsButton.setText("0" + seconds);
        } else if (seconds < 10) {
            minutesButton.setText("" + minutes);
            secondsButton.setText("0" + seconds);
        } else if (minutes < 10) {
            minutesButton.setText("0" + minutes);
            secondsButton.setText("" + seconds);
        } else {
            minutesButton.setText("" + minutes);
            secondsButton.setText("" + seconds);
        }
    }

    private void updateScore(Button scoreButton, String scoreValue) {
        scoreButton.setText(scoreValue);

        if (scoreValue.length() == 1) {
            scoreValue = "0" + scoreValue;
        }

        if (isConnected && isUnlocked) {
            if (scoreButton.getTag().toString().contains("Home")) {
                connectedThread.write("G" + macAddress + "2" + scoreValue);
            } else if (scoreButton.getTag().toString().contains("Away")) {
                connectedThread.write("H" + macAddress + "2" + scoreValue);
            }
        } else {
            connectionHandler.post(notConnectedRunnable);
        }
    }

    private void updatePeriod(Button periodButton, String periodValue) {
        periodButton.setText(periodValue);

        if (isConnected && isUnlocked) {
            connectedThread.write("P" + macAddress + "1" + periodValue);
        } else {
            connectionHandler.post(notConnectedRunnable);
        }
    }

    private void initializeCountDownTimer() {
        updateCounterView();
        scoreboardTimer = new CountDownTimer(timerValue + 100, 999) {
            @Override
            public void onTick(long millisUntilFinished) {
                if (isConnected && isUnlocked) {
                    connectedThread.write("I" + macAddress + "0");
                }
                timerValue = millisUntilFinished - 100;
                updateCounterView();
            }

            @Override
            public void onFinish() {
                startStopButton.toggle();
                isCounting = false;
            }
        };
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_scoring, menu);

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        if (id == R.id.action_connect) {
            connectionButtonPressed();
        } else if (id == R.id.action_release) {
            releaseScoreboard();
        }

        return super.onOptionsItemSelected(item);
    }

    /**
     * ******************************* BLUETOOTH CONNECTION *********************************
     */

    private void releaseScoreboard() {

    }

    private void unlockScoreboard() {
        if (isConnected) {
            connectedThread.write("U" + macAddress + "4" + "1234");
            connectionHandler.post(unlockSuccessRunnable);
            isUnlocked = true;
        } else {
            connectionHandler.post(notConnectedRunnable);
        }
    }

    private void connectionButtonPressed() {
        initializeAdapter();
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
            Toast.makeText(context, "Connecting...", Toast.LENGTH_LONG).show();

            initializeConnection();
        } else {
            Toast.makeText(context, "Already Connected", Toast.LENGTH_SHORT).show();
        }
    }

    private void initializeAdapter() {
        bluetoothAdapter = null;
        bluetoothDevice = null;
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

        if (!bluetoothAdapter.isEnabled()) {
            Intent enableBluetooth = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBluetooth, 0);
        }

        if (bluetoothAdapter != null) {
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
    }

    private void initializeConnection() {
        if (connectThread != null) {
            connectThread.cancel();
            connectThread = null;
        }

        connectThread = new ConnectThread(bluetoothDevice);
        connectThread.start();
    }

    public synchronized void manageConnectedSocket(BluetoothSocket socket, final BluetoothDevice device) {
        if (socket.isConnected()) {
            connectionHandler.post(connectionSuccessRunnable);

            connectedThread = null;
            connectedThread = new ConnectedThread(socket);
            connectedThread.start();

            new UnlockAndSyncToScoreboard().execute();

        } else {
            connectionHandler.post(connectionFailRunnable);
        }
    }

    private class UnlockAndSyncToScoreboard extends AsyncTask<Void, Void, Void> {

        @Override
        protected Void doInBackground(Void... params) {
            try {
                Thread.sleep(500, 0);
                unlockScoreboard();

            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            try {
                Thread.sleep(50, 0);
                syncToScoreboard();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    private class ConnectThread extends Thread {
        private final BluetoothSocket btSocket;
        private final BluetoothDevice btDevice;

        public ConnectThread(BluetoothDevice device) {
            BluetoothSocket tmp = null;
            btDevice = device;

            UUID uuid = UUID.fromString("00001101-0000-1000-8000-00805f9b34fb");

            try {
                tmp = btDevice.createRfcommSocketToServiceRecord(uuid);
            } catch (IOException e) {
                e.printStackTrace();
            }

            btSocket = tmp;
        }

        public void run() {
            bluetoothAdapter.cancelDiscovery();

            try {
                btSocket.connect();
            } catch (IOException e) {
                try {
                    btSocket.close();
                } catch (IOException e1) {
                    e1.printStackTrace();
                    return;
                }
                e.printStackTrace();
            }
            manageConnectedSocket(btSocket, btDevice);
        }

        public void cancel() {
            try {
                btSocket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
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
            isConnected = true;
        }

        @Override
        public void run() {
            byte[] buffer = new byte[1024];
            int bytes;

            while (true) {
                try {
                    bytes = inputStream.read(buffer);

                } catch (IOException e) {
                    cancel();
                }

                //USE HANDLER TO READ MESSAGES
            }
        }

        public void write(String string) {
            byte[] bytes = string.getBytes();

            try {
                outputStream.write(bytes);
            } catch (IOException e) {
                cancel();
            }
        }

        public void cancel() {
            if (!hasError) {
                hasError = true;
                connectionHandler.post(connectionDroppedRunnable);
            }
            try {
                btSocket.close();
            } catch (IOException e) {
            }

            connectedThread = null;
            isConnected = false;
            isUnlocked = false;
            isPaired = false;
        }
    }

    /**
     * ******************************* DIALOG *********************************
     */

    private void numberDialog(final Button buttonPressed) {
        final Dialog dialog = new Dialog(context);
        dialog.setContentView(R.layout.dialog_number_pad);
        String titleString = "Set " + buttonPressed.getTag().toString() + " : " + buttonPressed.getText().toString();
        dialog.setTitle(titleString);

        final TextView numberView = (TextView) dialog.findViewById(R.id.numTextView);
        final boolean[] firstNumber = {true};
        numberView.setText(buttonPressed.getText().toString());

        Button num0 = (Button) dialog.findViewById(R.id.num0);
        Button num1 = (Button) dialog.findViewById(R.id.num1);
        Button num2 = (Button) dialog.findViewById(R.id.num2);
        Button num3 = (Button) dialog.findViewById(R.id.num3);
        Button num4 = (Button) dialog.findViewById(R.id.num4);
        Button num5 = (Button) dialog.findViewById(R.id.num5);
        Button num6 = (Button) dialog.findViewById(R.id.num6);
        Button num7 = (Button) dialog.findViewById(R.id.num7);
        Button num8 = (Button) dialog.findViewById(R.id.num8);
        Button num9 = (Button) dialog.findViewById(R.id.num9);

        ArrayList<Button> buttons = new ArrayList<>();
        buttons.add(num0);
        buttons.add(num1);
        buttons.add(num2);
        buttons.add(num3);
        buttons.add(num4);
        buttons.add(num5);
        buttons.add(num6);
        buttons.add(num7);
        buttons.add(num8);
        buttons.add(num9);

        for (final Button button : buttons) {
            button.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (firstNumber[0]) {
                        numberView.setText("");
                        firstNumber[0] = false;
                    }

                    numberView.append(button.getText().toString());
                }
            });
        }

        Button delete = (Button) dialog.findViewById(R.id.deleteButton);
        delete.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String text = numberView.getText().toString();

                if (text.length() > 0) {
                    String newText = text.substring(0, text.length() - 1);
                    numberView.setText(newText);
                }
            }
        });

        Button done = (Button) dialog.findViewById(R.id.doneButton);
        done.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                String numberString = numberView.getText().toString();
                String buttonTag = buttonPressed.getTag().toString();

                if (numberString.length() < 1) {
                    Toast.makeText(context, "No Input - Cancelled", Toast.LENGTH_SHORT).show();
                    dialog.dismiss();
                } else if (buttonTag.contains("Score")) {
                    if (numberString.length() == 1 || numberString.length() == 2) {
                        updateScore(buttonPressed, numberString);
                        dialog.dismiss();
                    } else if (numberString.length() > 2) {
                        Toast.makeText(context, "Max Score is 99", Toast.LENGTH_SHORT).show();
                    }
                } else if (buttonTag.contains("eriod")) {
                    if (numberString.length() == 1) {
                        updatePeriod(buttonPressed, numberString);
                        dialog.dismiss();
                    } else {
                        Toast.makeText(context, "Max Period is 9", Toast.LENGTH_SHORT).show();
                    }
                } else if (buttonTag.contains("Timer")) {
                    if (numberString.length() == 1) {
                        buttonPressed.setText("0" + numberString);
                        updateTimerValue();
                        dialog.dismiss();
                    } else if (numberString.length() == 2) {
                        Long numberValue = Long.parseLong(numberString);

                        if (buttonTag.contains("Seconds") && numberValue > 60) {
                            Toast.makeText(context, "Max Seconds is 59", Toast.LENGTH_SHORT).show();
                        } else {
                            buttonPressed.setText(numberString);
                            updateTimerValue();
                            dialog.dismiss();
                        }
                    } else if (numberString.length() > 2) {
                        if (buttonTag.contains("Minutes")) {
                            Toast.makeText(context, "Max Minutes is 99", Toast.LENGTH_SHORT).show();
                        } else {
                            Toast.makeText(context, "Max Seconds is 59", Toast.LENGTH_SHORT).show();
                        }
                    }
                }
            }
        });

        dialog.show();
    }

    private class NumberClickListener implements View.OnClickListener {
        Button buttonClicked = null;

        public NumberClickListener(Button button) {
            buttonClicked = button;
        }

        @Override
        public void onClick(View v) {
            if (buttonClicked.getTag().toString().contains("Timer") && startStopButton.isChecked()) {
                scoreboardTimer.cancel();
                timerValue = previousTimerValue;
                startStopButton.toggle();
            }

            numberDialog(buttonClicked);
        }
    }
}
