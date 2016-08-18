<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet version="2.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform" xmlns:fo="http://www.w3.org/1999/XSL/Format" xmlns:xs="http://www.w3.org/2001/XMLSchema" xmlns:fn="http://www.w3.org/2005/xpath-functions" xmlns:str="http://www.saic.com/precis/2009/06/structures" xmlns:ulex="ulex:message:structure:1.0" xmlns:base="http://www.saic.com/precis/2009/06/base" xmlns:ns="http://ucore.gov/ucore/2.0" xmlns:as="http://uicds.org/AlertService" xmlns:cap="urn:oasis:names:tc:emergency:cap:1.1" xmlns:ims="http://uicds.org/IncidentManagementService" xmlns:inc="http://uicds.org/incident" xmlns:nc="http://niem.gov/niem/niem-core/2.0" xmlns:nga="http://niem.gov/niem/nga/2.0" xmlns:niem-xsd="http://niem.gov/niem/proxy/xsd/2.0" xmlns:p="urn:oasis:names:tc:emergency:EDXL:DE:1.0" xmlns:s="http://niem.gov/niem/structures/2.0" xmlns:unece="http://niem.gov/niem/unece_rec20-misc/2.0" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xmlns:ns1="http://www.opengis.net/gml/3.2" xmlns:my="http://metadata.dod.mil/mdr/ns/DDMS/2.0/" exclude-result-prefixes="my">
	<xsl:output method="html" indent="no" encoding="UTF-8" doctype-public="-//W3C//DTD HTML 4.0 Transitional//EN" doctype-system="http://www.w3.org/TR/html4/loose.dtd"/>
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
				<xsl:apply-templates select="inc:Incident"/>
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
	<xsl:template match="inc:Incident">
		<tr bgcolor="LightYellow">
			<td>Identifier</td>
			<td colspan="2">
				<xsl:value-of select="@s:id"/>
			</td>
		</tr>
		<tr bgcolor="LightYellow">
			<td>Interest Group ID</td>
			<td colspan="2">
				<xsl:value-of select="nc:ActivityIdentification/nc:IdentificationID"/>
			</td>
		</tr>
		<tr bgcolor="LightYellow">
			<td>Name</td>
			<td colspan="2">
				<xsl:value-of select="nc:ActivityName"/>
			</td>
		</tr>
		<tr bgcolor="LightYellow">
			<td>Category</td>
			<td colspan="2">
				<xsl:value-of select="nc:ActivityCategoryText"/>
			</td>
		</tr>
		<tr bgcolor="LightYellow">
			<td>Date</td>
			<td colspan="2">
				<xsl:value-of select="nc:ActivityDate/nc:DateTime"/>
			</td>
		</tr>
		<tr bgcolor="LightYellow">
			<td>Description</td>
			<td colspan="2">
				<xsl:call-template name="processTextForHyperlink">
					<xsl:with-param name="string" select="nc:ActivityDescriptionText" as="xs:string"/>
				</xsl:call-template>
			</td>
			<td/>
		</tr>
		<tr bgcolor="LightYellow">
			<td>Owner Core</td>
			<td colspan="2">
				<xsl:value-of select="inc:OwningCore"/>
			</td>
		</tr>
		<xsl:apply-templates select="inc:SharedCoreName"/>
		<xsl:apply-templates select="nc:IncidentLocation"/>
	</xsl:template>
	<xsl:template match="inc:SharedCoreName">
		<tr bgcolor="LightYellow">
			<td>Shared Core</td>
			<td colspan="2">
				<xsl:value-of select="current()"/>
			</td>
		</tr>
	</xsl:template>
	<!-- -->
	<!-- Incident -->
	<!-- -->
	<xsl:template match="nc:IncidentLocation">
		<tr bgcolor="LemonChiffon">
			<td>Location</td>
			<td>Address</td>
			<td>
				<xsl:value-of select="nc:LocationAddress/nc:AddressFullText"/>
			</td>
		</tr>
		<tr bgcolor="LemonChiffon">
			<td/>
			<td>Area Polygon Region</td>
			<td>
				<table border="1" frame="void" width="100%">
					<tbody>
						<xsl:apply-templates select="nc:LocationArea/nc:AreaPolygonGeographicCoordinate"/>
					</tbody>
				</table>
			</td>
		</tr>
		<xsl:apply-templates select="nc:LocationArea/nc:AreaCircularRegion"/>
	</xsl:template>
	<xsl:template match="nc:LocationArea/nc:AreaPolygonGeographicCoordinate">
		<tr bgcolor="LemonChiffon">
			<td>Polygon Coordinate</td>
			<td>
				<table border="0">
					<tbody>
						<tr>
							<td>Latitude:</td>
							<td>
								<xsl:for-each select="nc:GeographicCoordinateLatitude">
									<table border="0">
										<tbody>
											<td>
												<xsl:value-of select="nc:LatitudeDegreeValue"/>
											</td>
											<td>degrees; </td>
											<td>
												<xsl:value-of select="nc:LatitudeMinuteValue"/>
											</td>
											<td>minutes; </td>
											<td>
												<xsl:value-of select="nc:LatitudeSecondValue"/>
											</td>
											<td>seconds</td>
										</tbody>
									</table>
								</xsl:for-each>
							</td>
						</tr>
						<td>Longitude:</td>
						<td>
							<xsl:for-each select="nc:GeographicCoordinateLongitude">
								<table border="0">
									<tbody>
										<td>
											<xsl:value-of select="nc:LongitudeDegreeValue"/>
										</td>
										<td>degrees; </td>
										<td>
											<xsl:value-of select="nc:LongitudeMinuteValue"/>
										</td>
										<td>minutes; </td>
										<td>
											<xsl:value-of select="nc:LongitudeSecondValue"/>
										</td>
										<td>seconds</td>
									</tbody>
								</table>
							</xsl:for-each>
						</td>
					</tbody>
				</table>
			</td>
		</tr>
	</xsl:template>
	<xsl:template match="nc:LocationArea/nc:AreaCircularRegion">
		<tr bgcolor="LemonChiffon">
			<td/>
			<td>Area Circular Region</td>
			<td>
				<table border="1" frame="void" width="100%">
					<tbody>
						<xsl:apply-templates select="nc:CircularRegionCenterCoordinate/nc:GeographicCoordinateLatitude"/>
						<xsl:apply-templates select="nc:CircularRegionCenterCoordinate/nc:GeographicCoordinateLongitude"/>
						<xsl:apply-templates select="nc:CircularRegionRadiusLengthMeasure"/>
					</tbody>
				</table>
			</td>
		</tr>
	</xsl:template>
	<xsl:template match="nc:CircularRegionCenterCoordinate/nc:GeographicCoordinateLatitude">
		<tr>
			<td>Center's Latitude Coordinate</td>
			<td>
				<table border="0">
					<tbody>
						<td>Latitude:</td>
						<td>
							<xsl:value-of select="nc:LatitudeDegreeValue"/>
						</td>
						<td>degrees; </td>
						<td>
							<xsl:value-of select="nc:LatitudeMinuteValue"/>
						</td>
						<td>minutes; </td>
						<td>
							<xsl:value-of select="nc:LatitudeSecondValue"/>
						</td>
						<td>seconds</td>
					</tbody>
				</table>
			</td>
		</tr>
	</xsl:template>
	<xsl:template match="nc:CircularRegionCenterCoordinate/nc:GeographicCoordinateLongitude">
		<tr>
			<td>Center's Longitude Coordinate</td>
			<td>
				<table border="0">
					<tbody>
						<td>Longitude:</td>
						<td>
							<xsl:value-of select="nc:LongitudeDegreeValue"/>
						</td>
						<td>degrees; </td>
						<td>
							<xsl:value-of select="nc:LongitudeMinuteValue"/>
						</td>
						<td>minutes; </td>
						<td>
							<xsl:value-of select="nc:LongitudeSecondValue"/>
						</td>
						<td>seconds</td>
					</tbody>
				</table>
			</td>
		</tr>
	</xsl:template>
	<xsl:template match="nc:CircularRegionRadiusLengthMeasure">
		<tr bgcolor="LemonChiffon">
			<td>Radius Length</td>
			<td>
				<xsl:value-of select="nc:MeasurePointValue"/>
			</td>
		</tr>
		<tr bgcolor="LemonChiffon">
			<td>Radius Length Unit Code</td>
			<td>
				<xsl:value-of select="nc:LengthUnitCode"/>
			</td>
		</tr>
	</xsl:template>
	<!-- -->
	<!-- end of Structured Payload -->
	<!-- -->
	<!-- Start of misc./supporting named templates -->
	<!-- -->
	<!-- processTextForHyperLink -->
	<!-- Replace URLs in free text with hyperlink equivalents -->
	<!-- Input: string -->
	<!-- Output: string with all URLs replaced with hyperlinks -->
	<!-- -->
	<xsl:template name="processTextForHyperlink">
		<xsl:param name="string" select="string(.)"/>
		<xsl:call-template name="processTokens">
			<xsl:with-param name="str" select="$string"/>
			<xsl:with-param name="delimeter" select="' '"/>
		</xsl:call-template>
		<!-- unfortunately, these are XSLT 2.0 functions
		<xsl:analyze-string select="$string" regex="https?://[^ ]+">
			<xsl:matching-substring>
				<a href="{.}">
					<xsl:value-of select="."/>
				</a>
			</xsl:matching-substring>
			<xsl:non-matching-substring>
				<xsl:value-of select="."/>
			</xsl:non-matching-substring>
		</xsl:analyze-string>
