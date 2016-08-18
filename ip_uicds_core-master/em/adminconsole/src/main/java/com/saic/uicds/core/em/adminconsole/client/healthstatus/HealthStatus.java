package com.saic.uicds.core.em.adminconsole.client.healthstatus;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.google.gwt.dom.client.Node;
import com.google.gwt.dom.client.NodeList;
import com.google.gwt.event.dom.client.ChangeEvent;
import com.google.gwt.event.dom.client.ChangeHandler;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.dom.client.KeyUpEvent;
import com.google.gwt.event.dom.client.KeyUpHandler;
import com.google.gwt.event.logical.shared.CloseEvent;
import com.google.gwt.event.logical.shared.CloseHandler;
import com.google.gwt.user.client.Timer;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.DockPanel;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.ListBox;
import com.google.gwt.user.client.ui.PopupPanel;
import com.google.gwt.user.client.ui.ScrollPanel;
import com.google.gwt.user.client.ui.TextBox;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.saic.uicds.core.em.adminconsole.client.controller.BaseController;
import com.saic.uicds.core.em.adminconsole.client.stropheXmpp.Connection;
import com.saic.uicds.core.em.adminconsole.client.stropheXmpp.ConnectionStatus;
import com.saic.uicds.core.em.adminconsole.client.stropheXmpp.Element;
import com.saic.uicds.core.em.adminconsole.client.stropheXmpp.Handler;
import com.saic.uicds.core.em.adminconsole.client.view.Util;

/**
 * HealthStatus
 * 
 * DockPanel UI contians: NorthDock - BoshConnection Status, Disconnect, and Jid
 * Registration CenterDock - Components and its Status EastDock - Shared Cores
 * Information
 * 
 * @author Santhosh Amanchi - Image Matters, LLC
 * @created
 */

public class HealthStatus extends DockPanel implements HealthStatusConstants {

	public Map<String, ComponentPanel> componentPanelMap = new HashMap<String, ComponentPanel>();
	private String connectionJid;
	private List<String> otherSessionJids = new ArrayList<String>();
	private Button registerButton = new Button(REGISTER_USERS);
	private Button disconnectButton = new Button(DISCONNECT);
	private Connection localConnection;
	private Connection remoteConnection;
	private CustomDialogBox addDialogBox;
	private VerticalPanel jidPanel;
	private Button regButton;
	private Button delButton;
	private Button addButton;
	private TextBox jidTextBox;
	private Label statusLabel = new Label("Status: CONNECTING");
	private int jidLimit = 10;
	private ListBox registerJidList = new ListBox();
	private List<String> deleteJidList;
	private int connectonTimeOut = 60000;
	private SharedCorePanel sharedCorePanel;
	private String coreName;
	private String corePassword;
	private String boshServiceUrl;
	private String password;
	private String consoleResource;
	

	public HealthStatus() {

		setHorizontalAlignment(DockPanel.ALIGN_RIGHT);
		HorizontalPanel northPanel = createNorthDock();
		add(northPanel, DockPanel.NORTH);
		add(new HTML("<left><br>", true), DockPanel.NORTH);

		VerticalPanel centerPanel = createCenterDockPanel();
		add(centerPanel, DockPanel.CENTER);

		VerticalPanel eastDock = createEastDock();
		add(eastDock, DockPanel.EAST);

		startBoshConnection();
		//statusLabel.getElement().setAttribute("tabIndex", "0");
		statusLabel.getElement().setAttribute("aria-label",
				"XMPP " + statusLabel.getText());
		registerButton.getElement().setAttribute("aria-label",
				"Register Users: Add or remove one or more users");
		registerJidList.getElement().setAttribute("aria-label",
				"List of registered users");
		registerJidList.setTitle("RegisteredJidList");

	}

	private void startBoshConnection() {
		BaseController.directoryServiceProxyAsync
				.getConfigurationMap(new AsyncCallback<Map<String, String>>() {
					@Override
					public void onFailure(Throwable e) {
						Util.ERROR("Failed to retreive the Core Name: "
								+ e.getMessage());
					}

					@Override
					public void onSuccess(Map<String, String> configurationMap) {
						coreName = configurationMap.get("coreName");
						corePassword = configurationMap.get("corePassword");
						consoleResource = configurationMap.get("resource");
						boshServiceUrl = configurationMap.get("boshServiceUrl");
						String timeOut = configurationMap
								.get("connectionTimeOut");
						if (timeOut != null) {
							connectonTimeOut = Integer.parseInt(timeOut);
						}
						establishConnection(coreName, consoleResource,
								corePassword, boshServiceUrl);
					}
				});
	}

