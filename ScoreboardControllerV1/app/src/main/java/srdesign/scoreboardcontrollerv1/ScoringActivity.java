package srdesign.scoreboardcontrollerv1;

import android.app.Dialog;
import android.content.Context;
import android.os.CountDownTimer;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.ToggleButton;

public class ScoringActivity extends ActionBarActivity {

    final Context context = this;
    private ToggleButton startStopButton;
    private Button resetButton;

    private TextView timerValueView;

    private long timerValue = 5001;

    private CountDownTimer countDownTimer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_scoring);

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
                timerValue = 5001;
                initializeCountDownTimer();
            }
        });
    }

    private void initializeCountDownTimer() {
        timerValueView.setText("0:05");
        countDownTimer = new CountDownTimer(timerValue, 1) {
            @Override
            public void onTick(long millisUntilFinished) {
                timerValue = millisUntilFinished;

                int seconds = (int) (millisUntilFinished/1000);
                int minutes = seconds/60;
                seconds = seconds % 60;

                if (seconds < 10) {
                    timerValueView.setText("" + minutes + ":0" + seconds);
                } else {
                    timerValueView.setText("" + minutes + ":" + seconds);
                }
            }

            @Override
            public void onFinish() {
                timerValueView.setText("0:00");
                startStopButton.toggle();
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

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
}
