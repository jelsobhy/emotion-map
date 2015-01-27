package de.telekom.lab.emo.gui;

import java.text.SimpleDateFormat;
import java.util.Calendar;

import android.app.Dialog;
import android.content.Context;
import android.view.Gravity;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.DatePicker.OnDateChangedListener;
import android.widget.LinearLayout;
import android.widget.LinearLayout.LayoutParams;
import android.widget.TableRow;
import android.widget.TextView;
import de.telekom.lab.emo.R;
import de.telekom.lab.emo.gui.RangeSeekBar.OnRangeSeekBarChangeListener;

public class DateRangeDialog extends Dialog implements OnClickListener,OnRangeSeekBarChangeListener<Long>, OnDateChangedListener{
	
	Context context;
	Calendar minDate=Calendar.getInstance();
	Calendar maxDate=Calendar.getInstance();
	
	Calendar currentMin,currentMax;
	
	TextView mintv,maxtv;
	RangeChangeListener rangeChangeListener;
	RangeSeekBar<Long> seekBar ;
	SimpleDateFormat sdf= new SimpleDateFormat("yyyy.MM.dd");
	DatePicker dpf,dpu;
	final int DPF_ID=1000;
	final int DPU_ID=1001;
	
	public DateRangeDialog(Context context) {
		super(context);
		this.context=context;
	}
	
	public void setRange(Calendar mindat,Calendar maxdate, Calendar currentMin, Calendar currentMax, RangeChangeListener mapViewActivity){
		this.maxDate=maxdate;
		this.minDate=mindat;
		
		this.currentMin=currentMin;
		this.currentMax=currentMax;
		
		this.rangeChangeListener=mapViewActivity;
		initialize();
	}
	
	private void initialize(){
		setContentView(R.layout.time_range_dialog);
    	
    	TableRow tr = (TableRow) findViewById(R.id.tableRowRangeSeekBar);
    	
    	
    	mintv=(TextView) findViewById(R.id.textViewStart);
    	maxtv=(TextView) findViewById(R.id.textViewEnd);
    	mintv.setText(sdf.format(minDate.getTime()));
    	maxtv.setText(sdf.format(maxDate.getTime()));
    	
    	seekBar = new RangeSeekBar<Long>(minDate.getTimeInMillis(), maxDate.getTimeInMillis(), context);
    	seekBar.setOnRangeSeekBarChangeListener(this);

    	tr.addView(seekBar);
    	
    	TextView advance=(TextView) findViewById(R.id.textViewAdvanced);
    	advance.setOnClickListener(this);
    	
    	Button cancelButton=(Button) findViewById(R.id.buttonCancel);
    	cancelButton.setOnClickListener(new View.OnClickListener() {
			
			public void onClick(View v) {
				DateRangeDialog.this.cancel();
			}
		});
    	
    	Button setButton=(Button) findViewById(R.id.buttonSet);
    	setButton.setOnClickListener(new View.OnClickListener() {
			
			public void onClick(View v) {
				DateRangeDialog.this.cancel();
				rangeChangeListener.onRangeChanged(DateRangeDialog.this.currentMin, DateRangeDialog.this.currentMax);
			}
		});
	}
	
	public void onClick(View arg0) {
		LinearLayout main= (LinearLayout) findViewById(R.id.mainViewExtend);
		int childCount=main.getChildCount();
		if (childCount>0){
			main.removeAllViews();
			TextView advance=(TextView) findViewById(R.id.textViewAdvanced);
			advance.setText(context.getResources().getString(R.string.timeRangeDialogMoreDetails));
			dpf=null;
			dpu=null;
			return;
		}
		
		main.removeAllViews();
		LinearLayout llf=new LinearLayout(this.context);
	
		llf.setOrientation(LinearLayout.HORIZONTAL);
		TextView tvf= new TextView(this.context);
		tvf.setText(context.getResources().getString(R.string.timeRangeDialogFrom));
		tvf.setLayoutParams(new LinearLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.MATCH_PARENT, 0.4f));
		tvf.setGravity(Gravity.CENTER_VERTICAL);
		llf.addView(tvf);
		
		dpf= new DatePicker(this.context);
		dpf.setId(DPF_ID);
		dpf.init(currentMin.get(Calendar.YEAR), currentMin.get(Calendar.MONTH), currentMin.get(Calendar.DAY_OF_MONTH), this);
		