	public void establishConnection(final String coreName,
			final String consoleResource, final String password,
			final String boshService) {
		BaseController.loggingServiceProxyAsync
				.getConnectionInfo(new AsyncCallback<Map<String, String>>() {
					@Override
					public void onFailure(Throwable e) {
						Util.ERROR("Failed to retreive the Core Name: "
								+ e.getMessage());
					}

					@Override
					public void onSuccess(Map<String, String> connectionMap) {
						String jid = coreName;
						localConnection = new Connection(boshService);
						localConnection.doconnect(jid, password,
								localStatusCallback);

						String remoteJid = connectionMap.get("remoteJid");
						if (remoteJid == null) {
							jid = coreName + "/" + consoleResource;
							remoteConnection = new Connection(boshService);
							remoteConnection.doconnect(jid, password,
									remoteStatusCallback);
						}
						initiateTimer();
					}
				});
	}

	/** BoshConnectionStatus Callback */
	private ConnectionStatus localStatusCallback = new ConnectionStatus() {
		@Override
		public void statusChanged(Status status, String reason) {
			if (status.equals(Status.CONNECTED)) {
				statusLabel.setText("Status: " + status);
				localConnection.goOnline();
				connectionJid = localConnection.getCoreJid();
				localConnection.addMessageHandler(null, null, "message", null,
						localMessageHandler);
				BaseController.loggingServiceProxyAsync.setLocalCoreJid(
						connectionJid, new ConnectionCallback());
				disconnectButton.setEnabled(true);
			} else if (status.equals(Status.CONNECTING)) {
				statusLabel.setText("Status: " + status);
			} else if (status.equals(Status.DISCONNECTED)) {
				statusLabel.setText("Status: " + status);
			}
			statusLabel.getElement().setAttribute("aria-label",
					"XMPP " + statusLabel.getText());
		}
	};

	/** BoshConnectionStatus Callback */
	private ConnectionStatus remoteStatusCallback = new ConnectionStatus() {
		@Override
		public void statusChanged(Status status, String reason) {
			if (status.equals(Status.CONNECTED)) {
				remoteConnection.goOnline();
				remoteConnection.getRid();
				remoteConnection.getSid();
				String rid = remoteConnection.getRidValue();
				String sid = remoteConnection.getSidValue();
				String jid = remoteConnection.getCoreJid();
				Map<String, String> connectionInfo = new HashMap<String, String>();
				connectionInfo.put("remoteJid", jid);
				connectionInfo.put("rid", rid);
				connectionInfo.put("sid", sid);

				BaseController.loggingServiceProxyAsync.setConnectionInfo(
						connectionInfo, new AsyncCallback<String>() {
							@Override
							public void onFailure(Throwable arg0) {
							}

							@Override
							public void onSuccess(String arg0) {
							}
						});
				remoteConnection.addMessageHandler(null, null, "message", null,
						remoteMessageHandler);
			}
		}
	};

	/** Message Handler for Xmpp Messages */
	private Handler<Element> localMessageHandler = new Handler<Element>() {
		@Override
		public boolean handle(Object element) {
			Element elem = (Element) element;
			String from = elem.getAttribute("from");
			String to = elem.getAttribute("to");
			if (from.contains("CoreConnection")
					|| from.contains("ConsoleConnection")) {
				String message = getMessageFromElement(elem);
				message += "From: " + from + " \nTo: " + to;
				if (message.contains("DirectoryService-CoreStatus:")) {
					handleCorMessage(message);
				} else if (message.contains("Remote-CoreStatus:")) {
					handleRemoteMessage(message);
				} else if (message.contains("Notify-CoreStatus:")) {
					handleCoreEnhancedMessage(message);
				} else if (message.contains("ComponentId-History")) {
					handleHistoryMessage(message);
				} else if (message.contains("EstablishConsoleConnection")) {
					establishRemoteConnection();
				} else {
					if (!message.contains("History"))
						handleLogMessage(message);
				}
			}
			return true;
		}

	};

