package pinpoint.dash.phone.fragments;

import net.osmand.plus.R;
import pinpoint.dash.phone.fragments.PhoneTabFragment;

public class PhoneRecentCallsFragment extends PhoneTabFragment {
    public PhoneRecentCallsFragment() {
    	super(PhoneMenuFragment.RECENT_TAB);
    }

    @Override
    public int getResource() {
        return R.layout.phone_recent_calls_fragment;
    }
}