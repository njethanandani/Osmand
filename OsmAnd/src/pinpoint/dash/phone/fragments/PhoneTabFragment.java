package pinpoint.dash.phone.fragments;

import pinpoint.dash.phone.activities.PhoneActivity;
import android.app.Fragment;
import android.app.FragmentTransaction;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

/**
 * This is the secondary fragment, displaying the details of a particular
 * item.
 */
public abstract class PhoneTabFragment extends Fragment {

    public PhoneTabFragment(int index) {
        Bundle args = new Bundle();
        args.putInt("index", index);
        setArguments(args);
    }

    public int getShownIndex() {
        return getArguments().getInt("index", 0);
    }
    
    public abstract int getResource();
    
    protected PhoneActivity getPhoneActivity() {
        return (PhoneActivity) getActivity();
    }
    
    public void show(int resourceId) {
        // Execute a transaction, replacing any existing fragment
        // with this one inside the frame.
        FragmentTransaction ft = getFragmentManager().beginTransaction();
        ft.replace(resourceId, this);
        ft.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE);
        ft.commit();
    }
    
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        //TextView view = new TextView(getActivity());
        //view.setText("map");
        View view = inflater.inflate(getResource(), container, false);
        return view;
    }
}