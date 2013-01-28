package pinpoint.dash.phone.fragments;

import pinpoint.dash.phone.R;
import android.app.FragmentTransaction;
import android.app.Fragment;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

/**
 * Top-level menu fragment, showing the menu options that the user can
 * pick. Upon picking an item, it takes care of displaying the
 * appropriate data to the user.
 */
public class PhoneMenuFragment extends Fragment {
    public static final int FAVORITES_TAB = 0;
    public static final int CONTACTS_TAB = 1;
    public static final int RECENT_TAB = 2;
    public static final int PLACE_CALL_TAB = 3;
    public static final int SETTINGS_TAB = 4;
    private static int DEFAULT_TAB = 0;

    int mCurrentSelectedTab = 0;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        if (savedInstanceState != null) {
        	mCurrentSelectedTab = getSelectedTab(savedInstanceState);
        }

        View view = inflater.inflate(R.layout.phone_menu_fragment, container, false);
        wireTabButtons(view);
    	showDetails(mCurrentSelectedTab);
        return view;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt("selectedTab", mCurrentSelectedTab);
    }
    
    int getSelectedTab(Bundle savedInstanceState) {
        if (savedInstanceState == null) {
        	return DEFAULT_TAB;
        } else {
        	return savedInstanceState.getInt("selectedTab", 0);
        }
    }
    
    void wireTabButtons(View view) {
    	View buttonView = view.findViewById(R.id.phoneFavoritesButton);
        setOnClickListener(buttonView, FAVORITES_TAB);
        
        buttonView = (View) view.findViewById(R.id.phoneContactsButton);
        setOnClickListener(buttonView, CONTACTS_TAB);
        
        buttonView = (View) view.findViewById(R.id.phoneRecentButton);
        setOnClickListener(buttonView, RECENT_TAB);
        
        buttonView = (View) view.findViewById(R.id.phonePlaceCallButton);
        setOnClickListener(buttonView, PLACE_CALL_TAB);

        buttonView = (View) view.findViewById(R.id.phoneSettingsButton);
        setOnClickListener(buttonView, SETTINGS_TAB);
    }

    void setOnClickListener(View view, final int index) {
        view.setOnClickListener(new View.OnClickListener() {
        	@Override
            public void onClick(View view) {
            	showDetails(index);
            }
        });
    }
    
    /**
     * Helper function to show the details of a selected item, by
     * displaying a fragment in-place in the current UI.
     */
    void showDetails(int tabIndex) {
        // Check what fragment is currently shown, replace if needed.
        TabFragment tab = (TabFragment)
                getFragmentManager().findFragmentById(R.id.Details);
        if (tab == null || tab.getShownIndex() != tabIndex) {
        	switch (tabIndex) {
        	case FAVORITES_TAB:
                    tab = new PhoneFavoritesFragment();
                    break;
        	case CONTACTS_TAB:
                    tab = new PhoneContactsFragment();
                    break;
        	case RECENT_TAB:
                    tab = new PhoneRecentCallsFragment();
                    break;
        	case PLACE_CALL_TAB:
                    tab = new PhonePlaceCallFragment();
                    break;
                case SETTINGS_TAB:
                    tab = new PhoneSettingsFragment();
                    break;
        	}
            // Execute a transaction, replacing any existing fragment
            // with this one inside the frame.
            FragmentTransaction ft = getFragmentManager().beginTransaction();
            ft.replace(R.id.Details, tab);
            ft.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE);
            ft.commit();
        }
    }
    
}

