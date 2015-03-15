package srdesign.scoreboardcontrollerv1;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import java.util.ArrayList;

/**
 * Created by rebeccakehl on 3/28/15.
 */
public class SportAdapter extends BaseAdapter {
    private ArrayList<Sport> sports;
    private Context context;

    public SportAdapter(ArrayList<Sport> sportList, Context ctx) {
        this.sports = sportList;
        this.context = ctx;
    }

    @Override
    public int getCount() {
        return sports.size();
    }

    @Override
    public Object getItem(int position) {
        return sports.get(position);
    }

    @Override
    public long getItemId(int position) {
        return 0;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        if (convertView == null) {
            LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            convertView = inflater.inflate(R.layout.row_layout, parent, false);
        }

        TextView sportNameField = (TextView) convertView.findViewById(R.id.sport_name_field);
        Sport sport = sports.get(position);

        sportNameField.setText(sport.name);

        return convertView;
    }
}
