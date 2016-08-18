module namespace functx = "http://www.functx.com";

declare function functx:change-element-names-deep 
  ( $nodes as node()* ,
    $oldNames as xs:QName* ,
    $newNames as xs:QName* )  as node()* {
       
  if (count($oldNames) != count($newNames))
  then error(xs:QName('functx:Different_number_of_names'))
  else
   for $node in $nodes
   return if ($node instance of element())
          then element
                 {functx:if-empty
                    ($newNames[index-of($oldNames,
                                           node-name($node))],
                     node-name($node)) }
                 {$node/@*,
                  functx:change-element-names-deep($node/node(),
                                           $oldNames, $newNames)}
          else if ($node instance of document-node())
          then functx:change-element-names-deep($node/node(),
                                           $oldNames, $newNames)
          else $node
 } ;

(:
 : Changes the namespace of all xml nodes
 :)
declare function functx:change-element-ns-deep 
  ( $nodes as node()* ,
    $newns as xs:string ,
    $prefix as xs:string )  as node()* {
       
  for $node in $nodes
  return if ($node instance of element())
         then (element
               {QName ($newns,
                          concat($prefix,
                                    if ($prefix = '')
                                    then ''
                                    else ':',
                                    local-name($node)))}
               {$node/@*,
                functx:change-element-ns-deep($node/node(),
                                           $newns, $prefix)})
         else if ($node instance of document-node())
         then functx:change-element-ns-deep($node/node(),
                                           $newns, $prefix)
         else $node
 } ;


declare function functx:if-empty 
  ( $arg as item()? ,
    $value as item()* )  as item()* {
       
  if (string($arg) != '')
  then data($arg)
  else $value
 } ;
 
 declare function functx:month-abbrev-en 
  ( $date as xs:anyAtomicType? )  as xs:string? {
   ('Jan', 'Feb', 'Mar', 'Apr', 'May', 'Jun',
    'Jul', 'Aug', 'Sep', 'Oct', 'Nov', 'Dec')
    [month-from-dateTime($date)]
 } ;
 
 declare function functx:timezone-from-duration 
  ( $duration as xs:dayTimeDuration )  as xs:string {
       
   if (string($duration) = ('PT0S','-PT0S'))
   then 'Z'
   else if (matches(string($duration),'-PT[1-9]H'))
   then replace(string($duration),'PT([1-9])H','0$1:00')
   else if (matches(string($duration),'PT[1-9]H'))
   then replace(string($duration),'PT([1-9])H','+0$1:00')
   else if (matches(string($duration),'-PT1[0-4]H'))
   then replace(string($duration),'PT(1[0-4])H','$1:00')
   else if (matches(string($duration),'PT1[0-4]H'))
   then replace(string($duration),'PT(1[0-4])H','+$1:00')
   else error(xs:QName('functx:Invalid_Duration_Value'))
 } ;
 
 declare function functx:pad-integer-to-length 
  ( $integerToPad as xs:anyAtomicType? ,
    $length as xs:integer )  as xs:string {
       
   if ($length < string-length(string($integerToPad)))
   then error(xs:QName('functx:Integer_Longer_Than_Length'))
   else concat
         (functx:repeat-string(
            '0',$length - string-length(string($integerToPad))),
          string($integerToPad))
 };
 
 declare function functx:repeat-string 
  ( $stringToRepeat as xs:string? ,
    $count as xs:integer )  as xs:string {
    string-join((for $i in 1 to $count return $stringToRepeat),'')
 } ;