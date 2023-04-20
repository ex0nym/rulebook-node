package io.exonym.lite.stats;

import org.apache.commons.math3.util.ResizableDoubleArray;

public class Tracker {
	
	public enum CoordinateType {xAndY, yOnly};
	private ResizableDoubleArray rdaX = null;
	private ResizableDoubleArray rdaY = new ResizableDoubleArray();
	private boolean xAndYValues = false; 
	private int windowSize = 10;
	private final int order = 2;
	private boolean recompute = true;
	private PolynomialD polynomial = null;
	
	public Tracker(CoordinateType type) {
		this.xAndYValues = (type == CoordinateType.xAndY);
		this.rdaY.setNumElements(windowSize);
		if (this.xAndYValues){
			this.rdaX = new ResizableDoubleArray();
			this.rdaX.setNumElements(windowSize);
			
		}
	}

	private void computePolynomial() {
		double[] yElements = rdaY.getElements();
		double[][] yxData = null;
		if (yElements!=null){
			yxData = new double[yElements.length][2];
			double[] xElements = null; 
			if (xAndYValues){
				xElements = rdaX.getElements();
				
			} 
			for (int i = 0; i < yxData.length; i++) {
				yxData[i][0] = yElements[i];
				yxData[i][1] = (xAndYValues ? xElements[i] : (double)i);
				
			}
			this.polynomial = new PolynomialD(order, yxData);
			
		} else {
			this.polynomial = null;
			
		}
	}
	
	public double computeXforMaxAcceleration(){
		PolynomialD a = this.getPolynomial().getDydx().getDydx();
		double[] coeff = a.getCoeff();
		double x = -coeff[0] / (2D * coeff[1]);
		return x; 
		
	}
	
	public double computeXforMaxAccelerationWithinDomain() throws Exception{
		if (xAndYValues) {
			// TODO bug here that doesn't take into consideration when x values are defined
			throw new Exception("TODO here");
			
		}
		double x = computeXforMaxAcceleration();
		if (x >= 0 && x < this.rdaY.getNumElements()){  
			return x;
			
		} else {
			return Double.NaN;
			
		}
	}	

	public void addElement(double x, double y) throws Exception{
		if (!xAndYValues){
			throw new Exception("Programming error.  You have previously defined only an X Coodinate ");
			
		}
		this.rdaX.addElement(x);
		this.rdaY.addElement(y);
		this.recompute = true;
		
	}
	
	public void addElement(double y) throws Exception{
		if (xAndYValues){
			throw new Exception("Programming error.  You have previously defined X & Y Coodinates");
			
		}
		this.rdaY.addElement(y);
		this.recompute = true;
		
	}
	
	public PolynomialD getPolynomial(){
		if (recompute){
			computePolynomial();
			
		}
		recompute = false;
		return this.polynomial;
		
	}
	
	public void setWindowSize(int windowSize){
		this.rdaY.setNumElements(windowSize);
		if (this.xAndYValues){
			this.rdaX.setNumElements(windowSize);
			
		}
		this.windowSize=windowSize;
		this.recompute=true; 
		
	}

	public int getWindowSize() {
		return windowSize;

	}
}
