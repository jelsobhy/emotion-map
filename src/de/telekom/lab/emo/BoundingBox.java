package de.telekom.lab.emo;

public class BoundingBox {
	private double xMin,xMax,yMin,yMax;
	
	public BoundingBox(){
		xMin=0;
		xMax=0;
		yMin=0;
		yMax=0;
	}
	
	public BoundingBox(double xmin,double ymin,double xmax,double ymax){
		xMin=xmin;
		xMax=xmax;
		yMin=ymin;
		yMax=ymax;
	}
	
	public String serverSideFormated(){
		return String.format("(%f,%f,%f,%f)", this.xMin,this.yMin,this.xMax, this.yMax);
	}
	
}
