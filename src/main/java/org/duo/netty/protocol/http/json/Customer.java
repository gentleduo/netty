package org.duo.netty.protocol.http.json;

import java.util.List;

public class Customer {

    private String FirstName;
    private String lastName;
    private List middleNames;

    public String getFirstName() {
        return FirstName;
    }

    public void setFirstName(String firstName) {
        FirstName = firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public List getMiddleNames() {
        return middleNames;
    }

    public void setMiddleNames(List middleNames) {
        this.middleNames = middleNames;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("Customer{");
        sb.append("FirstName='").append(FirstName).append('\'');
        sb.append(", lastName='").append(lastName).append('\'');
        sb.append(", middleNames=").append(middleNames);
        sb.append('}');
        return sb.toString();
    }
}
