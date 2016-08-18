xquery version "1.0";
module namespace ns='http://uicds.org/modules/transform/kml';

import module namespace functx = 'http://www.functx.com' at '../../common/functx.xq';
import module namespace props = 'http://uicds.org/modules/util/properties' at '../../common/properties.xq';

declare namespace conf = "util:config";

declare namespace NS_PROPS = "util:properties";
declare namespace NS_PRECIS_STRUCTURES = "http://www.saic.com/precis/2009/06/structures";
declare namespace NS_PRECIS_BASE = "http://www.saic.com/precis/2009/06/base";
declare namespace NS_ULEX_STRUCTURE = "ulex:message:structure:1.0";
declare namespace NS_UCORE = "http://ucore.gov/ucore/2.0";
declare namespace NS_GML = "http://www.opengis.net/gml/3.2";
declare namespace NS_WMC = "http://www.opengis.net/context";
declare namespace NS_XLINK = "http://www.w3.org/1999/xlink";

declare namespace georss = "http://www.georss.org/georss";
declare namespace kml = "http://www.opengis.net/kml/2.2";
declare default element namespace "http://www.opengis.net/kml/2.2";

(:
 :  This is the entry point
 :)
declare function ns:render($config as node(),  $parameters as node(), $workproducts as node()) as node()* {

let $baseURL := functx:if-empty($config//NS_PROPS:prop[@name='baseURL']/@value, 'https://localhost')

let $incidentIDs := fn:distinct-values($workproducts[NS_PRECIS_STRUCTURES:WorkProduct/NS_ULEX_STRUCTURE:PackageMetadata[1]/NS_PRECIS_STRUCTURES:WorkProductIdentification/NS_PRECIS_BASE:Type="Incident"]/NS_ULEX_STRUCTURE:PackageMetadata[1]/NS_PRECIS_STRUCTURES:WorkProductProperties[1]/NS_PRECIS_BASE:AssociatedGroups[1]/NS_PRECIS_BASE:Identifier[1])

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
        
        <Style id="Feature-Geometry">
            <IconStyle>
                <Icon>
                    <href>http://maps.google.com/mapfiles/kml/paddle/ylw-blank.png</href>
                </Icon>
            </IconStyle>
            <PolyStyle>
                <color>7F00FFFF</color>
                <colorMode>normal</colorMode>
                <fill>1</fill>
                <outline>1</outline> 
            </PolyStyle>
            <LineStyle>
                <color>FF00FFFF</color>
                <width>2</width>
            </LineStyle>            		
            <BalloonStyle>
                <text>
                    <![CDATA[
            		  <h3>Feature Information</h3>
            		  <hr width="325px" />
            		  <b>Description:</b> $[descriptor] <br /><br />
               		  <a href="]]>{$baseURL}<![CDATA[/uicds/pub/search?productID=$[wpID]&productVersion=$[ver]&format=xml">Retrieve WorkProduct</a><br /><br />
               		  $[geDirections]
               		]]>
                </text>
            </BalloonStyle>
        </Style>
        
        
        <name>UICDS</name>
        <Snippet>Returned {fn:count($workproducts//NS_PRECIS_STRUCTURES:WorkProduct)} results at {fn:current-dateTime()}</Snippet>
        </Document>
        {
           for $incident in $workproducts//NS_PRECIS_STRUCTURES:WorkProduct[NS_ULEX_STRUCTURE:PackageMetadata[1]/NS_PRECIS_STRUCTURES:WorkProductIdentification/NS_PRECIS_BASE:Type="Incident"]
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

                        {ns:genPlacemarks($config, $incident)}
                        <Folder>
                            <name>Overlays</name>
                                <!-- { ns:genAddedOverlays($config, $incident)} -->
                        </Folder>
                        
                    </Folder>
                    
                    {

					for $subType in fn:distinct-values($workproducts//NS_PRECIS_STRUCTURES:WorkProduct[NS_ULEX_STRUCTURE:PackageMetadata[1]/NS_PRECIS_STRUCTURES:WorkProductProperties[1]/NS_PRECIS_BASE:AssociatedGroups[1]/NS_PRECIS_BASE:Identifier[1]/text()=$interestGroup and NS_ULEX_STRUCTURE:PackageMetadata[1]/NS_PRECIS_STRUCTURES:WorkProductIdentification/NS_PRECIS_BASE:Type/text() != "Incident"]/NS_ULEX_STRUCTURE:PackageMetadata/NS_PRECIS_STRUCTURES:WorkProductIdentification/NS_PRECIS_BASE:Type)

                    return  
                        <Folder>
                            <name>{$subType}</name>
                            {
                                for $subproduct in $workproducts//NS_PRECIS_STRUCTURES:WorkProduct[NS_ULEX_STRUCTURE:PackageMetadata[1]/NS_PRECIS_STRUCTURES:WorkProductProperties[1]/NS_PRECIS_BASE:AssociatedGroups[1]/NS_PRECIS_BASE:Identifier[1]=$interestGroup and fn:data(NS_ULEX_STRUCTURE:PackageMetadata[1]/NS_PRECIS_STRUCTURES:WorkProductIdentification[1]/NS_PRECIS_BASE:Type)=$subType]
                                
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
                                    {ns:genPlacemarks($config, $subproduct)}
                                </Document>
                            }
                        </Folder>
                    }
                </Folder>
        }
        <Folder>
            <name>Work Products (Unassociated)</name>
            {
			(:
            let $unassociatedWPs := $workproducts//NS_PRECIS_STRUCTURES:WorkProduct[fn:not(fn:contains($incidentIDs,functx:if-empty(NS_ULEX_STRUCTURE:PackageMetadata[1]/NS_PRECIS_STRUCTURES:WorkProductProperties[1]/NS_PRECIS_BASE:AssociatedGroups[1]/NS_PRECIS_BASE:Identifier[1],"no identifier"))) and NS_ULEX_STRUCTURE:PackageMetadata[1]/NS_PRECIS_STRUCTURES:WorkProductIdentification/NS_PRECIS_BASE:Type/text() != "Incident"] 
			:)
            
			            let $unassociatedWPs := $workproducts//NS_PRECIS_STRUCTURES:WorkProduct[fn:not(NS_ULEX_STRUCTURE:PackageMetadata[1]/NS_PRECIS_STRUCTURES:WorkProductProperties[1]/NS_PRECIS_BASE:AssociatedGroups[1]/NS_PRECIS_BASE:Identifier[1]) and NS_ULEX_STRUCTURE:PackageMetadata[1]/NS_PRECIS_STRUCTURES:WorkProductIdentification/NS_PRECIS_BASE:Type/text() != "Incident"] 
			
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
                                    {ns:genPlacemarks($config, $subproduct)}                                    
                                </Document>
                            }
                        </Folder>
            }
        </Folder>
    </Folder>
</kml>
};

declare function ns:genPlacemarks($config as node(),  $workproduct as node()*) as node()* {
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
                     
           	        {ns:dispatchGeo($config, $location)}
               
          	</Placemark>
};


declare function ns:genIncident($config as node(),  $incidentProducts as node()*) as node()* {

    for $workproduct in $incidentProducts
        let $type := fn:data($workproduct/NS_ULEX_STRUCTURE:PackageMetadata[1]/NS_PRECIS_STRUCTURES:WorkProductIdentification/NS_PRECIS_BASE:Type)
        let $identifier := functx:if-empty(fn:data($workproduct/NS_UCORE:Digest[1]/NS_UCORE:Event[1]/NS_UCORE:Identifier[1]), "no identifier available")
        return
            if ($type="Incident") then
                <name>{$identifier}</name>
            else
                <Folder><name>{$type}</name></Folder>
};


(: 
 : -------------------------------------
 : WVS - NEW GEO and VALUE RENDERING CODE
 : -------------------------------------
 :)

(:
 :  Recursive processor for location (address / geospatial) types
 :)
declare function ns:dispatchGeo($config as node(), $x as node()) as node()* {  
typeswitch ($x)

    (: Geospatial Location Processing (GML) :)
    case element (NS_GML:CircleByCenterPoint) return ns:CircleByCenterPoint($config, $x)
    case element (NS_GML:Polygon) return ns:Polygon($config, $x)
    case element (NS_GML:Point) return ns:Point($config, $x)
    case element (NS_GML:Envelope) return ns:Envelope($config, $x)
    case element (NS_GML:LineString) return ns:LineString($config, $x)
    
    (: Cyber Address Processing (DDMS) :)
    
    (: Physical Address Processing (DDMS) :)
    
    (: Default Transformer (passthru) :)
    default return ns:passthruGeo($config, $x)
};


declare function ns:generate-id($config as node(), $x as node()) as xs:string {
    let $base := $x/ancestor::NS_PRECIS_STRUCTURES:WorkProduct//NS_PRECIS_STRUCTURES:WorkProductIdentification/NS_PRECIS_BASE:Identifier/text()
    let $guid := fn:concat($base, "#", fn:data($x/@id) )
    return
        $guid
};

declare function ns:generate-name($config as node(), $x as node()) as node()* {
    (:  TODO:  change to use only the "pretty name" identifier :)
    let $title := functx:if-empty($x/NS_UCORE:Identifier[@NS_UCORE:code='label']/text(), "no title given")
    return
        <name>{$title}</name>
};

declare function ns:generate-description($config as node(), $x as node()) as node()* {
    let $title := functx:if-empty($x/NS_UCORE:Descriptor/text(), "no description given")
    return
        <description>{$title}</description>
};


declare function ns:generate-Snippet($config as node(), $x as node()) as node()* {
    let $categories := fn:data($x/NS_UCORE:What/@NS_UCORE:code)
    return
      if (fn:count($categories) != 0) then
          <Snippet>
            {$categories}
          </Snippet>
      else
        ()
};

declare function ns:generate-extendedData($config as node(), $x as node()) as node()* {
    let $simpleProperties := $x/NS_UCORE:SimpleProperty
    return
      if ($simpleProperties) then
          <ExtendedData>
          {
           for $simpleProperty in $simpleProperties
           return
                <Data name="{$simpleProperty/@NS_UCORE:label}">
                    <value>{$simpleProperty/text()}</value>
                </Data>
          }
          </ExtendedData>
      else
        ()
};

declare function ns:generate-link($config as node(), $x as node()) as node()* {
    let $baseURL := fn:data($config//NS_PROPS:prop[@name='baseURL']/@value)
    let $productID := $x/ancestor::NS_PRECIS_STRUCTURES:WorkProduct//NS_PRECIS_STRUCTURES:WorkProductIdentification/NS_PRECIS_BASE:Identifier/text()
    let $productVersion := $x/ancestor::NS_PRECIS_STRUCTURES:WorkProduct//NS_PRECIS_STRUCTURES:WorkProductIdentification/NS_PRECIS_BASE:Version/text()
    let $link := fn:concat($baseURL,"/uicds/pub/search?productID=",$productID,"&amp;productVersion=",$productVersion,"&amp;format=xml")
    return
        if ($link) then 
            <link>{$link}</link>
        else
            ()
};

declare function ns:Polygon($config as node(), $x as node()) as node()* {
    let $posList := $x/NS_GML:exterior/NS_GML:LinearRing/NS_GML:pos
    return
    if ($posList) then
       <Polygon>
           <outerBoundaryIs>
           	<LinearRing>
           		<coordinates> 
           		{
           			for $pt in $posList
               			let $pos := fn:tokenize($pt, '\s')
               			return
               				string-join( ($pos[2],$pos[1] ), ",")
               	}
               	{
                   (: TODO: better solution that brute force addition of first point :)
                   let $pos := fn:tokenize($x/NS_GML:exterior/NS_GML:LinearRing/NS_GML:pos[1], '\s')
                   return
                       concat(" ", $pos[2], ",",$pos[1])
               }
           		</coordinates>
           	</LinearRing>
           </outerBoundaryIs>
       </Polygon>    
    else
    ()
};

declare function ns:Point($config as node(), $x as node()) as node()* {
    let $pos := fn:tokenize($x/NS_GML:pos, '\s')
    return
		if (fn:count($pos) > 0) then
			<Point>
				<coordinates>{$pos[2]},{$pos[1]}</coordinates>
			</Point>
		else 
		()

};

declare function ns:CircleByCenterPoint($config as node(), $x as node()) as node()* {
    let $pos := fn:tokenize($x/NS_GML:pos, '\s')
    return
    if (fn:count($pos) > 0) then
        <Point>
            <coordinates>{$pos[2]},{$pos[1]}</coordinates>
        </Point>
    else 
    ()
};

declare function ns:Envelope($config as node(), $x as node()) as node()* {
    let $pos1 := fn:tokenize($x/NS_GML:lowerCorner/text(), '\s')
    let $pos2 := fn:tokenize($x/NS_GML:upperCorner/text(), '\s')
    let $posList := fn:concat($pos1[2], ',', $pos1[1], ' ', $pos2[2], ',', $pos1[1], ' ', $pos2[2], ',', $pos2[1], ' ', $pos1[2], ',', $pos2[1], ' ', $pos1[2], ',', $pos1[1])
    return
        if ($posList) then
            <Polygon>
                 <outerBoundaryIs>
                     <LinearRing>
                         <coordinates>{$posList}</coordinates>
                     </LinearRing>
                 </outerBoundaryIs>
            </Polygon> 
         else
            ()
};

declare function ns:LineString($config as node(), $x as node()) as node()* {
    let $posList := $x/NS_GML:pos
    return
    if ($posList) then
      <LineString>
          	<coordinates>
          	{
          		for $pt in $posList
              		let $pos := fn:tokenize($pt, '\s')
              		return
              			string-join( ($pos[2],$pos[1] ), ",")
              }
              </coordinates>
      </LineString>
    else
    ()
};


(:
 :  Continue recursion for unknown elements...
 :)

declare function ns:passthruGeo($config as node(), $x as node()) as node()* {
    for $z in $x/node() return ns:dispatchGeo($config, $z)
};