	private void establishRemoteConnection() {
		BaseController.loggingServiceProxyAsync
				.getConnectionInfo(new AsyncCallback<Map<String, String>>() {
					@Override
					public void onFailure(Throwable e) {
						Util.ERROR("Failed to retreive the Core Name: "
								+ e.getMessage());
					}

					@Override
					public void onSuccess(Map<String, String> connectionMap) {
						String jid = coreName;
						String remoteJid = connectionMap.get("remoteJid");
						if (remoteJid == null) {
							jid = coreName + "/" + consoleResource;
							remoteConnection = new Connection(boshServiceUrl);
							remoteConnection.doconnect(jid, password,
									remoteStatusCallback);
						}
					}
				});
	}

	/** Message Handler for Xmpp Messages */
	private Handler<Element> remoteMessageHandler = new Handler<Element>() {
		@Override
		public boolean handle(Object element) {
			Element elem = (Element) element;
			String from = elem.getAttribute("from");
			String to = elem.getAttribute("to");
			String msg = getMessageFromElement(elem);
			String message = msg;
			message += "From: " + from + " \nTo: " + to;
			if (message.contains("Remote-CoreStatus:")) {
				handleRemoteMessage(message);
				sendMsgToOtherSessions(msg);
			} else if (message.contains("Shared-CoreStatus:")) {
				handleOtherSharedCoreMessage(message);
			} else if (message.contains("Notify-CoreStatus:")) {
				handleCoreEnhancedMessage(message);
			} else {
			}

			return true;
		}

		private void handleOtherSharedCoreMessage(final String message) {
			BaseController.loggingServiceProxyAsync
					.getAllJids(new AsyncCallback<List<String>>() {
						@Override
						public void onFailure(Throwable arg0) {
						}

						@Override
						public void onSuccess(List<String> jids) {
							otherSessionJids.clear();
							otherSessionJids.addAll(jids);
							otherSessionJids.remove(connectionJid);
							for (CorePanel core : sharedCorePanel.sharedCores
									.values()) {
								String coreName = core.getCoreId();
								String coreStatus = core.getCoreStatus();
								String sharedmessage = "Remote-CoreStatus: ["
										+ coreName + "]\n";
								sharedmessage += "Status: [" + coreStatus + "]";
								for (String jid : otherSessionJids) {
									remoteConnection.doSendMessage(
											sharedmessage, jid);
								}
							}
						}
					});
		}

		private void sendMsgToOtherSessions(final String msg) {
			BaseController.loggingServiceProxyAsync
					.getAllJids(new AsyncCallback<List<String>>() {
						@Override
						public void onFailure(Throwable arg0) {
						}

						@Override
						public void onSuccess(List<String> jids) {
							otherSessionJids.clear();
							otherSessionJids.addAll(jids);
							otherSessionJids.remove(connectionJid);
							for (String jid : otherSessionJids) {
								remoteConnection.doSendMessage(msg, jid);
							}
						}
					});
		}
	};

	/**
	 * AsyncCallback for Registering external Jids with LogginService to receive
	 * log event messages
	 */
	private class RegisterJidCallback implements AsyncCallback<List<String>> {
		public void onFailure(Throwable e) {
			Util.ERROR("Add Jid failed: " + e.getMessage());
		}

		public void onSuccess(List<String> jidList) {
			registerJidList.clear();
			for (String jid : jidList) {
				registerJidList.addItem(jid);
			}
			registerJidList.setFocus(true);
			delButton.setEnabled(false);
		}
	}

