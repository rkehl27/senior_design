package srdesign.scoreboardcontrollerv1;

import android.os.Parcel;
import android.os.Parcelable;

import com.google.gson.annotations.SerializedName;

/**
 * Created by rebeccakehl on 3/18/15.
 */
public class Sport implements Parcelable {
    public static final Creator<Sport> CREATOR = new Creator<Sport>() {
        @Override
        public Sport createFromParcel(Parcel source) {
            return new Sport(source);
        }

        @Override
        public Sport[] newArray(int size) {
            return new Sport[size];
        }
    };
    public final String name;
    @SerializedName("number_of_periods")
    public final int numberOfPeriods;
    @SerializedName("minutes_per_period")
    public final int minutesPerPeriod;
    @SerializedName("seconds_per_period")
    public final int secondsPerPeriod;
    @SerializedName("scoring_string")
    private final String scoreString;

    public Sport(Parcel src) {
        this.name = src.readString();
        this.numberOfPeriods = src.readInt();
        this.minutesPerPeriod = src.readInt();
        this.secondsPerPeriod = src.readInt();
        this.scoreString = src.readString();
    }

    public Sport(String name, int numPeriod, int minPerPeriod, int secPerPeriod, String scoreString) {
        this.name = name;
        this.numberOfPeriods = numPeriod;
        this.minutesPerPeriod = minPerPeriod;
        this.secondsPerPeriod = secPerPeriod;
        this.scoreString = scoreString;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(name);
        dest.writeInt(numberOfPeriods);
        dest.writeInt(minutesPerPeriod);
        dest.writeInt(secondsPerPeriod);
        dest.writeString(scoreString);
    }
}
