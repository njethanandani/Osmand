package pinpoint.dash.phone.fragments;

import pinpoint.dash.phone.R;
import pinpoint.dash.phone.fragments.PhoneTabFragment;

public class PhoneFavoritesFragment extends PhoneTabFragment {
    public PhoneFavoritesFragment() {
    	super(PhoneMenuFragment.FAVORITES_TAB);
    }

    @Override
    public int getResource() {
        return R.layout.phone_favorites_fragment;
    }
}