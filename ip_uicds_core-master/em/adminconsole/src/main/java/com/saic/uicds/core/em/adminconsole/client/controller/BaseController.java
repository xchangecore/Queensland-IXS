package com.saic.uicds.core.em.adminconsole.client.controller;

import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.rpc.ServiceDefTarget;
import com.saic.uicds.core.em.adminconsole.client.rpc.AgreementServiceProxyAsync;
import com.saic.uicds.core.em.adminconsole.client.rpc.DirectoryServiceProxyAsync;
import com.saic.uicds.core.em.adminconsole.client.rpc.IncidentManagementServiceProxyAsync;
import com.saic.uicds.core.em.adminconsole.client.rpc.InterestGroupManagementServiceProxyAsync;
import com.saic.uicds.core.em.adminconsole.client.rpc.LoggingServiceProxyAsync;
import com.saic.uicds.core.em.adminconsole.client.rpc.ProfileServiceProxyAsync;
import com.saic.uicds.core.em.adminconsole.client.rpc.ResourceInstanceServiceProxyAsync;
import com.saic.uicds.core.em.adminconsole.client.rpc.WorkProductServiceProxyAsync;

public class BaseController {

	// Directory Service Proxy
	public static final DirectoryServiceProxyAsync directoryServiceProxyAsync = (DirectoryServiceProxyAsync) GWT
			.create(com.saic.uicds.core.em.adminconsole.client.rpc.DirectoryServiceProxy.class);
	private static final ServiceDefTarget directoryServiceDefTarget = (ServiceDefTarget) directoryServiceProxyAsync;

	// Profile Service Proxy
	public static final ProfileServiceProxyAsync profileServiceProxyAsync = (ProfileServiceProxyAsync) GWT
			.create(com.saic.uicds.core.em.adminconsole.client.rpc.ProfileServiceProxy.class);
	private static final ServiceDefTarget profileServiceDefTarget = (ServiceDefTarget) profileServiceProxyAsync;

	// WorkProduct Service Proxy
	public static final WorkProductServiceProxyAsync workProductServiceProxyAsync = (WorkProductServiceProxyAsync) GWT
			.create(com.saic.uicds.core.em.adminconsole.client.rpc.WorkProductServiceProxy.class);
	private static final ServiceDefTarget workProductServiceDefTarget = (ServiceDefTarget) workProductServiceProxyAsync;

	// InterestGroupManagement Service Proxy
	public static final InterestGroupManagementServiceProxyAsync interestGroupManagementServiceProxyAsync = (InterestGroupManagementServiceProxyAsync) GWT
			.create(com.saic.uicds.core.em.adminconsole.client.rpc.InterestGroupManagementServiceProxy.class);
	private static final ServiceDefTarget interestGroupManagementServiceDefTarget = (ServiceDefTarget) interestGroupManagementServiceProxyAsync;

	// Incident Service Proxy
	public static final IncidentManagementServiceProxyAsync incidentManagementServiceProxyAsync = (IncidentManagementServiceProxyAsync) GWT
			.create(com.saic.uicds.core.em.adminconsole.client.rpc.IncidentManagementServiceProxy.class);
	private static final ServiceDefTarget incidentManagementServiceDefTarget = (ServiceDefTarget) incidentManagementServiceProxyAsync;

	// Agreement Service Proxy
	public static final AgreementServiceProxyAsync agreementServiceProxyAsync = (AgreementServiceProxyAsync) GWT
			.create(com.saic.uicds.core.em.adminconsole.client.rpc.AgreementServiceProxy.class);
	private static final ServiceDefTarget agreementServiceDefTarget = (ServiceDefTarget) agreementServiceProxyAsync;

	// ResourceInstance Service Proxy
	public static final ResourceInstanceServiceProxyAsync resourceInstanceServiceProxyAsync = (ResourceInstanceServiceProxyAsync) GWT
			.create(com.saic.uicds.core.em.adminconsole.client.rpc.ResourceInstanceServiceProxy.class);
	private static final ServiceDefTarget resourceInstanceServiceDefTarget = (ServiceDefTarget) resourceInstanceServiceProxyAsync;

	// ResourceInstance Service Proxy
	public static final LoggingServiceProxyAsync loggingServiceProxyAsync = (LoggingServiceProxyAsync) GWT
			.create(com.saic.uicds.core.em.adminconsole.client.rpc.LoggingServiceProxy.class);
	private static final ServiceDefTarget loggingServiceDefTarget = (ServiceDefTarget) loggingServiceProxyAsync;

	// initialize every service
	static {
		directoryServiceDefTarget
				.setServiceEntryPoint("proxy/DirectoryServiceProxy");
		profileServiceDefTarget
				.setServiceEntryPoint("proxy/ProfileServiceProxy");
		workProductServiceDefTarget
				.setServiceEntryPoint("proxy/WorkProductServiceProxy");
		interestGroupManagementServiceDefTarget
				.setServiceEntryPoint("proxy/InterestGroupManagementServiceProxy");
		incidentManagementServiceDefTarget
				.setServiceEntryPoint("proxy/IncidentManagementServiceProxy");
		agreementServiceDefTarget
				.setServiceEntryPoint("proxy/AgreementServiceProxy");
		resourceInstanceServiceDefTarget
				.setServiceEntryPoint("proxy/ResourceInstanceServiceProxy");
		loggingServiceDefTarget
				.setServiceEntryPoint("proxy/LoggingServiceProxy");
	}

}
