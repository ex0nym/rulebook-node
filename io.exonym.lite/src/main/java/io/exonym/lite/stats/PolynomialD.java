package io.exonym.lite.stats;

//import org.apache.commons.csv.CSVFormat;
//import org.apache.commons.csv.CSVParser;
//import org.apache.commons.csv.CSVRecord;

import java.io.File;
import java.io.PrintWriter;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Iterator;

public class PolynomialD {
	
	private final double[][] dataset;
	private int orderOfPolynomial; 
	private final double[] coeff;
	private PolynomialD dydx;
	private double[] maxima = null; 
	private double[] minima  = null;

	public PolynomialD(int order, double[][] datasetYX) {
		this.dataset = datasetYX;
		this.orderOfPolynomial = order;
		this.coeff = Regression.leastSquares(dataset, order);
		
	}

	public PolynomialD(double[] coeff) {
		this.dataset = null;
		this.orderOfPolynomial = coeff.length-1;
		this.coeff = coeff;
		
	}
	
	public PolynomialD(int order, int[] datasetX) {
		this.dataset = new double[datasetX.length][2];
		for (int i = 0; i < dataset.length; i++) {
			dataset[i][0] = (double)datasetX[i];
			dataset[i][1] = (double)i;
			
		}
		this.orderOfPolynomial = order; 
		this.coeff = Regression.leastSquares(dataset, order);		
		
	}
	
	public PolynomialD(int order, double[] datasetX) {
		this.dataset = new double[datasetX.length][2];
		for (int i = 0; i < dataset.length; i++) {
			dataset[i][0] = (double)datasetX[i];
			dataset[i][1] = (double)i;
			
		}
		this.orderOfPolynomial = order; 
		this.coeff = Regression.leastSquares(dataset, order);	
	}

	private PolynomialD computeDydx() {
		double[] result = new double[orderOfPolynomial];
		for (int i = 1; i < coeff.length; i++) {
			double a = coeff[i] * i;
			result[i-1] = a; 
			
		}
		return new PolynomialD(result);
		
	}
	
	// https://en.wikipedia.org/wiki/Root-finding_algorithm
	private void computeExtrema(){
		
		
	}
	
	public double[][] valuesForRange(double low, double high, double interval) throws Exception{
		int steps = (int) ((high - low) / interval) + 1;
		double[][] result = new double[steps][2];
		for (int i = 0; i < steps; i++) {
			double x = low + (double)i * interval;
			double y = f(x); 
			result[i][0] = y; 
			result[i][1] = x;
			
		}
		return result; 
		
	}
	
	public double f(double x) throws Exception{
		double result = 0;
		for (int i = 0; i < coeff.length; i++) {
			double x0 = 1; 
			for (int j = 0; j < i; j++) {
				x0 *= x;  
				
			}
			result += coeff[i] * x0;
			
		}
		return result;
		
	}
	
	public PolynomialD stretchCompressVertically(double a){
		double[] coeff = this.coeff.clone(); 
		for (int i = 0; i < coeff.length; i++) {
			coeff[i] *= a; 
			
		}
		return new PolynomialD(coeff);
		
	}
	
	public PolynomialD stretchCompressHorizontally(double a){
		double[] result = this.coeff.clone();
		if (result.length > 1){
			for (int i = 1; i < result.length; i++) {
				double sc = a;
				for (int j = 1; j < i; j++) {
					sc *= sc; 
					
				}
				result[i] *= sc;
				
			}
		}
		return new PolynomialD(result);
		
	}
	
	public PolynomialD reflectX(){
		double[] result = coeff.clone();
		for (int i = 0; i < coeff.length; i++) {
			result[i] = result[i] * -1;
			
		}
		return new PolynomialD(result);
		
	}
	
	public PolynomialD reflectY(){
		double[] result = coeff.clone();
		if (result.length > 1){
			for (int i = 1; i < result.length; i++) {
				if (i%2 != 0){
					result[i] = result[i] * -1;
					
				} else {
					result[i] = result[i]; 
							
				}
			}
			return new PolynomialD(result); 

		} else {
			return new PolynomialD(result);
			
		}
	} 

	public double[] getMaxima() {
		computeExtrema();
		return maxima;
		
	}

	public double[] getMinima() {
		computeExtrema();
		return minima;
		
	}

	public double[][] getDataset() {
		return dataset;
		
	}

	public int getOrder() {
		return orderOfPolynomial;
		
	}

	public double[] getCoeff() {
		return coeff;
		
	}
	
	public PolynomialD getDydx() {
		if (dydx == null){
			this.dydx = (orderOfPolynomial >= 1 ? computeDydx() : null);
			
		} 
		return dydx;
		
	}
	
//	public static PolynomialD[] readCsv(File file) throws Exception{
//		try {
//			CSVParser parser = CSVParser.parse(file, Charset.defaultCharset(), CSVFormat.RFC4180);
//			ArrayList<CSVRecord> records = (ArrayList<CSVRecord>) parser.getRecords();
//			PolynomialD[] result = new PolynomialD[records.size()];
//			int i = 0;
//			for (Iterator<CSVRecord> iterator = records.iterator(); iterator.hasNext();) {
//				CSVRecord csvRecord = (CSVRecord) iterator.next();
//				String order = csvRecord.get(0);
//				int s = Integer.parseInt(order.substring(order.lastIndexOf(" ")+1, order.length()));
//				double[] coeff = new double[s+1];
//				for (int j = 0; j < coeff.length; j++) {
//					coeff[j] = Double.parseDouble(csvRecord.get(j+1));
//
//				}
//				PolynomialD poly = new PolynomialD(coeff);
//				result[i] = poly; i++;
//
//			}
//			return result;
//
//		} catch (Exception e) {
//			throw e;
//
//		}
//	}

	public static void outputToXml(String file, String setName, PolynomialD[] polys, 
			double low, double high, double interval) throws Exception {
		
		Container[] cs = new Container[polys.length];
		for (int i = 0; i < polys.length; i++) {
			cs[i] = new Container(polys[i].valuesForRange(low, high, interval));
			
		}
		int size = cs[0].set.length;
		
		try (PrintWriter pw = new PrintWriter(file, "UTF-8")){
			pw.println("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
			pw.println("<Document>");
			
			for (int i = 0; i < size; i++) {
				pw.println("\t<item>");
				pw.println("\t\t<set>" + cs[0].set[i][1] + "</set>");
				
				for (int j = 0; j < cs.length; j++) {
					pw.println("\t\t<set" + j + ">" + cs[j].set[i][0] + "</set" + j + ">");

				}
				pw.println("\t</item>");
				
			}
			pw.println("</Document>");
			
		} catch (Exception e) {
			throw e;
			
		}
	}	
	
	public String toString(){
		String result = "Order " + this.orderOfPolynomial;
		for (int i = 0; i < coeff.length; i++) {
			result += ", " + coeff[i];
			
		}
		return result; 
		
	}
	
	private static class Container {
		public final double[][] set;
		
		public Container(double[][] set) {
			this.set=set; 
			
		}
	}	
}