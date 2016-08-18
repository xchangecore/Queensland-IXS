(:
 :  This is the primary entry point for UICDS queries.
 :  The dispatch method expects a <params xmlns="util:parameter" /> object
 :  which is passed to the queryBuilder to generate the xquery search conditions.s
 :)

module namespace search = "http://uicds.org/modules/search";

import module namespace v="http://exist-db.org/versioning";

import module namespace geom = "http://exist-db.org/xquery/geom";

import module namespace wkt = "http://uicds.org/modules/util/wkt"
at "xmldb:exist:///db/UICDS/modules/util/wkt.xq";

import module namespace props = "http://uicds.org/modules/util/properties"
at "xmldb:exist:///db/UICDS/modules/util/properties.xq";

import module namespace functx = "http://www.functx.com"
at "xmldb:exist:///db/UICDS/modules/util/functx.xq";

import module namespace wp = "http://uicds.org/modules/util/workproduct"
at "xmldb:exist:///db/UICDS/modules/util/workproduct.xq";

import module namespace xml-v = "http://uicds.org/modules/search/view/xml"
at "xmldb:exist:///db/UICDS/modules/view/viewXML.xq";

import module namespace rss-v = "http://uicds.org/modules/search/view/rss"
at "xmldb:exist:///db/UICDS/modules/view/viewRSS.xq";

import module namespace w3crss-v = "http://uicds.org/modules/search/view/w3crss"
at "xmldb:exist:///db/UICDS/modules/view/viewW3CRSS.xq";

import module namespace kml-v = "http://uicds.org/modules/search/view/kml"
at "xmldb:exist:///db/UICDS/modules/view/viewKML.xq";

declare namespace util="http://exist-db.org/xquery/util";
declare namespace NS_PRECIS_STRUCTURES = "http://www.saic.com/precis/2009/06/structures";
declare namespace NS_PRECIS_BASE = "http://www.saic.com/precis/2009/06/base";
declare namespace NS_ULEX_STRUCTURE = "ulex:message:structure:1.0";
declare namespace NS_UCORE = "http://ucore.gov/ucore/2.0";
declare namespace NS_ULEX = "ulex:message:structure:1.0";
declare namespace NS_GML = "http://www.opengis.net/gml/3.2";

