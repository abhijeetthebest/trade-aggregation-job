package models;

// AggregatedTrade.java
public class AggregatedTrade {
    private String groupId;
    private double price;
    private String direction;
    private long totalVolume;

    public AggregatedTrade() {}

    public AggregatedTrade(String groupId, double price, String direction, long totalVolume) {
        this.groupId = groupId;
        this.price = price;
        this.direction = direction;
        this.totalVolume = totalVolume;
    }

    public String getGroupId() { return groupId; }
    public double getPrice() { return price; }
    public String getDirection() { return direction; }
    public long getTotalVolume() { return totalVolume; }

    public void setGroupId(String groupId) { this.groupId = groupId; }
    public void setPrice(double price) { this.price = price; }
    public void setDirection(String direction) { this.direction = direction; }
    public void setTotalVolume(long totalVolume) { this.totalVolume = totalVolume; }
}