	ClickHandler jidListHandler = new ClickHandler() {
		@SuppressWarnings("deprecation")
		@Override
		public void onClick(ClickEvent event) {
			final CustomDialogBox registerDialogBox = new CustomDialogBox(
					"Registered Jid(s)", true);
			registerDialogBox.center();

			VerticalPanel panel = new VerticalPanel();
			panel.setWidth("215px");
			panel.setSpacing(5);

			VerticalPanel jidPanel = new VerticalPanel();
			jidPanel.setSpacing(3);
			jidPanel.add(registerJidList);

			HorizontalPanel buttonPanel = new HorizontalPanel();
			buttonPanel.setSpacing(3);
			Button addButton = new Button("Add", addJidPanelHandler);
			addButton.getElement().setAttribute("aria-label",
					"Add opens a popup to enter one or more core JID");
			delButton = new Button("Remove", deleteJidHandler);
			delButton
					.getElement()
					.setAttribute(
							"aria-label",
							"Delete opens a popup to confirm unregistering the list of users selected in the list box");
			delButton.setEnabled(false);
			addButton.setEnabled(true);
			
			Button closeButton = new Button("Close");
			closeButton.addClickHandler(new ClickHandler() {
				@Override
				public void onClick(ClickEvent arg0) {
					registerDialogBox.removeFromParent();
				}
			});
			
			buttonPanel.add(addButton);
			buttonPanel.add(delButton);
			buttonPanel.add(closeButton);

			panel.add(jidPanel);
			panel.add(buttonPanel);
			registerDialogBox.setWidget(panel);
			registerDialogBox.getElement().setAttribute("tabIndex", "0");
			registerJidList.setWidth("250px");
			registerJidList.setMultipleSelect(true);
			registerJidList.addChangeHandler(new ChangeHandler() {
				@Override
				public void onChange(ChangeEvent arg0) {
					delButton.setEnabled(true);
				}
			});
			BaseController.loggingServiceProxyAsync
					.getRegisteredCoreIds(new AsyncCallback<List<String>>() {
						@Override
						public void onSuccess(List<String> jidList) {
							registerJidList.clear();
							for (String jid : jidList) {
								registerJidList.addItem(jid);
							}
							registerDialogBox.showDialog();
							delButton.setEnabled(false);
						}

						@Override
						public void onFailure(Throwable e) {
							Util.ERROR("Error retreiving registered jids"
									+ e.getMessage());
						}
					});
		}
	};

	ClickHandler addJidHandler = new ClickHandler() {
		@Override
		public void onClick(ClickEvent event) {
			TextBox jidTextBox = new TextBox();
			jidTextBox.setWidth("200px");
			jidTextBox.setTitle("Jid");
			jidPanel.add(jidTextBox);
			if (jidPanel.getWidgetCount() == jidLimit) {
				addButton.setEnabled(false);
			}
		}
	};

	ClickHandler addJidPanelHandler = new ClickHandler() {
		@Override
		public void onClick(ClickEvent event) {
			addDialogBox = new CustomDialogBox("Register Jid(s)", true);
			addDialogBox.center();
			addDialogBox.showDialog();
			addDialogBox.getElement().setAttribute("tabIndex", "0");
			addDialogBox.addCloseHandler(new CloseHandler<PopupPanel>() {
				@Override
				public void onClose(CloseEvent<PopupPanel> arg0) {
					registerJidList.setFocus(true);
				}
			});
			VerticalPanel panel = new VerticalPanel();
			panel.setWidth("215px");
			panel.setSpacing(5);

			jidPanel = new VerticalPanel();
			jidPanel.setSpacing(3);
			jidTextBox = new TextBox();
			jidTextBox.setTitle("Jid");
			jidTextBox
					.getElement()
					.setAttribute(
							"aria-label",
							"TextBox to enter user Jid. "
									+ "A valid jid is: string followed by @ followed by string. "
									+ "AddAnother and Register buttons will not be enabled "
									+ "unless atleast one valid jid is entered");
			jidTextBox.addKeyUpHandler(new KeyUpHandler() {
				@Override
				public void onKeyUp(KeyUpEvent arg0) {
					boolean validJid = isValid(jidTextBox.getText());
					if (validJid) {
						addButton.setEnabled(true);
						regButton.setEnabled(true);
					} else {
						if (addButton.isEnabled())
							addButton.setEnabled(false);
						if (regButton.isEnabled())
							regButton.setEnabled(false);
					}
				}
			});
			jidTextBox.setWidth("200px");
			jidPanel.add(jidTextBox);

			HorizontalPanel buttonPanel = new HorizontalPanel();
			buttonPanel.setSpacing(3);
			regButton = new Button("Register", registerHandler);
			regButton
					.getElement()
					.setAttribute("aria-label",
							"Register the users entered to receive notifications from core");
			addButton = new Button("Add Another", addJidHandler);
			addButton.getElement().setAttribute("aria-label",
					"Add another user to receive notifications from core");
			regButton.setEnabled(false);
			addButton.setEnabled(false);
			buttonPanel.add(addButton);
			buttonPanel.add(regButton);

			panel.add(jidPanel);
			panel.add(buttonPanel);
			addDialogBox.setWidget(panel);
		}
	};
	
	