declare function search:search($props as node()) as node() {
    
    if (props:get-property($props, "format", "")[1]="debug") then
        <debug>
        	{$props}
        	<query>
        		{search:buildQuery($props)}
        	</query>
        		{ 
        			let $start := util:system-time()
        			let $allResults := util:eval(search:buildQuery($props))
        			let $workproducts := xml-v:view($props, $allResults)
        
                    let $totalResults := count($allResults)
                    let $startIndex := xs:double(props:get-property($props, "startIndex", "1")[1])
                    let $count := props:get-property($props, "count", xs:string($totalResults) )[1]
                    let $itemsPerPage := count($workproducts/NS_PRECIS_STRUCTURES:WorkProduct)
        
        			return
        				<results>
        					<info>Returned {$itemsPerPage} items of {$totalResults} items in {util:system-time() - $start}.  Requested {$count} items beginning at item index {$startIndex}.</info>
        					{$workproducts}
        				</results>
        		}
        </debug>

    else if (props:get-property($props, "format", "")[1]="xml") then
    	xml-v:view($props, util:eval(search:buildQuery($props)))
    else if (props:get-property($props, "format", "")[1]="rss") then
    	rss-v:view($props, util:eval(search:buildQuery($props)))
    else if (props:get-property($props, "format", "")[1]="w3crss") then
    	w3crss-v:view($props, util:eval(search:buildQuery($props)))	
    else if (props:get-property($props, "format", "")[1]="kml") then
    	kml-v:view($props, util:eval(search:buildQuery($props)))	
    else if (props:get-property($props, "format", "")[1]="html") then
        
        <html>
            <style type="text/css">
                .container {{width: 90%}}
                table .xml-table {{border: 1px solid black; border-collapse:collapse; width: 100%}}
                .text {{color: black; font-style:normal}}
                .element-name {{color: black; font-style:normal; text-align: left}}
                .element-value {{font-style:normal; color: black; text-align: left}}
                .attribute-name {{color: gray; font-style:italic; text-align: left}}
                .attribute-value {{font-style:italic; color: gray; text-align: left}}
                .container:nth-child(odd) {{background: #FFFFF0}}
                .container:nth-child(even) {{background: #FAF0E6}}
            </style>
        <body>
            <div>
                Search results...
            </div>
            {
                for $wp in util:eval(search:buildQuery($props))//NS_PRECIS_STRUCTURES:WorkProduct
                    return
                        <div class="container">
                        <h3 style="text-decoration:underline">Work Product: {$wp/NS_ULEX_STRUCTURE:PackageMetadata/NS_PRECIS_STRUCTURES:WorkProductIdentification/NS_PRECIS_BASE:Identifier/text()}</h3>
                            {wp:xml-to-html-table($wp)}
                        </div>
            }
    	</body>
    	</html>
    else
    	<error>An unknown or invalid format was specfied.</error>	
};

declare function search:getReturnExpression($props as node()) as xs:string {
       if (props:get-property($props, "full", "")="true") then
            "$workproduct"
		else
            "<WorkProduct xmlns='http://www.saic.com/precis/2009/06/structures'>
                {$workproduct/@*}
                {$workproduct/* except $workproduct/NS_ULEX:StructuredPayload}
                </WorkProduct>"
};

declare function search:buildQuery($props as node()) as xs:string {
    
        let $id := props:get-property($props, "productID", "")
        let $reqVersion := props:get-property($props, "productVersion", "")
        
        return
        (: if there is a version number and product ID :)
        (: if a specific version number is requested, all other parameters are ignored :)
        if ( ($id) and ($reqVersion) ) then
            string-join((
                "let $documentURI := fn:base-uri ( /NS_PRECIS_STRUCTURES:WorkProduct[NS_ULEX_STRUCTURE:PackageMetadata/NS_PRECIS_STRUCTURES:WorkProductIdentification/NS_PRECIS_BASE:Identifier='", $id, "'] )",
                "let $vCollection := '/db/system/versions'",
                " return
                if ($documentURI) then 
                    let $revision := collection($vCollection)//v:version/v:properties[v:version='", $reqVersion, "' and v:document='", $id, "'][1]/v:revision
                    return
                    if ($revision) then
                        let $workproduct := v:doc(doc($documentURI), $revision)
            			return ",
            			search:getReturnExpression($props),
            		" else
            			let $documentURI := fn:base-uri ( /NS_PRECIS_STRUCTURES:WorkProduct[NS_ULEX_STRUCTURE:PackageMetadata/NS_PRECIS_STRUCTURES:WorkProductIdentification/NS_PRECIS_BASE:Identifier='", $id, "' and NS_ULEX_STRUCTURE:PackageMetadata/NS_PRECIS_STRUCTURES:WorkProductIdentification/NS_PRECIS_BASE:Version='", $reqVersion, "'] )",
            			"return 
            			if ($documentURI) then
            				let $workproduct := fn:doc($documentURI)
            				return ",
                                search:getReturnExpression($props),
            			" else
                            <error>Requested version not available</error>
                else
                    <error>Requested document not available</error>
                "), "")

        (: there is a version number or product id or neither :)
         else
        	string-join((
        		"for $workproduct in /NS_PRECIS_STRUCTURES:WorkProduct ",
                search:buildSimpleConditions($props, "interestGroup", "NS_ULEX:PackageMetadata/NS_PRECIS_STRUCTURES:WorkProductProperties/NS_PRECIS_BASE:AssociatedGroups/NS_PRECIS_BASE:Identifier", ""),
                search:buildSimpleConditions($props, "productID", "NS_ULEX:PackageMetadata/NS_PRECIS_STRUCTURES:WorkProductIdentification/NS_PRECIS_BASE:Identifier", ""),
        		search:buildSimpleConditions($props, "productType", "NS_ULEX:PackageMetadata/NS_PRECIS_STRUCTURES:WorkProductIdentification/NS_PRECIS_BASE:Type", ""),
        		search:buildSimpleConditions($props, "productState", "NS_ULEX:PackageMetadata/NS_PRECIS_STRUCTURES:WorkProductIdentification/NS_PRECIS_BASE:State", "Active"),
        		search:buildTemporalStartCondition($props, "createdBegin", "NS_ULEX:PackageMetadata/NS_PRECIS_STRUCTURES:WorkProductProperties/NS_PRECIS_BASE:Created", ""),
        		search:buildTemporalStartCondition($props, "updatedBegin", "NS_ULEX:PackageMetadata/NS_PRECIS_STRUCTURES:WorkProductProperties/NS_PRECIS_BASE:LastUpdated", ""),
        		search:buildTemporalEndCondition($props, "createdEnd", "NS_ULEX:PackageMetadata/NS_PRECIS_STRUCTURES:WorkProductProperties//NS_PRECIS_BASE:Created", ""),
        		search:buildTemporalEndCondition($props, "updatedEnd", "NS_ULEX:PackageMetadata/NS_PRECIS_STRUCTURES:WorkProductProperties/NS_PRECIS_BASE:LastUpdated", ""),
        		search:buildSimpleConditions($props, "createdBy", "NS_ULEX:PackageMetadata/NS_PRECIS_STRUCTURES:WorkProductProperties/NS_PRECIS_BASE:CreatedBy", ""),
        		search:buildSimpleConditions($props, "updatedBy", "NS_ULEX:PackageMetadata/NS_PRECIS_STRUCTURES:WorkProductProperties/NS_PRECIS_BASE:LastUpdatedBy", ""),
        		search:buildSimpleConditions($props, "what", "NS_UCORE:Digest//NS_UCORE:What/@NS_UCORE:code", ""),
        		search:buildSimpleConditions($props, "mimeType", "NS_ULEX:PackageMetadata/NS_PRECIS_STRUCTURES:WorkProductProperties/NS_PRECIS_BASE:MimeType", ""),
        		search:buildSpatialConditions($props, "bbox"),  		
                "return ",
                    search:getReturnExpression($props)
                ), " ")
};

declare function search:buildSimpleConditions($props as node(), $property-name as xs:string, $xpath as xs:string, $default as xs:string) as xs:string {
    let $terms := ""
    return
    if (props:property-names-contains($props, $property-name)) then
    	string-join(( "[",
    		substring(string-join(( $terms,
    			for $val in props:get-property($props, $property-name, "")
    				return
    					<f>{$xpath}='{$val}'</f>
    		), " or ") ,5),
    		"]"
    	), "")
    else if ( $default ) then
        <f>[{$xpath}='{$default}']</f>
    else
    	<f>{()}</f>
};

declare function search:buildTemporalStartCondition($props as node(), $property-name as xs:string, $xpath as xs:string, $default as xs:string) as xs:string {
    if (props:property-names-contains($props, $property-name)) then
    	let $val := props:get-property($props, $property-name, "")[1]
    		return
    			<f>[xs:dateTime({$xpath}) ge xs:dateTime('{$val}')]</f>
    else
    	<f>{()}</f>
};

declare function search:buildTemporalEndCondition($props as node(), $property-name as xs:string, $xpath as xs:string, $default as xs:string) as xs:string {
    if (props:property-names-contains($props, $property-name)) then
    	let $val := props:get-property($props, $property-name, "")[1]
    		return
    			<f>[xs:dateTime({$xpath}) le xs:dateTime('{$val}')]</f>
    else
    	<f>{()}</f>
};

declare function search:buildSpatialConditions($props as node(), $property-name as xs:string) as xs:string {
    let $terms := ""
    return
    if (props:property-names-contains($props, $property-name)) then
    	string-join(( "[",
    		substring(string-join(( $terms,
    			for $val in props:get-property($props, "bbox", "")
    				return
    					<f>geom:intersects(geom:bboxToWKT('{$val}'),wkt:gmlToWKT(NS_UCORE:Digest//NS_UCORE:GeoLocation) )</f>
    		), " or ") ,5),
    		"]"
    	), "")
    else
    	<f>{()}</f>
};

