package de.telekom.lab.emo;

import android.annotation.SuppressLint;
import android.content.Context;
import android.view.View;
import android.widget.RelativeLayout;
 
@SuppressLint("ViewConstructor")
public class CircleView extends RelativeLayout {
         static final int centerId = 111;
         private final int radius;


         
         public CircleView(Context context, int radius, View[] elements,int width, int height) {
                   super(context);
                   this.radius = radius;
 
                   @SuppressWarnings("deprecation")
				RelativeLayout.LayoutParams lpView = new RelativeLayout.LayoutParams(
                                      RelativeLayout.LayoutParams.FILL_PARENT,
                                      RelativeLayout.LayoutParams.FILL_PARENT);
                   this.setLayoutParams(lpView);
 
                   View center = new View(context);
                   center.setId(centerId);
                   RelativeLayout.LayoutParams lpcenter = new RelativeLayout.LayoutParams(
                                      0, 0);
                   lpcenter.addRule(CENTER_HORIZONTAL);
                   lpcenter.addRule(CENTER_VERTICAL);
                   
                   center.setLayoutParams(lpcenter);
                   this.addView(center);
 
                   double angle = 0;
                   

                   for (int i = 0; i < elements.length; i++) {
                	   
                	   RelativeLayout.LayoutParams lp = new RelativeLayout.LayoutParams(
                               RelativeLayout.LayoutParams.WRAP_CONTENT,
                               RelativeLayout.LayoutParams.WRAP_CONTENT);
                	   lp.addRule(RelativeLayout.ABOVE, centerId);
                	   lp.addRule(RIGHT_OF, centerId);
                	   
                	   elements[i].measure(0, 0);
                	   double step = (2 * i * Math.PI) / elements.length;
                	   angle =+ step;
                       int widthH = elements[i].getMeasuredWidth() / 2;
                       int heightH = elements[i].getMeasuredHeight() / 2;
                       double x =  (radius * Math.cos(angle)) - heightH;
                       double y =  (radius * Math.sin(angle)) - widthH;
                       
                       lp.setMargins((int)y, 0, 0, (int)x);
                       elements[i].setLayoutParams(lp);
                       this.addView(elements[i]);
                    
                   }
         }
         
         
         @Override
        protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        }


		public int getRadius() {
			return radius;
		}
 
}