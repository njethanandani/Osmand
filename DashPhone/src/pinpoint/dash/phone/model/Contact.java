package pinpoint.dash.phone.model;

import java.util.ArrayList;
import java.util.List;

// TODO(natashaj): Replace with ContactsContract Android contacts api
public class Contact {
    private String name;
    private List<ContactPhoneData> phones;
    
    public Contact(String name) {
        setName(name);
    }
    
    public Contact(String name, List<ContactPhoneData> phones) {
        setName(name);
        setPhones(phones);
    }
    
    public String getName() {
        return name;
    }
    
    public void setName(String name) {
        this.name = name;
    }
    
    public List<ContactPhoneData> getPhones() {
        if (phones == null) {
            phones = new ArrayList<ContactPhoneData>();
        }
        return phones;
    }
    
    public void setPhones(List<ContactPhoneData> phones) {
        this.phones = phones;
    }
    
    @Override
    public String toString() {
        return getName();
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        
        if (!(o instanceof Contact)) {
            return false;
        }
        
        Contact other = (Contact) o;
        boolean isEqual = (getName() == null ? other.getName() == null
                : getName().equals(other.getName()));
        if (isEqual) {
            // TODO(natashaj): add code to compare phones data with hash map
        }
        return isEqual;
    }
}
