package pinpoint.dash.phone.fragments;

import net.osmand.plus.R;
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