	ClickHandler deleteJidHandler = new ClickHandler() {
		@Override
		public void onClick(ClickEvent event) {
			deleteJidList = new ArrayList<String>();
			int length = registerJidList.getItemCount();
			String list = "";
			for (int i = 0; i < length; i++) {
				if (registerJidList.isItemSelected(i)) {
					deleteJidList.add(registerJidList.getItemText(i));
					list += registerJidList.getItemText(i) + "; ";
				}
			}
			final CustomDialogBox unRegisterPopup = new CustomDialogBox(
					"Unregister Jid(s)", true);
			unRegisterPopup.addCloseHandler(new CloseHandler<PopupPanel>() {
				@Override
				public void onClose(CloseEvent<PopupPanel> arg0) {
					registerJidList.setFocus(true);
				}
			});
			VerticalPanel panel = new VerticalPanel();
			panel.setSpacing(3);
			panel.add(new Label("Confirm unregistering Jids: {" + list + "}"));
			Button okButton = new Button("Ok");
			okButton.getElement().setAttribute("aria-label", "Confirm unregistering Jids: {" + list + "}");
			panel.setCellHorizontalAlignment(okButton,
					HorizontalPanel.ALIGN_CENTER);
			okButton.addClickHandler(new ClickHandler() {
				@Override
				public void onClick(ClickEvent arg0) {
					unRegisterPopup.hide();
					BaseController.loggingServiceProxyAsync.unRegisterCoreId(
							deleteJidList, new AsyncCallback<List<String>>() {
								@Override
								public void onFailure(Throwable e) {
									Util.ERROR("Failed to unregister jids: "
											+ e.getMessage());
								}

								@Override
								public void onSuccess(List<String> jidList) {
									registerJidList.clear();
									for (String jid : jidList) {
										registerJidList.addItem(jid);
									}
									registerJidList.setFocus(true);
									delButton.setEnabled(false);
								}
							});

				}
			});
			panel.add(okButton);
			unRegisterPopup.add(panel);
			unRegisterPopup.showDialog();
			unRegisterPopup.center();
		}
	};

	ClickHandler registerHandler = new ClickHandler() {
		@Override
		public void onClick(ClickEvent event) {

			int length = jidPanel.getWidgetCount();
			List<String> jidList = new ArrayList<String>();
			for (int i = 0; i < length; i++) {
				TextBox jidTextBox = (TextBox) jidPanel.getWidget(i);
				String jid = jidTextBox.getText();
				boolean validJid = isValid(jid);
				if (validJid) {
					jidList.add(jid);
				}
			}
			BaseController.loggingServiceProxyAsync.registerCoreId(jidList,
					new RegisterJidCallback());
			addDialogBox.hide();
		}

	};

	ClickHandler disconnectHandler = new ClickHandler() {
		@Override
		public void onClick(ClickEvent event) {
			localConnection.doDisconnect();
			disconnectButton.setEnabled(false);
		}
	};

	public void handleHistoryMessage(String message) {
		try {
			String[] extractProperties = message.split("]");
			String componentId = extractProperties[0];
			componentId = componentId.substring(
					componentId.lastIndexOf("[") + 1).trim();
			String status = extractProperties[1];
			status = status.substring(status.lastIndexOf("[") + 1).trim();
			String timeStamp = extractProperties[2];
			timeStamp = timeStamp.substring(timeStamp.lastIndexOf("[") + 1)
					.trim();
			String logMessage = extractProperties[3];

			ComponentPanel component = componentPanelMap.get(componentId);
			component.appendLogHistory(componentId, status, timeStamp,
					logMessage, message);
		} catch (Exception e) {
			Util.ERROR("Error" + e);
		}

	}

	private void handleCoreEnhancedMessage(String message) {
		String[] core = message.split("]");
		String coreName = core[0].substring(core[0].lastIndexOf("[") + 1);
		if (sharedCorePanel != null) {
			sharedCorePanel.updateCorePopup(coreName);
		}

	}

	private String getMessageFromElement(Element element) {
		NodeList<Node> nodes = element.getChildNodes();
		int length = nodes.getLength();
		String message = "";
		for (int i = 0; i < length; i++) {
			Node node = nodes.getItem(i);
			NodeList<Node> childNodes = node.getChildNodes();
			Node childNode = childNodes.getItem(0);
			message += childNode.getNodeValue() + "\n";
		}
		return message;
	}

