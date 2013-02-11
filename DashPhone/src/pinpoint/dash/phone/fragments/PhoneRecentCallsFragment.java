package pinpoint.dash.phone.fragments;

import pinpoint.dash.phone.R;
import pinpoint.dash.phone.fragments.TabFragment;

public class PhoneRecentCallsFragment extends TabFragment {
    public PhoneRecentCallsFragment() {
    	super(PhoneMenuFragment.RECENT_TAB);
    }

    @Override
    public int getResource() {
        return R.layout.phone_recent_calls_fragment;
    }
}