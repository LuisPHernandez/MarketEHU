package domain;

import java.util.Date;

/**
 * Agrupa los datos que describen una venta nueva.
 */
public class SaleData {
    private final String title;
    private final String description;
    private final int status;
    private final float price;
    private final Date pubDate;

    public SaleData(String title, String description, int status, float price, Date pubDate) {
        this.title = title;
        this.description = description;
        this.status = status;
        this.price = price;
        this.pubDate = pubDate;
    }

    public String getTitle() { return title; }
    public String getDescription() { return description; }
    public int getStatus() { return status; }
    public float getPrice() { return price; }
    public Date getPubDate() { return pubDate; }
}

