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
	    <speech id="{@id}" xml:lang="{@xml:lang}" type="phoneme-timings" text="{normalize-space(.)}">
            <!--xsl:copy-of select="document('mary.acoustparams')"/-->
	        <xsl:apply-templates select="document('mary.acoustparams')">
            </xsl:apply-templates>
        </speech>
    </xsl:template>


  <xsl:template match="mary:*">
    <xsl:apply-templates/>
  </xsl:template>

  <xsl:template match="mary:boundary">
      <xsl:if test="@duration">
        <!-- Create a <ph> child representing silence -->
        <!-- with the specified duration              -->
        <ph p="_" dur="{@duration}"/>
      </xsl:if>
  </xsl:template>

  <xsl:template match="mary:syllable">
      <xsl:if test="@stress">
        <xsl:variable name="stress" select="@stress"/>
      </xsl:if>
      <xsl:apply-templates/>
  </xsl:template>

  <xsl:template match="mary:ph">
      <xsl:variable name="stress" select="../@stress"/>
      <xsl:variable name="vowel">
      <xsl:choose>
        <xsl:when test="contains('Vi:IU{@r=AOu:E:EIAIOIaU@Ue:y:Yo:a:', @p)">1</xsl:when>
        <xsl:otherwise>0</xsl:otherwise>
      </xsl:choose>
    </xsl:variable>
    <ph p="{@p}" dur="{@d}">
      <xsl:if test="$stress and $vowel!=0">
        <xsl:attribute name="stress">
          <xsl:value-of select="$stress"/>
        </xsl:attribute>
      </xsl:if>
    </ph>
  </xsl:template>

  <xsl:template match="mary:mark">
    <mark name="{@name}"/>
  </xsl:template>



  <xsl:template match="text()"/>

</xsl:stylesheet>
