<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform" xmlns:xnl="urn:oasis:names:tc:ciq:xnl:3" xmlns:geo-oasis="urn:oasis:names:tc:emergency:EDXL:HAVE:1.0:geo-oasis" xmlns:have="urn:oasis:names:tc:emergency:EDXL:HAVE:1.0" xmlns:ucore="http://ucore.gov/ucore/2.0" xmlns:gml="http://www.opengis.net/gml" xmlns:ucoregml="http://www.opengis.net/gml/3.2">
  <xsl:output version="1.0" method="xml" indent="yes" encoding="UTF-8" omit-xml-declaration="yes"/>
  <xsl:param name="HospitalOrgIDBase">Hospital_</xsl:param>
  <xsl:param name="GeoLocationIDBase">GeoLocation_</xsl:param>
  <xsl:param name="LocatedAtIDBase">LocatedAt_</xsl:param>
  <xsl:param name="PointIDBase">Point_</xsl:param>
  <xsl:param name="EventIDBase">Event_</xsl:param>
  <xsl:template match="*">
    <xsl:apply-templates select="//have:HospitalStatus"/>
  </xsl:template>
  <xsl:template match="have:HospitalStatus">
    <ucore:Digest>
      <ucore:Event>
        <xsl:attribute name="id"><xsl:value-of select="$EventIDBase"/><xsl:value-of select="count(self::node())"/></xsl:attribute>
        <ucore:Descriptor>EDXL-HAVE Hospital Status</ucore:Descriptor>
        <ucore:Identifier ucore:codespace="urn:oasis:names:tc:emergency:EDXL:HAVE:1.0" ucore:code="HospitalStatus" ucore:label="ID">Hospital</ucore:Identifier>
        <ucore:What ucore:codespace="http://ucore.gov/ucore/2.0/codespace/" ucore:code="CommunicationEvent"/>
      </ucore:Event>
      <xsl:apply-templates select="have:Hospital"/>
    </ucore:Digest>
  </xsl:template>
  <xsl:template match="have:Hospital">
    <xsl:apply-templates select="have:Organization/have:OrganizationInformation"/>
    <xsl:apply-templates select="have:Organization/have:OrganizationGeoLocation/gml:Point"/>
  </xsl:template>
  <xsl:template match="have:OrganizationInformation">
    <xsl:apply-templates select="xnl:OrganisationName"/>
  </xsl:template>
  <xsl:template match="xnl:OrganisationName">
    <ucore:Organization>
      <xsl:attribute name="id"><xsl:value-of select="$HospitalOrgIDBase"/><xsl:value-of select="count(../../../preceding-sibling::have:Hospital)"/></xsl:attribute>
      <ucore:Identifier ucore:codespace="urn:oasis:names:tc:emergency:EDXL:HAVE:1.0" ucore:code="OrganisationName" ucore:label="ID">
        <xsl:value-of select="xnl:NameElement/text()"/>
      </ucore:Identifier>
      <xsl:apply-templates select="../../../have:HospitalBedCapacityStatus/have:BedCapacity"/>
      <ucore:What ucore:codespace="http://ucore.gov/ucore/2.0/codespace/" ucore:code="Organization"/>
        <ucore:What ucore:codespace="http://ucore.gov/ucore/2.0/codespace/" ucore:code="Facility"/>
      <ucore:Name>
        <ucore:Value>
          <xsl:value-of select="xnl:NameElement/text()"/>
        </ucore:Value>
      </ucore:Name>
    </ucore:Organization>
  </xsl:template>
  <xsl:template match="have:OrganizationGeoLocation/gml:Point">
    <ucore:Location>
      <xsl:attribute name="id"><xsl:value-of select="$GeoLocationIDBase"/><xsl:value-of select="count(../../../preceding-sibling::have:Hospital)"/></xsl:attribute>
      <ucore:GeoLocation>
        <ucore:Point>
          <ucoregml:Point>
            <xsl:attribute name="id" namespace="http://www.opengis.net/gml/3.2"><xsl:value-of select="$PointIDBase"/><xsl:value-of select="count(../../../preceding-sibling::have:Hospital)"/></xsl:attribute>
            <ucoregml:pos srsName="EPSG:4326">
              <xsl:value-of select="gml:pos/text()"/>
            </ucoregml:pos>
          </ucoregml:Point>
        </ucore:Point>
      </ucore:GeoLocation>
    </ucore:Location>
    <ucore:LocatedAt>
      <xsl:attribute name="id"><xsl:value-of select="$LocatedAtIDBase"/><xsl:value-of select="count(../../../preceding-sibling::have:Hospital)"/></xsl:attribute>
      <ucore:EntityRef>
        <xsl:attribute name="ref"><xsl:value-of select="$HospitalOrgIDBase"/><xsl:value-of select="count(../../../preceding-sibling::have:Hospital)"/></xsl:attribute>
      </ucore:EntityRef>
      <ucore:LocationRef>
        <xsl:attribute name="ref"><xsl:value-of select="$GeoLocationIDBase"/><xsl:value-of select="count(../../../preceding-sibling::have:Hospital)"/></xsl:attribute>
      </ucore:LocationRef>
    </ucore:LocatedAt>
  </xsl:template>
  <xsl:template match="have:HospitalBedCapacityStatus/have:BedCapacity">
    <ucore:SimpleProperty ucore:code="BedCapacity" ucore:codespace="urn:oasis:names:tc:emergency:EDXL:HAVE:1.0">
      <xsl:attribute name="label" namespace="http://ucore.gov/ucore/2.0"><xsl:value-of select="have:BedType/text()"/></xsl:attribute>
      <xsl:value-of select="have:Capacity/have:CapacityStatus/text()"/>:<xsl:value-of select="have:Capacity/have:AvailableCount/text()"/>
    </ucore:SimpleProperty>
  </xsl:template>
</xsl:stylesheet>
