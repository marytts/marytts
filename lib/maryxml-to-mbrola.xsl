<?xml version="1.0" encoding="ISO-8859-1" ?>
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
                xmlns:mary="http://mary.dfki.de/2002/MaryXML"
                version="1.0">
  <xsl:output method="text"
              encoding="ISO-8859-1"/>

 <!-- boundary -->
  <xsl:template match="mary:boundary">
    <xsl:if test="@duration">_ <xsl:value-of select="@duration"/>
    <xsl:text>&#10;</xsl:text>
    </xsl:if>
  </xsl:template>


 <!-- ph -->
  <xsl:template match="mary:ph">
    <xsl:value-of select="@p"/>
    <xsl:text>   </xsl:text>
    <xsl:value-of select="@d"/>
    <xsl:if test="@f0">
      <xsl:text>   </xsl:text>
      <xsl:value-of select="@f0"/>
    </xsl:if>
    <xsl:text>&#10;</xsl:text>
  </xsl:template>

  <xsl:template match="text()">
  </xsl:template>

 <!-- voice -->
  <xsl:template match="mary:voice">
    <xsl:variable name="previous-voice-name"
                  select="string(ancestor::mary:voice[1]/@name)"/>
    <xsl:variable name="previous-voice-gender"
                  select="string(ancestor::mary:voice[1]/@gender)"/>
    <xsl:variable name="current-voice-name" select="string(@name)"/>
    <xsl:variable name="current-voice-gender" select="string(@gender)"/>
    <xsl:if test="not($previous-voice-name = $current-voice-name and
                      $previous-voice-gender = $current-voice-gender)">
      <xsl:text>;voice</xsl:text>
      <xsl:if test="$current-voice-name != ''">
        <xsl:text> name=</xsl:text>
        <xsl:value-of select="$current-voice-name"/>
      </xsl:if>
      <xsl:if test="$current-voice-gender != ''">
        <xsl:text> gender=</xsl:text>
        <xsl:value-of select="$current-voice-gender"/>
      </xsl:if>
      <xsl:text>&#10;</xsl:text>
    </xsl:if>
    <xsl:apply-templates/>
    <xsl:if test="not($previous-voice-name = $current-voice-name and
                      $previous-voice-gender = $current-voice-gender) and
                  not($previous-voice-name = '' and $previous-voice-gender = '')">
      <xsl:text>;voice</xsl:text>
      <xsl:if test="$previous-voice-name != ''">
        <xsl:text> name=</xsl:text>
        <xsl:value-of select="$previous-voice-name"/>
      </xsl:if>
      <xsl:if test="$previous-voice-gender != ''">
        <xsl:text> gender=</xsl:text>
        <xsl:value-of select="$previous-voice-gender"/>
      </xsl:if>
      <xsl:text>&#10;</xsl:text>
    </xsl:if>
  </xsl:template>

  <!-- Default rule: -->
  <xsl:template match="/|*">
    <xsl:apply-templates/>
  </xsl:template>
</xsl:stylesheet>
