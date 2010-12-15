<?xml version="1.0" encoding="iso-8859-1"?>
<xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
xmlns="http://www.w3.org/2001/10/synthesis"
xmlns:bml="http://www.mindmakers.org/projects/BML">
 
<xsl:output method="xml" encoding="ISO-8859-1" indent="yes"/>
<xsl:strip-space elements="*|text()"/>

  <xsl:template match="/">
    <xsl:apply-templates/>
  </xsl:template>

   <xsl:template match="bml:speech">
     <speak xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
             xml:lang="{@xml:lang}">
          <xsl:copy-of select="child::*|text()"/>
      </speak>
    </xsl:template>

</xsl:stylesheet>