-->
	</xsl:template>
	<xsl:template name="processTokens">
		<xsl:param name="str" select="."/>
		<xsl:param name="delimeter" select="' '"/>
		<xsl:choose>
			<xsl:when test="contains($str,$delimeter)">
				<xsl:call-template name="processHyperLink">
					<xsl:with-param name="token" select="substring-before($str,$delimeter)"/>
				</xsl:call-template>
				<xsl:call-template name="processTokens">
					<xsl:with-param name="str" select="substring-after($str,$delimeter)"/>
					<xsl:with-param name="delimeter" select="$delimeter"/>
				</xsl:call-template>
			</xsl:when>
			<xsl:otherwise>
				<xsl:call-template name="processHyperLink">
					<xsl:with-param name="token" select="$str"/>
				</xsl:call-template>
			</xsl:otherwise>
		</xsl:choose>
	</xsl:template>
	<xsl:template name="processHyperLink">
		<xsl:param name="token" select="."/>
		<xsl:choose>
			<xsl:when test="contains($token,'http:')">
				<a href="{$token}">
					<xsl:value-of select="$token"/>
				</a>
			</xsl:when>
			<xsl:when test="contains($token,'https:')">
				<a href="{$token}">
					<xsl:value-of select="$token"/>
				</a>
			</xsl:when>
			<xsl:otherwise>
				<xsl:value-of select="$token"/>
				<xsl:text> </xsl:text>
			</xsl:otherwise>
		</xsl:choose>
	</xsl:template>
</xsl:stylesheet>
