<?xml version="1.0" encoding="UTF-8" ?>
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
                version="1.0"
                xmlns="http://mary.dfki.de/2002/MaryXML"
                xmlns:emo="http://www.w3.org/2009/10/emotionml"
                xmlns:helpers="xalan://emotionml.xslt.Helpers"
                exclude-result-prefixes="emo helpers">
  <xsl:output method="xml"
              encoding="UTF-8"
              indent="yes"/>
  <xsl:strip-space elements="*"/>
  <xsl:param name="voice" select="'unknown'" />

  <!-- Root node -->
  <xsl:template match="/emo:emotionml">
    <xsl:variable name="language">
      <xsl:choose>
        <xsl:when test="@xml:lang"><xsl:value-of select="@xml:lang"/></xsl:when>
        <xsl:otherwise>de</xsl:otherwise>
      </xsl:choose>
    </xsl:variable>
    <maryxml version="0.5" xml:lang="{$language}">
       <xsl:apply-templates/>
    </maryxml>
  </xsl:template>

<!-- *************************************************** -->
<!-- ***************** Emotion categories ************** -->
<!-- *************************************************** -->

<!-- Use the "style" attribute of MaryXML's <prosody> attribute -->
<!-- to express emotion categories. Works only with certain voices -->
<!-- (as of 2012, dfki-pavoque-styles) -->
<xsl:template match="emo:emotion[emo:category and helpers:expressedThrough(., 'voice', true())]">
  <xsl:variable name="cat" select="emo:category/@name"/>
  <xsl:choose>
    <xsl:when test="$voice='dfki-pavoque-styles'">
	  <xsl:variable name="style">
	    <xsl:choose>
	      <xsl:when test="helpers:declaredCategoryVocabulary(.) = 'http://www.w3.org/TR/emotion-voc/xml#big6'">
	        <xsl:choose>
	          <xsl:when test="$cat='anger'">angry</xsl:when>
	          <xsl:when test="$cat='disgust'">angry</xsl:when>
	          <xsl:when test="$cat='fear'">neutral</xsl:when>
	          <xsl:when test="$cat='happiness'">happy</xsl:when>
	          <xsl:when test="$cat='sadness'">sad</xsl:when>
	          <xsl:when test="$cat='surprise'">neutral</xsl:when>
	        </xsl:choose>
	      </xsl:when>
	      <xsl:when test="helpers:declaredCategoryVocabulary(.) = 'http://www.w3.org/TR/emotion-voc/xml#everyday-categories'">
	        <xsl:choose>
	          <xsl:when test="$cat='affectionate'">happy</xsl:when>
	          <xsl:when test="$cat='afraid'">neutral</xsl:when>
	          <xsl:when test="$cat='amused'">happy</xsl:when>
	          <xsl:when test="$cat='angry'">angry</xsl:when>
	          <xsl:when test="$cat='bored'">sad</xsl:when>
	          <xsl:when test="$cat='confident'">poker</xsl:when>
	          <xsl:when test="$cat='content'">poker</xsl:when>
	          <xsl:when test="$cat='disappointed'">sad</xsl:when>
	          <xsl:when test="$cat='excited'">happy</xsl:when>
	          <xsl:when test="$cat='happy'">happy</xsl:when>
	          <xsl:when test="$cat='interested'">happy</xsl:when>
	          <xsl:when test="$cat='loving'">happy</xsl:when>
	          <xsl:when test="$cat='pleased'">happy</xsl:when>
	          <xsl:when test="$cat='relaxed'">poker</xsl:when>
	          <xsl:when test="$cat='sad'">sad</xsl:when>
	          <xsl:when test="$cat='satisfied'">happy</xsl:when>
	          <xsl:when test="$cat='worried'">sad</xsl:when>
	        </xsl:choose>
	      </xsl:when>
	    </xsl:choose>
	  </xsl:variable>
	  <xsl:choose>
	    <xsl:when test="$style">
	      <prosody style="{$style}">
	        <xsl:apply-templates/>
	      </prosody>
	    </xsl:when>
	    <xsl:otherwise>
	      <xsl:apply-templates/>
	    </xsl:otherwise>
	  </xsl:choose>
    </xsl:when>
    <xsl:otherwise><!-- A voice without different styles -->
      <xsl:variable name="arousal">
	    <xsl:choose>
	      <xsl:when test="helpers:declaredCategoryVocabulary(.) = 'http://www.w3.org/TR/emotion-voc/xml#big6'">
	        <xsl:choose>
	          <xsl:when test="$cat='anger'">0.9</xsl:when>
	          <xsl:when test="$cat='disgust'">0.8</xsl:when>
	          <xsl:when test="$cat='fear'">0.8</xsl:when>
	          <xsl:when test="$cat='happiness'">0.6</xsl:when>
	          <xsl:when test="$cat='sadness'">0.2</xsl:when>
	          <xsl:when test="$cat='surprise'">0.7</xsl:when>
	        </xsl:choose>
	      </xsl:when>
	      <xsl:when test="helpers:declaredCategoryVocabulary(.) = 'http://www.w3.org/TR/emotion-voc/xml#everyday-categories'">
	        <xsl:choose>
	          <xsl:when test="$cat='affectionate'">0.4</xsl:when>
	          <xsl:when test="$cat='afraid'">0.3</xsl:when>
	          <xsl:when test="$cat='amused'">0.5</xsl:when>
	          <xsl:when test="$cat='angry'">0.9</xsl:when>
	          <xsl:when test="$cat='bored'">0.1</xsl:when>
	          <xsl:when test="$cat='confident'">0.6</xsl:when>
	          <xsl:when test="$cat='content'">0.4</xsl:when>
	          <xsl:when test="$cat='disappointed'">0.3</xsl:when>
	          <xsl:when test="$cat='excited'">0.8</xsl:when>
	          <xsl:when test="$cat='happy'">0.6</xsl:when>
	          <xsl:when test="$cat='interested'">0.6</xsl:when>
	          <xsl:when test="$cat='loving'">0.5</xsl:when>
	          <xsl:when test="$cat='pleased'">0.5</xsl:when>
	          <xsl:when test="$cat='relaxed'">0.4</xsl:when>
	          <xsl:when test="$cat='sad'">0.2</xsl:when>
	          <xsl:when test="$cat='satisfied'">0.4</xsl:when>
	          <xsl:when test="$cat='worried'">0.2</xsl:when>
	        </xsl:choose>
	      </xsl:when>
	      <xsl:otherwise>0.5</xsl:otherwise>
	    </xsl:choose>
      </xsl:variable>
      <xsl:call-template name="emotionmlDimensions">
        <xsl:with-param name="arousal" select="$arousal"/>
        <xsl:with-param name="pleasure" select="0.5"/>
        <xsl:with-param name="dominance" select="0.5"/>
      </xsl:call-template>
    </xsl:otherwise>
  </xsl:choose>
