<xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform">
  <xsl:output method="xml" indent="yes"/>
  <xsl:template match="/Products">
    <Rows>
      <xsl:for-each select="Product[Discontinued='false' and string-length(ProductID)&gt;0 and number(UnitPrice)&gt;=0]">
        <Row>
          <ProductID><xsl:value-of select="ProductID"/></ProductID>
          <Name><xsl:value-of select="ProductName"/></Name>
          <Price><xsl:value-of select="format-number(UnitPrice,'0.00')"/></Price>
          <Stock><xsl:value-of select="UnitsInStock"/></Stock>
        </Row>
      </xsl:for-each>
    </Rows>
  </xsl:template>
</xsl:stylesheet>
