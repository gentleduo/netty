package org.duo.netty.protocol.http.json;

public class Order {

    private Customer customer;
    private Address billTo;
    private Address shipTo;

    public Customer getCustomer() {
        return customer;
    }

    public void setCustomer(Customer customer) {
        this.customer = customer;
    }

    public Address getBillTo() {
        return billTo;
    }

    public void setBillTo(Address billTo) {
        this.billTo = billTo;
    }

    public Address getShipTo() {
        return shipTo;
    }

    public void setShipTo(Address shipTo) {
        this.shipTo = shipTo;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("Order{");
        sb.append("customer=").append(customer);
        sb.append(", billTo=").append(billTo);
        sb.append(", shipTo=").append(shipTo);
        sb.append('}');
        return sb.toString();
    }
}
