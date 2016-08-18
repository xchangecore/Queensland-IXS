/*
 * Created on 10/11/2005
 * 
 */
package com.saic.uicds.core.infrastructure.util;

/**
 * 
 * @author Jeremy Ford ('assisted' by Google)
 *
 * Your basic Semaphore object with entry() and exit()
 * 
 */

public class Semaphore {
	
	private int allowed;
	
	public Semaphore(int limit) {
		allowed = limit;
		if (allowed < 1) 
			allowed = 1;
	}
	
	public final synchronized void entry() {
		while (allowed <= 0) {
			try{
				wait();
			} catch (Exception whatever) {}
		}
		allowed--; 
	}
	
	public final synchronized void exit() {
		allowed++;
		notify();
	}
	
	public final synchronized void throttle() {
		while (allowed <= 0) {
			try {
				wait();
			} catch (Exception whatever) {}
		}
	}
}
