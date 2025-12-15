package models;

// TradeMessage.java
public class TradeMessage {
    private String groupId;
    private double price;
    private long volume;
    private String direction;

    // Default constructor for Jackson
    public TradeMessage() {}

    public TradeMessage(String groupId, double price, long volume, String direction) {
        this.groupId = groupId;
        this.price = price;
        this.volume = volume;
        this.direction = direction;
    }

    // Getters and setters
    public String getGroupId() { return groupId; }
    public void setGroupId(String groupId) { this.groupId = groupId; }
    public double getPrice() { return price; }
    public void setPrice(double price) { this.price = price; }
    public long getVolume() { return volume; }
    public void setVolume(long volume) { this.volume = volume; }
    public String getDirection() { return direction; }
    public void setDirection(String direction) { this.direction = direction; }
}
