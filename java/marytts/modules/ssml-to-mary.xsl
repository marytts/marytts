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
                xmlns:ssml="http://www.w3.org/2001/10/synthesis"
                xmlns="http://mary.dfki.de/2002/MaryXML">
  <xsl:output method="xml"
              encoding="UTF-8"
              indent="yes"/>
  <xsl:strip-space elements="*|text()"/>


  <!-- root element: -->
  <xsl:template match="ssml:speak">
    <maryxml xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
             xml:lang="{@xml:lang}"
             version="0.5">
      <xsl:apply-templates/>
    </maryxml>
  </xsl:template>


 <!-- paragraph paragraph paragraph paragraph paragraph paragraph paragraph -->
  <xsl:template match="ssml:p">
    <xsl:choose>
      <xsl:when test="@xml:lang">
        <voice xml:lang="{@xml:lang}">
          <p>
            <xsl:apply-templates/>
          </p>
        </voice>
      </xsl:when>
      <xsl:otherwise>
        <p>
          <xsl:apply-templates/>
        </p>
      </xsl:otherwise>
    </xsl:choose>
  </xsl:template>


 <!-- sentence sentence sentence sentence sentence sentence sentence -->
  <xsl:template match="ssml:s">
    <s>
      <xsl:if test="@xml:lang">
        <xsl:attribute name="xml:lang"><xsl:value-of select="@xml:lang"/></xsl:attribute>
      </xsl:if>
      <xsl:apply-templates/>
    </s>
  </xsl:template>


<!-- say-as say-as say-as say-as say-as say-as say-as say-as -->
  <xsl:template match="ssml:say-as">
     <xsl:choose>
      <xsl:when test="@interpret-as='characters'">
      	<say-as type="spell-out">
	      <xsl:apply-templates/>
		</say-as>
      </xsl:when>
      <xsl:when test="@interpret-as='ordinal'">
      	<say-as type="number:ordinal">
	      <xsl:apply-templates/>
		</say-as>
      </xsl:when>
      <xsl:when test="@interpret-as='digits'">
      	<say-as type="number:digits">
	      <xsl:apply-templates/>
		</say-as>
      </xsl:when>
      <xsl:when test="@interpret-as='cardinal'">
      	<say-as type="number:cardinal">
	      <xsl:apply-templates/>
		</say-as>
      </xsl:when>
      <xsl:when test="@interpret-as='telephone'">
      	<say-as type="telephone">
	      <xsl:apply-templates/>
		</say-as>
      </xsl:when>
      <xsl:when test="@interpret-as='number'">
          <xsl:choose>
			<xsl:when test="@format">
			  <say-as type="{@interpret-as}:{@format}">
			    <xsl:apply-templates/>
			  </say-as>
		    </xsl:when>
			<xsl:otherwise>
			  <say-as type="{@interpret-as}">
				<xsl:apply-templates/>
			  </say-as>
			</xsl:otherwise>
		  </xsl:choose>
      </xsl:when>
      <xsl:when test="@interpret-as='date'">
          <xsl:choose>
			<xsl:when test="@format">
			  <say-as type="{@interpret-as}:{@format}">
			    <xsl:apply-templates/>
			  </say-as>
		    </xsl:when>
			<xsl:otherwise>
			  <say-as type="{@interpret-as}">
				<xsl:apply-templates/>
			  </say-as>
			</xsl:otherwise>
		  </xsl:choose>
      </xsl:when>
      <xsl:when test="@interpret-as='time'">
          <xsl:choose>
			<xsl:when test="(starts-with(@format, 'hms'))">
			  <say-as type="{@interpret-as}:{@format}">
			    <xsl:apply-templates/>
			  </say-as>
		    </xsl:when>
			<xsl:otherwise>
			  <say-as type="{@interpret-as}">
				<xsl:apply-templates/>
			  </say-as>
			</xsl:otherwise>
		  </xsl:choose>
      </xsl:when>
      <xsl:otherwise>
        <xsl:apply-templates/>
      </xsl:otherwise>
    </xsl:choose>
  </xsl:template>

  
  
