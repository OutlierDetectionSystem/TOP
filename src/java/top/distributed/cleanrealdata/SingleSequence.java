package top.distributed.cleanrealdata;

import java.util.Date;

public class SingleSequence {
	private Date date;
	private int sequence;

	public SingleSequence(Date date, int sequence) {
		this.sequence = sequence;
		this.date = date;
	}

	public int getSequence() {
		return sequence;
	}

	public void setSequence(int sequence) {
		this.sequence = sequence;
	}

	public Date getDate() {
		return date;
	}

	public void setDate(Date date) {
		this.date = date;
	}

}
