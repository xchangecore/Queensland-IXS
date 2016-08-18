(:
 :  Utilities for working with properties
 :  The format is a set of unordered <prop> elements.  The value of the attributes need not be unique. 
 :    <props xmlns="util:properties">
 :      <prop name="item1" value=""/>
 :      <prop name="productState" value="Active"/>
 :      <prop name="productType" value="Incident"/>
 :    </props>
 :)

module namespace properties = "http://uicds.org/modules/util/properties";

declare namespace props = "util:properties";

declare function properties:property-names-contains($props as node(), $prop-name as xs:string) as xs:boolean {
	exists($props//props:prop[@name=$prop-name])
};

declare function properties:get-property($props as node(), $prop-name as xs:string, $default-value as xs:string) as xs:string* {
   if (exists($props//props:prop[@name=$prop-name])) then
	   let $prop-nodes := $props/props:prop[@name=$prop-name]
	   let $prop-values := 
    	  for $prop-node in $prop-nodes 
	      return 
    	    if ($prop-node/@value) 
        	then string($prop-node/@value) 
	        else $default-value 
	   return  $prop-values
	else
		$default-value
};

declare function properties:add-property($props as node(), $prop-name as xs:string, $prop-value as xs:string) as node() {
    <props xmlns="util:properties">
        {
            for $prop in $props
            return 
                $prop
        }
        <prop name="{$prop-name}" value="{$prop-value}" />
    </props>
};

declare function properties:get-property-names($props as node()) as xs:string* {
   for $prop-name in distinct-values($props/props:prop/@name)
   return $prop-name
   };
   
declare function properties:buildQueryString($props as node()) {
    
    let $queryString := ""
    let $prop-nodes := $props//props:prop
    return
    		substring(string-join(( $queryString,
    			for $prop in $prop-nodes
    				return
    					string-join(($prop/@name, $prop/@value),"=")
    		), "&amp;"),2)
};