		dpf.setLayoutParams(new LinearLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT, 0.6f));
		llf.addView(dpf);
		LinearLayout.LayoutParams lp=new LayoutParams( LayoutParams.FILL_PARENT, LayoutParams.FILL_PARENT);
		lp.setMargins(5, 0, 5, 0);
		llf.setLayoutParams(lp);
		
		LinearLayout llu=new LinearLayout(this.context);
		llu.setOrientation(LinearLayout.HORIZONTAL);
		TextView tvu= new TextView(this.context);
		tvu.setText(context.getResources().getString(R.string.timeRangeDialogUntil));
		tvu.setGravity(Gravity.CENTER_VERTICAL);
		tvu.setLayoutParams(new LinearLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.MATCH_PARENT, 0.4f));
		
		llu.addView(tvu);
		dpu= new DatePicker(this.context);
		dpu.setId(DPU_ID);
		dpu.init(currentMax.get(Calendar.YEAR), currentMax.get(Calendar.MONTH), currentMax.get(Calendar.DAY_OF_MONTH), this);
		
		dpu.setLayoutParams(new LinearLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT, 0.6f));
		llu.addView(dpu);
		llu.setLayoutParams(lp);
		
		main.addView(llf);
		main.addView(llu);
		
		TextView advance=(TextView) findViewById(R.id.textViewAdvanced);
		advance.setText(context.getResources().getString(R.string.timeRangeDialogLessDetals));
		
	}

	
	private void updateAllTimeRanges(){
			// text views
			mintv.setText(sdf.format(currentMin.getTime()));
			maxtv.setText(sdf.format(currentMax.getTime()));
			
			// date pickers
			if (dpf!=null){
				dpf.updateDate(currentMin.get(Calendar.YEAR), currentMin.get(Calendar.MONTH), currentMin.get(Calendar.DAY_OF_MONTH));
				dpu.updateDate(currentMax.get(Calendar.YEAR), currentMax.get(Calendar.MONTH), currentMax.get(Calendar.DAY_OF_MONTH));
			}
			
			// updating the seekbar
			seekBar.setSelectedMinValue(currentMin.getTimeInMillis());
			seekBar.setSelectedMaxValue(currentMax.getTimeInMillis());
	}
	
	public void onRangeSeekBarValuesChanged(RangeSeekBar<?> bar, Long minValue,
			Long maxValue) {
		if (currentMin.getTimeInMillis()==minValue && currentMin.getTimeInMillis()==maxValue) return;
		
		currentMin.setTimeInMillis(minValue);
		currentMax.setTimeInMillis(maxValue);
		updateAllTimeRanges();
		
	}

	public void onDateChanged(DatePicker view, int year, int monthOfYear,
			int dayOfMonth) {
		switch (view.getId()){
			case DPF_ID:
				if (currentMin.get(Calendar.YEAR)==year&&
						currentMin.get(Calendar.MONTH)==monthOfYear &&
								currentMin.get(Calendar.DAY_OF_MONTH)==dayOfMonth)
					return;
				Calendar newMin=Calendar.getInstance();
				newMin.set(year, monthOfYear, dayOfMonth);
				// range check
				if (newMin.before(minDate)){
					dpf.updateDate(minDate.get(Calendar.YEAR), minDate.get(Calendar.MONTH), minDate.get(Calendar.DAY_OF_MONTH));
					currentMin=minDate;
					updateAllTimeRanges();
					return;
				}
				if (newMin.after(maxDate)){
					dpf.updateDate(maxDate.get(Calendar.YEAR), maxDate.get(Calendar.MONTH), maxDate.get(Calendar.DAY_OF_MONTH));
					currentMin=maxDate;
					updateAllTimeRanges();
					return;
				}
				
				currentMin= newMin;
				updateAllTimeRanges();
				break;
			case DPU_ID:
				if (currentMax.get(Calendar.YEAR)==year&&
						currentMax.get(Calendar.MONTH)==monthOfYear &&
								currentMax.get(Calendar.DAY_OF_MONTH)==dayOfMonth)
					return;
				// range check
				Calendar newMax=Calendar.getInstance();
				newMax.set(year, monthOfYear, dayOfMonth);
				if (newMax.after(maxDate)){
					dpu.updateDate(maxDate.get(Calendar.YEAR), maxDate.get(Calendar.MONTH), maxDate.get(Calendar.DAY_OF_MONTH));
					currentMax=maxDate;
					updateAllTimeRanges();
					return;
				}
				if (newMax.before(minDate)){
					dpu.updateDate(minDate.get(Calendar.YEAR), minDate.get(Calendar.MONTH), minDate.get(Calendar.DAY_OF_MONTH));
					currentMax=minDate;
					updateAllTimeRanges();
					return;
				}
				
				currentMax= newMax;
				updateAllTimeRanges();
				break;
		
		}
			
		
	}

}

