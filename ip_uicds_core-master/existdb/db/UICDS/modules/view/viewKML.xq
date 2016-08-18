module namespace kml-v = "http://uicds.org/modules/search/view/kml";

import module namespace geom = "http://exist-db.org/xquery/geom";

import module namespace wkt = "http://uicds.org/modules/util/wkt"
at "xmldb:exist:///db/UICDS/modules/util/wkt.xq";

import module namespace props = "http://uicds.org/modules/util/properties"
at "xmldb:exist:///db/UICDS/modules/util/properties.xq";

import module namespace functx = "http://www.functx.com"
at "xmldb:exist:///db/UICDS/modules/util/functx.xq";

declare namespace c = "util:config";
declare namespace p = "util:properties";

declare namespace NS_PRECIS_STRUCTURES = "http://www.saic.com/precis/2009/06/structures";
declare namespace NS_PRECIS_BASE = "http://www.saic.com/precis/2009/06/base";
declare namespace NS_ULEX_STRUCTURE = "ulex:message:structure:1.0";
declare namespace NS_UCORE = "http://ucore.gov/ucore/2.0";
declare namespace NS_GML = "http://www.opengis.net/gml/3.2";
declare namespace NS_WMC = "http://www.opengis.net/context";
declare namespace NS_XLINK = "http://www.w3.org/1999/xlink";

