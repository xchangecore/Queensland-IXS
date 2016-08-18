<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform" version="1.0">

<xsl:template match="/xmlroot">
  <html>
    <head>
        <title>Manifest Attributes</title>
    </head>
    <body>
     <br/>
        <h1 style="text-align: left; color: blue">Manifest Attributes</h1>
        <table style="border: 1px">
          <thead style="font-weight: bold; background-color: #87CEFA">
          <th>Name</th>
          <th>Value</th>
          </thead>
          <xsl:for-each select="entry">
            <tr>
              <td><xsl:value-of select="name"/></td>
              <td><xsl:value-of select="value"/></td>
            </tr>
          </xsl:for-each>
        </table>
     <br/>
    </body>
  </html>
</xsl:template>
</xsl:stylesheet>
