<?xml version="1.0" encoding="ISO-8859-1"?>
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

  <xsl:import href="emotion-to-mary.xsl"/>

  <xsl:output method="xml"
    encoding="ISO-8859-15"
    indent="yes"/>

  <xsl:strip-space elements="*|text()"/> 

  <!-- root element: -->
  <xsl:template match="apml">
    <!-- because xml:lang is often missing: use "en" as default -->
    <xsl:variable name="lang">
      <xsl:choose>
        <xsl:when test="@xml:lang">
          <xsl:value-of select="@xml:lang"/>
        </xsl:when>
        <xsl:otherwise>en</xsl:otherwise>
      </xsl:choose>
    </xsl:variable>
    
    <!-- attention: in the apml files xml:lang seems to be missing quite often -->
    <maryxml xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
      version="0.3">
      <xsl:attribute name="xml:lang"><xsl:value-of select="$lang"/></xsl:attribute>
      <xsl:comment> Created by apml-to-mary.xsl </xsl:comment>
      <mark name="apml:apml_start"/>
      <xsl:apply-templates/>
      <mark name="apml:apml_end"/>
    </maryxml>
  </xsl:template>
  
  <!-- turnallocation turnallocation turnallocation turnallocation turnallocation -->
  <xsl:template match="turnallocation">
    <mark name="apml:turnallocation_start"/>
    <xsl:apply-templates/>
    <mark name="apml:turnallocation_end"/>
  </xsl:template>
  
  <!-- performative performative performative performative performative -->
  <xsl:template match="performative">
    <mark name="apml:performative_start"/>
    <xsl:apply-templates/>
    <mark name="apml:performative_end"/>
  </xsl:template>

  <!-- theme theme theme theme theme -->
  <xsl:template match="theme">
    <mark name="apml:theme_start"/>
    <xsl:choose>
      <xsl:when test="@affect">
        <xsl:call-template name="emotion2acousticParameters"/>
      </xsl:when>
      <xsl:otherwise>
        <xsl:apply-templates/>
      </xsl:otherwise>
    </xsl:choose>
    <mark name="apml:theme_end"/>
  </xsl:template>

  <!-- rheme rheme rheme rheme rheme -->
  <xsl:template match="rheme">
    <mark name="apml:rheme_start"/>
    <xsl:choose>
      <xsl:when test="@affect">
        <xsl:call-template name="emotion2acousticParameters"/>
      </xsl:when>
      <xsl:otherwise>
        <xsl:apply-templates/>
      </xsl:otherwise>
    </xsl:choose>
    <mark name="apml:rheme_end"/>
  </xsl:template>
  
  
  <!-- NAMED template for processing tobi accents -->
  <xsl:template name="tobitone">
    <!-- is there a tobi-accent? -->
    <xsl:variable name="tobiaccent">
      <xsl:choose>
        <!-- Hstar|Lstar|LplusHstar|LstarplusH|HstarplusL|HplusLstar -->
        <xsl:when test="@x-pitchaccent='Hstar'"      >H*</xsl:when>
        <xsl:when test="@x-pitchaccent='Lstar'"      >L*</xsl:when>
        <xsl:when test="@x-pitchaccent='LstarplusH'" >L*+H</xsl:when>
        <xsl:when test="@x-pitchaccent='HstarplusL'" >H*+L</xsl:when>
        <xsl:when test="@x-pitchaccent='HplusLstar'" >H+L*</xsl:when>
        <xsl:otherwise>unknown</xsl:otherwise>
      </xsl:choose>
    </xsl:variable>
    <xsl:choose>
      <xsl:when test="$tobiaccent != 'unknown'">
        <t>
          <xsl:attribute name="accent">
            <xsl:value-of select="$tobiaccent"/>
          </xsl:attribute>
          <xsl:apply-templates/>
        </t>
      </xsl:when>
      <xsl:otherwise>
        <xsl:apply-templates/>
      </xsl:otherwise>
    </xsl:choose>
  </xsl:template>
  
  <!-- emphasis emphasis emphasis emphasis emphasis -->
  <xsl:template match="emphasis">
    <mark name="apml:emphasis_start"/>   
    <xsl:choose>
      <xsl:when test="@level='strong'">
        <!-- <LEVEL><xsl:value-of select="@level"/></LEVEL> -->
        <prosody pitch="+30%" rate="-20%" force-accent="syllable">
          <phonology precision="precise">
            <xsl:call-template name="tobitone"/>
          </phonology>
        </prosody>
      </xsl:when>
      <xsl:when test="@level='medium'">
        <!-- <LEVEL><xsl:value-of select="@level"/></LEVEL> -->
        <prosody pitch="+20%" rate="-10%" force-accent="word">
          <phonology precision="normal">
            <xsl:call-template name="tobitone"/>
          </phonology>
        </prosody>
      </xsl:when>
      <xsl:when test="@level='weak'">
        <!-- <LEVEL><xsl:value-of select="@level"/></LEVEL> -->
        <prosody pitch="+10%" rate="-5%" force-accent="word">
          <phonology precision="normal">
            <xsl:call-template name="tobitone"/>
          </phonology>
        </prosody>
      </xsl:when>
      <xsl:otherwise>
        <xsl:call-template name="tobitone"/>
      </xsl:otherwise>
    </xsl:choose>
    <mark name="apml:emphasis_end"/>
  </xsl:template>
  

 <!-- boundary boundary boundary boundary boundary -->
 <!-- which breakindex to use  -->
  <xsl:template match="boundary">

      <!-- conversion to tobi -->
      <xsl:variable name="tobi">
        <xsl:choose>
          <xsl:when test="@type='LL'">L-L%</xsl:when>
          <xsl:when test="@type='HH'">H-H%</xsl:when>
          <xsl:when test="@type='LH'">L-H%</xsl:when>
          <xsl:when test="@type='HL'">H-L%</xsl:when>
          <xsl:when test="@type='L'">L-</xsl:when>
          <xsl:when test="@type='H'">H-</xsl:when>
          <xsl:otherwise>unknown</xsl:otherwise>
        </xsl:choose>
      </xsl:variable>

      <!-- breakindex abhaengig von tone machen -->
      <xsl:variable name="bi">
        <xsl:choose>
          <xsl:when test="@type='L' or @type='H'">3</xsl:when>
          <xsl:otherwise>6</xsl:otherwise>
        </xsl:choose>
      </xsl:variable>

     <!-- TODO $bi should be 4 when sentence-internal, and 6 otherwise -->

    <mark name="apml:boundary"/>
    <boundary>
      <xsl:attribute name="tone">
        <xsl:value-of select="$tobi"/>
      </xsl:attribute>
      <xsl:attribute name="breakindex">
        <xsl:value-of select="$bi"/>
      </xsl:attribute>
    </boundary>
  </xsl:template>

 <!-- pause pause pause pause pause -->
  <xsl:template match="pause">
    <!-- TODO allowing "1.2s" , "300ms" in MaryXML.xsd -->
    <!-- breakindex depends on length of pause -->
    <!-- TODO which breakindex to use ??? -->
    <xsl:variable name="bi">
      <xsl:choose>
        <xsl:when test="@sec &lt;= '0.3'">4</xsl:when>
        <xsl:otherwise>6</xsl:otherwise>
      </xsl:choose>
    </xsl:variable>
    <mark name="apml:pause"/>
    <boundary breakindex="4">
      <xsl:attribute name="duration">
        <!-- converting sec to ms -->
        <xsl:value-of select="@sec * 1000"/>
      </xsl:attribute>
      <xsl:attribute name="breakindex">
        <xsl:value-of select="$bi"/>
      </xsl:attribute>
    </boundary>

  </xsl:template>
  
  <xsl:template match="text()">
    <xsl:text>&#10;</xsl:text>
    <xsl:value-of select="normalize-space(.)"/>
    <xsl:text>&#10;</xsl:text>
  </xsl:template>


  <!-- FROM: emotion-to-mary.xsl -->
  <!-- Helpers -->

  <xsl:template name="emotion2acousticParameters">
     <xsl:comment> emotion2acousticParameters: affect=<xsl:value-of select="@affect"/> </xsl:comment>
     <xsl:variable name="activation">
       <xsl:choose>
        <xsl:when test="@activation">
          <xsl:value-of select="@activation"/>
        </xsl:when>
        <xsl:when test="@affect">
          <xsl:call-template name="category2dimension">
            <xsl:with-param name="category"  select="@affect"/>
            <xsl:with-param name="intensity" select="@intensity"/>
            <xsl:with-param name="dimension" select="'activation'"/>
          </xsl:call-template>
        </xsl:when>
        <xsl:otherwise>10</xsl:otherwise>
      </xsl:choose>
    </xsl:variable>
    <xsl:variable name="evaluation">
      <xsl:choose>
        <xsl:when test="@evaluation">
          <xsl:value-of select="@evaluation"/>
        </xsl:when>
        <xsl:when test="@affect">
          <xsl:call-template name="category2dimension">
            <xsl:with-param name="category" select="@affect"/>
            <xsl:with-param name="intensity" select="@intensity"/>
            <xsl:with-param name="dimension" select="'evaluation'"/>
          </xsl:call-template>
        </xsl:when>
        <xsl:otherwise>20</xsl:otherwise>
      </xsl:choose>
    </xsl:variable>
    <xsl:variable name="power">
      <xsl:choose>
        <xsl:when test="@power">
          <xsl:value-of select="@power"/>
        </xsl:when>
        <xsl:when test="@affect">
          <xsl:call-template name="category2dimension">
            <xsl:with-param name="category" select="@affect"/>
            <xsl:with-param name="intensity" select="@intensity"/>
            <xsl:with-param name="dimension" select="'power'"/>
          </xsl:call-template>
        </xsl:when>
        <xsl:otherwise>30</xsl:otherwise>
      </xsl:choose>
    </xsl:variable>

    <!--
    <xsl:comment> Calling 0: acousticParameters
    activation=<xsl:value-of select="$activation"/>
    evaluation=<xsl:value-of select="$evaluation"/>
    from stylesheet emotion-to-mary.xsl
    </xsl:comment>
    -->
    <xsl:call-template name="acousticParameters">
      <xsl:with-param name="activation" select="$activation"/>
      <xsl:with-param name="evaluation" select="$evaluation"/>
      <xsl:with-param name="power"      select="$power"/>
    </xsl:call-template>
  </xsl:template>



  <xsl:template name="category2dimension">
    <xsl:param name="category">joy</xsl:param>
    <xsl:param name="intensity">1</xsl:param>
    <xsl:param name="dimension">activation</xsl:param>
    <!-- 
    <xsl:comment> Called category2dimension </xsl:comment>
    <xsl:comment>category2dimension: category=<xsl:value-of select="$category"/> dimension=<xsl:value-of select="$dimension"/> intensity=<xsl:value-of select="$intensity"/>
    </xsl:comment>
  -->
    <xsl:variable name="norm-intens">
      <xsl:choose>
        <xsl:when test="$intensity &gt;=0 and $intensity &lt;= 1">
          <xsl:value-of select="$intensity"/>
        </xsl:when>
        <xsl:otherwise>0</xsl:otherwise>
      </xsl:choose>
    </xsl:variable>

  <!-- @affect in theme and rheme -->
  <!--  thoes with 'j' are found in the table below
        those with 'XX' have been guesstimated by hannes
  j     anger
  j     disappointment
 XX      disgust
  j     disliking
  j     distress
 XX      embarrassment
  XX     envy
  j     fear
  jX     fear(s)-confirmed
  j     gloating
  j     gratification
  j     gratitude
  j     happy-for
  j     hate
  j     hope
