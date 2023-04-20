package io.exonym.lite.stats;


import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class StatisticI {
	

	private static final Logger logger = LogManager.getLogger(StatisticI.class);
	private int max, min, nonZeroMin, range, mean;
	private int[] set;
	
	public StatisticI(int[] set) {
		this.set=set;
		computeMaxMinRange();
		computeMean();
		
	}
	
	private void computeMaxMinRange() {
		min = Integer.MAX_VALUE;
		nonZeroMin = Integer.MAX_VALUE;
		
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
		int sum = 0;
		for (int i = 0; i < set.length; i++) {
			sum += set[i];
			
		}
		mean = sum / set.length;
		
	}

	public int getMax() {
		return max;
	}

	public int getMin() {
		return min;
	}

	public int getNonZeroMin() {
		return nonZeroMin;
	}

	public int getRange() {
		return range;
	}

	public int getMean() {
		return mean;
	}

	public int[] getSet() {
		return set;
	}
	
	
}
