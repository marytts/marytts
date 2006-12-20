<?xml version="1.0" encoding="iso-8859-1"?>
<xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
xmlns:mary="http://mary.dfki.de/2002/MaryXML"
xmlns:bml="http://www.mindmakers.org/projects/BML"
xmlns="http://www.mindmakers.org/projects/BML"
exclude-result-prefixes="bml mary #default"
>

<xsl:output method="xml" encoding="ISO-8859-1" indent="yes"/>
<xsl:strip-space elements="*|text()"/>

    <xsl:template match="@*|node()">
      <xsl:copy>
          <xsl:apply-templates select="@*|node()"/>
      </xsl:copy>
    </xsl:template>

    <xsl:template match="bml:speech">
        <!-- Insert the plain text as a comment, in order to increase -->
        <!-- debugging readability: -->
        <xsl:comment>
            <xsl:value-of select="normalize-space(.)"/>
        </xsl:comment>
	    <!--xsl:copy-of select="."/-->
	    <speech id="{@id}" xml:lang="{@xml:lang}" type="application/maryxml-acoustparams">
            <xsl:copy-of select="document('mary.acoustparams')"/>
	        <!--xsl:apply-templates select="document('mary.acoustparams')">
            </xsl:apply-templates-->
        </speech>
    </xsl:template>


  <xsl:template match="text()"/>

</xsl:stylesheet>
