<?xml version="1.0" encoding="ISO-8859-15" ?>
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
              encoding="ISO-8859-15"
              indent="yes"/>
  <xsl:strip-space elements="*|text()"/>

  <xsl:template match="SABLE">
    <maryxml xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
             xml:lang="{@xml:lang}"
             version="0.3">
      <xsl:apply-templates/>
    </maryxml>
  </xsl:template>

  <!-- BREAK BREAK BREAK BREAK BREAK BREAK BREAK BREAK -->
  <xsl:template match="BREAK">
    <xsl:variable name="bi">
      <xsl:choose>
        <xsl:when test="@LEVEL='large'" >6</xsl:when>
        <xsl:when test="@LEVEL='medium'">4</xsl:when>
        <xsl:when test="@LEVEL='small'" >3</xsl:when>
        <xsl:when test="@LEVEL='none'">none</xsl:when>
        <xsl:otherwise>4</xsl:otherwise>
      </xsl:choose>
    </xsl:variable>

    <xsl:variable name="tone">
      <xsl:choose>
        <xsl:when test="$bi='none'">none</xsl:when>
        <xsl:when test="$bi >= 4">
          <xsl:choose>
            <xsl:when test="@TYPE='?'">h-h%</xsl:when>
            <xsl:when test="@TYPE='!'">l-l%</xsl:when>
            <xsl:when test="@TYPE='.'">l-l%</xsl:when>
            <xsl:when test="@TYPE=','">h-h%</xsl:when>
            <xsl:otherwise>unknown</xsl:otherwise>
          </xsl:choose>
        </xsl:when>
        <xsl:when test="$bi = 3">
          <xsl:choose>
            <xsl:when test="@TYPE='?'">h-</xsl:when>
            <xsl:when test="@TYPE='!'">l-</xsl:when>
            <xsl:when test="@TYPE='.'">l-</xsl:when>
            <xsl:when test="@TYPE=','">h-</xsl:when>
            <xsl:otherwise>unknown</xsl:otherwise>
          </xsl:choose>
        </xsl:when>
      </xsl:choose>
    </xsl:variable>

    <xsl:choose>
      <xsl:when test="$tone and $bi">
        <boundary tone="{$tone}" breakindex="{$bi}"/>
      </xsl:when>
      <xsl:when test="$tone">
        <boundary tone="{$tone}"/>
      </xsl:when>
      <xsl:when test="$bi">
        <boundary breakindex="{$bi}"/>
      </xsl:when>
    </xsl:choose>
  </xsl:template>

  <!-- EMPH EMPH EMPH EMPH EMPH EMPH EMPH EMPH -->
  <xsl:template match="EMPH">
    <xsl:choose>
      <xsl:when test="@LEVEL='strong' or @LEVEL >= 2">
        <prosody pitch="+30%" rate="-20%" force-accent="syllable">
          <phonology precision="precise">
            <xsl:apply-templates/>
          </phonology>
        </prosody>
      </xsl:when>
      <xsl:when test="@LEVEL='moderate' or 2 > @LEVEL and @LEVEL >= 1">
        <prosody pitch="+10%" rate="-10%" force-accent="word">
          <phonology precision="normal">
            <xsl:apply-templates/>
          </phonology>
        </prosody>
      </xsl:when>
      <xsl:when test="@LEVEL='none' or 1 > @LEVEL and @LEVEL >= 0.5">
            <phonology precision="normal">
              <prosody force-accent="none">
                <xsl:apply-templates/>
              </prosody>
            </phonology>
      </xsl:when>
      <xsl:when test="@LEVEL='reduced' or 0.5 > @LEVEL">
        <prosody range="-30%" rate="+20%" force-accent="none">
          <phonology precision="sloppy">
            <xsl:apply-templates/>
          </phonology>
        </prosody>
      </xsl:when>
      <xsl:otherwise> <!-- like moderate -->
        <prosody pitch="+10%" rate="-10%" force-accent="word">
          <phonology precision="normal">
            <xsl:apply-templates/>
          </phonology>
        </prosody>
      </xsl:otherwise>
    </xsl:choose>
  </xsl:template>


  <!-- PITCH PITCH PITCH PITCH PITCH PITCH PITCH PITCH -->
  <xsl:template match="PITCH">
    <xsl:variable name="baseline">
      <xsl:choose>
        <xsl:when 
        test="(starts-with(@BASE, '+') or starts-with(@BASE, '-')) and
              (string-length(@BASE) = 5 and
               string(number(substring(@BASE, 2, 3))) != 'NaN' and
               substring(@BASE, 5, 1) = '%'
               or
               string-length(@BASE) = 4 and
               string(number(substring(@BASE, 2, 2))) != 'NaN' and
               substring(@BASE, 4, 1) = '%'
               or
               string-length(@BASE) = 3 and
               string(number(substring(@BASE, 2, 1))) != 'NaN' and
               substring(@BASE, 3, 1) = '%'
              )">
          <!-- it's a valid percentage number -->
          <xsl:value-of select="@BASE"/>
        </xsl:when>
        <xsl:when test="@BASE='highest'">+50%</xsl:when>
        <xsl:when test="@BASE='high'"   >+25%</xsl:when>
        <xsl:when test="@BASE='medium'" >+0%</xsl:when>
        <xsl:when test="@BASE='low'"    >-10%</xsl:when>
        <xsl:when test="@BASE='lowest'" >-20%</xsl:when>
        <xsl:when test="@BASE='default'">+0%</xsl:when>
        <xsl:otherwise>+0%</xsl:otherwise>
      </xsl:choose>
    </xsl:variable>

    <xsl:variable name="range">
      <xsl:choose>
        <xsl:when 
        test="(starts-with(@RANGE, '+') or starts-with(@RANGE, '-')) and
              (string-length(@RANGE) = 5 and
               string(number(substring(@RANGE, 2, 3))) != 'NaN' and
               substring(@RANGE, 5, 1) = '%'
               or
               string-length(@RANGE) = 4 and
               string(number(substring(@RANGE, 2, 2))) != 'NaN' and
               substring(@RANGE, 4, 1) = '%'
               or
               string-length(@RANGE) = 3 and
               string(number(substring(@RANGE, 2, 1))) != 'NaN' and
               substring(@RANGE, 3, 1) = '%'
              )">
          <!-- it's a valid percentage number -->
          <xsl:value-of select="@RANGE"/>
        </xsl:when>
        <xsl:when test="@RANGE='highest'">+50%</xsl:when>
        <xsl:when test="@RANGE='high'"   >+25%</xsl:when>
        <xsl:when test="@RANGE='medium'" >+0%</xsl:when>
        <xsl:when test="@RANGE='low'"    >-10%</xsl:when>
        <xsl:when test="@RANGE='lowest'" >-20%</xsl:when>
        <xsl:when test="@RANGE='default'">+0%</xsl:when>
        <xsl:otherwise>+0%</xsl:otherwise>
      </xsl:choose>
    </xsl:variable>

    <prosody>
      <xsl:if test="@BASE">
        <xsl:attribute name="pitch">
          <xsl:value-of select="$baseline"/>
        </xsl:attribute>
      </xsl:if>
      <xsl:if test="@RANGE">
        <xsl:attribute name="range">
          <xsl:value-of select="$range"/>
        </xsl:attribute>
      </xsl:if>

      <xsl:apply-templates/>
    </prosody>
  </xsl:template>

  <!-- RATE RATE RATE RATE RATE RATE RATE RATE -->
  <xsl:template match="RATE">
    <xsl:variable name="speed">
      <xsl:choose>
        <xsl:when 
        test="(starts-with(@SPEED, '+') or starts-with(@SPEED, '-')) and
              (string-length(@SPEED) = 5 and
               string(number(substring(@SPEED, 2, 3))) != 'NaN' and
               substring(@SPEED, 5, 1) = '%'
               or
               string-length(@SPEED) = 4 and
               string(number(substring(@SPEED, 2, 2))) != 'NaN' and
               substring(@SPEED, 4, 1) = '%'
               or
               string-length(@SPEED) = 3 and
               string(number(substring(@SPEED, 2, 1))) != 'NaN' and
               substring(@SPEED, 3, 1) = '%'
              )">
          <!-- it's a valid percentage number -->
          <xsl:value-of select="@SPEED"/>
        </xsl:when>
        <xsl:when test="@SPEED='fastest'">+50%</xsl:when>
        <xsl:when test="@SPEED='fast'"   >+25%</xsl:when>
        <xsl:when test="@SPEED='medium'" >+0%</xsl:when>
        <xsl:when test="@SPEED='slow'"   >-10%</xsl:when>
        <xsl:when test="@SPEED='slowest'">-20%</xsl:when>
        <xsl:otherwise>+0%</xsl:otherwise>
      </xsl:choose>
    </xsl:variable>

    <xsl:choose>
      <xsl:when test="@SPEED">

        <prosody rate="{$speed}">
          <xsl:apply-templates/>
        </prosody>

      </xsl:when>
      <xsl:otherwise>
        <xsl:apply-templates/>
      </xsl:otherwise>
    </xsl:choose>
  </xsl:template>

  <!-- SAYAS SAYAS SAYAS SAYAS SAYAS SAYAS SAYAS SAYAS -->
  <xsl:template match="SAYAS">
    <xsl:variable name="type">
      <xsl:choose>
        <xsl:when test="@MODE='literal'">spell-out</xsl:when>
        <xsl:when test="@MODE='date'">
          <xsl:choose>
            <xsl:when test="@MODETYPE='DMY'">date:dmy</xsl:when>
            <xsl:when test="@MODETYPE='MDY'">date:mdy</xsl:when>
            <xsl:when test="@MODETYPE='YMD'">date:ymd</xsl:when>
            <xsl:when test="@MODETYPE='YM'" >date:ym</xsl:when>
            <xsl:when test="@MODETYPE='MY'" >date:my</xsl:when>
            <xsl:when test="@MODETYPE='MD'" >date:md</xsl:when>
            <xsl:otherwise>date</xsl:otherwise>
          </xsl:choose>
        </xsl:when>
        <xsl:when test="@MODE='time'">
          <xsl:choose>
            <xsl:when test="@MODETYPE='HMS'">time:hms</xsl:when>
            <xsl:when test="@MODETYPE='HM'" >time:hm</xsl:when>
            <xsl:otherwise>time</xsl:otherwise>
          </xsl:choose>
        </xsl:when>
        <xsl:when test="@MODE='phone'">telephone</xsl:when>
        <xsl:when test="@MODE='net'">
          <xsl:choose>
            <xsl:when test="@MODETYPE='EMAIL'">net:email</xsl:when>
            <xsl:when test="@MODETYPE='URL'"  >net:uri</xsl:when>
            <xsl:otherwise>net</xsl:otherwise>
          </xsl:choose>
        </xsl:when>
        <xsl:when test="@MODE='postal'"  >address</xsl:when>
        <xsl:when test="@MODE='currency'">currency</xsl:when>
        <xsl:when test="@MODE='math'"    >number</xsl:when>
        <xsl:when test="@MODE='fraction'">number</xsl:when>
        <xsl:when test="@MODE='ordinal'" >number:ordinal</xsl:when>
        <xsl:when test="@MODE='cardinal'">number:integer</xsl:when>
        <xsl:when test="@MODE='measure'" >measure</xsl:when>
        <xsl:when test="@MODE='name'"    >name</xsl:when>
        <!-- and ignore all unknown mode values -->
      </xsl:choose>
    </xsl:variable>

     <xsl:choose>
      <xsl:when test="$type">

        <say-as type="{$type}">
          <xsl:apply-templates/>
        </say-as>

      </xsl:when>
      <xsl:otherwise>
        <xsl:apply-templates/>
      </xsl:otherwise>
    </xsl:choose>
  </xsl:template>

  <!-- SPEAKER SPEAKER SPEAKER SPEAKER SPEAKER SPEAKER SPEAKER SPEAKER -->
  <xsl:template match="SPEAKER">
    <voice>
      <xsl:if test="@GENDER">
        <xsl:attribute name="gender">
          <xsl:value-of select="@GENDER"/>
        </xsl:attribute>
      </xsl:if>
      <xsl:if test="@NAME">
        <xsl:attribute name="name">
          <xsl:value-of select="@NAME"/>
        </xsl:attribute>
      </xsl:if>

      <xsl:apply-templates/>
    </voice>
  </xsl:template>



  <xsl:template match="text()">
    <xsl:text>&#10;</xsl:text>
    <xsl:value-of select="normalize-space(.)"/>
    <xsl:text>&#10;</xsl:text>
  </xsl:template>

</xsl:stylesheet>