	private void handleRemoteMessage(String message) {
		String[] core = message.split("]");
		String coreName = core[0].substring(core[0].lastIndexOf("[") + 1);
		String status = core[1].substring(core[1].lastIndexOf("[") + 1);
		if (sharedCorePanel != null) {
			sharedCorePanel.updateCoreStatus(coreName, status);
		}
	}

	/**
	 * Handle messages sent from CoreConnection(DirectoryService) for sharedCore
	 * updates
	 */
	private void handleCorMessage(String message) {
		String[] core = message.split("]");
		String coreName = core[0].substring(core[0].lastIndexOf("[") + 1);
		String operation = core[1].substring(core[1].lastIndexOf("[") + 1);
		if (sharedCorePanel != null) {
			if (operation.contains("create")) {
				sharedCorePanel.createCore(coreName);
			} else if (operation.contains("update")) {
				sharedCorePanel.updateCore(coreName);
			} else if (operation.contains("remove")) {
				sharedCorePanel.removeCore(coreName);
			}
		}
	}

	/** Handle Messages sent form LoggingService */
	private void handleLogMessage(String message) {
		try {
			String[] extractProperties = message.split("]");
			String componentId = extractProperties[0];
			componentId = componentId.substring(
					componentId.lastIndexOf("[") + 1).trim();
			String status = extractProperties[1];
			status = status.substring(status.lastIndexOf("[") + 1).trim();
			String timeStamp = extractProperties[2];
			timeStamp = timeStamp.substring(timeStamp.lastIndexOf("[") + 1)
					.trim();
			String logMessage = extractProperties[3];

			ComponentPanel component = componentPanelMap.get(componentId);
			component.handleMessage(componentId, status, timeStamp, logMessage,
					message);
		} catch (Exception e) {
			Util.ERROR("Error" + e);
		}
	}

	/** Creates component panels */
	private VerticalPanel createCenterDockPanel() {
		VerticalPanel centerDock = new VerticalPanel();
		centerDock.setStyleName("centerDockBorder");
		ScrollPanel scrollPanel = new ScrollPanel();
		VerticalPanel centerPanel = new VerticalPanel();

		Label componentLabel = new Label("Component Status");
		centerPanel.add(componentLabel);
		centerPanel.setCellHorizontalAlignment(componentLabel,
				HorizontalPanel.ALIGN_CENTER);

		ComponentPanel existDecPanel = new ComponentPanel(EXIST);
		componentPanelMap.put("exist", existDecPanel);
		centerPanel.add(existDecPanel);

		ComponentPanel opendsDecPanel = new ComponentPanel(OPENDS);
		componentPanelMap.put("opends", opendsDecPanel);
		centerPanel.add(opendsDecPanel);

		ComponentPanel openFireDecPanel = new ComponentPanel(OPENFIRE);
		componentPanelMap.put("openfire", openFireDecPanel);
		centerPanel.add(openFireDecPanel);

		ComponentPanel tomcatDecPanel = new ComponentPanel(TOMCAT);
		componentPanelMap.put("tomcat", tomcatDecPanel);
		centerPanel.add(tomcatDecPanel);

		ComponentPanel uicdsDecPanel = new ComponentPanel(UICDS);
		componentPanelMap.put("uicds", uicdsDecPanel);
		centerPanel.add(uicdsDecPanel);

		centerPanel.setSize("450px", "500px");
		centerPanel.setSpacing(15);

		scrollPanel.setSize("505px", "500px");
		scrollPanel.add(centerPanel);

		centerDock.add(scrollPanel);
		return centerDock;
	}

	private VerticalPanel createEastDock() {
		sharedCorePanel = new SharedCorePanel();
		sharedCorePanel.setStyleName("eastDockBorder");
		sharedCorePanel.addCores();
		return sharedCorePanel;
	}

