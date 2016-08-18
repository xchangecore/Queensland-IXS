module namespace w3crss-v = "http://uicds.org/modules/search/view/w3crss";

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

declare function w3crss-v:view($props as node(),$workproducts as node()*) as item() {
    let $opt := util:declare-option("exist:serialize","method=xml media-type=application/xml omit-xml-declaration=no indent=yes")
    
    let $baseURL := props:get-property(doc("/db/UICDS/config.xml")/c:config/p:props,"baseURL","http://localhost")
    
    let $totalResults := count($workproducts)
    let $startIndex := xs:double(props:get-property($props, "startIndex", "1")[1])
    let $count := props:get-property($props, "count", xs:string($totalResults))[1]
    let $workproducts := subsequence($workproducts, $startIndex, number($count) )
    let $itemsPerPage := count($workproducts)
    return
    	<rss version="2.0"  xmlns:geo="http://www.w3.org/2003/01/geo/wgs84_pos#" xmlns:atom="http://www.w3.org/2005/Atom" >
    	<channel>
    	<title>UICDS RSS Feed (W3C Geo)</title>
    		<opensearch:totalResults xmlns:opensearch="http://a9.com/-/spec/opensearch/1.1/" >{$totalResults}</opensearch:totalResults>
    		<opensearch:startIndex xmlns:opensearch="http://a9.com/-/spec/opensearch/1.1/" >{$startIndex}</opensearch:startIndex>
    		<opensearch:itemsPerPage xmlns:opensearch="http://a9.com/-/spec/opensearch/1.1/" >{$itemsPerPage}</opensearch:itemsPerPage>
    	<description>UICDS Results Feed, W3C RSS Format</description>
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
       
           return
        		<item>
        			<guid isPermaLink="false">{$productID}</guid>
        			<title>{$identifier}</title>
        			<description>{$descriptor}</description>
        			<category>{$what}</category>
        			{
        			     if ($workproduct/NS_ULEX_STRUCTURE:PackageMetadata/NS_PRECIS_STRUCTURES:WorkProductProperties/NS_PRECIS_BASE:Created) then
                                <pubDate>{w3crss-v:rssDateFormat($workproduct/NS_ULEX_STRUCTURE:PackageMetadata/NS_PRECIS_STRUCTURES:WorkProductProperties/NS_PRECIS_BASE:Created)}</pubDate>
        			     else
        			         ()
                    }
        			<link>{$baseURL}/uicds/pub/search?productID={$workproduct//NS_PRECIS_STRUCTURES:WorkProductIdentification/NS_PRECIS_BASE:Identifier/text()}&amp;productVersion={$workproduct//NS_PRECIS_STRUCTURES:WorkProductIdentification/NS_PRECIS_BASE:Version/text()}&amp;format=xml</link>

                     {                     
                         if (fn:count($centroid)=2) then
                 			<geo:lat>{fn:item-at($centroid,1)}</geo:lat>
                 		else
                 		 ()
               		 }
               		 {
                 		 if (fn:count($centroid)=2) then
                 			<geo:long>{fn:item-at($centroid,2)}</geo:long>
                 		else
                 		     ()
                 	  }
        		</item>
        	}
    	</channel>
    	</rss>
};

declare function w3crss-v:rssDateFormat( $dateTime as xs:anyAtomicType ) as xs:string {
        let $day := string(fn:day-from-dateTime($dateTime))
        let $month := string(functx:month-abbrev-en($dateTime))
        let $year := string(fn:year-from-dateTime($dateTime))
        let $time := string-join( ( functx:pad-integer-to-length(string(fn:hours-from-dateTime($dateTime)),2), functx:pad-integer-to-length(string(fn:minutes-from-dateTime($dateTime)),2), functx:pad-integer-to-length( substring-before( string(fn:seconds-from-dateTime($dateTime)),"."), 2)), ":")
        let $offset := fn:replace(functx:timezone-from-duration(fn:timezone-from-dateTime($dateTime)),":","")
        return
            string-join( ( $day, $month, $year, $time, $offset  ), ' ')
};
