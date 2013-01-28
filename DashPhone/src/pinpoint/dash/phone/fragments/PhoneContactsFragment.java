package pinpoint.dash.phone.fragments;

import pinpoint.dash.phone.R;
import pinpoint.dash.phone.fragments.TabFragment;

public class PhoneContactsFragment extends TabFragment {
    public PhoneContactsFragment() {
    	super(PhoneMenuFragment.CONTACTS_TAB);
    }

    @Override
    public int getResource() {
        return R.layout.phone_contacts_fragment;
    }
}