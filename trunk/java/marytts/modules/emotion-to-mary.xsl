<?xml version="1.0" encoding="UTF-8" ?>
<!--
Copyright 2000-2006 DFKI GmbH.
All Rights Reserved.  Use is subject to license terms.

Permission is hereby granted, free of charge, to use and distribute
this software and its documentation without restriction, including
without limitation the rights to use, copy, modify, merge, publish,
distribute, sublicense, and/or sell copies of this work, and to
permit persons to whom this work is furnished to do so, subject to
the following conditions:

1. The code must retain the above copyright notice, this list of
   conditions and the following disclaimer.
2. Any modifications must be clearly marked as such.
3. Original authors' names are not deleted.
4. The authors' names are not used to endorse or promote products
   derived from this software without specific prior written
   permission.

DFKI GMBH AND THE CONTRIBUTORS TO THIS WORK DISCLAIM ALL WARRANTIES WITH
REGARD TO THIS SOFTWARE, INCLUDING ALL IMPLIED WARRANTIES OF
MERCHANTABILITY AND FITNESS, IN NO EVENT SHALL DFKI GMBH NOR THE
CONTRIBUTORS BE LIABLE FOR ANY SPECIAL, INDIRECT OR CONSEQUENTIAL
DAMAGES OR ANY DAMAGES WHATSOEVER RESULTING FROM LOSS OF USE, DATA OR
PROFITS, WHETHER IN AN ACTION OF CONTRACT, NEGLIGENCE OR OTHER TORTIOUS
ACTION, ARISING OUT OF OR IN CONNECTION WITH THE USE OR PERFORMANCE OF
THIS SOFTWARE.
-->
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
                version="1.0"
                xmlns="http://mary.dfki.de/2002/MaryXML">
  <xsl:output method="xml"
              encoding="UTF-8"
              indent="yes"/>
  <xsl:strip-space elements="*|text()"/>

  <!-- emotion -->
  <xsl:template match="/emotion">
    <xsl:variable name="language">
      <xsl:choose>
        <xsl:when test="@xml:lang"><xsl:value-of select="@xml:lang"/></xsl:when>
        <xsl:otherwise>de</xsl:otherwise>
      </xsl:choose>
    </xsl:variable>
    <maryxml xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
             version="0.4" xml:lang="{$language}">
      <xsl:call-template name="acousticParameters">
        <xsl:with-param name="activation" select="@activation"/>
        <xsl:with-param name="evaluation" select="@evaluation"/>
        <xsl:with-param name="power" select="@power"/>
      </xsl:call-template>
    </maryxml>
  </xsl:template>

  <xsl:template name="acousticParameters">
    <!-- These parameters are normalised from -100 to 100: -->
    <xsl:param name="activation"/>
    <xsl:param name="evaluation"/>
    <xsl:param name="power"/>
    <xsl:variable name="pitch"
    select="format-number(round(0.3*$activation + 0.1*$evaluation - 0.1*$power), '+#;-#')"/>

    <xsl:variable name="pitch-dynamics"
    select="format-number(round(-15 + 0.3*$activation - 0.3*$power), '+#;-#')"/>

    <!-- Range in semitones -->
    <xsl:variable name="range" select="format-number(4 + 0.04*$activation, '#.##')"/>

    <xsl:variable name="_range-dynamics"
    select="format-number(round(-40 + 1.2*$activation + 0.4*$power), '+#;-#')"/>
    <!-- A range-dynamics below -100 does not make sense -->
    <xsl:variable name="range-dynamics">
        <xsl:choose>
            <xsl:when test="$_range-dynamics &lt; -100">-100</xsl:when>
            <xsl:otherwise><xsl:value-of select="$_range-dynamics"/></xsl:otherwise>
        </xsl:choose>
    </xsl:variable>


    <xsl:variable name="accent-prominence"
    select="format-number(round(0.5*$activation - 0.5*$evaluation), '+#;-#')"/>
    <xsl:variable name="preferred-accent-shape">
      <xsl:choose>
        <xsl:when test="$evaluation &lt; -20">falling</xsl:when>
        <xsl:when test="$evaluation &gt; 40">alternating</xsl:when>
        <xsl:otherwise>rising</xsl:otherwise>
      </xsl:choose>
    </xsl:variable>
    <xsl:variable name="accent-slope"
    select="format-number(round(1*$activation - 0.5*$evaluation), '+#;-#')"/>

    <xsl:variable name="preferred-boundary-type">
      <xsl:choose>
        <xsl:when test="$power &gt; 0">low</xsl:when>
        <xsl:otherwise>high</xsl:otherwise>
      </xsl:choose>
    </xsl:variable>

    <xsl:variable name="rate"
    select="format-number(round(0.5*$activation + 0.2*$evaluation), '+#;-#')"/>

    <xsl:variable name="number-of-pauses"
    select="format-number(round(0.7*$activation), '+#;-#')"/>
    <xsl:variable name="pause-duration"
    select="format-number(round(-0.2*$activation), '+#;-#')"/>

    <xsl:variable name="vowel-duration"
    select="format-number(round(0.3*$evaluation + 0.3*$power), '+#;-#')"/>
    <xsl:variable name="nasal-duration"
    select="format-number(round(0.3*$evaluation + 0.3*$power), '+#;-#')"/>
    <xsl:variable name="liquid-duration"
    select="format-number(round(0.3*$evaluation + 0.3*$power), '+#;-#')"/>
    <xsl:variable name="plosive-duration"
    select="format-number(round(0.5*$activation - 0.3*$evaluation), '+#;-#')"/>
    <xsl:variable name="fricative-duration"
    select="format-number(round(0.5*$activation - 0.3*$evaluation), '+#;-#')"/>

    <xsl:variable name="volume"
    select="round(50 + 0.33*$activation)"/>

    <prosody>
        <xsl:attribute name="pitch"><xsl:value-of select="$pitch"/>%</xsl:attribute>
        <xsl:attribute name="pitch-dynamics"><xsl:value-of select="$pitch-dynamics"/>%</xsl:attribute>
        <xsl:attribute name="range"><xsl:value-of select="$range"/>st</xsl:attribute>
        <xsl:attribute name="range-dynamics"><xsl:value-of select="$range-dynamics"/>%</xsl:attribute>
        <xsl:attribute name="preferred-accent-shape"><xsl:value-of select="$preferred-accent-shape"/></xsl:attribute>
        <xsl:attribute name="accent-slope"><xsl:value-of select="$accent-slope"/>%</xsl:attribute>
        <xsl:attribute name="accent-prominence"><xsl:value-of select="$accent-prominence"/>%</xsl:attribute>
        <xsl:attribute name="preferred-boundary-type"><xsl:value-of select="$preferred-boundary-type"/></xsl:attribute>
        <xsl:attribute name="rate"><xsl:value-of select="$rate"/>%</xsl:attribute>
        <xsl:attribute name="number-of-pauses"><xsl:value-of select="$number-of-pauses"/>%</xsl:attribute>
        <xsl:attribute name="pause-duration"><xsl:value-of select="$pause-duration"/>%</xsl:attribute>
        <xsl:attribute name="vowel-duration"><xsl:value-of select="$vowel-duration"/>%</xsl:attribute>
        <xsl:attribute name="nasal-duration"><xsl:value-of select="$nasal-duration"/>%</xsl:attribute>
        <xsl:attribute name="liquid-duration"><xsl:value-of select="$liquid-duration"/>%</xsl:attribute>
        <xsl:attribute name="plosive-duration"><xsl:value-of select="$plosive-duration"/>%</xsl:attribute>
        <xsl:attribute name="fricative-duration"><xsl:value-of select="$fricative-duration"/>%</xsl:attribute>
        <xsl:attribute name="volume"><xsl:value-of select="$volume"/></xsl:attribute>
      <xsl:apply-templates/>
    </prosody>


  </xsl:template>

  <xsl:template match="text()">
    <xsl:text>&#10;</xsl:text>
    <xsl:value-of select="normalize-space(.)"/>
    <xsl:text>&#10;</xsl:text>
  </xsl:template>

</xsl:stylesheet>