	private HorizontalPanel createNorthDock() {
		// North
		HorizontalPanel northPanel = new HorizontalPanel();
		northPanel.setHorizontalAlignment(VerticalPanel.ALIGN_LEFT);
		northPanel.setSpacing(3);
		northPanel.setWidth("625px");

		HorizontalPanel connectPanel = new HorizontalPanel();
		connectPanel.setSpacing(3);
		connectPanel.add(statusLabel);
		statusLabel.setWidth("350px");
		connectPanel.add(disconnectButton);
		disconnectButton.addClickHandler(disconnectHandler);
		disconnectButton.setEnabled(false);
		disconnectButton.setVisible(false);
		connectPanel.add(disconnectButton);
		connectPanel.setCellVerticalAlignment(statusLabel,
				VerticalPanel.ALIGN_MIDDLE);
		connectPanel.setCellHorizontalAlignment(statusLabel,
				HorizontalPanel.ALIGN_RIGHT);

		HorizontalPanel registerJidPanel = new HorizontalPanel();
		registerJidPanel.setSpacing(1);
		registerButton.addClickHandler(jidListHandler);
		registerJidPanel.add(registerButton);

		northPanel.add(connectPanel);
		northPanel.setCellHorizontalAlignment(connectPanel,
				HorizontalPanel.ALIGN_RIGHT);
		northPanel.setCellVerticalAlignment(connectPanel,
				VerticalPanel.ALIGN_MIDDLE);
		northPanel.add(registerJidPanel);
		northPanel.setCellHorizontalAlignment(registerJidPanel,
				HorizontalPanel.ALIGN_CENTER);
		northPanel.setCellVerticalAlignment(registerJidPanel,
				VerticalPanel.ALIGN_MIDDLE);
		return northPanel;
	}

	public String getCoreName() {
		return coreName;
	}

	public boolean isValid(String jid) {
		if (jid.contains("@")) {
			String[] jidField = jid.split("@");
			if (jidField.length > 1) {
				if (!jidField[0].equals("") && !jidField[1].equals("")) {
					return true;
				}
			}
		}
		return false;
	}

	public void setCoreName(String coreName) {
		this.coreName = coreName;
	}

	private void initiateTimer() {
		Timer connectionTimer = new Timer() {
			@Override
			public void run() {
				if (statusLabel.getText().toLowerCase().contains("connecting")) {
					statusLabel.setWidth("265px");
					statusLabel
							.setText("Status: Not Connected (Connection TimeOut)");
					statusLabel.getElement().setAttribute("aria-label",
							"XMPP " + statusLabel.getText());
					localConnection.doReset();
				}
			}
		};
		connectionTimer.schedule(connectonTimeOut);
	}

	/** AsyncCallback for BoshConnection via Strophe */
	private class ConnectionCallback implements AsyncCallback<String> {
		public void onFailure(Throwable e) {
			Util.ERROR("Local Core Jid failed: " + e.getMessage());
		}

		public void onSuccess(String jid) {
			BaseController.loggingServiceProxyAsync
					.updateComponentHistory(new AsyncCallback<Map<String, List<String>>>() {
						@Override
						public void onFailure(Throwable arg0) {
						}

						@Override
						public void onSuccess(Map<String, List<String>> history) {
							if (history != null) {
								for (String componentId : history.keySet()) {
									List<String> list = history
											.get(componentId);
									for (String message : list) {
										handleHistoryMessage(message);
									}
								}
							}
							BaseController.loggingServiceProxyAsync
									.sendHistroy(false,
											new AsyncCallback<String>() {
												@Override
												public void onFailure(
														Throwable arg0) {
												}

												@Override
												public void onSuccess(
														String jids) {
												}
											});
						}

						private void handleHistoryMessage(String message) {
							try {
								String[] extractProperties = message.split("]");
								String componentId = extractProperties[0];
								componentId = componentId.substring(
										componentId.lastIndexOf("[") + 1)
										.trim();
								String status = extractProperties[1];
								status = status.substring(
										status.lastIndexOf("[") + 1).trim();
								String timeStamp = extractProperties[2];
								timeStamp = timeStamp.substring(
										timeStamp.lastIndexOf("[") + 1).trim();
								String logMessage = extractProperties[3];

								ComponentPanel component = componentPanelMap
										.get(componentId);
								component.appendLogHistory(componentId, status,
										timeStamp, logMessage, message);
							} catch (Exception e) {
								Util.ERROR("Error" + e);
							}
						}
					});
		}
	}

	public void initiateShutdown() {
		localConnection.doReset();
		boolean remote = false;
		if (remoteConnection != null) {
			remote = true;
			remoteConnection.doReset();
		}
		BaseController.loggingServiceProxyAsync.unregisterLocalJid(
				connectionJid, remote, new AsyncCallback<String>() {
					@Override
					public void onFailure(Throwable arg0) {
					}

					@Override
					public void onSuccess(String arg0) {
					}
				});
	}

}