<!-- phoneme phoneme phoneme phoneme phoneme phoneme phoneme phoneme -->
  <xsl:template match="ssml:phoneme">
    <xsl:choose>
      <xsl:when test="@alphabet='x-sampa'">
        <t sampa="{@ph}">
          <xsl:apply-templates/>
        </t>
      </xsl:when>
      <xsl:otherwise>
        <xsl:apply-templates/>
      </xsl:otherwise>
    </xsl:choose>
  </xsl:template>


<!--  sub sub sub sub sub sub sub sub sub sub sub sub sub sub -->
  <xsl:template match="ssml:sub">
    <xsl:choose>
      <xsl:when test="@alias">
        <mtu orig="{normalize-space(.)}">
          <xsl:value-of select="@alias"/>
        </mtu>
      </xsl:when>
      <xsl:otherwise>
        <xsl:apply-templates/>
      </xsl:otherwise>
    </xsl:choose>
  </xsl:template>


  <!-- voice voice voice voice voice voice voice voice -->
  <xsl:template match="ssml:voice">
    <voice>
      <xsl:if test="@xml:lang">
        <xsl:attribute name="xml:lang">
          <xsl:value-of select="@xml:lang"/>
        </xsl:attribute>
      </xsl:if>
      <xsl:if test="@gender">
        <xsl:attribute name="gender">
          <xsl:value-of select="@gender"/>
        </xsl:attribute>
      </xsl:if>
      <xsl:if test="@age">
        <xsl:attribute name="age">
          <xsl:value-of select="@age"/>
        </xsl:attribute>
      </xsl:if>
      <xsl:if test="@variant">
        <xsl:attribute name="variant">
          <xsl:value-of select="@variant"/>
        </xsl:attribute>
      </xsl:if>
      <xsl:if test="@name">
        <xsl:attribute name="name">
          <xsl:value-of select="@name"/>
        </xsl:attribute>
      </xsl:if>

      <xsl:apply-templates/>
    </voice>
  </xsl:template>


  <!-- emphasis emphasis emphasis emphasis emphasis emphasis emphasis -->
  <xsl:template match="ssml:emphasis">
    <xsl:choose>
      <xsl:when test="@level='strong'">
        <prosody pitch="+30%" rate="-20%" force-accent="syllable">
          <phonology precision="precise">
            <xsl:apply-templates/>
          </phonology>
        </prosody>
      </xsl:when>
      <xsl:when test="@level='moderate'">
        <prosody pitch="+10%" rate="-10%" force-accent="word">
          <phonology precision="normal">
            <xsl:apply-templates/>
          </phonology>
        </prosody>
      </xsl:when>
      <xsl:when test="@level='none'">
            <phonology precision="normal">
              <prosody force-accent="none">
                <xsl:apply-templates/>
              </prosody>
            </phonology>
      </xsl:when>
      <xsl:when test="@level='reduced'">
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


  <!-- break break break break break break break break -->
  <xsl:template match="ssml:break">
    <xsl:variable name="bi">
      <xsl:choose>
        <xsl:when test="@strength='x-strong'" >6</xsl:when>
        <xsl:when test="@strength='strong'" >5</xsl:when>
        <xsl:when test="@strength='medium'">4</xsl:when>
        <xsl:when test="@strength='weak'" >3</xsl:when>
        <xsl:when test="@strength='x-weak'" >2</xsl:when>
        <xsl:when test="@strength='none'">none</xsl:when>
        <xsl:otherwise>4</xsl:otherwise>
      </xsl:choose>
    </xsl:variable>

    <xsl:variable name="duration">
      <xsl:if test="@time">
        <xsl:choose>
          <xsl:when test="substring-before(@time, 'ms')">
            <xsl:value-of select="substring-before(@time, 'ms')"/>
          </xsl:when>
          <xsl:when test="substring-before(@time, 's')">
            <xsl:value-of select="substring-before(@time, 's') * 1000"/>
          </xsl:when>
        </xsl:choose>
      </xsl:if>
    </xsl:variable>

    <xsl:variable name="tone">
      <xsl:choose>
        <xsl:when test="$bi='none'">none</xsl:when>
        <xsl:when test="$bi >= 4">unknown</xsl:when>
        <xsl:when test="$bi = 3">unknown</xsl:when>
      </xsl:choose>
    </xsl:variable>

    <xsl:if test="$tone or $bi or $duration">
      <boundary>
        <xsl:if test="$tone">
          <xsl:attribute name="tone"><xsl:value-of select="$tone"/></xsl:attribute>
        </xsl:if>
        <xsl:if test="$bi">
          <xsl:attribute name="breakindex"><xsl:value-of select="$bi"/></xsl:attribute>
        </xsl:if>
        <xsl:if test="$duration and $duration != ''">
          <xsl:attribute name="duration"><xsl:value-of select="$duration"/></xsl:attribute>
        </xsl:if>
      </boundary>
    </xsl:if>
  </xsl:template>


  <!-- prosody prosody prosody prosody prosody prosody prosody prosody -->
  <xsl:template match="ssml:prosody">
    <xsl:variable name="pitch">
      <xsl:choose>
        <xsl:when test="(starts-with(@pitch, '+') or starts-with(@pitch, '-')) and
                         (substring(@pitch, string-length(@pitch)) = '%' and
                          string(number(substring(@pitch, 2, string-length(@pitch) - 2))) != 'NaN'
                          or
                          substring(@pitch, string-length(@pitch) - 1) = 'st' and
                          string(number(substring(@pitch, 2, string-length(@pitch) - 3))) != 'NaN'
                          or
                          substring(@pitch, string-length(@pitch) - 1) = 'Hz' and
                          string(number(substring(@pitch, 2, string-length(@pitch) - 3))) != 'NaN'
                         )">
          <!-- it's a relative change, in %, semitones or Hz -->
          <xsl:value-of select="@pitch"/>
        </xsl:when>
        <xsl:when test="substring(@pitch, string-length(@pitch) - 1) = 'Hz' and
                        string(number(substring-before(@pitch, 'Hz'))) != 'NaN'">
          <!-- it's a positive number followed by Hz -->
          <xsl:value-of select="@pitch"/>
        </xsl:when>
        <xsl:when test="@pitch='x-high'" >+80%</xsl:when>
        <xsl:when test="@pitch='high'"   >+50%</xsl:when>
        <xsl:when test="@pitch='medium'" >+0%</xsl:when>
        <xsl:when test="@pitch='low'"    >-20%</xsl:when>
        <xsl:when test="@pitch='x-low'"  >-40%</xsl:when>
        <xsl:when test="@pitch='default'">+0%</xsl:when>
        <xsl:otherwise>+0%</xsl:otherwise>
      </xsl:choose>
    </xsl:variable>

    <xsl:variable name="range">
      <xsl:choose>
        <xsl:when test="(starts-with(@range, '+') or starts-with(@range, '-')) and
                         (substring(@range, string-length(@range)) = '%' and
                          string(number(substring(@range, 2, string-length(@range) - 2))) != 'NaN'
                          or
                          substring(@range, string-length(@range) - 1) = 'st' and
                          string(number(substring(@range, 2, string-length(@range) - 3))) != 'NaN'
                          or
                          substring(@range, string-length(@range) - 1) = 'Hz' and
                          string(number(substring(@range, 2, string-length(@range) - 3))) != 'NaN'
                         )">
          <!-- it's a relative change, in %, semitones or Hz -->
          <xsl:value-of select="@range"/>
        </xsl:when>
        <xsl:when test="substring(@range, string-length(@range) - 1) = 'Hz' and
                        string(number(substring-before(@range, 'Hz'))) != 'NaN'">
          <!-- it's a positive number followed by Hz -->
          <xsl:value-of select="@range"/>
        </xsl:when>
        <xsl:when test="@range='x-high'" >+80%</xsl:when>
        <xsl:when test="@range='high'"   >+50%</xsl:when>
        <xsl:when test="@range='medium'" >+0%</xsl:when>
        <xsl:when test="@range='low'"    >-20%</xsl:when>
        <xsl:when test="@range='x-low'"  >-40%</xsl:when>
        <xsl:when test="@range='default'">+0%</xsl:when>
        <xsl:otherwise>+0%</xsl:otherwise>
      </xsl:choose>
    </xsl:variable>

    <xsl:variable name="rate">
      <xsl:choose>
        <xsl:when test="(starts-with(@rate, '+') or starts-with(@rate, '-')) and
                        substring(@rate, string-length(@rate)) = '%' and
                        string(number(substring(@rate, 2, string-length(@rate) - 2))) != 'NaN'">
          <!-- it's a valid percentage number -->
          <xsl:value-of select="@rate"/>
        </xsl:when>
        <xsl:when test="@rate &gt; 0">
          <!-- it's a positive number serving as a tempo factor -->
          <!-- convert it into a percentage delta: -->
          <xsl:call-template name="factor-to-percent">
            <xsl:with-param name="factor" select="@rate"/>
          </xsl:call-template>
        </xsl:when>
        <xsl:when test="@rate='x-fast'" >+70%</xsl:when>
        <xsl:when test="@rate='fast'"   >+50%</xsl:when>
        <xsl:when test="@rate='medium'" >+0%</xsl:when>
        <xsl:when test="@rate='slow'"   >-20%</xsl:when>
        <xsl:when test="@rate='x-slow'" >-40%</xsl:when>
        <xsl:when test="@rate='default'" >+0%</xsl:when>
        <xsl:otherwise>+0%</xsl:otherwise>
      </xsl:choose>
    </xsl:variable>

    <xsl:variable name="volume">
      <xsl:choose>
        <xsl:when test="starts-with(@volume, '+') or starts-with(@volume, '-') and
                        string(number(substring(@volume, 2))) != 'NaN'">
          <!-- it's a relative change, e.g. +25 or -2.3 -->
          <xsl:value-of select="@volume"/>
        </xsl:when>
        <xsl:when test="@volume &gt;= 0">
          <!-- it's an absolute number (scale: 0 <= volume <= 100) -->
          <xsl:value-of select="@volume"/>
        </xsl:when>
        <xsl:when test="@volume='x-loud'" >100</xsl:when>
        <xsl:when test="@volume='loud'"   >75</xsl:when>
        <xsl:when test="@volume='medium'" >50</xsl:when>
        <xsl:when test="@volume='soft'"   >30</xsl:when>
        <xsl:when test="@volume='x-soft'" >15</xsl:when>
        <xsl:when test="@volume='silent'" >0</xsl:when>
        <xsl:when test="@volume='default'">50</xsl:when>
        <xsl:otherwise>50</xsl:otherwise>
      </xsl:choose>
    </xsl:variable>
    
    <!-- contour: the pitch contour -->
    <xsl:variable name="contour">
	  <xsl:value-of select="@contour"/>
	</xsl:variable>
	
    <!-- duration: the desired time to take to read the element contents. -->

    <prosody>
      <xsl:if test="@pitch">
        <xsl:attribute name="pitch">
          <xsl:value-of select="$pitch"/>
        </xsl:attribute>
      </xsl:if>
      <xsl:if test="@range">
        <xsl:attribute name="range">
          <xsl:value-of select="$range"/>
        </xsl:attribute>
      </xsl:if>
      <xsl:if test="@rate">
        <xsl:attribute name="rate">
          <xsl:value-of select="$rate"/>
        </xsl:attribute>
      </xsl:if>
      <xsl:if test="@volume">
        <xsl:attribute name="volume">
          <xsl:value-of select="$volume"/>
        </xsl:attribute>
      </xsl:if>
      <xsl:if test="@contour">
      	<xsl:attribute name="contour">
      	  <xsl:value-of select="$contour"/>
      	</xsl:attribute>
      </xsl:if>
      <xsl:apply-templates/>
    </prosody>
  </xsl:template>


  <!-- audio audio audio audio audio audio audio -->
  <xsl:template match="ssml:audio">
    <audio src="@src">
      <xsl:apply-templates/>
    </audio>
  </xsl:template>

  <!-- mark mark mark mark mark mark mark mark mark -->
  <xsl:template match="ssml:mark">
    <mark name="{@name}"/>
  </xsl:template>


  <!-- desc unimplemented -->


  <xsl:template match="text()">
    <xsl:text>&#10;</xsl:text>
    <xsl:value-of select="normalize-space(.)"/>
    <xsl:text>&#10;</xsl:text>
  </xsl:template>

  <!-- =========================================================== -->
  <!-- ======================== Helpers ========================== -->
  <!-- =========================================================== -->

  <xsl:template name="factor-to-percent">
    <xsl:param name="factor">1</xsl:param>
    <xsl:if test="$factor &gt;= 1">+</xsl:if>
    <xsl:value-of select="round(($factor - 1) * 100)"/>
    <xsl:value-of select="'%'"/>
  </xsl:template>





</xsl:stylesheet>
