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
<!-- normalise-maryxml.xsl
Purpose: "Normalise" maryXML data to the old pseudo-XML form,
i.e. one tag or text token per line starting at the beginning of the line.
-->

<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform" version="1.0">
  <xsl:output method="xml" encoding="UTF-8"
              indent="no"
  />
  <xsl:strip-space elements="*|text()"/>

  <xsl:template match="*[count(child::text()|child::*)=0]">
    <xsl:copy>
      <xsl:apply-templates select="*|@*|text()"/>
    </xsl:copy>
    <xsl:text>&#10;</xsl:text>
  </xsl:template>

  <xsl:template match="*">
    <xsl:copy>
      <xsl:apply-templates select="@*"/>
      <xsl:text>&#10;</xsl:text>
      <xsl:apply-templates select="*|text()"/>
    </xsl:copy>
    <xsl:text>&#10;</xsl:text>
  </xsl:template>

  <xsl:template match="@*">
    <xsl:copy/>
  </xsl:template>

  <xsl:template match="text()">
    <xsl:value-of select="normalize-space(.)"/>
    <xsl:text>&#10;</xsl:text>
  </xsl:template>

</xsl:stylesheet>
