package srdesign.scoreboardcontrollerv1;

import android.app.Activity;
import android.app.Dialog;
import android.app.FragmentManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.app.ActionBarDrawerToggle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.google.gson.Gson;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;

/**
 * Created by rebeccakehl on 3/18/15.
 */
public class SportActivity extends ActionBarActivity implements SportListFragment.SportSelectedListener, GameCustomizerFragment.StartGameListener, SportCustomizerFragment.SportCustomizerListener{
    final Context context = this;
    public ArrayList<Sport> sportList;
    private ArrayList<Sport> customSports;
    public GameCustomizerFragment gameCustomizerFragment;
    public SportListFragment sportListFragment;
    public SportCustomizerFragment sportCustomizerFragment;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_sport);

        try {
            readAndParseInteriorJSON();
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    private void readAndParseInteriorJSON() throws JSONException {
        sportList = new ArrayList<Sport>();
        String json = null;

        try {
            InputStream is = getAssets().open("sport.json");
            int size = is.available();

            byte[] buffer = new byte[size];

            is.read(buffer);
            is.close();

            json = new String(buffer, "UTF-8");
        } catch (IOException e) {
            e.printStackTrace();
        }

        JSONObject jsonObject = new JSONObject(json);
        JSONArray jsonArray = jsonObject.getJSONArray("sports");

        for (int i = 0; i < jsonArray.length(); i++) {
            JSONObject object = jsonArray.getJSONObject(i);
            String sportName = object.getString("name");
            int numPeriods = object.getInt("number_of_periods");
            int minutesPerPeriod = object.getInt("minutes_per_period");
            String scoreString = object.getString("scoring_string");

//            ArrayList<Integer> scoreArray = new ArrayList<>();
//            String[] scoreParts = scoreString.split(",");
//
//            for (String part : scoreParts) {
//                Integer partVal = Integer.valueOf(part);
//                scoreArray.add(partVal);
//            }

            Sport newSport = new Sport(sportName, numPeriods, minutesPerPeriod, 0, scoreString);
            sportList.add(newSport);
        }
        getCustomSports();
    }

    private void getCustomSports() {
        customSports = new ArrayList<Sport>();
        SharedPreferences sharedPreferences = getPreferences(Context.MODE_PRIVATE);

        int size = sharedPreferences.getInt("array_size", 0);

        Gson gson = new Gson();
        for (int i=0; i<size; i++) {
            String json = sharedPreferences.getString("sport_" + i, null);
            Sport sport = gson.fromJson(json, Sport.class);
            sportList.add(sport);
            customSports.add(sport);
        }

        launchSportList();
    }

    private void addSportToPrefs(Sport sport) {
        customSports.add(sport);

        SharedPreferences sharedPreferences = getPreferences(Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();

        Gson gson = new Gson();

        for (int i=0; i<customSports.size(); i++) {
            editor.remove("sport_"  + i);
            String json = gson.toJson(customSports.get(i));
            editor.putString("sport_" + i, json);
        }

        editor.putInt("array_size", customSports.size());

        editor.commit();
    }

    private void launchSportList() {
        FragmentManager fm = getFragmentManager();

        Bundle bundle = new Bundle();
        bundle.putParcelableArrayList("sports", sportList);

        sportListFragment = new SportListFragment();
        sportListFragment.setArguments(bundle);
        fm.beginTransaction().replace(android.R.id.content, sportListFragment).commit();
    }

    public void launchSportCustomizer() {
        sportCustomizerFragment = new SportCustomizerFragment();
        android.app.FragmentTransaction transaction = getFragmentManager().beginTransaction();
        transaction.replace(android.R.id.content, sportCustomizerFragment)
                .addToBackStack("sportcustom")
                .commit();
    }

    @Override
    public void onBackPressed() {
        if (getFragmentManager().getBackStackEntryCount() > 0) {
            getFragmentManager().popBackStack();
        } else {
            super.onBackPressed();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_sport, menu);

        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.action_add_item) {
            launchSportCustomizer();
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void sportSelected(Sport selectedSport) {

        Bundle bundle = new Bundle();
        bundle.putParcelable("sport", selectedSport);

        gameCustomizerFragment = new GameCustomizerFragment();
        gameCustomizerFragment.setArguments(bundle);
        android.app.FragmentTransaction transaction = getFragmentManager().beginTransaction();
        transaction.replace(android.R.id.content, gameCustomizerFragment)
                .addToBackStack("gamecustom")
                .commit();

    }

    @Override
    public void gameStarted(Bundle args) {
        Intent intent = new Intent(this, ScoringActivity.class);
        int homeRadio = args.getInt("homeRadio");
        int awayRadio = args.getInt("awayRadio");
        boolean buzzerEnable = args.getBoolean("buzzerEnable");
        Sport selectedSport = args.getParcelable("sport");

        intent.putExtra("homeRadio", homeRadio);
        intent.putExtra("awayRadio", awayRadio);
        intent.putExtra("buzzerEnable", buzzerEnable);
        intent.putExtra("sport", selectedSport);

        startActivity(intent);
    }

    @Override
    public void sportCustomizerDone(Sport sport) {
        addSportToPrefs(sport);
        sportList.add(sport);
        launchSportList();
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
                } else {
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

    private void updateTimerValue() {
//        long minutesValue = Long.parseLong(minutesButton.getText().toString());
//        long secondsValue = Long.parseLong(secondsButton.getText().toString());

//        timerValue = (minutesValue * 60 + secondsValue) * 1000;
//        previousTimerValue = timerValue;
//
//        String timerString = minutesButton.getText().toString() + secondsButton.getText().toString();
//
//        if (isConnected && isUnlocked) {
//            connectedThread.write("F" + macAddress + "4" + timerString);
//        } else {
//            Toast.makeText(context, "Not Connected", Toast.LENGTH_SHORT).show();
//        }
    }

    private class NumberClickListener implements View.OnClickListener {
        Button buttonClicked = null;

        public NumberClickListener(Button button) {
            buttonClicked = button;
        }

        @Override
        public void onClick(View v) {
//            if (buttonClicked.getTag().toString().contains("Timer") && startStopButton.isChecked()) {
//                scoreboardTimer.cancel();
//                timerValue = previousTimerValue;
//                startStopButton.toggle();
//            }

            numberDialog(buttonClicked);
        }
    }
}
