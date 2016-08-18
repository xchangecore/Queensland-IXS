/**
 * 
 */
package com.saic.uicds.core.em.processes.icsgen;

import com.saic.uicds.core.em.messages.IncidentStateNotificationMessage;

/**
 * @author roger
 * 
 */
public interface IncidentStateMessageAdapter {
    public void handleIncidentState(IncidentStateNotificationMessage message);
}
