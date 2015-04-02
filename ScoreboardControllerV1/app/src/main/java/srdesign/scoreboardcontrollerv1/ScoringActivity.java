package srdesign.scoreboardcontrollerv1;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

import java.util.ArrayList;
import java.util.UUID;

public class ScoringActivity extends ActionBarActivity {

    public static final int MESSAGE_STATE_CHANGE = 1;
    public static final int MESSAGE_READ = 2;
    private static final int MESSAGE_WRITE = 3;
    public static final int MESSAGE_TOAST = 4;
    private static final int REQUEST_CONNECT_DEVICE = 1;
    private static final int REQUEST_ENABLE_BT = 2;
    private static BluetoothService bluetoothService = null;
    private final Context context = this;
    private ToggleButton startStopButton;
    private Button minutesButton;
    private Button secondsButton;
    private Button homeScoreButton;
    private Button awayScoreButton;
    private Button periodButton;
    private long previousTimerValue = 00;
    private long timerValue = 00;
    private CountDownTimer scoreboardTimer;
    private String macAddress = "112233445566";
    //    private boolean hasError = false;
    private String homeScoreColor = "1";
    private String awayScoreColor = "1";
    private String buzzerEnableString = "0";
    private boolean isCounting = false;
    private BluetoothAdapter btAdapter;
    private BluetoothDevice bluetoothDevice;
    private boolean hasConnected = false;
    private boolean hasUnlocked = false;
    private final Handler bluetoothHandler = new Handler() {
        @Override
        public void handleMessage(Message mess) {
            Log.v("mess", String.valueOf(mess));
            switch (mess.what) {
                case MESSAGE_STATE_CHANGE:
                    switch (mess.arg1) {
                        case BluetoothService.STATE_CONNECTED:
                            Toast.makeText(context, "Connected!", Toast.LENGTH_SHORT).show();
                            hasConnected = true;
                            bluetoothConnectItem.setVisible(false);
                            unlockScoreboard();
                            break;
                        case BluetoothService.STATE_CONNECTING:
                            Toast.makeText(context, "Connecting...", Toast.LENGTH_SHORT).show();
                            hasConnected = false;
                            break;
                        case BluetoothService.STATE_LISTEN:
                            //do nothing
                        case BluetoothService.STATE_NONE:
                            //Toast.makeText(context, "Not Connected", Toast.LENGTH_SHORT).show();

                            hasConnected = false;
                            bluetoothConnectItem.setVisible(true);
                            syncToScoreboardItem.setVisible(false);
                            syncFromScoreboardItem.setVisible(false);
                            releaseScoreboardItem.setVisible(false);

                            //TODO: Handle Not Connected
                    }
                    break;
                case MESSAGE_READ:
                    byte[] readBuf = (byte[]) mess.obj;

                    if (!hasUnlocked) {
                        if (readBuf[0] == 1) {
                            hasUnlocked = true;
                            unlockScoreboardItem.setVisible(false);
                            releaseScoreboardItem.setVisible(true);
                            syncToScoreboardItem.setVisible(true);
                            syncFromScoreboardItem.setVisible(true);
                        } else {
                            Toast.makeText(context, "Could not Unlock", Toast.LENGTH_SHORT).show();
                            bluetoothConnectItem.setVisible(true);
                            syncToScoreboardItem.setVisible(false);
                            syncFromScoreboardItem.setVisible(false);
                            releaseScoreboardItem.setVisible(false);
                        }
                    } else {
                        updateView(readBuf);
                    }
                    break;
                case MESSAGE_WRITE:
                    byte[] writeBuff = (byte[]) mess.obj;
                    String message = new String(writeBuff);
                    //TODO: Handle message from Bluetooth
                    //Toast.makeText(context, message, Toast.LENGTH_LONG).show();
                    break;
                case MESSAGE_TOAST:
                    Toast.makeText(context, mess.getData().getString("toast"), Toast.LENGTH_SHORT).show();
                    break;
            }
        }
    };
    private boolean isPaired = false;
    private MenuItem releaseScoreboardItem;
    private MenuItem bluetoothConnectItem;
    private MenuItem syncToScoreboardItem;
    private MenuItem syncFromScoreboardItem;
    private MenuItem unlockScoreboardItem;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_scoring);

        readMacAddressFromPrefs();

        int homeRadio = 0;
        int awayRadio = 0;
        boolean buzzerEnable = false;
        Sport selectedSport = null;

        Bundle extras = getIntent().getExtras();
        if (extras != null) {
            homeRadio = extras.getInt("homeRadio");
            awayRadio = extras.getInt("awayRadio");
            buzzerEnable = extras.getBoolean("buzzerEnable");
            selectedSport = extras.getParcelable("sport");
        }

        if (buzzerEnable) {
            buzzerEnableString = "1";
        } else {
            buzzerEnableString = "0";
        }

        setTitle(selectedSport.name);

        buildView(selectedSport, homeRadio, awayRadio);

        updateCounterView();
    }

    private void readMacAddressFromPrefs() {
        SharedPreferences sharedPreferences = getPreferences(Context.MODE_PRIVATE);
        macAddress = sharedPreferences.getString("macaddr", null);

        if (macAddress == null) {
            String uuid = UUID.randomUUID().toString();
            uuid = uuid.replace("-", "");
            macAddress = uuid.substring(0, 12);

            SharedPreferences.Editor editor = sharedPreferences.edit();

            editor.remove("macaddr");
            editor.putString("macaddr", macAddress);
            editor.apply();
        }
    }

    private void buildView(Sport selectedSport, int homeRadio, int awayRadio) {
        timerValue = (selectedSport.minutesPerPeriod * 60 + selectedSport.secondsPerPeriod) * 1000;
        previousTimerValue = timerValue;

        minutesButton = (Button) findViewById(R.id.minutesButton);
        secondsButton = (Button) findViewById(R.id.secondsButton);

        minutesButton.setOnClickListener(new NumberClickListener(minutesButton, selectedSport));
        secondsButton.setOnClickListener(new NumberClickListener(secondsButton, selectedSport));

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
                if (scoreboardTimer != null) {
                    scoreboardTimer.cancel();
                }
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

        homeScoreButton = (Button) findViewById(R.id.homeScoreButton);
        if (homeRadio == 0) {
            homeScoreButton.setTextColor(Color.RED);
            homeScoreColor = "1";
        } else {
            homeScoreButton.setTextColor(Color.BLUE);
            homeScoreColor = "0";
        }
        homeScoreButton.setOnClickListener(new NumberClickListener(homeScoreButton, selectedSport));

        awayScoreButton = (Button) findViewById(R.id.awayScoreButton);
        if (awayRadio == 0) {
            awayScoreButton.setTextColor(Color.RED);
            awayScoreColor = "1";
        } else {
            awayScoreButton.setTextColor(Color.BLUE);
            awayScoreColor = "0";
        }
        awayScoreButton.setOnClickListener(new NumberClickListener(awayScoreButton, selectedSport));

        periodButton = (Button) findViewById(R.id.periodButton);
        periodButton.setOnClickListener(new NumberClickListener(periodButton, selectedSport));
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        releaseScoreboard();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case REQUEST_CONNECT_DEVICE:
                if (resultCode == Activity.RESULT_OK) {
                    String address = data.getExtras().getString(DeviceListActivity.EXTRA_DEVICE_ADDRESS);

                    bluetoothDevice = btAdapter.getRemoteDevice(address);

                    if (address.matches("30:14:11:27:03:19")) {
                        isPaired = true;
                        bluetoothService = new BluetoothService(context, bluetoothHandler);
                        connectionButtonPressed();
                    } else {
                        Toast.makeText(context, "Please connect to HC-06", Toast.LENGTH_SHORT).show();
                    }
                }
                break;
            case REQUEST_ENABLE_BT:
                if (resultCode != Activity.RESULT_OK) {
                    finishDialogNoBluetooth();
                } else {
                    connectionButtonPressed();
                }
                break;
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_scoring, menu);

        for (int i = 0; i < menu.size(); i++) {
            MenuItem tempItem = menu.getItem(i);
            switch (tempItem.getItemId()) {
                case R.id.action_connect:
                    bluetoothConnectItem = tempItem;
                    break;
                case R.id.action_release:
                    releaseScoreboardItem = tempItem;
                    break;
                case R.id.action_sync_from:
                    syncFromScoreboardItem = tempItem;
                    break;
                case R.id.action_sync_to:
                    syncToScoreboardItem = tempItem;
                    break;
                case R.id.action_unlock:
                    unlockScoreboardItem = tempItem;
                    break;
            }
        }

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        switch (item.getItemId()) {
            case R.id.action_connect:
                connectionButtonPressed();
                break;
            case R.id.action_release:
                releaseScoreboard();
                releaseScoreboardItem.setVisible(false);
                break;
            case R.id.action_sync_from:
                syncFromScoreboard();
                break;
            case R.id.action_sync_to:
                syncToScoreboard();
                break;
            case R.id.action_unlock:
                unlockScoreboard();
                break;
        }

        return super.onOptionsItemSelected(item);
    }

    private void unlockScoreboard() {
        Log.v("DEBUG", "Unlocked");
        if (hasConnected) {
            String unlockMessage = "U" + macAddress + "4" + "1234";
            send(unlockMessage);
        }
    }

    private void releaseScoreboard() {
        if (hasUnlocked) {
            hasUnlocked = false;
            hasConnected = false;

            send("Z" + macAddress + "0");
            bluetoothService.stop();
        }
    }

    private void syncToScoreboard() {
        String homeScoreTens = "0";
        String homeScoreOnes;
        String awayScoreTens = "0";
        String awayScoreOnes;
        if (homeScoreButton.getText().length() == 2) {
            homeScoreTens = String.valueOf(homeScoreButton.getText().toString().charAt(0));
            homeScoreOnes = String.valueOf(homeScoreButton.getText().toString().charAt(1));
        } else {
            homeScoreOnes = String.valueOf(homeScoreButton.getText().toString().charAt(0));
        }

        if (awayScoreButton.getText().length() == 2) {
            awayScoreTens = String.valueOf(awayScoreButton.getText().toString().charAt(0));
            awayScoreOnes = String.valueOf(awayScoreButton.getText().toString().charAt(1));
        } else {
            awayScoreOnes = String.valueOf(awayScoreButton.getText().toString().charAt(0));
        }

        String period = periodButton.getText().toString();
        String tensMinutes = String.valueOf(minutesButton.getText().toString().charAt(0));
        String minutes = String.valueOf(minutesButton.getText().toString().charAt(1));
        String tensSeconds = String.valueOf(secondsButton.getText().toString().charAt(0));
        String seconds = String.valueOf(secondsButton.getText().toString().charAt(1));

        String refreshScoreboardString = "a" + macAddress + "<" + homeScoreColor
                + homeScoreTens + homeScoreOnes + awayScoreColor + awayScoreTens
                + awayScoreOnes + tensMinutes + minutes + tensSeconds + seconds
                + period + buzzerEnableString;

        Log.d("Refresh", refreshScoreboardString);

        send(refreshScoreboardString);
    }

    private void syncFromScoreboard() {
        send("X" + macAddress + "0");
    }

    private void updateTimerValue() {
        long minutesValue = Long.parseLong(minutesButton.getText().toString());
        long secondsValue = Long.parseLong(secondsButton.getText().toString());

        timerValue = (minutesValue * 60 + secondsValue) * 1000;
        previousTimerValue = timerValue;

        String timerString = minutesButton.getText().toString() + secondsButton.getText().toString();


        if (hasUnlocked) {
            String updateTimerString = "F" + macAddress + "4" + timerString;
            send(updateTimerString);
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

        if (hasUnlocked) {
            if (scoreButton.getTag().toString().contains("Home")) {
                String homeUpdate = "G" + macAddress + "2" + scoreValue;
                send(homeUpdate);
            } else if (scoreButton.getTag().toString().contains("Away")) {
                String awayUpdate = "H" + macAddress + "2" + scoreValue;
                send(awayUpdate);
            }
        }
    }

    private void updatePeriod(Button periodButton, String periodValue) {
        periodButton.setText(periodValue);

        if (hasUnlocked) {
            String updatePeriod = "P" + macAddress + "1" + periodValue;
            send(updatePeriod);
        }
    }

    private void initializeCountDownTimer() {
        updateCounterView();
        scoreboardTimer = new CountDownTimer(timerValue + 100, 999) {
            @Override
            public void onTick(long millisUntilFinished) {
                if (hasUnlocked) {
                    String updateTimer = "I" + macAddress + "0";
                    send(updateTimer);
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

    private void updateView(byte[] buffer) {
        int homeColor = buffer[0];
        int homeScore = buffer[1];
        int awayColor = buffer[2];
        int awayScore = buffer[3];
        int tensMinutes = buffer[4];
        int onesMinutes = buffer[5];
        int tensSeconds = buffer[6];
        int onesSeconds = buffer[7];
        int period = buffer[8];
        int buzzer = buffer[9];

        if (homeColor == 1) {
            homeScoreButton.setTextColor(Color.RED);
            homeScoreColor = "0";
        } else {
            homeScoreButton.setTextColor(Color.BLUE);
            homeScoreColor = "1";
        }

        if (awayColor == 1) {
            awayScoreButton.setTextColor(Color.RED);
            awayScoreColor = "0";
        } else {
            awayScoreButton.setTextColor(Color.BLUE);
            awayScoreColor = "1";
        }

        homeScoreButton.setText(String.valueOf(homeScore));
        awayScoreButton.setText(String.valueOf(awayScore));

        minutesButton.setText(String.format("%d%d", tensMinutes, onesMinutes));
        secondsButton.setText(String.format("%d%d", tensSeconds, onesSeconds));

        periodButton.setText(String.valueOf(period));

        buzzerEnableString = String.valueOf(buzzer);
    }

    /**
     * ******************************* BLUETOOTH CONNECTION *********************************
     */

    void send(String out) {
        if (out.length() > 0) {
            bluetoothService.write(out);
        }
    }

    void finishDialogNoBluetooth() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage(R.string.alert_dialog_no_bt)
                .setIcon(android.R.drawable.ic_dialog_info)
                .setTitle(R.string.app_name)
                .setCancelable(false)
                .setPositiveButton(R.string.alert_dialog_ok, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        //finish();
                    }
                });
        AlertDialog alert = builder.create();
        alert.show();
    }

    private void connectionButtonPressed() {
        btAdapter = null;
        btAdapter = BluetoothAdapter.getDefaultAdapter();

        if (!btAdapter.isEnabled()) {
            Intent enableBluetooth = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBluetooth, REQUEST_ENABLE_BT);
        } else if (!isPaired) {
            Intent serverIntent = new Intent(this, DeviceListActivity.class);
            startActivityForResult(serverIntent, REQUEST_CONNECT_DEVICE);
        } else if (!hasConnected) {
            bluetoothService.stop();
            bluetoothService.start();
            bluetoothService.connect(bluetoothDevice);
        }
    }

    /**
     * ******************************* DIALOG *********************************
     */

    private void numberDialog(final Button buttonPressed, final Sport selectedSport) {
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
                    int periodVal = Integer.valueOf(numberString);
                    if (periodVal <= selectedSport.numberOfPeriods) {
                        updatePeriod(buttonPressed, numberString);
                        dialog.dismiss();
                    } else {
                        Toast.makeText(context, "Max Period is " + selectedSport.numberOfPeriods, Toast.LENGTH_SHORT).show();
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
        final Sport selectedSport;

        public NumberClickListener(Button button, Sport sport) {
            buttonClicked = button;
            selectedSport = sport;
        }

        @Override
        public void onClick(View v) {
            if (buttonClicked.getTag().toString().contains("Timer") && startStopButton.isChecked()) {
                scoreboardTimer.cancel();
                timerValue = previousTimerValue;
                startStopButton.toggle();
            }

            numberDialog(buttonClicked, selectedSport);
        }
    }
}
