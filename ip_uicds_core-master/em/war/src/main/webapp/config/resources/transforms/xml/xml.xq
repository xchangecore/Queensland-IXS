xquery version "1.0";
module namespace ns='http://uicds.org/modules/transform/xml';

import module namespace props = 'http://uicds.org/modules/util/properties' at '../../common/properties.xq';

declare namespace NS_PROPS = "util:properties";
declare namespace NS_PRECIS_STRUCTURES = "http://www.saic.com/precis/2009/06/structures";

(:
 :  This is the entry point
 :)
declare function ns:render($config as node(),  $parameters as node(), $workproducts as node()) as node()* {
    let $totalResults := fn:count($workproducts//NS_PRECIS_STRUCTURES:WorkProduct)
    let $startIndex := xs:double(props:get-property($parameters, "startIndex", "1")[1])
    let $itemsPerPage := $totalResults
    return
        if ($totalResults=1) then 
            $workproducts//NS_PRECIS_STRUCTURES:WorkProduct[1]
        else 
        <WorkProductList xmlns='http://www.saic.com/precis/2009/06/structures'>
    		<opensearch:totalResults xmlns:opensearch="http://a9.com/-/spec/opensearch/1.1/" >{$totalResults}</opensearch:totalResults>
    		<opensearch:startIndex xmlns:opensearch="http://a9.com/-/spec/opensearch/1.1/" >{$startIndex}</opensearch:startIndex>
    		<opensearch:itemsPerPage xmlns:opensearch="http://a9.com/-/spec/opensearch/1.1/" >{$itemsPerPage}</opensearch:itemsPerPage>
    		{
    			for $workproduct in $workproducts//NS_PRECIS_STRUCTURES:WorkProduct
    			return
    				$workproduct
    		}
        </WorkProductList>
};