XX       jealousy
  j     joy
  j     liking
  j     love
  j     pride
  j     relief
  j     remorse
  j     reproach
  j     resentment
XX       sadness
  j     satisfaction
  j     shame
  j     sorry-for
XX       surprise (?? good or bad ??)
   -->

    <xsl:variable name="c">
      <xsl:choose>
        <xsl:when test="$category='admiration'"     >A  27   E  53   P  17   </xsl:when>
        <xsl:when test="$category='anger'"          >A  34.0 E -35.6 P  20   </xsl:when>
        <xsl:when test="$category='disappointment'" >A   2.4 E -24.9 P -37.2 </xsl:when>
        <xsl:when test="$category='disgust'"        >A -17.2 E -40.1 P -52.4 </xsl:when>
        <xsl:when test="$category='disliking'"      >A  15   E -35   P -10   </xsl:when>
        <xsl:when test="$category='distress'"       >A -17.2 E -40.1 P -52.4 </xsl:when>
        <xsl:when test="$category='embarassment'"   >A -17.2 E -40.1 P -52.4 </xsl:when>
        <!-- like resentment --> 
        <xsl:when test="$category='envy'"           >A   0.1   E -40   P -20 </xsl:when>
        <xsl:when test="$category='fear'"           >A  14.8 E -44.4 P -79.4 </xsl:when>
        <xsl:when test="$category='fear-confirmed'" >A -30   E -50   P -70   </xsl:when>
        <xsl:when test="$category='gloating'"       >A  40   E  30   P  30   </xsl:when>
        <xsl:when test="$category='gratification'"  >A -14.9 E  33.1 P  12.2 </xsl:when>
        <xsl:when test="$category='gratitude'"      >A  20   E  40   P -30   </xsl:when>
        <xsl:when test="$category='happy-for'"      >A  17.3 E  42.2 P  12.5 </xsl:when>
        <xsl:when test="$category='hate'"           >A  60   E -60   P  30   </xsl:when>
        <xsl:when test="$category='hope'"           >A  20   E  20   P -10   </xsl:when>
        <xsl:when test="$category='jealousy'"       >A   0.2   E -40   P -20 </xsl:when>
        <xsl:when test="$category='joy'"            >A  17.3 E  42.2 P  12.5 </xsl:when>
        <xsl:when test="$category='liking'"         >A -14.9 E  33.1 P  12.2 </xsl:when>
        <xsl:when test="$category='love'"           >A   1.2 E  33.3 P  14.9 </xsl:when>
        <xsl:when test="$category='pride'"          >A  30   E  40   P  30   </xsl:when>
        <xsl:when test="$category='relief'"         >A   3   E  33   P  -3   </xsl:when>
        <xsl:when test="$category='remorse'"        >A   4.6 E -26.3 P -62.3 </xsl:when>
        <xsl:when test="$category='reproach'"       >A  -3   E -30   P  43   </xsl:when>
        <xsl:when test="$category='resentment'"     >A   0   E -40   P -20   </xsl:when>
        <xsl:when test="$category='sadness'"        >A -20   E -40   P -5    </xsl:when>
        <xsl:when test="$category='satisfaction'"   >A -14.9 E  33.1 P  12.2 </xsl:when>
        <xsl:when test="$category='shame'"          >A   4.6 E -26.3 P -62.3 </xsl:when>
        <xsl:when test="$category='sorry-for'"      >A -17.2 E -40.1 P -52.4 </xsl:when>
        <xsl:when test="$category='surprise'"       >A  40   E   0   P  0    </xsl:when>
        <xsl:otherwise                              >A  10   E   10   P  10    </xsl:otherwise>
      </xsl:choose>
    </xsl:variable>

    <xsl:choose>
      <xsl:when test="$c">
        <xsl:variable name="activation">
          <xsl:value-of select="normalize-space(substring-before(substring-after($c,'A'), 'E'))"/>
        </xsl:variable>
        <xsl:variable name="evaluation">
          <xsl:value-of select="normalize-space(substring-before(substring-after($c,'E'), 'P'))"/>
        </xsl:variable>
        <xsl:variable name="power">
          <xsl:value-of select="normalize-space(substring-after($c, 'P'))"/>
        </xsl:variable>
        <xsl:choose>
          <xsl:when test="$dimension='activation'">
            <xsl:value-of select="$activation * $norm-intens"/>
          </xsl:when>
          <xsl:when test="$dimension='evaluation'">
            <xsl:value-of select="$evaluation * $norm-intens"/>
          </xsl:when>
          <xsl:when test="$dimension='power'">
            <xsl:value-of select="$power * $norm-intens"/>
          </xsl:when>
        </xsl:choose>
      </xsl:when>
      <xsl:otherwise>0</xsl:otherwise>
    </xsl:choose>
  </xsl:template>


</xsl:stylesheet>
