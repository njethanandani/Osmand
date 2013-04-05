package pinpoint.dash.phone.fragments;

import java.util.List;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.AdapterView.OnItemClickListener;
import net.osmand.plus.R;
import pinpoint.dash.phone.fragments.PhoneTabFragment;
import pinpoint.dash.phone.model.Contact;

public class PhoneContactsFragment extends PhoneTabFragment {
    private ListView contactsList;
    private ArrayAdapter<Contact> contactsArrayAdapter;

    public PhoneContactsFragment() {
    	super(PhoneMenuFragment.CONTACTS_TAB);
    }

    @Override
    public int getResource() {
        return R.layout.phone_contacts_fragment;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        View view = inflater.inflate(getResource(), container, false);
        initViews(view);
        return view;
    }
    
    @Override
    public void onResume() {
        super.onResume();
        showContacts();
    }

    private void showContacts() {
        if (getPhoneActivity().getSelectedDevice() == null) {
            // TODO(natashaj): Show warning
        }
        
        contactsArrayAdapter.clear();
        List<Contact> contacts = getPhoneActivity().getContacts();
        for (int i = 0; i < contacts.size(); i++) {
            contactsArrayAdapter.add(contacts.get(i));
        }
    }
    
    private void initViews(View view) {
        contactsArrayAdapter = new ArrayAdapter<Contact>(this.getActivity(),
                android.R.layout.simple_list_item_1);
        contactsList = (ListView) view.findViewById(R.id.contactsList);
        contactsList.setAdapter(contactsArrayAdapter);
        contactsList.setOnItemClickListener(new OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view,
                    int position, long id) {
                Contact selectedContact = contactsArrayAdapter.getItem(position);
                //final Intent contactDetailsIntent = new Intent(getActivity(), ContactDetailsActivity.class);
                //activity.startActivityForResult(mapIndent, 0);
                // TODO(natashaj): Launch ContactDetailsActivity
            }
        });
    }

}