declare function kml-v:view($props as node(), $workproducts as node()*) as item() {
let $opt := util:declare-option("exist:serialize","method=xml media-type=application/vnd.google-earth.kml+xml omit-xml-declaration=no indent=yes")

let $baseURL := props:get-property(doc("/db/UICDS/config.xml")/c:config/p:props,"baseURL","http://localhost")

let $incidentIDs := fn:distinct-values($workproducts[NS_ULEX_STRUCTURE:PackageMetadata[1]/NS_PRECIS_STRUCTURES:WorkProductIdentification/NS_PRECIS_BASE:Type="Incident"]/NS_ULEX_STRUCTURE:PackageMetadata[1]/NS_PRECIS_STRUCTURES:WorkProductProperties[1]/NS_PRECIS_BASE:AssociatedGroups[1]/NS_PRECIS_BASE:Identifier[1])

return
<kml xmlns="http://www.opengis.net/kml/2.2">
    <Folder>
        <Document>
        <Style id="Incident-Balloon">
            <BalloonStyle>
                <text>
                    <![CDATA[
               		  <h3>Event: $[event]</h3>
               		  <hr width="325px" />
               		  <b>Type: </b> $[code] <br />
               		  <b>Description: </b> $[descriptor] <br /><br />
               		  <b>Submitted By: </b> $[createdBy]<br />
               		  <b>Submitted At: </b> $[created]<br />
               		  <b>Last Updated By: </b> $[lastUpdatedBy]<br />
               		  <b>Last Updated At: </b> $[lastUpdated]<br /><br />
               		  <a href="]]>{$baseURL}<![CDATA[/uicds/pub/search?productID=$[wpID]&productVersion=$[ver]&format=xml">Retrieve WorkProduct</a><br /><br />
               		  $[geDirections]
               		]]>
                </text>
            </BalloonStyle>
        </Style>
        <Style id="WorkProduct-Balloon">
            <BalloonStyle>
                <text>
                    <![CDATA[
               		  <h3>Work Product ID: $[wpID] </h3>
               		  <hr width="325px" />
               		  <b>Type: </b> $[type] <br />
               		  <b>Description: </b> $[descriptor] <br /><br />
               		  <b>Submitted By: </b> $[createdBy]<br />
               		  <b>Submitted At: </b> $[created]<br />
               		  <b>Last Updated By: </b> $[lastUpdatedBy]<br />
               		  <b>Last Updated At: </b> $[lastUpdated]<br /><br />
               		  <a href="]]>{$baseURL}<![CDATA[/uicds/pub/search?productID=$[wpID]&productVersion=$[ver]&format=xml">Retrieve WorkProduct</a><br /><br />
               		]]>
                </text>
            </BalloonStyle>
        </Style>        
        
        <Style id="Incident-Geometry">
            <IconStyle>
                <color>ff0000ff</color>
                <Icon>
                    <href>http://maps.google.com/mapfiles/kml/shapes/open-diamond.png</href>
                </Icon>
            </IconStyle>
            <PolyStyle>
                <color>990000aa</color>
                <colorMode>normal</colorMode>
                <fill>1</fill>
                <outline>1</outline> 
            </PolyStyle>
            <LineStyle>
                <color>ff0099aa</color>
                <width>2</width>
            </LineStyle>
            <BalloonStyle>
                <text>
                    <![CDATA[
               		  <h3>Event: $[event]</h3>
               		  <hr width="325px" />
               		  <b>Type: </b> $[code] <br />
               		  <b>Description: </b> $[descriptor] <br /><br />
               		  <b>Submitted By: </b> $[createdBy]<br />
               		  <b>Submitted At: </b> $[created]<br />
               		  <b>Last Updated By: </b> $[lastUpdatedBy]<br />
               		  <b>Last Updated At: </b> $[lastUpdated]<br /><br />
               		  <a href="]]>{$baseURL}<![CDATA[/uicds/pub/search?productID=$[wpID]&productVersion=$[ver]&format=xml">Retrieve WorkProduct</a><br /><br />
               		  $[geDirections]
               		]]>
                </text>
            </BalloonStyle>
        </Style>
        
        <Style id="Alert-Geometry">
            <IconStyle>
                <color>ff00ffff</color>
                <Icon>
                    <href>http://maps.google.com/mapfiles/kml/shapes/triangle.png</href>
                </Icon>
            </IconStyle>
            <PolyStyle>
                <color>990000aa</color>
                <colorMode>normal</colorMode>
                <fill>1</fill>
                <outline>1</outline> 
            </PolyStyle>
            <LineStyle>
                <color>ff0099aa</color>
                <width>2</width>
            </LineStyle>
            <BalloonStyle>
                <text>
                    <![CDATA[
               		  <h3>Alert: $[event]</h3>
               		  <hr width="325px" />
               		  <b>Type: </b> $[code] <br />
               		  <b>Description: </b> $[descriptor] <br /><br />
               		  <b>Submitted By: </b> $[createdBy]<br />
               		  <b>Submitted At: </b> $[created]<br />
               		  <b>Last Updated By: </b> $[lastUpdatedBy]<br />
               		  <b>Last Updated At: </b> $[lastUpdated]<br /><br />
               		  <a href="]]>{$baseURL}<![CDATA[/uicds/pub/search?productID=$[wpID]&productVersion=$[ver]&format=xml">Retrieve WorkProduct</a><br /><br />
               		  $[geDirections]
               		]]>
                </text>
            </BalloonStyle>
        </Style>
        
        <Style id="SOI-Geometry">
            <IconStyle>
                <color>ff00ffff</color>
                <Icon>
                    <href>http://maps.google.com/mapfiles/kml/shapes/donut.png</href>
                </Icon>
            </IconStyle>
            <PolyStyle>
                <color>9900ffff</color>
                <colorMode>normal</colorMode>
                <fill>1</fill>
                <outline>1</outline> 
            </PolyStyle>
            <LineStyle>
                <color>ff55ffff</color>
                <width>2</width>
            </LineStyle>            		
            <BalloonStyle>
                <text>
                    <![CDATA[
            		  <h3>Sensor Location</h3>
            		  <hr width="325px" />
            		  <b>Description:</b> $[descriptor] <br /><br />
               		  <a href="]]>{$baseURL}<![CDATA[/uicds/pub/search?productID=$[wpID]&productVersion=$[ver]&format=xml">Retrieve WorkProduct</a><br /><br />
               		  $[geDirections]
               		]]>
                </text>
            </BalloonStyle>
        </Style>
        
        <name>UICDS</name>
        <Snippet>Returned {fn:count($workproducts)} results at {fn:current-dateTime()}</Snippet>
        </Document>
        {
           for $incident in $workproducts[NS_ULEX_STRUCTURE:PackageMetadata[1]/NS_PRECIS_STRUCTURES:WorkProductIdentification/NS_PRECIS_BASE:Type="Incident"]
         	let $productID := fn:data($incident/NS_ULEX_STRUCTURE:PackageMetadata[1]/NS_PRECIS_STRUCTURES:WorkProductIdentification/NS_PRECIS_BASE:Identifier)
           	let $ver := fn:data($incident/NS_ULEX_STRUCTURE:PackageMetadata[1]/NS_PRECIS_STRUCTURES:WorkProductIdentification/NS_PRECIS_BASE:Version)
           	let $type := fn:data($incident/NS_ULEX_STRUCTURE:PackageMetadata[1]/NS_PRECIS_STRUCTURES:WorkProductIdentification/NS_PRECIS_BASE:Type)
           	let $state := fn:data($incident/NS_ULEX_STRUCTURE:PackageMetadata[1]/NS_PRECIS_STRUCTURES:WorkProductIdentification/NS_PRECIS_BASE:State)
           	
           	let $identifier := functx:if-empty(fn:data($incident/NS_UCORE:Digest[1]/NS_UCORE:Event[1]/NS_UCORE:Identifier[1]), "no identifier available")
           	let $what := functx:if-empty(fn:data($incident/NS_UCORE:Digest[1]/NS_UCORE:Event[1]/NS_UCORE:What[1]/@NS_UCORE:code), "unknown")
           	let $descriptor := functx:if-empty(fn:data($incident/NS_UCORE:Digest[1]/NS_UCORE:Event[1]/NS_UCORE:Descriptor[1]), "no description available")
       	   	let $createdBy := functx:if-empty(fn:data($incident/NS_ULEX_STRUCTURE:PackageMetadata[1]/NS_PRECIS_STRUCTURES:WorkProductProperties/NS_PRECIS_BASE:CreatedBy), "unknown")
           	let $created := functx:if-empty(fn:data($incident/NS_ULEX_STRUCTURE:PackageMetadata[1]/NS_PRECIS_STRUCTURES:WorkProductProperties/NS_PRECIS_BASE:Created), "unknown")
           	let $lastUpdatedBy := functx:if-empty(fn:data($incident/NS_ULEX_STRUCTURE:PackageMetadata[1]/NS_PRECIS_STRUCTURES:WorkProductProperties/NS_PRECIS_BASE:LastUpdatedBy), "unknown")
           	let $lastUpdated := functx:if-empty(fn:data($incident/NS_ULEX_STRUCTURE:PackageMetadata[1]/NS_PRECIS_STRUCTURES:WorkProductProperties/NS_PRECIS_BASE:LastUpdated), "unknown")
           	let $interestGroup := functx:if-empty(fn:data($incident/NS_ULEX_STRUCTURE:PackageMetadata[1]/NS_PRECIS_STRUCTURES:WorkProductProperties[1]/NS_PRECIS_BASE:AssociatedGroups[1]/NS_PRECIS_BASE:Identifier[1]), "no interest group")
           	
           return
                <Folder>
                    <name>{$identifier}</name>
                    <Folder>
                        <name>Incident</name>
                        <Snippet>{$descriptor}</Snippet>
                     <styleUrl>#Incident-Balloon</styleUrl>
                     <ExtendedData>
                       <Data name="wpID">
                     <value>{$productID}</value>
                       </Data>
                       <Data name="ver">
                     <value>{$ver}</value>
                       </Data>
                       <Data name="type">
                     <value>{$type}</value>
                       </Data>
                       <Data name="state">
                     <value>{$state}</value>
                       </Data>	      
                       <Data name="event">
                           <value>{$identifier}</value>
                       </Data>
                       <Data name="code">
                     <value>{$what}</value>
                       </Data>
                       <Data name="descriptor">
                           <value>{$descriptor}</value>
                       </Data>
                       <Data name="created">
                           <value>{$created}</value>
                       </Data>
                       <Data name="createdBy">
                           <value>{$createdBy}</value>
                       </Data>
                       <Data name="lastUpdated">
                           <value>{$lastUpdated}</value>
                       </Data>
                       <Data name="lastUpdatedBy">
                           <value>{$lastUpdatedBy}</value>
                       </Data>
                     </ExtendedData>

                        {kml-v:genPlacemarks($incident)}
                        <Folder>
                            <name>Overlays</name>
                                {kml-v:genDefaultOverlay($incident)}
                                {kml-v:genAddedOverlays($incident)}
                        </Folder>
                        
                    </Folder>
                    
                    {
                    for $subType in fn:distinct-values($workproducts[NS_ULEX_STRUCTURE:PackageMetadata[1]/NS_PRECIS_STRUCTURES:WorkProductProperties[1]/NS_PRECIS_BASE:AssociatedGroups[1]/NS_PRECIS_BASE:Identifier[1]=$interestGroup and NS_ULEX_STRUCTURE:PackageMetadata[1]/NS_PRECIS_STRUCTURES:WorkProductIdentification/NS_PRECIS_BASE:Type!="Incident"]/NS_ULEX_STRUCTURE:PackageMetadata/NS_PRECIS_STRUCTURES:WorkProductIdentification/NS_PRECIS_BASE:Type)
                    return  
                        <Folder>
                            <name>{$subType}</name>
                            {
                                for $subproduct in $workproducts[NS_ULEX_STRUCTURE:PackageMetadata[1]/NS_PRECIS_STRUCTURES:WorkProductProperties[1]/NS_PRECIS_BASE:AssociatedGroups[1]/NS_PRECIS_BASE:Identifier[1]=$interestGroup and fn:data(NS_ULEX_STRUCTURE:PackageMetadata[1]/NS_PRECIS_STRUCTURES:WorkProductIdentification[1]/NS_PRECIS_BASE:Type)=$subType]
                                
                               	let $productID := fn:data($subproduct/NS_ULEX_STRUCTURE:PackageMetadata[1]/NS_PRECIS_STRUCTURES:WorkProductIdentification/NS_PRECIS_BASE:Identifier)
                              	let $ver := fn:data($subproduct/NS_ULEX_STRUCTURE:PackageMetadata[1]/NS_PRECIS_STRUCTURES:WorkProductIdentification/NS_PRECIS_BASE:Version)
                              	let $type := fn:data($subproduct/NS_ULEX_STRUCTURE:PackageMetadata[1]/NS_PRECIS_STRUCTURES:WorkProductIdentification/NS_PRECIS_BASE:Type)
                              	let $state := fn:data($subproduct/NS_ULEX_STRUCTURE:PackageMetadata[1]/NS_PRECIS_STRUCTURES:WorkProductIdentification/NS_PRECIS_BASE:State)
                              	
                              	let $identifier := functx:if-empty(fn:data($subproduct/NS_UCORE:Digest[1]/NS_UCORE:Event[1]/NS_UCORE:Identifier[1]), "no identifier available")
                              	let $what := functx:if-empty(fn:data($subproduct/NS_UCORE:Digest[1]/NS_UCORE:Event[1]/NS_UCORE:What[1]/@NS_UCORE:code), "unknown")
                              	let $descriptor := functx:if-empty(fn:data($subproduct/NS_UCORE:Digest[1]/NS_UCORE:Event[1]/NS_UCORE:Descriptor[1]), "no description available")
                          	    let $createdBy := functx:if-empty(fn:data($subproduct/NS_ULEX_STRUCTURE:PackageMetadata[1]/NS_PRECIS_STRUCTURES:WorkProductProperties/NS_PRECIS_BASE:CreatedBy), "unknown")
                              	let $created := functx:if-empty(fn:data($subproduct/NS_ULEX_STRUCTURE:PackageMetadata[1]/NS_PRECIS_STRUCTURES:WorkProductProperties/NS_PRECIS_BASE:Created), "unknown")
                              	let $lastUpdatedBy := functx:if-empty(fn:data($subproduct/NS_ULEX_STRUCTURE:PackageMetadata[1]/NS_PRECIS_STRUCTURES:WorkProductProperties/NS_PRECIS_BASE:LastUpdatedBy), "unknown")
                              	let $lastUpdated := functx:if-empty(fn:data($subproduct/NS_ULEX_STRUCTURE:PackageMetadata[1]/NS_PRECIS_STRUCTURES:WorkProductProperties/NS_PRECIS_BASE:LastUpdated), "unknown")
                              	let $interestGroup := functx:if-empty(fn:data($subproduct/NS_ULEX_STRUCTURE:PackageMetadata[1]/NS_PRECIS_STRUCTURES:WorkProductProperties[1]/NS_PRECIS_BASE:AssociatedGroups[1]/NS_PRECIS_BASE:Identifier[1]), "no interest group")
                    
                                return
                                <Document>
                                    <name>{$subType} Document</name>
                                    <Snippet>{fn:data($subproduct/NS_ULEX_STRUCTURE:PackageMetadata[1]/NS_PRECIS_STRUCTURES:WorkProductIdentification/NS_PRECIS_BASE:Identifier)}</Snippet>
                                     <styleUrl>#WorkProduct-Balloon</styleUrl>
                                    <ExtendedData>
                                      <Data name="wpID">
                                    <value>{$productID}</value>
                                      </Data>
                                      <Data name="ver">
                                    <value>{$ver}</value>
                                      </Data>
                                      <Data name="type">
                                    <value>{$type}</value>
                                      </Data>
                                      <Data name="state">
                                    <value>{$state}</value>
                                      </Data>	      
                                      <Data name="event">
                                          <value>{$identifier}</value>
                                      </Data>
                                      <Data name="code">
                                    <value>{$what}</value>
                                      </Data>
                                      <Data name="descriptor">
                                          <value>{$descriptor}</value>
                                      </Data>
                                      <Data name="created">
                                          <value>{$created}</value>
                                      </Data>
                                      <Data name="createdBy">
                                          <value>{$createdBy}</value>
                                      </Data>
                                      <Data name="lastUpdated">
                                          <value>{$lastUpdated}</value>
                                      </Data>
                                      <Data name="lastUpdatedBy">
                                          <value>{$lastUpdatedBy}</value>
                                      </Data>
                                    </ExtendedData>
                                    {kml-v:genPlacemarks($subproduct)}
                                </Document>
                            }
                        </Folder>
                    }
                </Folder>
        }
        <Folder>
            <name>Work Products (Unassociated)</name>
            {
            let $unassociatedWPs := $workproducts[fn:not(fn:contains($incidentIDs,functx:if-empty(NS_ULEX_STRUCTURE:PackageMetadata[1]/NS_PRECIS_STRUCTURES:WorkProductProperties[1]/NS_PRECIS_BASE:AssociatedGroups[1]/NS_PRECIS_BASE:Identifier[1],"no identifier")))] 
            
        return         
         for $subType in fn:distinct-values($unassociatedWPs/NS_ULEX_STRUCTURE:PackageMetadata[1]/NS_PRECIS_STRUCTURES:WorkProductIdentification/NS_PRECIS_BASE:Type)
                    return  
                        <Folder>
                            <name>{$subType}s</name>
                            {
                                for $subproduct in $unassociatedWPs[fn:data(NS_ULEX_STRUCTURE:PackageMetadata[1]/NS_PRECIS_STRUCTURES:WorkProductIdentification[1]/NS_PRECIS_BASE:Type)=$subType]
                                
                               	let $productID := fn:data($subproduct/NS_ULEX_STRUCTURE:PackageMetadata[1]/NS_PRECIS_STRUCTURES:WorkProductIdentification/NS_PRECIS_BASE:Identifier)
                              	let $ver := fn:data($subproduct/NS_ULEX_STRUCTURE:PackageMetadata[1]/NS_PRECIS_STRUCTURES:WorkProductIdentification/NS_PRECIS_BASE:Version)
                              	let $type := fn:data($subproduct/NS_ULEX_STRUCTURE:PackageMetadata[1]/NS_PRECIS_STRUCTURES:WorkProductIdentification/NS_PRECIS_BASE:Type)
                              	let $state := fn:data($subproduct/NS_ULEX_STRUCTURE:PackageMetadata[1]/NS_PRECIS_STRUCTURES:WorkProductIdentification/NS_PRECIS_BASE:State)
                              	
                              	let $identifier := functx:if-empty(fn:data($subproduct/NS_UCORE:Digest[1]/NS_UCORE:Event[1]/NS_UCORE:Identifier[1]), "no identifier available")
                              	let $what := functx:if-empty(fn:data($subproduct/NS_UCORE:Digest[1]/NS_UCORE:Event[1]/NS_UCORE:What[1]/@NS_UCORE:code), "unknown")
                              	let $descriptor := functx:if-empty(fn:data($subproduct/NS_UCORE:Digest[1]/NS_UCORE:Event[1]/NS_UCORE:Descriptor[1]), "no description available")
                          	    let $createdBy := functx:if-empty(fn:data($subproduct/NS_ULEX_STRUCTURE:PackageMetadata[1]/NS_PRECIS_STRUCTURES:WorkProductProperties/NS_PRECIS_BASE:CreatedBy), "unknown")
                              	let $created := functx:if-empty(fn:data($subproduct/NS_ULEX_STRUCTURE:PackageMetadata[1]/NS_PRECIS_STRUCTURES:WorkProductProperties/NS_PRECIS_BASE:Created), "unknown")
                              	let $lastUpdatedBy := functx:if-empty(fn:data($subproduct/NS_ULEX_STRUCTURE:PackageMetadata[1]/NS_PRECIS_STRUCTURES:WorkProductProperties/NS_PRECIS_BASE:LastUpdatedBy), "unknown")
                              	let $lastUpdated := functx:if-empty(fn:data($subproduct/NS_ULEX_STRUCTURE:PackageMetadata[1]/NS_PRECIS_STRUCTURES:WorkProductProperties/NS_PRECIS_BASE:LastUpdated), "unknown")
                    
                                return
                                <Document>
                                    <name>{$subType} Document</name>
                                    <Snippet>{fn:data($subproduct/NS_ULEX_STRUCTURE:PackageMetadata[1]/NS_PRECIS_STRUCTURES:WorkProductIdentification/NS_PRECIS_BASE:Identifier)}</Snippet>
                                     <styleUrl>#WorkProduct-Balloon</styleUrl>
                                    <ExtendedData>
                                      <Data name="wpID">
                                    <value>{$productID}</value>
                                      </Data>
                                      <Data name="ver">
                                    <value>{$ver}</value>
                                      </Data>
                                      <Data name="type">
                                    <value>{$type}</value>
                                      </Data>
                                      <Data name="state">
                                    <value>{$state}</value>
                                      </Data>	      
                                      <Data name="event">
                                          <value>{$identifier}</value>
                                      </Data>
                                      <Data name="code">
                                    <value>{$what}</value>
                                      </Data>
                                      <Data name="descriptor">
                                          <value>{$descriptor}</value>
                                      </Data>
                                      <Data name="created">
                                          <value>{$created}</value>
                                      </Data>
                                      <Data name="createdBy">
                                          <value>{$createdBy}</value>
                                      </Data>
                                      <Data name="lastUpdated">
                                          <value>{$lastUpdated}</value>
                                      </Data>
                                      <Data name="lastUpdatedBy">
                                          <value>{$lastUpdatedBy}</value>
                                      </Data>
                                    </ExtendedData>
                                    {kml-v:genPlacemarks($subproduct)}                                    
                                </Document>
                            }
                        </Folder>
            }
        </Folder>
    </Folder>
</kml>
};

