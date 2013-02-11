package pinpoint.dash.phone.model;

public class Phone {
    private String name;
    private String address;
    
    public Phone(String name, String address) {
        setName(name);
        setAddress(address);
    }
    
    public String getName() {
        return name;
    }
    
    public void setName(String name) {
        this.name = name;
    }
    
    public String getAddress() {
        return address;
    }
    
    public void setAddress(String address) {
        this.address = address;
    }
    
    @Override
    public String toString() {
        return getName() + "\n" + getAddress();
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        
        if (!(o instanceof Phone)) {
            return false;
        }
        
        Phone other = (Phone) o;
        return (getName() == null ? other.getName() == null
                : getName().equals(other.getName())) &&
               (getAddress() == null ? other.getAddress() == null
                : getAddress().equals(other.getAddress()));
    }
}
