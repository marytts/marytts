<?xml version="1.0" encoding="UTF-8" ?>
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
                version="2.0"
                xmlns="http://mary.dfki.de/2002/MaryXML"
                xmlns:emo="http://www.w3.org/2009/10/emotionml"
                xmlns:func="http://mary.dfki.de/2002/MaryXML/function"
                exclude-result-prefixes="emo func">
  <xsl:output method="xml"
              encoding="UTF-8"
              indent="yes"/>
  <xsl:strip-space elements="*"/>


  <!-- Helper functions for more readable code -->
  <xsl:function name="func:declaredCategoryVocabulary">
      <xsl:param name="emotion" />
      <xsl:choose>
        <xsl:when test="$emotion/@category-set">
          <xsl:value-of select="$emotion/@category-set"/>
        </xsl:when>
        <xsl:otherwise>
          <xsl:value-of select="$emotion/ancestor::emo:emotionml/@category-set"/>
        </xsl:otherwise>
      </xsl:choose>
  </xsl:function>
    
  <xsl:function name="func:declaredDimensionVocabulary">
      <xsl:param name="emotion" />
      <xsl:choose>
        <xsl:when test="$emotion/@dimension-set">
          <xsl:value-of select="$emotion/@dimension-set"/>
        </xsl:when>
        <xsl:otherwise>
          <xsl:value-of select="$emotion/ancestor::emo:emotionml/@dimension-set"/>
        </xsl:otherwise>
      </xsl:choose>
  </xsl:function>
  
  <xsl:function name="func:expressedThroughVoice">
    <xsl:param name="emotion"/>
    <xsl:choose>
      <xsl:when test="not($emotion/@expressed-through)">
        <!-- No expressed-through attribute = no restriction -->
        <xsl:value-of select="true()"/>
      </xsl:when>
      <xsl:when test="index-of(tokenize($emotion/@expressed-through, '\s+'), 'voice')">
        <!-- voice is one of the listed modalities -->
        <xsl:value-of select="true()"/>
      </xsl:when>
      <xsl:otherwise>
        <xsl:value-of select="false()"/>
      </xsl:otherwise>
    </xsl:choose>
  </xsl:function>

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
<xsl:template match="emo:emotion[emo:category and func:expressedThroughVoice(.)=true()]">
  <xsl:variable name="cat" select="emo:category/@name"/>
  <xsl:variable name="style">
    <xsl:choose>
      <xsl:when test="func:declaredCategoryVocabulary(.) = 'http://www.w3.org/TR/emotion-voc/xml#big6'">
        <xsl:choose>
          <xsl:when test="$cat='anger'">angry</xsl:when>
          <xsl:when test="$cat='disgust'">angry</xsl:when>
          <xsl:when test="$cat='fear'">neutral</xsl:when>
          <xsl:when test="$cat='happiness'">happy</xsl:when>
          <xsl:when test="$cat='sadness'">sad</xsl:when>
          <xsl:when test="$cat='surprise'">neutral</xsl:when>
        </xsl:choose>
      </xsl:when>
      <xsl:when test="func:declaredCategoryVocabulary(.) = 'http://www.w3.org/TR/emotion-voc/xml#everyday-emotions'">
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
</xsl:template>

<!-- *************************************************** -->
<!-- ***************** Emotion dimensions ************** -->
<!-- *************************************************** -->

<!-- Use the Schroeder 2003 rules for rendering a position in three-dimensional emotion space.  -->
<!-- Using the PAD vocabulary as the standard three-dimensional vocabulary. -->
<xsl:template match="emo:emotion[emo:dimension]">
  <xsl:choose>
    <xsl:when test="func:declaredDimensionVocabulary(.) = 'http://www.w3.org/TR/emotion-voc/xml#pad-dimensions'">
      <!-- Convert range [0, 1] to range [-100, 100]: -->
      <xsl:variable name="activation" select="emo:dimension[@name='arousal']/@value * 200 - 100" />
      <xsl:variable name="evaluation" select="emo:dimension[@name='pleasure']/@value * 200 - 100" />
      <xsl:variable name="power" select="emo:dimension[@name='dominance']/@value * 200 - 100" />
      <xsl:call-template name="acousticParameters">
        <xsl:with-param name="activation" select="$activation"/>
        <xsl:with-param name="evaluation" select="$evaluation"/>
        <xsl:with-param name="power" select="$power"/>
      </xsl:call-template>
    </xsl:when>
    <xsl:otherwise>
      <xsl:apply-templates/>
    </xsl:otherwise>
  </xsl:choose>
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

	<!-- Prosody attributes which cannot be realized with 2012 voices commented out -->
    <prosody>
        <xsl:attribute name="pitch"><xsl:value-of select="$pitch"/>%</xsl:attribute>
<!--         <xsl:attribute name="pitch-dynamics"><xsl:value-of select="$pitch-dynamics"/>%</xsl:attribute> -->
        <xsl:attribute name="range"><xsl:value-of select="$range"/>st</xsl:attribute>
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
      <xsl:apply-templates/>
    </prosody>
  </xsl:template>

  <xsl:template match="text()">
    <xsl:value-of select="."/>
  </xsl:template>

</xsl:stylesheet>
