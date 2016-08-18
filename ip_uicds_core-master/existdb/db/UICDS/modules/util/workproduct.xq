(:
 :  Utilities for working with work products
 :)

module namespace workproduct = "http://uicds.org/modules/util/workproduct";

import module namespace v="http://exist-db.org/versioning";

import module namespace functx = "http://www.functx.com"
at "xmldb:exist:///db/UICDS/modules/util/functx.xq";

declare namespace util="http://exist-db.org/xquery/util";
declare namespace NS_PRECIS_STRUCTURES = "http://www.saic.com/precis/2009/06/structures";
declare namespace NS_PRECIS_BASE = "http://www.saic.com/precis/2009/06/base";
declare namespace NS_ULEX_STRUCTURE = "ulex:message:structure:1.0";
declare namespace NS_UCORE = "http://ucore.gov/ucore/2.0";
declare namespace NS_ULEX = "ulex:message:structure:1.0";
declare namespace NS_GML = "http://www.opengis.net/gml/3.2";

(:
 :  Returns a workproduct by ID and optionally a version number
 :)
declare function workproduct:doc($id as xs:string, $version as xs:integer?) as node() {

    let $docURI := fn:base-uri ( /NS_PRECIS_STRUCTURES:WorkProduct[NS_ULEX_STRUCTURE:PackageMetadata/NS_PRECIS_STRUCTURES:WorkProductIdentification/NS_PRECIS_BASE:Identifier=$id] )
    
    let $vCollection := concat("/db/system/versions", $docURI)
    let $baseName := concat($vCollection, "/", $docName, ".base")    

    let $collection := util:collection-name($doc)
    let $docName := util:document-name($doc)
    let $vCollection := concat("/db/system/versions", $collection)
    let $baseName := concat($vCollection, "/", $docName, ".base")
    return
        if (not(doc-available($baseName))) then
            ()
        else if (exists($rev)) then
            v:apply-patch(
                doc($baseName),
				for $version in
                	collection($vCollection)/v:version[v:properties[v:document = $docName]
                    	[v:revision <= $rev]][v:diff]
					order by xs:long($version/v:properties/v:revision) ascending
				return
					$version
            )
		else
			doc($baseName)
};

(:
 :  Returns an XML fragment showing the version history of the workproduct
 :)
declare function workproduct:history($doc as node()) as element(v:history) {
    let $collection := util:collection-name($doc)
    let $docName := util:document-name($doc)
    let $vCollection := concat("/db/system/versions", $collection)
	return
		<v:history>
			<v:document>{base-uri($doc)}</v:document>
			<v:revisions>
			{
				for $v in collection($vCollection)//v:properties[v:document = $docName]
				order by xs:long($v/v:revision) ascending
				return
					<v:revision rev="{$v/v:revision}">
					{ $v/v:version, $v/v:date, $v/v:user}
					</v:revision>
			}
			</v:revisions>
		</v:history>
};


declare function workproduct:xml-to-html-table($element) {
   if (exists ($element/(@*|*)))
     then 
     <table class="xml-table">
        {if (exists($element/text()))
         then <tr class="text">
                  <th></th>
                  <td>{$element/text()}</td>
              </tr>
         else ()
       }
       {for $attribute in $element/@*
       return
          <tr class="attribute">
             <th class="attribute-name">@{name($attribute)}</th>
             <td class="attribute-value">{string($attribute)}</td>
          </tr>
       }
       {for $node in $element/*
       return 
          <tr class="element">
             <th class="element-name">{name($node)}</th> 
             <td class="element-value">{workproduct:xml-to-html-table($node)}</td>
          </tr>       
        }
    </table>
    else $element/text()
  };
