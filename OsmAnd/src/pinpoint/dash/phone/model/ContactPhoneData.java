package pinpoint.dash.phone.model;

public class ContactPhoneData {

    private String number;
    private ContactPhoneType type;
    
    public String getNumber() {
        return number;
    }
    
    public void setNumber(String number) {
        this.number = number;
    }
    
    public ContactPhoneType getType() {
        return type;
    }
    
    public void setType(ContactPhoneType type) {
        this.type = type;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        
        if (!(o instanceof ContactPhoneData)) {
            return false;
        }
        
        ContactPhoneData other = (ContactPhoneData) o;
        return (getType() == other.getType()) &&
                (getNumber() == null ? other.getNumber() == null : getNumber().equals(other.getNumber()));
    }
}