declare function kml-v:genPlacemarks($workproduct as node()*) as node()* {
         	let $productID := fn:data($workproduct/NS_ULEX_STRUCTURE:PackageMetadata[1]/NS_PRECIS_STRUCTURES:WorkProductIdentification/NS_PRECIS_BASE:Identifier)
           	let $ver := fn:data($workproduct/NS_ULEX_STRUCTURE:PackageMetadata[1]/NS_PRECIS_STRUCTURES:WorkProductIdentification/NS_PRECIS_BASE:Version)
           	let $type := fn:data($workproduct/NS_ULEX_STRUCTURE:PackageMetadata[1]/NS_PRECIS_STRUCTURES:WorkProductIdentification/NS_PRECIS_BASE:Type)
           	let $state := fn:data($workproduct/NS_ULEX_STRUCTURE:PackageMetadata[1]/NS_PRECIS_STRUCTURES:WorkProductIdentification/NS_PRECIS_BASE:State)
           	
           	let $identifier := functx:if-empty(fn:data($workproduct/NS_UCORE:Digest[1]/NS_UCORE:Event[1]/NS_UCORE:Identifier[1]), "no identifier available")
           	let $what := functx:if-empty(fn:data($workproduct/NS_UCORE:Digest[1]/NS_UCORE:Event[1]/NS_UCORE:What[1]/@NS_UCORE:code), "unknown")
           	let $descriptor := functx:if-empty(fn:data($workproduct/NS_UCORE:Digest[1]/NS_UCORE:Event[1]/NS_UCORE:Descriptor[1]), "no description available")
       	    let $createdBy := functx:if-empty(fn:data($workproduct/NS_ULEX_STRUCTURE:PackageMetadata[1]/NS_PRECIS_STRUCTURES:WorkProductProperties/NS_PRECIS_BASE:CreatedBy), "unknown")
           	let $created := functx:if-empty(fn:data($workproduct/NS_ULEX_STRUCTURE:PackageMetadata[1]/NS_PRECIS_STRUCTURES:WorkProductProperties/NS_PRECIS_BASE:Created), "unknown")
           	let $lastUpdatedBy := functx:if-empty(fn:data($workproduct/NS_ULEX_STRUCTURE:PackageMetadata[1]/NS_PRECIS_STRUCTURES:WorkProductProperties/NS_PRECIS_BASE:LastUpdatedBy), "unknown")
           	let $lastUpdated := functx:if-empty(fn:data($workproduct/NS_ULEX_STRUCTURE:PackageMetadata[1]/NS_PRECIS_STRUCTURES:WorkProductProperties/NS_PRECIS_BASE:LastUpdated), "unknown")
           	let $interestGroup := functx:if-empty(fn:data($workproduct/NS_ULEX_STRUCTURE:PackageMetadata[1]/NS_PRECIS_STRUCTURES:WorkProductProperties[1]/NS_PRECIS_BASE:AssociatedGroups[1]/NS_PRECIS_BASE:Identifier[1]), "no interest group")
        
            for $location in $workproduct/NS_UCORE:Digest//NS_UCORE:GeoLocation
           	return
           	  <Placemark>
           	        <name>{$identifier}</name>
                     <styleUrl>#{$type}-Geometry</styleUrl>
                     <ExtendedData>
                       <Data name="wpID">
                     <value>{$productID}</value>
                       </Data>
                       <Data name="ver">
                     <value>{$ver}</value>
                       </Data>
                       <Data name="type">
                     <value>{$type}</value>
                       </Data>
                       <Data name="state">
                     <value>{$state}</value>
                       </Data>	      
                       <Data name="event">
                           <value>{$identifier}</value>
                       </Data>
                       <Data name="code">
                     <value>{$what}</value>
                       </Data>
                       <Data name="descriptor">
                           <value>{$descriptor}</value>
                       </Data>
                       <Data name="created">
                           <value>{$created}</value>
                       </Data>
                       <Data name="createdBy">
                           <value>{$createdBy}</value>
                       </Data>
                       <Data name="lastUpdated">
                           <value>{$lastUpdated}</value>
                       </Data>
                       <Data name="lastUpdatedBy">
                           <value>{$lastUpdatedBy}</value>
                       </Data>
                     </ExtendedData>
                     
           	        {kml-v:genGeometry($location)}
		<!--
                  <TimeSpan>
                     <begin>{$created}</begin>
                  </TimeSpan>
		-->                  
          	</Placemark>
};

