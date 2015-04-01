package srdesign.scoreboardcontrollerv1;

import android.app.Activity;
import android.app.Fragment;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Switch;

/**
 * Created by rebeccakehl on 3/28/15.
 */
public class GameCustomizerFragment extends Fragment {
    public Sport selectedSport;
    private RadioButton homeRedRadio;
    private RadioButton homeBlueRadio;
    private RadioButton awayRedRadio;
    private RadioButton awayBlueRadio;
    private Switch buzzerEnableSwitch;

    StartGameListener startGameListener;

    public interface StartGameListener {
        public void gameStarted(Bundle args);
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);

        startGameListener = (StartGameListener) activity;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        MenuItem item = menu.getItem(0);
        item.setVisible(false);

        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_game_customize, container, false);

        selectedSport = (Sport) getArguments().get("sport");

        getActivity().setTitle("Sport: " + selectedSport.name);

        Button startButton = (Button) view.findViewById(R.id.gameStartButton);
        homeRedRadio = (RadioButton) view.findViewById(R.id.homeRedRadioButton);
        homeBlueRadio = (RadioButton) view.findViewById(R.id.homeBlueRadioButton);
        awayRedRadio = (RadioButton) view.findViewById(R.id.awayRedRadioButton);
        awayBlueRadio = (RadioButton) view.findViewById(R.id.awayBlueRadioButton);
        buzzerEnableSwitch = (Switch) view.findViewById(R.id.switch1);

        startButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startGamePressed();
            }
        });

        return view;
    }

    private void startGamePressed() {
        Bundle args = new Bundle();

        int homeRadioId = 0;
        int awayRadioId = 0;

        if (homeRedRadio.isChecked()) {
            homeRadioId = 0;
        } else {
            homeRadioId = 1;
        }

        if (awayRedRadio.isChecked()) {
            awayRadioId = 0;
        } else {
            awayRadioId = 1;
        }

        boolean buzzerEnable = buzzerEnableSwitch.isChecked();

        args.putInt("homeRadio", homeRadioId);
        args.putInt("awayRadio", awayRadioId);
        args.putBoolean("buzzerEnable", buzzerEnable);
        args.putParcelable("sport", selectedSport);

        startGameListener.gameStarted(args);
    }
}
