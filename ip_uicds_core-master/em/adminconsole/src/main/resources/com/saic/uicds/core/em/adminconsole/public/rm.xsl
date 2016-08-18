<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet version="2.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform" xmlns:fo="http://www.w3.org/1999/XSL/Format" xmlns:xs="http://www.w3.org/2001/XMLSchema" xmlns:fn="http://www.w3.org/2005/xpath-functions" xmlns:str="http://www.saic.com/precis/2009/06/structures" xmlns:ulex="ulex:message:structure:1.0" xmlns:base="http://www.saic.com/precis/2009/06/base" xmlns:ns="http://ucore.gov/ucore/2.0" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xmlns:ns1="http://www.opengis.net/gml/3.2" xmlns:ct="urn:oasisRequestResource.1:names:tc:ciq:ct:3" xmlns:de="urn:oasis:names:tc:emergency:EDXL:DE:1.0" xmlns:geo-oasis="urn:oasis:names:tc:emergency:EDXL:HAVE:1.0:geo-oasis" xmlns:gml="http://www.opengis.net/gml" xmlns:rm="urn:oasis:names:tc:emergency:EDXL:RM:1.0" xmlns:rms="http://uicds.org/ResourceManagementService" xmlns:rmsg="urn:oasis:names:tc:emergency:EDXL:RM:1.0:msg" xmlns:smil20="http://www.w3.org/2001/SMIL20/" xmlns:smil20lang="http://www.w3.org/2001/SMIL20/Language" xmlns:xal="urn:oasis:names:tc:ciq:xal:3" xmlns:xlink="http://www.w3.org/1999/xlink" xmlns:xlink1="http://www.w3.org/1999/xlink1" xmlns:xnl="urn:oasis:names:tc:ciq:xnl:3" xmlns:xpil="urn:oasis:names:tc:ciq:xpil:3" exclude-result-prefixes="my" xmlns:my="http://metadata.dod.mil/mdr/ns/DDMS/2.0/">
	<xsl:template match="/">
		<html>
			<head>
				<title/>
			</head>
			<xsl:for-each select="str:WorkProduct">
				<xsl:apply-templates select="ulex:PackageMetadata"/>
				<xsl:apply-templates select="ns:Digest"/>
				<xsl:apply-templates select="ulex:StructuredPayload"/>
			</xsl:for-each>
		</html>
	</xsl:template>
	<!-- -->
	<!-- start of Package Metadata -->
	<!-- -->
	<xsl:template match="ulex:PackageMetadata">
		<table border="1" width="100%">
			<tr>
				<th bgcolor="DarkGray" colspan="3">Work Product Metadata</th>
			</tr>
			<tbody>
				<xsl:apply-templates select="ulex:DataOwnerMetadata"/>
				<xsl:apply-templates select="str:WorkProductIdentification"/>
				<xsl:apply-templates select="str:WorkProductProperties"/>
			</tbody>
		</table>
	</xsl:template>
	<xsl:template match="ulex:DataOwnerMetadata">
		<tr bgcolor="WhiteSmoke">
			<td>Data Owner</td>
			<td>Identifier</td>
			<td>
				<xsl:value-of select="ns:DataOwnerIdentifier/my:Organization/my:name"/>
			</td>
		</tr>
		<tr  bgcolor="WhiteSmoke">
			<td/>
			<td>Contact</td>
			<td>
				<xsl:value-of select="ns:DataOwnerContact/my:Organization/my:name"/>
			</td>
		</tr>
		<tr  bgcolor="WhiteSmoke">
			<td/>
			<td>Domain</td>
			<td>
				<xsl:value-of select="ulex:DataOwnerMetadataDomainAttribute/ulex:DomainName"/>
			</td>
		</tr>
	</xsl:template>
	<xsl:template match="str:WorkProductIdentification">
		<tr bgcolor="Gainsboro">
			<td>Work Product Identification</td>
			<td>Product ID</td>
			<td>
				<xsl:value-of select="base:Identifier"/>
			</td>
		</tr>
		<tr bgcolor="Gainsboro">
			<td/>
			<td>Product Type</td>
			<td>
				<xsl:value-of select="base:Type"/>
			</td>
		</tr>
		<tr bgcolor="Gainsboro">
			<td/>
			<td>Product Version</td>
			<td>
				<xsl:value-of select="base:Version"/>
			</td>
		</tr>
		<tr bgcolor="Gainsboro">
			<td/>
			<td>Product Checksum</td>
			<td>
				<xsl:value-of select="base:Checksum"/>
			</td>
		</tr>
		<tr bgcolor="Gainsboro">
			<td/>
			<td>Product State</td>
			<td>
				<xsl:value-of select="base:State"/>
			</td>
		</tr>
	</xsl:template>
	<xsl:template match="str:WorkProductProperties">
		<tr bgcolor="silver">
			<td>Work Product Properties</td>
			<td>Created</td>
			<td>
				<xsl:value-of select="base:Created"/>
			</td>
		</tr>
		<tr bgcolor="silver">
			<td/>
			<td>CreatedBy</td>
			<td>
				<xsl:value-of select="base:CreatedBy"/>
			</td>
		</tr>
		<tr bgcolor="silver">
			<td/>
			<td>LastUpdated</td>
			<td>
				<xsl:value-of select="base:LastUpdated"/>
			</td>
		</tr>
		<tr bgcolor="silver">
			<td/>
			<td>LastUpdatedBy</td>
			<td>
				<xsl:value-of select="base:LastUpdatedBy"/>
			</td>
		</tr>
		<tr bgcolor="silver">
			<td/>
			<td>Kilobytes</td>
			<td>
				<xsl:value-of select="base:Kilobytes"/>
			</td>
		</tr>
		<tr bgcolor="silver">
			<td/>
			<td>MimeType</td>
			<td>
				<xsl:value-of select="base:MimeType"/>
			</td>
		</tr>
		<tr bgcolor="silver">
			<td/>
			<td>Associated Interest Groups</td>
			<td>
				<table frame="void" width="100%">
					<tbody>
						<xsl:for-each select="base:AssociatedGroups/base:Identifier">
							<tr>
								<td>
									<xsl:value-of select="current()"/>
								</td>
							</tr>
						</xsl:for-each>
					</tbody>
				</table>
			</td>
		</tr>
	</xsl:template>
	<!-- -->
	<!-- end of Package Metadata -->
	<!-- -->
	<!-- -->
	<!-- start of Product Digest -->
	<!-- -->
	<xsl:template match="ns:Digest">
		<table border="1" width="100%">
			<tr bgcolor="PowderBlue">
				<th colspan="4">Work Product Digest</th>
			</tr>
			<tbody>
				<xsl:apply-templates select="ns:Event"/>
				<xsl:apply-templates select="ns:Location"/>
				<xsl:apply-templates select="ns:Organization"/>
				<xsl:apply-templates select="ns:OccursAt"/>
				<xsl:apply-templates select="ns:CauseOf"/>
				<xsl:apply-templates select="ns:LocatedAt"/>
				<xsl:apply-templates select="ns:InvolvedIn"/>
			</tbody>
		</table>
	</xsl:template>
	<xsl:template match="ns:Event">
		<tr bgcolor="Azure">
			<td>Event (What)</td>
			<td>ID</td>
			<td>
				<xsl:value-of select="@id"/>
			</td>
		</tr>
		<tr bgcolor="Azure">
			<td/>
			<td>Descriptor</td>
			<td>
				<xsl:value-of select="ns:Descriptor"/>
			</td>
		</tr>
		<tr bgcolor="Azure">
			<td/>
			<td valign="top">Properties</td>
			<td>
				<table frame="box" width="100%">
					<tbody>
						<xsl:apply-templates select="ns:SimpleProperty"/>
					</tbody>
				</table>
			</td>
		</tr>
		<xsl:for-each select="ns:What">
			<tr bgcolor="Azure">
				<td/>
				<td>What</td>
				<td>
					<xsl:value-of select="@ns:code"/>
				</td>
			</tr>
		</xsl:for-each>
	</xsl:template>
	<xsl:template match="ns:Location">
		<tr bgcolor="Azure">
			<td>Location (Where)</td>
			<td colspan="2">
				<table frame="void" width="100%">
					<tbody>
						<tr bgcolor="Azure">
							<td>Identifier:</td>
							<td>
								<xsl:value-of select="@id"/>
							</td>
						</tr>
						<tr bgcolor="Azure">
							<td>Descriptor:</td>
							<td>
								<xsl:value-of select="ns:Descriptor"/>
							</td>
						</tr>
					</tbody>
				</table>
			</td>
		</tr>
	</xsl:template>
	<xsl:template match="ns:Organization">
		<tr bgcolor="Azure">
			<td>Organization (Who)</td>
			<td colspan="2">
				<table frame="void" width="100%">
					<tbody>
						<tr bgcolor="Azure">
							<td>Identifier:</td>
							<td>
								<xsl:value-of select="@id"/>
							</td>
						</tr>
						<tr bgcolor="Azure">
							<td>Descriptor:</td>
							<td>
								<xsl:value-of select="ns:Descriptor"/>
							</td>
						</tr>
						<xsl:for-each select="ns:What">
							<tr bgcolor="Azure">
								<td>What:</td>
								<td>
									<xsl:value-of select="@ns:code"/>
								</td>
							</tr>
						</xsl:for-each>
					</tbody>
				</table>
			</td>
		</tr>
	</xsl:template>
	<xsl:template match="ns:SimpleProperty">
		<tr bgcolor="Azure">
			<td>
				<table frame="void" width="50%">
					<tbody>
						<tr>
							<td>Label:</td>
							<td>
								<xsl:value-of select="@ns:label"/>
							</td>
						</tr>
						<tr>
							<td>Code:</td>
							<td>
								<xsl:value-of select="@ns:code"/>
							</td>
						</tr>
						<tr>
							<td>Value:</td>
							<td>
								<xsl:value-of select="current()"/>
							</td>
						</tr>
					</tbody>
				</table>
			</td>
		</tr>
	</xsl:template>
	<xsl:template match="ns:OccursAt">
		<tr bgcolor="LightCyan">
			<td>Occurs At (When)</td>
			<td colspan="2">
				<table frame="void" width="100%">
					<tbody>
						<tr bgcolor="LightCyan">
							<td>Identifier:</td>
							<td>
								<xsl:value-of select="@id"/>
							</td>
						</tr>
						<tr bgcolor="LightCyan">
							<xsl:for-each select="ns:Time">
								<td>Time:</td>
								<td>
									<xsl:value-of select="ns:TimeInstant/ns:Value"/>
								</td>
							</xsl:for-each>
						</tr>
						<tr bgcolor="LightCyan">
							<xsl:for-each select="ns:EventRef">
								<td>Event:</td>
								<td>
									<xsl:value-of select="@ref"/>
								</td>
							</xsl:for-each>
						</tr>
						<tr bgcolor="LightCyan">
							<xsl:for-each select="ns:LocationRef">
								<td>Location:</td>
								<td>
									<xsl:value-of select="@ref"/>
								</td>
							</xsl:for-each>
						</tr>
					</tbody>
				</table>
			</td>
		</tr>
	</xsl:template>
	<xsl:template match="ns:CauseOf">
		<tr bgcolor="LightCyan">
			<td>Cause Of</td>
			<td colspan="2">
				<table frame="void" width="100%">
					<tbody>
						<tr bgcolor="LightCyan">
							<td>Identifier:</td>
							<td>
								<xsl:value-of select="@id"/>
							</td>
						</tr>
						<tr bgcolor="LightCyan">
							<xsl:for-each select="ns:Cause">
								<td>Cause:</td>
								<td>
									<xsl:value-of select="@ref"/>
								</td>
							</xsl:for-each>
						</tr>
						<tr bgcolor="LightCyan">
							<xsl:for-each select="ns:Effect">
								<td>Effect:</td>
								<td>
									<xsl:value-of select="@ref"/>
								</td>
							</xsl:for-each>
						</tr>
					</tbody>
				</table>
			</td>
		</tr>
	</xsl:template>
	<xsl:template match="ns:LocatedAt">
		<tr bgcolor="LightCyan">
			<td>Located At</td>
			<td colspan="2">
				<table frame="void" width="100%">
					<tbody>
						<tr bgcolor="LightCyan">
							<td>Identifier:</td>
							<td>
								<xsl:value-of select="@id"/>
							</td>
						</tr>
						<tr bgcolor="LightCyan">
							<xsl:for-each select="ns:Time">
								<td>Time:</td>
								<td>
									<xsl:value-of select="ns:TimeInstant/ns:Value"/>
								</td>
							</xsl:for-each>
						</tr>
						<tr bgcolor="LightCyan">
							<xsl:for-each select="ns:EntityRef">
								<td>Entity:</td>
								<td>
									<xsl:value-of select="@ref"/>
								</td>
							</xsl:for-each>
						</tr>
						<tr bgcolor="LightCyan">
							<xsl:for-each select="ns:LocationRef">
								<td>Location:</td>
								<td>
									<xsl:value-of select="@ref"/>
								</td>
							</xsl:for-each>
						</tr>
					</tbody>
				</table>
			</td>
		</tr>
	</xsl:template>
	<xsl:template match="ns:InvolvedIn">
		<tr bgcolor="LightCyan">
			<td>Involved In </td>
			<td colspan="2">
				<table frame="void" width="100%">
					<tbody>
						<tr bgcolor="LightCyan">
							<td>Identifier:</td>
							<td>
								<xsl:value-of select="@id"/>
							</td>
						</tr>
						<tr bgcolor="LightCyan">
							<xsl:for-each select="ns:Time">
								<td>Time:</td>
								<td>
									<xsl:value-of select="ns:TimeInstant/ns:Value"/>
								</td>
							</xsl:for-each>
						</tr>
						<tr bgcolor="LightCyan">
							<xsl:for-each select="ns:AgentRef">
								<td>Agent:</td>
								<td>
									<xsl:value-of select="@ref"/>
								</td>
							</xsl:for-each>
						</tr>
						<tr bgcolor="LightCyan">
							<xsl:for-each select="ns:EventRef">
								<td>Event:</td>
								<td>
									<xsl:value-of select="@ref"/>
								</td>
							</xsl:for-each>
						</tr>
					</tbody>
				</table>
			</td>
		</tr>
	</xsl:template>
	<!-- -->
	<!-- end of Product Digest -->
	<!-- -->
	<!-- -->
	<!-- start of Structured Payload -->
	<!-- -->
	<xsl:template match="ulex:StructuredPayload">
		<table border="1" width="100%">
			<tr bgcolor="khaki">
				<th colspan="3">Fractional Data Structured Payload</th>
			</tr>
			<tbody>
				<xsl:apply-templates select="ulex:StructuredPayloadMetadata"/>
				<xsl:apply-templates select="rmsg:RequestResource"/>
			</tbody>
		</table>
	</xsl:template>
	<xsl:template match="ulex:StructuredPayloadMetadata">
		<tr bgcolor="lightyellow">
			<td>Payload Type</td>
			<td colspan="2">
				<xsl:value-of select="ulex:CommunityURI"/>
			</td>
		</tr>
	</xsl:template>
	<xsl:template match="rmsg:RequestResource">
		<tr bgcolor="lightyellow">
			<td>Message ID</td>
			<td colspan="2">
				<xsl:value-of select="rmsg:MessageID"/>
			</td>
		</tr>
		<tr bgcolor="lightyellow">
			<td>Sent Date and Time</td>
			<td colspan="2">
				<xsl:value-of select="rmsg:SentDateTime"/>
			</td>
		</tr>
		<tr bgcolor="lightyellow">
			<td>Message Content Type</td>
			<td colspan="2">
				<xsl:value-of select="rmsg:MessageContentType"/>
			</td>
		</tr>
		<tr bgcolor="lightyellow">
			<td>OriginatingMessageID</td>
			<td colspan="2">
				<xsl:value-of select="rmsg:OriginatingMessageID"/>
			</td>
		</tr>
		<tr bgcolor="lightyellow">
			<td>Incident ID</td>
			<td colspan="2">
				<xsl:value-of select="rmsg:IncidentInformation/rm:IncidentID"/>
			</td>
		</tr>
		<xsl:apply-templates select="rmsg:ContactInformation"/>
		<xsl:apply-templates select="rmsg:ResourceInformation"/>
	</xsl:template>
	<xsl:template match="rmsg:ContactInformation">
		<tr bgcolor="LemonChiffon">
			<td>Contact Information</td>
			<td>
				<table frame="void" width="100%">
					<tbody>
						<tr bgcolor="LemonChiffon">
							<td>Description:</td>
							<td>
								<xsl:value-of select="rm:ContactDescription"/>
							</td>
						</tr>
						<tr bgcolor="LemonChiffon">
							<td>Role:</td>
							<td>
								<xsl:value-of select="rm:ContactRole"/>
							</td>
						</tr>
						<tr bgcolor="LemonChiffon">
							<td>Location:</td>
							<td>
								<xsl:value-of select="rm:ContactLocation/rm:LocationDescription"/>
							</td>
						</tr>
					</tbody>
				</table>
			</td>
		</tr>
	</xsl:template>
	<xsl:template match="rmsg:ResourceInformation">
		<tr bgcolor="LemonChiffon">
			<td>Resource Information</td>
			<td>
				<table frame="box" width="100%">
					<tbody>
						<tr bgcolor="LemonChiffon">
							<td>Identifier:</td>
							<td valign="top">
								<xsl:value-of select="rmsg:ResourceInfoElementID"/>
							</td>
						</tr>
						<tr bgcolor="LemonChiffon">
							<td>Type:</td>
							<td>
								<xsl:apply-templates select="rmsg:Resource"/>
							</td>
						</tr>
						<xsl:apply-templates select="rmsg:AssignmentInformation"/>
						<xsl:apply-templates select="rmsg:ScheduleInformation"/>
					</tbody>
				</table>
			</td>
		</tr>
	</xsl:template>
	<xsl:template match="rmsg:Resource">
		<table frame="box" width="100%">
			<tbody>
				<tr bgcolor="LemonChiffon">
					<td>Structure:</td>
					<td>
						<xsl:apply-templates select="rmsg:TypeStructure"/>
					</td>
				</tr>
				<tr bgcolor="LemonChiffon">
					<td>Information:</td>
					<td>
						<xsl:apply-templates select="rmsg:TypeInfo"/>
					</td>
				</tr>
			</tbody>
		</table>
	</xsl:template>
	<xsl:template match="rmsg:TypeStructure">
		<table frame="void" width="100%">
			<tbody>
				<tr bgcolor="LemonChiffon">
					<td>URN:</td>
					<td>
						<xsl:value-of select="rm:ValueListURN"/>
					</td>
				</tr>
				<tr bgcolor="LemonChiffon">
					<td>Type:</td>
					<td>
						<xsl:apply-templates select="rm:Value"/>
					</td>
				</tr>
			</tbody>
		</table>
	</xsl:template>
	<xsl:template match="rmsg:TypeInfo">
		<table frame="void" width="100%">
			<tbody>
				<tr bgcolor="LemonChiffon">
					<td>Resource:</td>
					<td>
						<xsl:value-of select="Resource"/>
					</td>
				</tr>
				<tr bgcolor="LemonChiffon">
					<td>Category:</td>
					<td>
						<xsl:apply-templates select="Category"/>
					</td>
				</tr>
				<tr bgcolor="LemonChiffon">
					<td>Kind:</td>
					<td>
						<xsl:apply-templates select="Kind"/>
					</td>
				</tr>
				<tr bgcolor="LemonChiffon">
					<td>Minimum Capabilities:</td>
					<td>
						<xsl:apply-templates select="MinimumCapabilities"/>
					</td>
				</tr>
			</tbody>
		</table>
	</xsl:template>
	<xsl:template match="rmsg:AssignmentInformation">
		<tr bgcolor="LemonChiffon">
			<td>Assignment Information</td>
			<td>
				<table frame="void" width="20%">
					<tbody>
						<tr bgcolor="LemonChiffon">
							<td>Quantity:</td>
							<td>
								<xsl:apply-templates select="rmsg:Quantity/rm:QuantityText"/>
							</td>
						</tr>
					</tbody>
				</table>
			</td>
		</tr>
	</xsl:template>
	<xsl:template match="rmsg:ScheduleInformation">
		<tr bgcolor="LemonChiffon">
			<td>Schedule Information</td>
			<td>
				<table frame="void" width="50%">
					<tbody>
						<tr bgcolor="LemonChiffon">
							<td>Type:</td>
							<td>
								<xsl:apply-templates select="rmsg:ScheduleType"/>
							</td>
						</tr>
						<tr bgcolor="LemonChiffon">
							<td>Location:</td>
							<td>
								<xsl:apply-templates select="rmsg:Location/rm:LocationDescription"/>
							</td>
						</tr>
					</tbody>
				</table>
			</td>
		</tr>
	</xsl:template>
	<!-- -->
	<!-- end of Structured Payload -->
	<!-- -->
</xsl:stylesheet>
