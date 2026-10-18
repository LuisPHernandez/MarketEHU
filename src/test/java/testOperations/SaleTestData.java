package testOperations;

import java.util.Date;

public class SaleTestData {
	private String title;
	private String description;
	private int status;
	private float price;
	private Date pubDate;

	public SaleTestData(String title, String description, int status, float price, Date pubDate) {
		this.title = title;
		this.description = description;
		this.status = status;
		this.price = price;
		this.pubDate = pubDate;
	}

	public String getTitle() {
		return title;
	}

	public String getDescription() {
		return description;
	}

	public int getStatus() {
		return status;
	}

	public float getPrice() {
		return price;
	}

	public Date getPubDate() {
		return pubDate;
	}
}