declare function kml-v:genDefaultOverlay($incident as node()) as node()* {
    let $baseURL := props:get-property(doc("/db/UICDS/config.xml")/c:config/p:props,"baseURL","http://localhost")
    let $interestGroup := functx:if-empty(fn:data($incident/NS_ULEX_STRUCTURE:PackageMetadata[1]/NS_PRECIS_STRUCTURES:WorkProductProperties[1]/NS_PRECIS_BASE:AssociatedGroups[1]/NS_PRECIS_BASE:Identifier[1]), "no interest group")
    return
         if (/NS_PRECIS_STRUCTURES:WorkProduct/NS_ULEX_STRUCTURE:PackageMetadata[1][(NS_PRECIS_STRUCTURES:WorkProductIdentification[1]/NS_PRECIS_BASE:Type[1]='Feature') and (NS_PRECIS_STRUCTURES:WorkProductProperties[1]/NS_PRECIS_BASE:AssociatedGroups[1]/NS_PRECIS_BASE:Identifier[1]/text()=$interestGroup) ]) then
                <GroundOverlay>
                <name>Incident Features (OGC:WMS)</name>
                <Snippet>{$interestGroup}</Snippet>
                <color>7fffffff</color>
                <drawOrder>1</drawOrder>
                <Icon>
                 <href>{$baseURL}/uicds/api/{$interestGroup}/features</href>
                 <refreshMode>onInterval</refreshMode>
                 <refreshInterval>60</refreshInterval>
                 <viewRefreshMode>onStop</viewRefreshMode>
                 <viewRefreshTime>10</viewRefreshTime>
                 <viewFormat>?REQUEST=GetMap&amp;VERSION=1.1.0&amp;SRS=EPSG:4326&amp;WIDTH=[horizPixels]&amp;HEIGHT=[vertPixels]&amp;BBOX=[bboxWest],[bboxSouth],[bboxEast],[bboxNorth]</viewFormat>
                </Icon>
                <LatLonBox>
                   <north>[bboxNorth]</north>
                   <south>[bboxSouth]</south>
                   <east>[bboxEast]</east>
                   <west>[bboxWest]</west>
                </LatLonBox>
                </GroundOverlay>
         else
             ()
};

