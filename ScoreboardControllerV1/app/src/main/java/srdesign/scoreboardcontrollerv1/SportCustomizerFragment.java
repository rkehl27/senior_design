package srdesign.scoreboardcontrollerv1;

import android.app.Activity;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.Context;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;

/**
 * Created by rebeccakehl on 3/28/15.
 */
public class SportCustomizerFragment extends DialogFragment {

    private SportCustomizerListener sportCustomizerListener;
    private Context context;

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);

        sportCustomizerListener = (SportCustomizerListener) activity;
    }

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
    }

    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        MenuItem item = menu.getItem(0);
        item.setVisible(false);
        super.onCreateOptionsMenu(menu, inflater);
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        getActivity().setTitle("Add New Sport");
        context = getActivity();

        View view = inflater.inflate(R.layout.fragment_sport_customize, container, false);

        final Button minutesButton = (Button) view.findViewById(R.id.minutesPeriod);
        final Button secondsButton = (Button) view.findViewById(R.id.secondsPeriod);

        minutesButton.setOnClickListener(new NumberClickListener(minutesButton));
        secondsButton.setOnClickListener(new NumberClickListener(secondsButton));

        final TextView periodView = (TextView) view.findViewById(R.id.numPeriods);

        Button incrementPeriod = (Button) view.findViewById(R.id.plusPeriod);
        incrementPeriod.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String periodText = periodView.getText().toString();
                int periodVal = Integer.valueOf(periodText);
                periodVal = periodVal + 1;
                if (periodVal > 9) {
                    periodVal = 9;
                }
                periodView.setText("" + periodVal);
            }
        });
        Button decrementPeriod = (Button) view.findViewById(R.id.minusPeriod);
        decrementPeriod.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String periodText = periodView.getText().toString();
                int periodVal = Integer.valueOf(periodText);
                periodVal = periodVal - 1;
                if (periodVal < 1) {
                    periodVal = 1;
                }
                periodView.setText("" + periodVal);
            }
        });

        final EditText sportNameText = (EditText) view.findViewById(R.id.sportName);

        Button doneButton = (Button) view.findViewById(R.id.customize_done);
        doneButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String sportName = sportNameText.getText().toString();
                int numPeriods = Integer.parseInt(periodView.getText().toString());
                int numMinutes = Integer.parseInt(minutesButton.getText().toString());
                int numSeconds = Integer.parseInt(secondsButton.getText().toString());

                if (sportName.length() > 0) {
                    Sport sport = new Sport(sportName, numPeriods, numMinutes, numSeconds, "");
                    sportCustomizerListener.sportCustomizerDone(sport);
                } else {
                    Toast.makeText(context, "You must have a name!", Toast.LENGTH_SHORT).show();
                }
            }
        });

        return view;
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
                        dialog.dismiss();
                    } else if (numberString.length() == 2) {
                        Long numberValue = Long.parseLong(numberString);

                        if (buttonTag.contains("Seconds") && numberValue > 60) {
                            Toast.makeText(context, "Max Seconds is 59", Toast.LENGTH_SHORT).show();
                        } else {
                            buttonPressed.setText(numberString);
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

    public interface SportCustomizerListener {
        public void sportCustomizerDone(Sport sport);
    }

    private class NumberClickListener implements View.OnClickListener {
        Button buttonClicked = null;

        public NumberClickListener(Button button) {
            buttonClicked = button;
        }

        @Override
        public void onClick(View v) {
            numberDialog(buttonClicked);
        }
    }
}
