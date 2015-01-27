package de.telekom.lab.emo.db;

public class POI implements Comparable {
	public int type;
	private int id;
	public double latR, lonR, alt;
	public double lat, lon;
	float altitude;
	public float distance;
	public String name;
	// position in image
	public int x, y;

	public POI() {
		lat = -1;
		lon = -1;
		type = -1;
		altitude = 0;
		name = "unknown";
	}

	public void set(int lat, int lon) {
		this.lat = (lat / (double) 1000000);
		this.lon = (lon / (double) 1000000);
		latR = Math.toRadians(lat);
		lonR = Math.toRadians(lon);
	}

	public void set(double lat, double lon) {
		this.lat = (lat);
		this.lon = (lon);
		latR = Math.toRadians(lat);
		lonR = Math.toRadians(lon);
	}

	public static double getRadian(int d) {
		return Math.toRadians(d / (double) 1000000);
	}

	Integer ID = null;

	public Integer getID() {
		return this.ID;
	}

	public int getIDNumber() {
		return this.id;
	}

	public void setID(int id) {
		this.id = id;
		this.ID = new Integer(this.id);
	}

	@Override
	public int compareTo(Object another) {
		if (another instanceof POI) {
			POI p = (POI) another;
			if (this.distance > p.distance) {
				return -1;
			} else if (this.distance < p.distance) {
				return 1;
			} else {
				return 0;
			}
		} else {
			return 0;
		}
	}


}
