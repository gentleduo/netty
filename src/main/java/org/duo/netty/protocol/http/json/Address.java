package org.duo.netty.protocol.http.json;

public class Address {

    private String city;
    private String country;
    private String state;
    private String postCode;

    public String getCity() {
        return city;
    }

    public void setCity(String city) {
        this.city = city;
    }

    public String getCountry() {
        return country;
    }

    public void setCountry(String country) {
        this.country = country;
    }

    public String getState() {
        return state;
    }

    public void setState(String state) {
        this.state = state;
    }

    public String getPostCode() {
        return postCode;
    }

    public void setPostCode(String postCode) {
        this.postCode = postCode;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("Address{");
        sb.append("city='").append(city).append('\'');
        sb.append(", country='").append(country).append('\'');
        sb.append(", state='").append(state).append('\'');
        sb.append(", postCode='").append(postCode).append('\'');
        sb.append('}');
        return sb.toString();
    }
}
