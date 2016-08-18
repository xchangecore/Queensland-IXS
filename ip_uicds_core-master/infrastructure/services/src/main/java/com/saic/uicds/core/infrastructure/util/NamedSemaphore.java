/*
 * Created on 10/11/2005
 * 
 */
package com.saic.uicds.core.infrastructure.util;

import java.util.HashMap;


/**
 * @author Jeremy Ford
 *
 * Implements a Named Semaphore.
 * 
 * i.e.
 *  Semaphore lock1 = NamedSemaphore.getNamedSemaphore("LOCK DESCRIPTION");
 *  try {
 *  lock1.entry();
 *    //do stuff;
 *  } finally {
 *    lock1.exit();
 *  }  
 *  NamedSemaphore.removeNamedSemaphore("LOCK DESCRIPTION");
 * 
 * 
 */
public class NamedSemaphore {

	/**
	 * static Map of (LockName, Semaphore)
	 */
	private static final HashMap namedSemaphores = new HashMap();
	
	/**
	 * 
	 */
	public NamedSemaphore() {
		super();		
	}
	
	public static synchronized void clear() {
		namedSemaphores.clear();
	}
	
	/**
	 * Used to get a Lock/Semaphore on a given Name.
	 * 	 * 
	 * @param lockName - Name/Description of the lock (i.e. resource) to be acquired
	 * @return Returns a Semaphore associated with the given lockName
	 */
	public static final synchronized Semaphore getNamedSemaphore(String lockName) {
		Semaphore thisLock = null;
		if (namedSemaphores.containsKey(lockName)) {
			thisLock = (Semaphore) namedSemaphores.get(lockName);
		} else {
			thisLock = new Semaphore(1);
			namedSemaphores.put(lockName, thisLock);
		}
		return thisLock;
	}
	
	/**
	 * Remove the Semaphore of the given lockName from the Map
	 * 
	 * @param lockName - Name of Semaphore to remove from the Map
	 */
	public static final synchronized void removeNamedSemaphore(String lockName) {
		Semaphore thisLock = null;
		if (namedSemaphores.containsKey(lockName)) {
			thisLock = (Semaphore) namedSemaphores.get(lockName);
			thisLock.entry();
			namedSemaphores.remove(lockName);
			thisLock.exit();
		} 
	}
	
}