</xsl:template>

<!-- *************************************************** -->
<!-- ***************** Emotion dimensions ************** -->
<!-- *************************************************** -->

<!-- Use the Schroeder 2003 rules for rendering a position in three-dimensional emotion space.  -->
<!-- Using the PAD vocabulary as the standard three-dimensional vocabulary. -->
<xsl:template match="emo:emotion[emo:dimension]">
  <xsl:choose>
    <xsl:when test="helpers:declaredDimensionVocabulary(.) = 'http://www.w3.org/TR/emotion-voc/xml#pad-dimensions'">
      <xsl:variable name="arousal" select="emo:dimension[@name='arousal']/@value" />
      <xsl:variable name="pleasure" select="emo:dimension[@name='pleasure']/@value" />
      <xsl:variable name="dominance" select="emo:dimension[@name='dominance']/@value" />
      <xsl:call-template name="emotionmlDimensions">
        <xsl:with-param name="arousal" select="$arousal"/>
        <xsl:with-param name="pleasure" select="$pleasure"/>
        <xsl:with-param name="dominance" select="$dominance"/>
      </xsl:call-template>
    </xsl:when>
    <xsl:when test="helpers:declaredDimensionVocabulary(.) = 'http://www.w3.org/TR/emotion-voc/xml#fsre-dimensions'">
      <xsl:variable name="arousal" select="emo:dimension[@name='arousal']/@value" />
      <xsl:variable name="pleasure" select="emo:dimension[@name='valence']/@value" />
      <xsl:variable name="dominance" select="emo:dimension[@name='potency']/@value" />
      <!-- ignoring 'unpredictability' dimension -->
      <xsl:call-template name="emotionmlDimensions">
        <xsl:with-param name="arousal" select="$arousal"/>
        <xsl:with-param name="pleasure" select="$pleasure"/>
        <xsl:with-param name="dominance" select="$dominance"/>
      </xsl:call-template>
    </xsl:when>
    <xsl:otherwise>
      <xsl:apply-templates/>
    </xsl:otherwise>
  </xsl:choose>
