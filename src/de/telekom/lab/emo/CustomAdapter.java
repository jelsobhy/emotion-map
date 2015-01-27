package de.telekom.lab.emo;

import java.util.List;

import android.app.Activity;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.TextView;

public class CustomAdapter extends BaseAdapter{
	
	private LayoutInflater inflater;
	private Activity context;
	private List<Item> mList;

	public CustomAdapter(Activity context, List<Item> mList) {
		this.setContext(context);
		this.mList = mList;
		inflater = (LayoutInflater)context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
	}

	public int getCount() {
		return mList.size();
	}

	public Object getItem(int position) {
		return mList.get(position);
	}

	public long getItemId(int position) {
		return position;
	}

	public View getView(final int position, View convertView, ViewGroup viewGroup) {
		View vi=convertView; 

		  if(convertView==null) 
	            vi = inflater.inflate(R.layout.popup_example, null);
        
        ImageView imageRecup = (ImageView) vi.findViewById(R.id.icon);   
        imageRecup.setImageBitmap(mList.get(position).getImage());   
        imageRecup.setPadding(5, 5, 0, 5);   
        TextView texte1Recup = (TextView) vi.findViewById(R.id.firstLine);   
        texte1Recup.setText(mList.get(position).getText());   
           
        final CheckBox cb1Recup =(CheckBox) vi.findViewById(R.id.cb1);   
        cb1Recup.setChecked(mList.get(position).isCheckBoxSetting());   
        cb1Recup.setOnClickListener(new OnClickListener() {   
               
            public void onClick(View arg0) {   
                   
                if(cb1Recup.isChecked()==true)   
                    {   
                    mList.get(position).setCheckBoxSetting(true);   
                    }   
                else  
                {   
                    mList.get(position).setCheckBoxSetting(false);   
                }   
               
            }   
        });   

        return vi;   

	}

	public List<Item> getmList() {
		return mList;
	}

	public void setmList(List<Item> mList) {
		this.mList = mList;
	}

	public Activity getContext() {
		return context;
	}

	public void setContext(Activity context) {
		this.context = context;
	}

}
