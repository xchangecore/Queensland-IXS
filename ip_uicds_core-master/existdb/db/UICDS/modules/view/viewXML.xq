module namespace xml-v = "http://uicds.org/modules/search/view/xml";

import module namespace props = "http://uicds.org/modules/util/properties"
at "xmldb:exist:///db/UICDS/modules/util/properties.xq";

declare namespace c = "util:config";
declare namespace p = "util:properties";

declare namespace NS_PRECIS_STRUCTURES = "http://www.saic.com/precis/2009/06/structures";
declare namespace NS_PRECIS_BASE = "http://www.saic.com/precis/2009/06/base";
declare namespace NS_ULEX_STRUCTURE = "ulex:message:structure:1.0";
declare namespace NS_UCORE = "http://ucore.gov/ucore/2.0";
declare namespace NS_GML = "http://www.opengis.net/gml/3.2";

declare function xml-v:view($props as node(), $workproducts as node()*) as item() {
    let $opt := util:declare-option("exist:serialize","method=xml media-type=application/xml omit-xml-declaration=no indent=yes")
    let $totalResults := count($workproducts)
    let $startIndex := xs:double(props:get-property($props, "startIndex", "1")[1])
    let $count := props:get-property($props, "count", xs:string($totalResults) )[1]
    let $workproducts := subsequence($workproducts, $startIndex, number($count) )
    let $itemsPerPage := count($workproducts)
    return
        <workproducts>
    		<opensearch:totalResults xmlns:opensearch="http://a9.com/-/spec/opensearch/1.1/" >{$totalResults}</opensearch:totalResults>
    		<opensearch:startIndex xmlns:opensearch="http://a9.com/-/spec/opensearch/1.1/" >{$startIndex}</opensearch:startIndex>
    		<opensearch:itemsPerPage xmlns:opensearch="http://a9.com/-/spec/opensearch/1.1/" >{$itemsPerPage}</opensearch:itemsPerPage>
    		{
    			for $workproduct in $workproducts
    			return
    				$workproduct
    		}
        </workproducts>
};
