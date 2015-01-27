package de.telekom.lab.emo.webservices;

import org.apache.http.HttpResponse;

public abstract interface WebServiceListener {
	public abstract void onResponse(HttpResponse httpResponse, int state);
}