</xsl:template>

  <xsl:template name="emotionmlDimensions">
    <!-- These parameters are normalised from 0.0 to 1.0: -->
    <xsl:param name="arousal"/>
    <xsl:param name="pleasure"/>
    <xsl:param name="dominance"/>
    <xsl:call-template name="acousticParameters">
      <!-- Convert range [0, 1] to range [-100, 100]: -->
      <xsl:with-param name="activation" select="$arousal * 200 - 100"/>
      <xsl:with-param name="evaluation" select="$pleasure * 200 - 100"/>
      <xsl:with-param name="power" select="$dominance * 200 - 100"/>
    </xsl:call-template>
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
    
    
    <xsl:variable name="globalslope" select="round(0.12*$power)"/>
    
    <xsl:variable name="contour" select="concat('(0%,', format-number($globalslope, '+#;-#'), 'st)',
                                              '(100%,', format-number(-$globalslope, '+#;-#'), 'st)')"/>

	<!-- Prosody attributes which cannot be realized with 2012 voices commented out -->
	<p>
      <prosody>
        <xsl:attribute name="pitch"><xsl:value-of select="$pitch"/>%</xsl:attribute>
<!--         <xsl:attribute name="pitch-dynamics"><xsl:value-of select="$pitch-dynamics"/>%</xsl:attribute> -->
<!--         <xsl:attribute name="range"><xsl:value-of select="$range"/>st</xsl:attribute> -->
<!--         <xsl:attribute name="range-dynamics"><xsl:value-of select="$range-dynamics"/>%</xsl:attribute> -->
<!--         <xsl:attribute name="preferred-accent-shape"><xsl:value-of select="$preferred-accent-shape"/></xsl:attribute> -->
<!--         <xsl:attribute name="accent-slope"><xsl:value-of select="$accent-slope"/>%</xsl:attribute> -->
<!--         <xsl:attribute name="accent-prominence"><xsl:value-of select="$accent-prominence"/>%</xsl:attribute> -->
<!--         <xsl:attribute name="preferred-boundary-type"><xsl:value-of select="$preferred-boundary-type"/></xsl:attribute> -->
        <xsl:attribute name="rate"><xsl:value-of select="$rate"/>%</xsl:attribute>
<!--         <xsl:attribute name="number-of-pauses"><xsl:value-of select="$number-of-pauses"/>%</xsl:attribute> -->
<!--         <xsl:attribute name="pause-duration"><xsl:value-of select="$pause-duration"/>%</xsl:attribute> -->
<!--         <xsl:attribute name="vowel-duration"><xsl:value-of select="$vowel-duration"/>%</xsl:attribute> -->
<!--         <xsl:attribute name="nasal-duration"><xsl:value-of select="$nasal-duration"/>%</xsl:attribute> -->
<!--         <xsl:attribute name="liquid-duration"><xsl:value-of select="$liquid-duration"/>%</xsl:attribute> -->
<!--         <xsl:attribute name="plosive-duration"><xsl:value-of select="$plosive-duration"/>%</xsl:attribute> -->
<!--         <xsl:attribute name="fricative-duration"><xsl:value-of select="$fricative-duration"/>%</xsl:attribute> -->
<!--         <xsl:attribute name="volume"><xsl:value-of select="$volume"/></xsl:attribute> -->
		<xsl:attribute name="contour"><xsl:value-of select="$contour"/></xsl:attribute>
        <xsl:apply-templates/>
      </prosody>
    </p>
  </xsl:template>

  <xsl:template match="text()">
    <xsl:value-of select="."/>
  </xsl:template>

</xsl:stylesheet>
