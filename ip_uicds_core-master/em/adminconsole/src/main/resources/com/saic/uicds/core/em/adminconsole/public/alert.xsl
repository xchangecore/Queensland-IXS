<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet version="2.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform" xmlns:fo="http://www.w3.org/1999/XSL/Format" xmlns:xs="http://www.w3.org/2001/XMLSchema" xmlns:fn="http://www.w3.org/2005/xpath-functions" xmlns:str="http://www.saic.com/precis/2009/06/structures" xmlns:ulex="ulex:message:structure:1.0" xmlns:base="http://www.saic.com/precis/2009/06/base" xmlns:ns="http://ucore.gov/ucore/2.0" xmlns:as="http://uicds.org/AlertService" xmlns:cap="urn:oasis:names:tc:emergency:cap:1.1" xmlns:ims="http://uicds.org/IncidentManagementService" xmlns:inc="http://uicds.org/incident" xmlns:nc="http://niem.gov/niem/niem-core/2.0" xmlns:nga="http://niem.gov/niem/nga/2.0" xmlns:niem-xsd="http://niem.gov/niem/proxy/xsd/2.0" xmlns:p="urn:oasis:names:tc:emergency:EDXL:DE:1.0" xmlns:s="http://niem.gov/niem/structures/2.0" xmlns:unece="http://niem.gov/niem/unece_rec20-misc/2.0" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xmlns:ns1="http://www.opengis.net/gml/3.2" xmlns:my="http://metadata.dod.mil/mdr/ns/DDMS/2.0/" exclude-result-prefixes="my">
	<xsl:output version="1.0" method="html" indent="no" encoding="UTF-8" doctype-public="-//W3C//DTD HTML 4.0 Transitional//EN" doctype-system="http://www.w3.org/TR/html4/loose.dtd"/>
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
		<tr bgcolor="WhiteSmoke">
			<td/>
			<td>Contact</td>
			<td>
				<xsl:value-of select="ns:DataOwnerContact/my:Organization/my:name"/>
			</td>
		</tr>
		<tr bgcolor="WhiteSmoke">
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
				<xsl:apply-templates select="ns:OccursAt"/>
			</tbody>
		</table>
	</xsl:template>
	<xsl:template match="ns:Event">
		<tr bgcolor="Azure">
			<td>Event (What)</td>
			<td>EventID</td>
			<td>
				<xsl:value-of select="@id"/>
			</td>
		</tr>
		<tr bgcolor="Azure">
			<td/>
			<td>EventDescriptor</td>
			<td>
				<xsl:value-of select="ns:Descriptor"/>
			</td>
		</tr>
		<tr bgcolor="Azure">
			<td/>
			<td>ActivityName</td>
			<td>
				<xsl:value-of select="ns:Identifier"/>
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
		<tr bgcolor="LightCyan">
			<td>Location (Where)</td>
			<td>Identifier</td>
			<td>
				<xsl:value-of select="@id"/>
			</td>
		</tr>
		<xsl:apply-templates select="ns:GeoLocation"/>
	</xsl:template>
	<xsl:template match="ns:GeoLocation">
		<xsl:apply-templates select="ns:Polygon"/>
		<xsl:apply-templates select="ns:CircleByCenterPoint"/>
	</xsl:template>
	<xsl:template match="ns:Polygon">
		<xsl:apply-templates select="ns1:Polygon"/>
	</xsl:template>
	<xsl:template match="ns1:Polygon">
		<tr bgcolor="LightCyan">
			<td/>
			<td bgcolor="LightCyan">Polygon</td>
			<td>
				<table frame="box" width="100%">
					<tbody>
						<tr>
							<td bgcolor="LightCyan">Polygon ID</td>
							<td bgcolor="LightCyan">
								<xsl:value-of select="@ns1:id"/>
							</td>
						</tr>
						<tr bgcolor="LightCyan">
							<td>SRS Name</td>
							<td>
								<xsl:value-of select="@srsName"/>
							</td>
						</tr>
						<xsl:apply-templates select="ns1:exterior/ns1:LinearRing"/>
					</tbody>
				</table>
			</td>
		</tr>
	</xsl:template>
	<xsl:template match="ns1:exterior/ns1:LinearRing">
		<tr bgcolor="LightCyan">
			<td>Linear Ring</td>
			<td>
				<table border="0">
					<tbody>
						<xsl:for-each select="ns1:pos">
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
	<xsl:template match="ns1:pos">
		<tr bgcolor="LightCyan">
			<td/>
			<td/>
			<td>Point</td>
			<td>
				<xsl:value-of select="current()"/>
			</td>
		</tr>
	</xsl:template>
	<xsl:template match="ns:CircleByCenterPoint">
		<xsl:apply-templates select="ns1:CircleByCenterPoint"/>
	</xsl:template>
	<xsl:template match="ns1:CircleByCenterPoint">
		<tr bgcolor="LightCyan">
			<td/>
			<td>Circle By Center Point</td>
			<td>
				<table frame="box" width="100%">
					<tbody>
						<tr bgcolor="LightCyan">
							<td>Center Point</td>
							<td>
								<xsl:value-of select="ns1:pos"/>
							</td>
						</tr>
						<tr bgcolor="LightCyan">
							<td>Radius</td>
							<td>
								<xsl:value-of select="ns1:radius"/>
							</td>
						</tr>
						<xsl:apply-templates select="ns1:radius"/>
					</tbody>
				</table>
			</td>
		</tr>
	</xsl:template>
	<xsl:template match="ns1:radius">
		<tr bgcolor="LightCyan">
			<td>Radius Unit of Measurement</td>
			<td>
				<xsl:value-of select="@uom"/>
			</td>
		</tr>
	</xsl:template>
	<xsl:template match="ns:OccursAt">
		<tr bgcolor="LightCyan">
			<td>Occurs At (When)</td>
			<td colspan="3">
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
	<!-- -->
	<!-- end of Product Digest -->
	<!-- -->
	<!-- -->
	<!-- start of Structured Payload -->
	<!-- -->
	<xsl:template match="ulex:StructuredPayload">
		<table border="1" width="100%">
			<tr bgcolor="Khaki">
				<th colspan="3">Fractional Data Structured Payload</th>
			</tr>
			<tbody>
				<xsl:apply-templates select="ulex:StructuredPayloadMetadata"/>
		    <xsl:apply-templates select="cap:alert"/>
			</tbody>
		</table>
	</xsl:template>
	<!-- -->
	<!-- Structured Payload Metadada -->
	<!-- -->
	<xsl:template match="ulex:StructuredPayloadMetadata">
		<tr bgcolor="LightYellow">
			<td>Payload Type</td>
			<td colspan="2">
				<xsl:value-of select="ulex:CommunityURI"/>
			</td>
		</tr>
	</xsl:template>
	<!-- -->
	<!-- alert -->
	<!-- -->
	<xsl:template match="cap:alert">
		<tr bgcolor="LightYellow">
			<td>identifier</td>
			<td colspan="2">
			  <xsl:value-of select="cap:identifier"/>
			</td>
		</tr>
		<tr bgcolor="LightYellow">
			<td>sender</td>
			<td colspan="2">
			  <xsl:value-of select="cap:sender"/>
			</td>
		</tr>
		<tr bgcolor="LightYellow">
			<td>sent</td>
			<td colspan="2">
			  <xsl:value-of select="cap:sent"/>
			</td>
		</tr>
		<tr bgcolor="LightYellow">
			<td>status</td>
			<td colspan="2">
			  <xsl:value-of select="cap:status"/>
			</td>
		</tr>
		<tr bgcolor="LightYellow">
			<td>msgType</td>
			<td colspan="2">
			  <xsl:value-of select="cap:msgType"/>
			</td>
		</tr>
		<tr bgcolor="LightYellow">
			<td>scope</td>
			<td colspan="2">
			  <xsl:value-of select="cap:scope"/>
			</td>
		</tr>
		<xsl:apply-templates select="cap:info"/>
	</xsl:template>
	<!-- -->
	<!-- alert:info -->
	<!-- -->
	<xsl:template match="cap:info">
		<tr bgcolor="LightYellow">
			<td>category</td>
			<td colspan="2">
				<xsl:value-of select="cap:category"/>
			</td>
		</tr>
		<tr bgcolor="LightYellow">
			<td>event</td>
			<td colspan="2">
				<xsl:value-of select="cap:event"/>
			</td>
		</tr>
		<tr bgcolor="LightYellow">
			<td>urgency</td>
			<td colspan="2">
				<xsl:value-of select="cap:urgency"/>
			</td>
		</tr>
		<tr bgcolor="LightYellow">
			<td>severity</td>
			<td colspan="2">
				<xsl:value-of select="cap:severity"/>
			</td>
		</tr>
		<tr bgcolor="LightYellow">
			<td>certainty</td>
			<td colspan="2">
				<xsl:value-of select="cap:certainty"/>
			</td>
		</tr>
		<tr bgcolor="LightYellow">
			<td>effective</td>
			<td colspan="2">
				<xsl:value-of select="cap:effective"/>
			</td>
		</tr>
		<tr bgcolor="LightYellow">
			<td>headline</td>
			<td colspan="2">
				<xsl:value-of select="cap:headline"/>
			</td>
		</tr>
		<tr bgcolor="LightYellow">
			<td>description</td>
			<td colspan="2">
				<xsl:value-of select="cap:description"/>
			</td>
		</tr>
		<xsl:apply-templates select="cap:area"/>
	</xsl:template>
	<!-- -->
	<!-- cap:area -->
	<!-- -->
	<xsl:template match="cap:area">
		<tr bgcolor="LightYellow">
			<td>areaDesc</td>
			<td colspan="2">
				<xsl:value-of select="cap:areaDesc"/>
			</td>
		</tr>
		<xsl:apply-templates select="cap:circle"/>
		<xsl:apply-templates select="cap:polygon"/>
	</xsl:template>
	<!-- -->
	<!-- cap:circle -->
	<!-- -->
	<xsl:template match="cap:circle">
		<tr bgcolor="LightYellow">
			<td>circle</td>
			<td colspan="2">
				<xsl:value-of select="text()"/>
			</td>
		</tr>
	</xsl:template>
	<!-- -->
	<!-- cap:polygon -->
	<!-- -->
	<xsl:template match="cap:polygon">
		<tr bgcolor="LightYellow">
			<td>polygon</td>
			<td colspan="2">
				<xsl:value-of select="text()"/>
			</td>
		</tr>
	</xsl:template>
	
</xsl:stylesheet>