declare function kml-v:genAddedOverlays($incident as node()) as node()* {
    let $baseURL := props:get-property(doc("/db/UICDS/config.xml")/c:config/p:props,"baseURL","http://localhost")
    let $interestGroup := functx:if-empty(fn:data($incident/NS_ULEX_STRUCTURE:PackageMetadata[1]/NS_PRECIS_STRUCTURES:WorkProductProperties[1]/NS_PRECIS_BASE:AssociatedGroups[1]/NS_PRECIS_BASE:Identifier[1]), "no interest group")
    (: Select on MapViewContext WPs in this interestGroup :)
    let $workproducts := /NS_PRECIS_STRUCTURES:WorkProduct[(NS_ULEX_STRUCTURE:PackageMetadata[1]/NS_PRECIS_STRUCTURES:WorkProductIdentification[1]/NS_PRECIS_BASE:Type[1]='MapViewContext') and (NS_ULEX_STRUCTURE:PackageMetadata[1]/NS_PRECIS_STRUCTURES:WorkProductProperties[1]/NS_PRECIS_BASE:AssociatedGroups[1]/NS_PRECIS_BASE:Identifier[1]/text()=$interestGroup)]
    for $context in $workproducts/NS_ULEX_STRUCTURE:StructuredPayload/NS_WMC:ViewContext/NS_WMC:LayerList/NS_WMC:Layer
    where ( ($context/NS_WMC:Title/text() != 'Base Map') and ($context/NS_WMC:Title/text() != 'Incident Features'))
    return
        <GroundOverlay>
                <name>{$context/NS_WMC:Title/text()} ({fn:data($context/NS_WMC:Server/@service)})</name>
                <Snippet>{$interestGroup}</Snippet>
                <color>7fffffff</color>
                <drawOrder>1</drawOrder>
                <Icon>
                 <href>{fn:data($context/NS_WMC:Server/NS_WMC:OnlineResource/@NS_XLINK:href)}</href>
                 <refreshMode>onInterval</refreshMode>
                 <refreshInterval>60</refreshInterval>
                 <viewRefreshMode>onStop</viewRefreshMode>
                 <viewRefreshTime>10</viewRefreshTime>
                 <viewFormat>&amp;WIDTH=[horizPixels]&amp;HEIGHT=[vertPixels]&amp;BBOX=[bboxWest],[bboxSouth],[bboxEast],[bboxNorth]</viewFormat>
                </Icon>
                <LatLonBox>
                   <north>[bboxNorth]</north>
                   <south>[bboxSouth]</south>
                   <east>[bboxEast]</east>
                   <west>[bboxWest]</west>
                </LatLonBox>
        </GroundOverlay>
};



