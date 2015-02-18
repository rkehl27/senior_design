package srdesign.scoreboardcontrollerv1;

import android.app.FragmentManager;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;

/**
 * Created by rebeccakehl on 2/18/15.
 */
public class ScoringActivity extends ActionBarActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.main_activity);

        FragmentManager fragmentManager = getFragmentManager();

        ScoringFragment scoreFragment = new ScoringFragment();
        fragmentManager.beginTransaction().replace(R.id.fragment_container, scoreFragment).commit();
    }
}
