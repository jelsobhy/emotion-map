package de.telekom.lab.emo.webservices;


import java.util.HashMap;
import java.util.List;

import org.apache.http.HttpVersion;
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

import de.telekom.lab.emo.db.EmoRecord;


public class WebService extends Thread{
	HashMap<String, String> kvp;
	
	public static final int FAIL=1;
	public static final int OK=2;
	public static final int IN_PROCESS=3;
	
	private static int commandOrder=0;

	
	//public static String WEB_SERVICE_URL="http://core4.bv.tu-berlin.de:8080/IndoorNSWeb";
	private StringBuffer parametersBuffer= new StringBuffer(200);
	private String servletName="callbyphone";
	
	HttpContext localContext = new BasicHttpContext();
	
	
	private static WebServiceListener wsl=null;
	
	public WebService(){
		commandOrder=0;
		kvp= new HashMap<String, String>();
		freeParameters();
		localContext.setAttribute("FROM", "ANDROID");
	}
	private void freeParameters(){
		parametersBuffer.delete(0, parametersBuffer.length());
	}
	
	public void addCallBackListener(WebServiceListener wsl){
		WebService.wsl=wsl;
	}
	
	public void addReqParameter(String key, String value){
		kvp.put(key, value);
	}	
	
	public String getReqParameter(String key){
		return kvp.get(key);
	}
	
	public String removeReqParameter(String key){
		return kvp.remove(key);
	}
	
	/**
	 * should remove. duplicated.
	 * @return
	 */
	public String make_REQUEST_INSERT(EmoRecord emr, String user){
		freeParameters();
		parametersBuffer.append(Operands.SERVICE_CALL_BY_PHONE);
		parametersBuffer.append('?');
		parametersBuffer.append(Operands.S_REQUEST);
		parametersBuffer.append("=in&emo=");
		parametersBuffer.append(emr.getEmoType());
		parametersBuffer.append("&exp=");
		parametersBuffer.append(Operands.EXPERIMENT_ID);
		parametersBuffer.append("&t=");
		parametersBuffer.append(emr.getTime());
		parametersBuffer.append("&la=");
		parametersBuffer.append(emr.getLat());
		parametersBuffer.append("&lo=");
		parametersBuffer.append(emr.getLon());
		parametersBuffer.append("&al=");
		parametersBuffer.append(emr.getAlt());
		parametersBuffer.append("&ac=");
		parametersBuffer.append(emr.getAcc());
		parametersBuffer.append("&ur=");
		parametersBuffer.append(user);
		parametersBuffer.append("&r=");
		parametersBuffer.append(Math.random()*1000);
		return Operands.WEB_SERVICE_URL+ parametersBuffer.toString();
	}
	
	public String make_REQUEST_GET_ALL(){
		freeParameters();
		parametersBuffer.append("wm?rq=all");
		return Operands.WEB_SERVICE_URL+parametersBuffer.toString();
	}
	
	public String make_REQUEST_SYNCHRONIZE(List<EmoRecord> emrs, String user){
		freeParameters();
		for (EmoRecord emr : emrs) {
		parametersBuffer.append(Operands.SERVICE_CALL_BY_PHONE);
		parametersBuffer.append('?');
		parametersBuffer.append(Operands.S_REQUEST);
		parametersBuffer.append("=in&emo=");
		parametersBuffer.append(emr.getEmoType());
		parametersBuffer.append("&exp=");
		parametersBuffer.append(Operands.EXPERIMENT_ID);
		parametersBuffer.append("&t=");
		parametersBuffer.append(emr.getTime());
		parametersBuffer.append("&la=");
		parametersBuffer.append(emr.getLat());
		parametersBuffer.append("&lo=");
		parametersBuffer.append(emr.getLon());
		parametersBuffer.append("&al=");
		parametersBuffer.append(emr.getAlt());
		parametersBuffer.append("&ac=");
		parametersBuffer.append(emr.getAcc());
		parametersBuffer.append("&ur=");
		parametersBuffer.append(user);
		parametersBuffer.append("&r=");
		parametersBuffer.append(Math.random()*1000);
		}
		return Operands.WEB_SERVICE_URL+ parametersBuffer.toString();
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
	boolean rleased=true;
	
	
	public void execute(String url){
		HTTPSender hs= new HTTPSender(url, wsl);
		hs.start();
	}
	public void execute(String url, WebServiceListener wsl){
		HTTPSender hs= new HTTPSender(url, wsl);
		hs.start();
	}
	
}
