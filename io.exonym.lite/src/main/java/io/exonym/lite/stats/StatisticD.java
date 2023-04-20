package io.exonym.lite.stats;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class StatisticD {
	
	private static final Logger logger = LogManager.getLogger(StatisticD.class);
	private double max, min, nonZeroMin, range, mean;
	private double[] set;
	
	public StatisticD(double[] set) {
		this.set=set; 
		computeMaxMinRange();
		computeMean();
		
	}
	
	private void computeMaxMinRange() {
		min = Double.MAX_VALUE; 
		nonZeroMin = Double.MAX_VALUE; 
		
		for (int i = 0; i < set.length; i++) {
			if (set[i] > max){
				max = set[i];
				
			} else if (set[i] < min){
				min = set[i];
				
			}
			if (set[i]!=0 && set[i] < nonZeroMin){
				nonZeroMin = set[i];
				
			}
		}
		range = max - min;
		
	}

	private void computeMean() {
		double sum = 0;
		for (int i = 0; i < set.length; i++) {
			sum += set[i];
			
		}
		mean = sum / set.length;
		
	}

	/**
	 * Computes the average value based on the number of ticks
	 *  
	 * specified in nTicksForAv at the beginning and the end of the file.
	 * 
	 * @param dataset the dataset to be analysed truncated at the beginning and end of the evaluation period. 
	 * @param nTicksForAv the number of ticks used to compute the average value.
	 * @return the slope based on linear interpolation of the the two results. 
	 * @throws Exception
	 */
	public static double computeDriftVelocityForTickMovement(double[] dataset, int nTicksForAv) throws Exception {
		try {
			if (dataset==null){
				throw new Exception("Dataset is null");
				
			} if (dataset.length < nTicksForAv * 1.5){
				throw new Exception("Dataset is not long enough for averaging over the number of ticks " + nTicksForAv);
				
			} if (nTicksForAv < 1){
				throw new Exception("Number of ticks to average over must be greater than 0. " + nTicksForAv);
				
			}
			double startAv = 0, endAv = 0;
			for (int i = 0; i < nTicksForAv; i++) {
				startAv += dataset[i];
				
			}
			int len = dataset.length-1;
			for (int i = len; i > len-nTicksForAv; i--) {
				endAv += dataset[i];
				
			}
			startAv /= nTicksForAv;
			endAv /= nTicksForAv;
			// y=mx+c (where c = startAv), (returning m);
			double x1 = dataset.length/startAv;  
			return (endAv - startAv)/x1; 
			
		} catch (Exception e) {
			throw e; 
			
		}
	}
	
	// Same as no drift? 
	public static double probabilityOfXTicksInTMoves(double x, double t, double drift) throws Exception{
		try {
			
			return 0; 
			
		} catch (Exception e) {
			throw e; 
		}
	}
	
	
	/**
	 * Approximate probability distribution.  2/sqrt(2*pi*t) * exp(-x/(2t))
	 * 
	 * @param x the number of ticks expected to move
	 * @param t the number of ticks willing to wait.
	 * @return the probability of that number being reached.
	 * 
	 * @throws Exception
	 */
	public static double probabilityOfXTicksInTMoves(double x, double t) throws Exception{
		try {
			if (x==0 || Double.isInfinite(x) || Double.isNaN(x)){
				throw new Exception("Poorly defined x=" + x);
				
			}  if (t == 0 || Double.isInfinite(t) || Double.isNaN(t)){
				throw new Exception("Poorly defined t=" + t);
				
			}	
			double coeff = 2d / (Math.sqrt(2d*Math.PI * t));
			return coeff * Math.exp(-x * x/(2d * t));

		} catch (Exception e) {
			throw e; 
			
		}
	}
	
	/**
	 * The probability that the value x will be returned given that x0 is the mean with a standard deviation of sigmaSquared.
	 * 
	 * @param x0
	 * @param x
	 * @param sigmaSquared
	 * @return
	 * @throws Exception
	 */
	public static double normalDistribution(double x0, double x, double sigmaSquared) throws Exception{
		try {
			if (Double.isInfinite(x0) || Double.isNaN(x0)){
				throw new Exception("Poorly defined x0=" + x0);
				
			} if (Double.isInfinite(x) || Double.isNaN(x)){
				throw new Exception("Poorly defined x=" + x);
				
			}  if (sigmaSquared == 0 || Double.isInfinite(sigmaSquared) || Double.isNaN(sigmaSquared)){
				throw new Exception("Poorly defined sigma=" + sigmaSquared);
				
			}
			double coeff = 1/(Math.sqrt(2d*Math.PI*sigmaSquared));
			double sq = (x-x0);
			return coeff * Math.exp(-sq * sq/(2d * sigmaSquared));
			
		} catch (Exception e) {
			throw e; 
			
		}
	}
	
	/**
	 * Computes the second moment for a zero correlation function.
	 * 
	 * @param moment1
	 * @return moment2
	 * @throws Exception
	 */
	public static double m2ForZeroCorrelation(double moment1) throws Exception {
		try {
			if (moment1 == 0 || Double.isInfinite(moment1) || Double.isNaN(moment1)){
				throw new Exception("Poorly defined moment1=" + moment1);
				
			}
			return moment1*moment1;
			
		} catch (Exception e) {
			throw e; 
			
		}
	}
	
	/**
	 * Computes sigmaSquared form a dataset.  Avoid using if possible.  
	 * 
	 * @see m2ForZeroCorrection, sigmaSquared(double moment1, double moment2) 
	 * where moment1 is the average for an even propability function.   
	 * @param dataset
	 * @param average
	 * @return
	 * @throws Exception
	 */
	public static double sigmaSquared(double[] dataset, double average) throws Exception{
		try {
			if (dataset==null){
				throw new Exception("dataset is null");
				
			} if (dataset.length < 1){
				throw new Exception("dataset contains less than 2 results");
				
			} if (average == 0 || Double.isInfinite(average) || Double.isNaN(average)){
				throw new Exception("Poorly defined average (" + average + ")");
				
			}
			double result = 0; 
			for (int i = 0; i < dataset.length; i++) {
				double xp = (dataset[i] - average);
				result += xp*xp;
			}
			return result/(dataset.length-1);
			
		} catch (Exception e) {
			throw e; 

		}
	}
	
	/**
	 * Computes sigmaSquared from M1 and M2.  
	 * (The fastest way to compute sigmaSquared if the information is available)
	 *  
	 * @param moment1
	 * @param moment2
	 * @return
	 * @throws Exception
	 * 
	 */
	public static double sigmaSquared(double moment1, double moment2) throws Exception{
		try {
			if (Double.isNaN(moment1) || Double.isNaN(moment2) || Double.isInfinite(moment1) || Double.isInfinite(moment2)){ 
				throw new Exception("Moments aren't real : M1=" + moment1 + " M2=" + moment2 );
				
			}
			return moment2 - moment1*moment1; 
			
		} catch (Exception e) {
			throw e; 
			
		}
	}
	
	
	/* public static void moment(double[] dataset, int k){
	}//*/
	
	/**
	 * Returns the average of a set of doubles.  
	 * The routine will ignore NaN but any NaN values will still count towards N
	 * 
	 * @param dataset
	 * @return
	 * @throws Exception Protects against zero length and null arrays.
	 * 
	 */
	public static double averageEqualProbability(double[] dataset) throws Exception {
		try {
			if (dataset==null){
				throw new Exception("Values array is null");
				
			} if (dataset.length==0){
				throw new Exception("Values array is zero length");
				
			}
			double total = 0; 
			for (int i = 0; i < dataset.length; i++) {
				if (!Double.isNaN(dataset[i])){
					total+=dataset[i];
					
				}
			}
			return total/dataset.length; 	
			
		} catch (Exception e) {
			throw e;
			
		}
	}
	
	/**
	 * Retrns the average of a set of doubles located in the specified column.
	 * The routine will ignore NaN but any NaN values will still count towards N
	 * 
	 * @param dataset
	 * @param col
	 * @return
	 * @throws Exception
	 * 
	 */
	public static double averageEqualProbability(double[][] dataset, int col) throws Exception {
		try {
			if (dataset==null){
				throw new Exception("Values array is null");
				
			} if (dataset.length==0){
				throw new Exception("Values array is zero length");
				
			}
			double total = 0; 
			for (int i = 0; i < dataset.length; i++) {
				if (!Double.isNaN(dataset[i][col])){
					total+=dataset[i][col];
					
				}
			}
			return total/dataset.length; 	
			
		} catch (Exception e) {
			throw e;
			
		}
	}

	public double getMax() {
		return max;
	}

	public double getMin() {
		return min;
	}

	public double getNonZeroMin() {
		return nonZeroMin;
	}

	public double getRange() {
		return range;
	}

	public double getMean() {
		return mean;
	}

	public double[] getSet() {
		return set;
	}
	
	
}