declare function kml-v:genIncident($incidentProducts as node()*) as node()* {

    for $workproduct in $incidentProducts
        let $type := fn:data($workproduct/NS_ULEX_STRUCTURE:PackageMetadata[1]/NS_PRECIS_STRUCTURES:WorkProductIdentification/NS_PRECIS_BASE:Type)
        let $identifier := functx:if-empty(fn:data($workproduct/NS_UCORE:Digest[1]/NS_UCORE:Event[1]/NS_UCORE:Identifier[1]), "no identifier available")
        return
            if ($type="Incident") then
                <name>{$identifier}</name>
            else
                <Folder><name>{$type}</name></Folder>
};

declare function kml-v:genGeometry($location) as node() {
        if ($location/NS_UCORE:Polygon) then
            <Polygon>
        		<outerBoundaryIs>
        			<LinearRing>
        				<coordinates> {
        					for $pt in $location//NS_GML:Polygon/NS_GML:exterior/NS_GML:LinearRing/NS_GML:pos
            					let $pos := fn:tokenize($pt, '\s')
            					return
            						string-join( (fn:item-at($pos,2),fn:item-at($pos,1) ), ",")
            					}
        				</coordinates>
        			</LinearRing>
        		</outerBoundaryIs>
        	</Polygon>
        else if ($location/NS_UCORE:Point) then
    		let $pos := fn:tokenize($location//NS_GML:Point/NS_GML:pos, '\s')
            return 
       		   <Point><coordinates>{fn:item-at($pos,2)},{fn:item-at($pos,1)}</coordinates></Point>
        else if ($location/NS_UCORE:CircleByCenterPoint) then
            let $pos := fn:tokenize($location//NS_GML:CircleByCenterPoint/NS_GML:pos, '\s')
            return
       		   <Point><coordinates>{fn:item-at($pos,2)},{fn:item-at($pos,1)}</coordinates></Point>
        else if ($location/NS_UCORE:LineString) then
    	    <LineString>
                <extrude>1</extrude>
                <tessellate>1</tessellate>
       				<coordinates> {
        					for $pt in $location//NS_GML:LineString/NS_GML:pos
            					let $pos := fn:tokenize($pt, '\s')
            					return
            						string-join( (fn:item-at($pos,2),fn:item-at($pos,1) ), ",")
            					}
        				</coordinates>
            </LineString>
        else
            ()

};
