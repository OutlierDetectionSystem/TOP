package top.centralized.cleandatasets;

import java.util.Date;

public class SingleItem {
	private Date date;
	private String item;

	public SingleItem(Date date, String item) {
		this.item = item;
		this.date = date;
	}

	public String getItem() {
		return item;
	}

	public void setItem(String item) {
		this.item = item;
	}

	public Date getDate() {
		return date;
	}

	public void setDate(Date date) {
		this.date = date;
	}

}
