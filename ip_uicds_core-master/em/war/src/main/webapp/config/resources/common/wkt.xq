(:
 :  Utilities for working the Well Known Text (WKT) format for simple features
 :  More information on this format can be found at the following lcoation
 :    http://en.wikipedia.org/wiki/Well-known_text
 :)
 
module namespace wkt = "http://uicds.org/modules/util/wkt";

declare namespace NS_GML = "http://www.opengis.net/gml/3.2";

declare function wkt:gmlToWKT($geometries as node()*) as xs:string {
    let $geoms := ""
    return
        string-join(( "GEOMETRYCOLLECTION ( ",
            substring( string-join(( $geoms,
                for $geometry in $geometries
                return
                    if ($geometry/descendant-or-self::NS_GML:CircleByCenterPoint) then
                        wkt:circleToWKT($geometry/descendant-or-self::NS_GML:CircleByCenterPoint[1])
                    else if ($geometry/descendant-or-self::NS_GML:Envelope) then
                        <f>POINT EMPTY</f>
                    else if ($geometry/descendant-or-self::NS_GML:LineString) then
                        wkt:linestringToWKT($geometry/descendant-or-self::NS_GML:LineString[1])
                    else if ($geometry/descendant-or-self::NS_GML:Point) then
                        wkt:pointToWKT($geometry/descendant-or-self::NS_GML:Point[1])
                    else if ($geometry/descendant-or-self::NS_GML:Polygon) then
                        wkt:polygonToWKT($geometry/descendant-or-self::NS_GML:Polygon[1])
                    else
                        <f>POINT EMPTY</f>
            ), ","), 2),
                    
        " )"), "")
};

(: TODO: calculate better alternative for circles; eg: 16 sided polygon 
 : How to handle uom?  :)
declare function wkt:circleToWKT($geometry as node()) as xs:string {
    (: POINT(lat lon ) :)
    let $pos := fn:tokenize($geometry/NS_GML:pos, '\s')
    let $rad := $geometry/NS_GML:radius/text()
    return
        <f>POINT ({string-join((fn:item-at($pos,1), fn:item-at($pos,2))," ")})</f>
};


declare function wkt:pointToWKT($geometry as node()) as xs:string {
    (: POINT(lat lon ) :)
    let $pos := fn:tokenize($geometry/NS_GML:pos, '\s')
    return
        <f>POINT ({string-join((fn:item-at($pos,1), fn:item-at($pos,2))," ")})</f>
};

declare function wkt:linestringToWKT($geometry as node()) as xs:string {
    (: LINESTRING ( (lat1 lon1, lat2 lon2, lat3 lon3, lat4 lon4, lat1 lon1 ) ) :)
    let $pts := ""
    return
    	string-join(( "LINESTRING (",
    		substring(string-join(( $pts,
    			for $pt in $geometry/NS_GML:pos
    			let $pos := fn:tokenize($pt, '\s')
    			return
    				<f>{string-join((fn:item-at($pos,1), fn:item-at($pos,2))," ")}</f>
    		), ","), 2),
    	")"), "")
};

(: TODO: modify to handle inner rings :)
declare function wkt:polygonToWKT($geometry as node()) as xs:string {
    (: POLYGON( (lat1 lon1, lat2 lon2, lat3 lon3, lat4 lon4, lat1 lon1 ) ) :)
    let $pts := ""
    let $posArray := $geometry/NS_GML:exterior/NS_GML:LinearRing/NS_GML:pos
    return
    if ( (fn:count($posArray) > 2) and ($posArray[1] = $posArray[fn:count($posArray)] ) ) then
    	string-join(( "POLYGON ( (",
    		substring(string-join(( $pts,
    			for $pt in $posArray
    			let $pos := fn:tokenize($pt, '\s')
    			return
    				<f>{string-join((fn:item-at($pos,1), fn:item-at($pos,2))," ")}</f>
    		), ","), 2),
    	") )"), "")
    else if (fn:count($posArray) > 1)  then
        string-join(( "LINESTRING (",
    		substring(string-join(( $pts,
    			for $pt in $posArray
    			let $pos := fn:tokenize($pt, '\s')
    			return
    				<f>{string-join((fn:item-at($pos,1), fn:item-at($pos,2))," ")}</f>
    		), ","), 2),
    	")"), "")
    else
        <f>POLYGON EMPTY</f>
};