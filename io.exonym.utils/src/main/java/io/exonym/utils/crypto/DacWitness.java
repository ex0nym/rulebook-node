package io.exonym.utils.crypto;

import com.ibm.zurich.idmx.interfaces.util.BigInt;

public class DacWitness {
	
	private final BigInt omega, x;
	
	public DacWitness(BigInt omega, BigInt x){
		this.omega = omega;
		this.x = x;
		
	}

	public BigInt getWitness() {
		return omega;
		
	}

	public BigInt getX() {
		return x;
		
	}
	
	public String toString(){
		return "\n" + omega + "\n" + x;
	}

}
