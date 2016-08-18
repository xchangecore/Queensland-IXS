(:
 :  This is the primary entry point for UICDS queries.
 :  The dispatch method expects a <props xmlns="util:properties" /> object
 :  which is passed to the queryBuilder to generate the xquery search conditions.s
 :)

module namespace dispatch = "http://uicds.org/modules/dispatch";

(:
 : import statements for modules
 :)
import module namespace props = "http://uicds.org/modules/util/properties"
at "xmldb:exist:///db/UICDS/modules/util/properties.xq";

import module namespace search = "http://uicds.org/modules/search" 
at "xmldb:exist:///db/UICDS/modules/search.xq";

(:
 : namespace declarations
 :)
declare namespace util="http://exist-db.org/xquery/util";
declare namespace NS_PRECIS_STRUCTURES = "http://www.saic.com/precis/2009/06/structures";

(:
 : function declarations 
 :)
declare function dispatch:dispatch($props as node()) as item() {

    let $resourcePath := props:get-property($props, "req.resourcePath", "")[1] 

    return
      if ($resourcePath = "/search") then
          search:search($props)
          
      else if ($resourcePath = "/config.xml") then
          fn:doc("/db/UICDS/config.xml")
     
      else if ($resourcePath = "/stats") then
         <html>
            This query executed as {xmldb:get-current-user()} on behalf of {props:get-property($props, "req.remoteUser", "---") } <br/>
            There are {fn:count(/NS_PRECIS_STRUCTURES:WorkProduct)} work products being stored. <br/>
            There have been {fn:count(fn:collection("/db/system/versions")/*)} total updates. <br />
         </html>

      else 
        <html>
            <h3>No match found for "{$resourcePath}".</h3>
        </html>
};
