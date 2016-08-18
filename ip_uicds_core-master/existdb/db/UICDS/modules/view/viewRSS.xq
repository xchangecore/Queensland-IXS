module namespace rss-v = "http://uicds.org/modules/search/view/rss";

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
declare namespace georss = "http://www.georss.org/georss";
declare namespace gml = "http://www.opengis.net/gml";

declare function rss-v:view($props as node(),$workproducts as node()*) as item() {
    let $opt := util:declare-option("exist:serialize","method=xml media-type=application/xml omit-xml-declaration=no indent=yes")
    
    let $baseURL := props:get-property(doc("/db/UICDS/config.xml")/c:config/p:props,"baseURL","http://localhost")
    
    let $totalResults := count($workproducts)
    let $startIndex := xs:double(props:get-property($props, "startIndex", "1")[1])
    let $count := props:get-property($props, "count", xs:string($totalResults))[1]
    let $workproducts := subsequence($workproducts, $startIndex, number($count) )
    let $itemsPerPage := count($workproducts)
    return
    	<rss version="2.0"
    	           xmlns:georss="http://www.georss.org/georss"
    	           xmlns:gml="http://www.opengis.net/gml" >
    	<channel>
    	<title>UICDS GeoRSS Feed</title>
    		<opensearch:totalResults xmlns:opensearch="http://a9.com/-/spec/opensearch/1.1/" >{$totalResults}</opensearch:totalResults>
    		<opensearch:startIndex xmlns:opensearch="http://a9.com/-/spec/opensearch/1.1/" >{$startIndex}</opensearch:startIndex>
    		<opensearch:itemsPerPage xmlns:opensearch="http://a9.com/-/spec/opensearch/1.1/" >{$itemsPerPage}</opensearch:itemsPerPage>
    	<description>UICDS Results Feed, GeoRSS Format</description>
    	<link>{$baseURL}/uicds</link>
    	{
    	for $workproduct in $workproducts
         	let $productID := fn:data($workproduct/NS_ULEX_STRUCTURE:PackageMetadata[1]/NS_PRECIS_STRUCTURES:WorkProductIdentification/NS_PRECIS_BASE:Identifier)
           	let $ver := fn:data($workproduct/NS_ULEX_STRUCTURE:PackageMetadata[1]/NS_PRECIS_STRUCTURES:WorkProductIdentification/NS_PRECIS_BASE:Version)
           	let $type := fn:data($workproduct/NS_ULEX_STRUCTURE:PackageMetadata[1]/NS_PRECIS_STRUCTURES:WorkProductIdentification/NS_PRECIS_BASE:Type)
           	let $state := fn:data($workproduct/NS_ULEX_STRUCTURE:PackageMetadata[1]/NS_PRECIS_STRUCTURES:WorkProductIdentification/NS_PRECIS_BASE:State)
           	
           	let $identifier := functx:if-empty(fn:data($workproduct/NS_UCORE:Digest/NS_UCORE:Event[1]/NS_UCORE:Identifier[1]), "no identifier available")
           	let $what := functx:if-empty(fn:data($workproduct/NS_UCORE:Digest/NS_UCORE:Event[1]/NS_UCORE:What[1]/@NS_UCORE:code), "unknown")
           	let $descriptor := functx:if-empty(fn:data($workproduct/NS_UCORE:Digest/NS_UCORE:Event[1]/NS_UCORE:Descriptor[1]), "no description available")
       	    let $createdBy := functx:if-empty(fn:data($workproduct/NS_ULEX_STRUCTURE:PackageMetadata[1]/NS_PRECIS_STRUCTURES:WorkProductProperties/NS_PRECIS_BASE:CreatedBy), "unknown")
           	let $created := functx:if-empty(fn:data($workproduct/NS_ULEX_STRUCTURE:PackageMetadata[1]/NS_PRECIS_STRUCTURES:WorkProductProperties/NS_PRECIS_BASE:Created), "unknown")
           	let $lastUpdatedBy := functx:if-empty(fn:data($workproduct/NS_ULEX_STRUCTURE:PackageMetadata[1]/NS_PRECIS_STRUCTURES:WorkProductProperties/NS_PRECIS_BASE:LastUpdatedBy), "unknown")
           	let $lastUpdated := functx:if-empty(fn:data($workproduct/NS_ULEX_STRUCTURE:PackageMetadata[1]/NS_PRECIS_STRUCTURES:WorkProductProperties/NS_PRECIS_BASE:LastUpdated), "unknown")
           	let $interestGroup := functx:if-empty(fn:data($workproduct/NS_ULEX_STRUCTURE:PackageMetadata[1]/NS_PRECIS_STRUCTURES:WorkProductProperties[1]/NS_PRECIS_BASE:AssociatedGroups[1]/NS_PRECIS_BASE:Identifier[1]), "no interest group")
           	
        	let $centroid := fn:tokenize( geom:getCentroid( wkt:gmlToWKT($workproduct/NS_UCORE:Digest[1]//NS_UCORE:GeoLocation) ), " " )
       
            let $location := $workproduct/NS_UCORE:Digest[1]/NS_UCORE:Location[1]/NS_UCORE:GeoLocation[1]
       
           return
        		<item>
        			<guid isPermaLink="false">{$productID}</guid>
        			<title>{$identifier}</title>
        			<description>{$descriptor}</description>
        			<category>{$what}</category>
        			{
        			     if ($workproduct/NS_ULEX_STRUCTURE:PackageMetadata/NS_PRECIS_STRUCTURES:WorkProductProperties/NS_PRECIS_BASE:Created) then
                                <pubDate>{rss-v:rssDateFormat($workproduct/NS_ULEX_STRUCTURE:PackageMetadata/NS_PRECIS_STRUCTURES:WorkProductProperties/NS_PRECIS_BASE:Created)}</pubDate>
        			     else
        			         ()
                    }
        			<link>{$baseURL}/uicds/pub/search?productID={$workproduct//NS_PRECIS_STRUCTURES:WorkProductIdentification/NS_PRECIS_BASE:Identifier/text()}&amp;productVersion={$workproduct//NS_PRECIS_STRUCTURES:WorkProductIdentification/NS_PRECIS_BASE:Version/text()}&amp;format=xml</link>

                    { if (not(fn:empty($location))) then
                        rss-v:genGeometry($location)
                       else 
                        ()
                    }
                        
        		</item>
        	}
    	</channel>
    	</rss>
};

declare function rss-v:genGeometry($location) as node() {
        if ($location/NS_UCORE:Polygon) then
            <georss:where>
                <gml:Polygon>
                    <gml:exterior>
                        <gml:LinearRing>
                            <gml:posList> {
            					for $pt in $location//NS_GML:Polygon/NS_GML:exterior/NS_GML:LinearRing/NS_GML:pos
                					let $pos := fn:tokenize($pt, '\s')
                					return
                						string-join( (fn:item-at($pos,1),fn:item-at($pos,2) ), " ")
                					}
            				</gml:posList>
                        </gml:LinearRing>
                    </gml:exterior>
                </gml:Polygon>
            </georss:where>
        else if ($location/NS_UCORE:Point) then
    		let $pos := fn:tokenize($location//NS_GML:Point/NS_GML:pos, '\s')
(:          let $pos := $location//NS_GML:Point/NS_GML:pos  :)
            return
               <georss:where>            
       		       <gml:Point>
       		           <gml:pos>{string-join( (fn:item-at($pos,1),fn:item-at($pos,2) ), " ")}</gml:pos>
       		       </gml:Point>
       		   </georss:where>
        else if ($location/NS_UCORE:CircleByCenterPoint) then
    		let $pos := fn:tokenize($location//NS_GML:CircleByCenterPoint/NS_GML:pos, '\s')
(:    		let $pos := fn:tokenize($location//NS_GML:Point/NS_GML:pos, '\s') :)
            return
               <georss:where>            
       		       <gml:Point>
       		           <gml:pos>{string-join( (fn:item-at($pos,1),fn:item-at($pos,2) ), " ")}</gml:pos>
       		       </gml:Point>
       		   </georss:where>
        else if ($location/NS_UCORE:LineString) then
    	    <georss:where>
                <gml:LineString>
                    <gml:posList> {
        					for $pt in $location//NS_GML:LineString/NS_GML:pos
            					let $pos := fn:tokenize($pt, '\s')
            					return
            						string-join( (fn:item-at($pos,1),fn:item-at($pos,2) ), " ")
            					}
        			</gml:posList>
                </gml:LineString>
            </georss:where>
        else
            ()

};

declare function rss-v:rssDateFormat( $dateTime as xs:anyAtomicType ) as xs:string {
        let $day := string(fn:day-from-dateTime($dateTime))
        let $month := string(functx:month-abbrev-en($dateTime))
        let $year := string(fn:year-from-dateTime($dateTime))
        let $time := string-join( ( functx:pad-integer-to-length(string(fn:hours-from-dateTime($dateTime)),2), functx:pad-integer-to-length(string(fn:minutes-from-dateTime($dateTime)),2), functx:pad-integer-to-length( substring-before( string(fn:seconds-from-dateTime($dateTime)),"."), 2)), ":")
        let $offset := fn:replace(functx:timezone-from-duration(fn:timezone-from-dateTime($dateTime)),":","")
        return
            string-join( ( $day, $month, $year, $time, $offset  ), ' ')
};

