package de.telekom.lab.emo.webservices;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.text.DecimalFormat;
import java.util.Calendar;

import org.apache.http.HttpResponse;
import org.apache.http.HttpVersion;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.conn.scheme.PlainSocketFactory;
import org.apache.http.conn.scheme.Scheme;
import org.apache.http.conn.scheme.SchemeRegistry;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.conn.tsccm.ThreadSafeClientConnManager;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpParams;
import org.apache.http.params.HttpProtocolParams;
import org.apache.http.protocol.BasicHttpContext;
import org.apache.http.protocol.HttpContext;

import android.util.Log;

public class HTTPSender extends Thread {
	String url=null;
	HttpContext localContext = new BasicHttpContext();
	
	private static Calendar cal;
	private static DecimalFormat df = new DecimalFormat("00");
	private int state =0;
	private WebServiceListener wsl=null;
	
	public HTTPSender(String url,WebServiceListener wsl){
		this.url=url;
		this.wsl=wsl;
		state=WebService.IN_PROCESS;
		localContext.setAttribute("FROM", "ANDROID HTC DESIRE)");
	}
	public void run() {
		Log.d("HTTPSender","start: "+url);
		HttpClient httpClient = getClient();
		HttpResponse httpResponse = null;
		HttpGet httpGet = new HttpGet(url);
		try {localContext.setAttribute("GENERATION_TIME", getPrefix());
		
			httpResponse = httpClient.execute(httpGet, localContext);
			state=WebService.OK;
			httpGet=null;
		} catch (Exception e) {
			state=WebService.FAIL;
			e.printStackTrace();
		} 
		httpClient.getConnectionManager().closeExpiredConnections();
		wsl.onResponse(httpResponse,state);
		httpGet=null;
		wsl=null;
		httpClient.getConnectionManager().shutdown();
		Log.d("HTTPSender","end");
	}
	
	public DefaultHttpClient getClient() {
		DefaultHttpClient ret = null;
		HttpParams params = new BasicHttpParams();
		HttpProtocolParams.setVersion(params, HttpVersion.HTTP_1_1);
		HttpProtocolParams.setContentCharset(params, "utf-8");
		SchemeRegistry registry = new SchemeRegistry();
		registry.register(new Scheme("http", PlainSocketFactory.getSocketFactory(), 80));
		ThreadSafeClientConnManager tsccm = new ThreadSafeClientConnManager(params, registry);
		ret = new DefaultHttpClient(tsccm, params);
		return ret;
		
	}
	private  String getPrefix() {
		cal = Calendar.getInstance();

		int h = cal.get(Calendar.HOUR_OF_DAY);
		int m = cal.get(Calendar.MINUTE);
		int s = cal.get(Calendar.SECOND);
		
		

		StringBuffer prefix = new StringBuffer();
		prefix.append("[");
		prefix.append(df.format(h));
		prefix.append(":");
		prefix.append(df.format(m));
		prefix.append(":");
		prefix.append(df.format(s));
		prefix.append("] ");

		return prefix.toString();
	}
}
