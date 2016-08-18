<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet version="2.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform" xmlns:fo="http://www.w3.org/1999/XSL/Format" xmlns:xs="http://www.w3.org/2001/XMLSchema" xmlns:fn="http://www.w3.org/2005/xpath-functions" xmlns:str="http://www.saic.com/precis/2009/06/structures" xmlns:ulex="ulex:message:structure:1.0" xmlns:base="http://www.saic.com/precis/2009/06/base" xmlns:ns="http://ucore.gov/ucore/2.0" xmlns:org="http://uicds.org/OrganizationElement" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xmlns:ns1="http://www.opengis.net/gml/3.2" exclude-result-prefixes="my" xmlns:my="http://metadata.dod.mil/mdr/ns/DDMS/2.0/">
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
				<th bgcolor="DimGray" colspan="3">Work Product Metadata</th>
			</tr>
			<tbody>
				<xsl:apply-templates select="ulex:DataOwnerMetadata"/>
				<xsl:apply-templates select="str:WorkProductIdentification"/>
				<xsl:apply-templates select="str:WorkProductProperties"/>
			</tbody>
		</table>
	</xsl:template>
	<xsl:template match="ulex:DataOwnerMetadata">
		<tr bgcolor="Wheat">
			<td>Data Owner</td>
			<td>Identifier</td>
			<td>
				<xsl:value-of select="ns:DataOwnerIdentifier/my:Organization/my:name"/>
			</td>
		</tr>
		<tr  bgcolor="Wheat">
			<td/>
			<td>Contact</td>
			<td>
				<xsl:value-of select="ns:DataOwnerContact/my:Organization/my:name"/>
			</td>
		</tr>
		<tr  bgcolor="Wheat">
			<td/>
			<td>Domain</td>
			<td>
				<xsl:value-of select="ulex:DataOwnerMetadataDomainAttribute/ulex:DomainName"/>
			</td>
		</tr>
	</xsl:template>
	<xsl:template match="str:WorkProductIdentification">
		<tr bgcolor="LightSteelBlue">
			<td>Work Product Identification</td>
			<td>Product ID</td>
			<td>
				<xsl:value-of select="base:Identifier"/>
			</td>
		</tr>
		<tr bgcolor="LightSteelBlue">
			<td/>
			<td>Product Type</td>
			<td>
				<xsl:value-of select="base:Type"/>
			</td>
		</tr>
		<tr bgcolor="LightSteelBlue">
			<td/>
			<td>Product Version</td>
			<td>
				<xsl:value-of select="base:Version"/>
			</td>
		</tr>
		<tr bgcolor="LightSteelBlue">
			<td/>
			<td>Product Checksum</td>
			<td>
				<xsl:value-of select="base:Checksum"/>
			</td>
		</tr>
		<tr bgcolor="LightSteelBlue">
			<td/>
			<td>Product State</td>
			<td>
				<xsl:value-of select="base:State"/>
			</td>
		</tr>
	</xsl:template>
	<xsl:template match="str:WorkProductProperties">
		<tr bgcolor="SlateGray">
			<td>Work Product Properties</td>
			<td>Created</td>
			<td>
				<xsl:value-of select="base:Created"/>
			</td>
		</tr>
		<tr bgcolor="SlateGray">
			<td/>
			<td>CreatedBy</td>
			<td>
				<xsl:value-of select="base:CreatedBy"/>
			</td>
		</tr>
		<tr bgcolor="SlateGray">
			<td/>
			<td>LastUpdated</td>
			<td>
				<xsl:value-of select="base:LastUpdated"/>
			</td>
		</tr>
		<tr bgcolor="SlateGray">
			<td/>
			<td>LastUpdatedBy</td>
			<td>
				<xsl:value-of select="base:LastUpdatedBy"/>
			</td>
		</tr>
		<tr bgcolor="SlateGray">
			<td/>
			<td>Kilobytes</td>
			<td>
				<xsl:value-of select="base:Kilobytes"/>
			</td>
		</tr>
		<tr bgcolor="SlateGray">
			<td/>
			<td>MimeType</td>
			<td>
				<xsl:value-of select="base:MimeType"/>
			</td>
		</tr>
		<tr bgcolor="SlateGray">
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
	<!-- -->
	<!-- start of Structured Payload -->
	<!-- -->
	<xsl:template match="ulex:StructuredPayload">
		<table border="1" width="100%">
			<tr bgcolor="SeaGreen">
				<th colspan="3">Fractional Data Structured Payload</th>
			</tr>
			<tbody>
				<xsl:apply-templates select="ulex:StructuredPayloadMetadata"/>
				<tr bgcolor="Orange">
					<td valign="top">Organizational Structure</td>
					<td colspan="2">
						<table>
							<tbody>
								<ul id="treemenu1" class="treeview">
									<xsl:apply-templates select="org:OrganizationElement"/>
								</ul>
							</tbody>
						</table>
					</td>
				</tr>
			</tbody>
		</table>
	</xsl:template>
	<xsl:template match="ulex:StructuredPayloadMetadata">
		<tr bgcolor="Orange">
			<td>Payload Type</td>
			<td colspan="2">
				<xsl:value-of select="ulex:CommunityURI"/>
			</td>
		</tr>
	</xsl:template>
	<xsl:template match="org:OrganizationElement">
		<li>Organization Name: <xsl:value-of select="org:OrganizationName"/>
			<table frame="box" bordercolor="black" width="0%">
				<tbody>
					<tr bgcolor="Tan">
						<td>Organization Type</td>
						<td>
							<xsl:value-of select="org:OrganizationType"/>
						</td>
					</tr>
					<tr bgcolor="Tan">
						<td valign="top">Person In Charge</td>
						<td>
							<table frame="void" width="100%">
								<tbody>
									<tr bgcolor="Tan">
										<td>Role Profile:</td>
										<td>
											<xsl:value-of select="org:PersonInCharge/org:RoleProfileRef"/>
										</td>
									</tr>
									<tr bgcolor="Tan">
										<td>Person Profile:</td>
										<td>
											<xsl:value-of select="org:PersonInCharge/org:PersonProfileRef"/>
										</td>
									</tr>
								</tbody>
							</table>
						</td>
					</tr>
				</tbody>
			</table>
			<xsl:for-each select="org:OrganizationElement">
				<ul>
					<xsl:apply-templates select="current()"/>
				</ul>
			</xsl:for-each>
		</li>
	</xsl:template>
	<!-- -->
	<!-- end of Structured Payload -->
	<!-- -->
</xsl:stylesheet>
