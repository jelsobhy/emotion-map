package de.telekom.lab.emo.webservices;
import java.io.ByteArrayOutputStream;

import org.apache.http.HttpResponse;

import android.os.Bundle;
import android.os.Handler;
import android.os.Message;

public class MyWSL implements WebServiceListener{
	

	
	private Handler mh;
	public MyWSL(Handler mh){
		this.mh=mh;
	}
	public void onResponse(HttpResponse httpResponse, int state) {

		if (httpResponse==null|| state!=WebService.OK)
			return;
		try {
			ByteArrayOutputStream outstream = new ByteArrayOutputStream();
			httpResponse.getEntity().writeTo(outstream);
			byte [] responseBody = outstream.toByteArray();
			outstream.flush();
			outstream.close();
			
			String st= new String(responseBody);
			Bundle b = new Bundle();
			
			
			b.putString("RS", st);
			Message msg = new Message();
			msg.setData(b);
			mh.sendMessage(msg);
			
			
			httpResponse=null;
			b=null;
			st=null;
			responseBody=null;
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} 
		
	}

}
