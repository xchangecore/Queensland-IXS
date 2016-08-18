xquery version "1.0";
module namespace ns='http://uicds.org/modules/transform/rss';

import module namespace functx = 'http://www.functx.com' at '../../common/functx.xq';
import module namespace props = 'http://uicds.org/modules/util/properties' at '../../common/properties.xq';

declare namespace NS_PROPS = "util:properties";
declare namespace NS_PRECIS_STRUCTURES = "http://www.saic.com/precis/2009/06/structures";
declare namespace NS_PRECIS_BASE = "http://www.saic.com/precis/2009/06/base";
declare namespace NS_UCORE = "http://ucore.gov/ucore/2.0";
declare namespace NS_ULEX = "ulex:message:structure:1.0";
declare namespace NS_GML = "http://www.opengis.net/gml/3.2";

declare namespace georss = "http://www.georss.org/georss";
declare namespace gml = "http://www.opengis.net/gml";

(:
 :  This is the entry point
 :)
declare function ns:render($config as node(),  $parameters as node(), $workproducts as node()) as node()* {
    let $totalResults := fn:count($workproducts//NS_PRECIS_STRUCTURES:WorkProduct)
    let $startIndex := xs:double(props:get-property($parameters, "startIndex", "1")[1])
    let $itemsPerPage := $totalResults
    return (
        <rss version='2.0' 
                xmlns:georss='http://www.georss.org/georss' 
                xmlns:gml='http://www.opengis.net/gml'>
            <channel>
            <title>UICDS geoRSS Feed</title>
            <link>{fn:data($config//NS_PROPS:prop[@name='baseURL']/@value)}</link>

            <opensearch:totalResults xmlns:opensearch="http://a9.com/-/spec/opensearch/1.1/" >{$totalResults}</opensearch:totalResults>
            <opensearch:startIndex xmlns:opensearch="http://a9.com/-/spec/opensearch/1.1/" >{$startIndex}</opensearch:startIndex>
        	<opensearch:itemsPerPage xmlns:opensearch="http://a9.com/-/spec/opensearch/1.1/" >{$itemsPerPage}</opensearch:itemsPerPage>            

            <description>UICDS Geo-Evemt Feed, geoRSS Format (with GML)</description>
            
                {
                    ns:dispatch($config, $workproducts)
                }
                
            </channel>
        </rss>
    )
};

(:
 :  Recursive processor for Entity and Event types...
 :)
declare function ns:dispatch($config as node(), $x as node()) as node()* {  
typeswitch ($x)

    (: Generate an RSS Item for each Entity :)
    case element (NS_UCORE:Entity) return ns:itemFromEntity($config, $x)
    case element (NS_UCORE:Organization) return ns:itemFromEntity($config, $x)
    case element (NS_UCORE:Person) return ns:itemFromEntity($config, $x)
    case element (NS_UCORE:Event) return ns:itemFromEntity($config, $x)

    (: Default Transformer (passthru) :)
    default return ns:passthru($config, $x)
};

(:
 :  Recursive processor for location (address / geospatial) types
 :)
declare function ns:dispatchGeo($config as node(), $x as node()) as node()* {  
typeswitch ($x)

    (: Geospatial Location Processing (GML) :)
    case element (NS_GML:CircleByCenterPoint) return ns:generate-where-CircleByCenterPoint($config, $x)
    case element (NS_GML:Polygon) return ns:generate-where-Polygon($config, $x)
    case element (NS_GML:Point) return ns:generate-where-Point($config, $x)
    case element (NS_GML:Envelope) return ns:generate-where-Envelope($config, $x)
    case element (NS_GML:LineString) return ns:generate-where-LineString($config, $x)
    case element (NS_GML:exterior) return ns:generate-exterior($config, $x)
    case element (NS_GML:LinearRing) return ns:generate-LinearRing($config, $x)
    
    (: Cyber Address Processing (DDMS) :)
    
    (: Physical Address Processing (DDMS) :)
    
    (: Default Transformer (passthru) :)
    default return ns:passthruGeo($config, $x)
};

declare function ns:itemFromEntity($config as node(), $x as node()) as node()* {
    <item>
        { ns:generate-guid($config, $x) }
        { ns:generate-title($config, $x) }
        { ns:generate-description($config, $x) }
        { ns:generate-category($config, $x) }
        { ns:generate-pubdate($config, $x) }
        { ns:generate-link($config, $x) }
        {
            typeswitch($x)
                case element (NS_UCORE:Event) return ns:generate-EventLocation($config, $x)
                default return ns:generate-EntityLocation($config, $x)
        }
    </item>
};


declare function ns:generate-guid($config as node(), $x as node()) as node()* {
    let $base := $x/ancestor::NS_PRECIS_STRUCTURES:WorkProduct//NS_PRECIS_STRUCTURES:WorkProductIdentification/NS_PRECIS_BASE:Identifier/text()
    let $guid := fn:concat($base, "#", fn:data($x/@id) )
    return
        <guid isPermaLink='false'>{$guid}</guid>
};

declare function ns:generate-title($config as node(), $x as node()) as node()* {
    (:  TODO:  change to use only the "pretty name" identifier :)
    let $title := functx:if-empty(fn:data($x/NS_UCORE:Identifier[1]), "no title given")
    return
        <title>{$title}</title>
};

declare function ns:generate-description($config as node(), $x as node()) as node()* {
    let $title := functx:if-empty(fn:data($x/NS_UCORE:Descriptor[1]), "no title given")
    return
        <description>{$title}</description>
};


declare function ns:generate-category($config as node(), $x as node()) as node()* {
    let $categories := fn:data($x/NS_UCORE:What/@NS_UCORE:code)
            for $category in $categories
            return
                <category>{$category}</category>
};


declare function ns:generate-pubdate($config as node(), $x as node()) as node()* {
    let $pubdate := ns:convertToRSSDate($x/ancestor::NS_PRECIS_STRUCTURES:WorkProduct//NS_PRECIS_STRUCTURES:WorkProductProperties/NS_PRECIS_BASE:LastUpdated/text())
    return
        if ($pubdate) then 
            <pubDate>{$pubdate}</pubDate>
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

declare function ns:generate-EventLocation($config as node(), $x as node()) as node()* {
    let $occursAtRefs := $x/parent::NS_UCORE:Digest/NS_UCORE:OccursAt//NS_UCORE:LocationRef[../NS_UCORE:EventRef/@ref=fn:data($x/@id)]
    return
        if ($occursAtRefs) then
            for $locationRef in $occursAtRefs
            return
                for $z in $x/parent::NS_UCORE:Digest//NS_UCORE:Location[@id=fn:data($locationRef/@ref)]
                    return ns:dispatchGeo($config, $z)
        else
            ()
};

declare function ns:generate-EntityLocation($config as node(), $x as node()) as node()* {
    let $locatedAtRefs := $x/parent::NS_UCORE:Digest/NS_UCORE:LocatedAt//NS_UCORE:LocationRef[../NS_UCORE:EntityRef/@ref=fn:data($x/@id)]
    return
        if ($locatedAtRefs) then
            for $locationRef in $locatedAtRefs
            return
                for $z in $x/parent::NS_UCORE:Digest//NS_UCORE:Location[@id=fn:data($locationRef/@ref)]
                    return ns:dispatchGeo($config, $z)
        else
            ()
};

declare function ns:generate-where-Polygon($config as node(), $x as node()) as node()* {
    <georss:where>
        <gml:Polygon>
            {ns:passthruGeo($config, $x)}
        </gml:Polygon>
    </georss:where>
};

declare function ns:generate-exterior($config as node(), $x as node()) as node()* {
    <gml:exterior>
        {ns:passthruGeo($config, $x)}
    </gml:exterior>
};

declare function ns:generate-LinearRing($config as node(), $x as node()) as node()* {
    <gml:LinearRing>
        {ns:generate-posList($config, $x)}
    </gml:LinearRing>           
};

declare function ns:generate-posList($config as node(), $x as node()) as node()* {
        <gml:posList> 
            {
            for $pt in $x/NS_GML:pos
                let $pos := fn:tokenize($pt, '\s')
                return
                    string-join( ($pos[1],$pos[2]) , " ")
            }
            {
                (: TODO: better solution that brute force addition of first point :)
                let $pos := fn:tokenize($x/NS_GML:pos[1], '\s')
                return
                    if ($pos and $x/parent::LinearRing) then
                        concat(" ", $pos[1], " ",$pos[2])
                    else 
                        ()
            }
        </gml:posList>
};

declare function ns:generate-where-Point($config as node(), $x as node()) as node()* {
    let $pos := $x/NS_GML:pos/text()
    return
        if ($pos) then
            <georss:where>            
                <gml:Point>
                    <gml:pos>{$pos}</gml:pos>
                </gml:Point>
            </georss:where>
        else
            ()
};

declare function ns:generate-where-CircleByCenterPoint($config as node(), $x as node()) as node()* {
    let $pos := $x/NS_GML:pos/text()
    return
        if ($pos) then
            <georss:where>            
                <gml:Point>
                    <gml:pos>{$pos}</gml:pos>
                </gml:Point>
            </georss:where>
        else
            ()
};

declare function ns:generate-where-Envelope($config as node(), $x as node()) as node()* {
    let $pos1 := fn:tokenize($x/NS_GML:lowerCorner/text(), '\s')
    let $pos2 := fn:tokenize($x/NS_GML:upperCorner/text(), '\s')
    let $posList := fn:string-join(($pos1[1], $pos1[2], $pos2[1], $pos1[2], $pos2[1], $pos2[2], $pos1[1], $pos2[2], $pos1[1], $pos1[2]), ' ')
    return
        if ($posList) then
            <georss:where>
                <gml:Polygon>
                   <gml:exterior>
                      <gml:LinearRing>
                         <gml:posList>{$posList}</gml:posList>
                      </gml:LinearRing>
                   </gml:exterior>
                </gml:Polygon>
            </georss:where>
         else
            ()
};

declare function ns:generate-where-LineString($config as node(), $x as node()) as node()* {
    <georss:where>
        <gml:LineString>
            {ns:generate-posList($config, $x)}
        </gml:LineString>
    </georss:where>
};

(:
 :  Continue recursion for unknown elements...
 :)
declare function ns:passthru($config as node(), $x as node()) as node()* {
    for $z in $x/node() return ns:dispatch($config, $z)
};

declare function ns:passthruGeo($config as node(), $x as node()) as node()* {
    for $z in $x/node() return ns:dispatchGeo($config, $z)
};

(:
 :  ------------------------------------------------------
 :   support functions (maybe move to external file?)
 :  ------------------------------------------------------
 :)

declare function ns:convertToRSSDate( $dateTime as xs:anyAtomicType ) as xs:string {
        let $day := string(fn:day-from-dateTime($dateTime))
        let $month := string(functx:month-abbrev-en($dateTime))
        let $year := string(fn:year-from-dateTime($dateTime))
        let $time := string-join( ( functx:pad-integer-to-length(string(fn:hours-from-dateTime($dateTime)),2), functx:pad-integer-to-length(string(fn:minutes-from-dateTime($dateTime)),2), functx:pad-integer-to-length( substring-before( string(fn:seconds-from-dateTime($dateTime)),"."), 2)), ":")
        let $offset := fn:replace(functx:timezone-from-duration(fn:timezone-from-dateTime($dateTime)),":","")
        return
            string-join( ( $day, $month, $year, $time, $offset  ), ' ')
};
