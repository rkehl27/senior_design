package srdesign.scoreboardcontrollerv1;

import android.app.Activity;
import android.app.ListFragment;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;

import java.util.ArrayList;

/**
 * Created by rebeccakehl on 3/18/15.
 */
public class SportListFragment extends ListFragment {
    public ArrayList<Sport> sportList;
    SportSelectedListener sportSelectedListener;

    public interface SportSelectedListener {
        public void sportSelected(Sport selectedSport);
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);

        sportSelectedListener = (SportSelectedListener) activity;
    }

    @Override
    public void onListItemClick(ListView l, View v, int position, long id) {
        super.onListItemClick(l, v, position, id);

        Sport selectedSport = (Sport) l.getItemAtPosition(position);

        sportSelectedListener.sportSelected(selectedSport);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        sportList = getArguments().getParcelableArrayList("sports");
        getActivity().setTitle("Sports");

        SportAdapter adapter = new SportAdapter(sportList, this.getActivity());
        setListAdapter(adapter);
        return super.onCreateView(inflater, container, savedInstanceState);
    }
}
