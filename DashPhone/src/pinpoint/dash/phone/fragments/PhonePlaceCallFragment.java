package pinpoint.dash.phone.fragments;

import pinpoint.dash.phone.R;
import pinpoint.dash.phone.fragments.TabFragment;

public class PhonePlaceCallFragment extends TabFragment {
    public PhonePlaceCallFragment() {
    	super(PhoneMenuFragment.PLACE_CALL_TAB);
    }

    @Override
    public int getResource() {
        return R.layout.phone_place_call_fragment;
    }
}