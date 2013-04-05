package pinpoint.dash.phone.fragments;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.AdapterView.OnItemClickListener;
import net.osmand.plus.R;
import pinpoint.dash.phone.activities.PhoneActivity;
import pinpoint.dash.phone.model.Device;

public class ContactDetailsFragment extends PhoneTabFragment {

    public ContactDetailsFragment() {
        super(PhoneMenuFragment.CONTACT_DETAILS_TAB);
    }

    @Override
    public int getResource() {
        return R.layout.contact_details_fragment;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        View view = inflater.inflate(getResource(), container, false);
        initViews(view);
        return view;
    }
    
    private void initViews(View view) {
    }
}
