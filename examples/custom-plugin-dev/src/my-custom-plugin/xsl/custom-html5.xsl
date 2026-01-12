<?xml version="1.0" encoding="UTF-8"?>
<!--
  Custom HTML5 XSLT Overrides

  This stylesheet extends the default html5 processing with custom enhancements.
  Add your custom templates here to modify the HTML output.
-->
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
                xmlns:xs="http://www.w3.org/2001/XMLSchema"
                xmlns:dita-ot="http://dita-ot.sourceforge.net/ns/201007/dita-ot"
                version="2.0"
                exclude-result-prefixes="xs dita-ot">

  <!-- Add custom class to body element -->
  <xsl:template match="*" mode="addBodyClass">
    <xsl:param name="class" select="''" as="xs:string"/>
    <xsl:value-of select="concat($class, ' my-custom-theme')"/>
  </xsl:template>

  <!-- Custom note styling -->
  <xsl:template match="*[contains(@class, ' topic/note ')]" mode="note-class">
    <xsl:text>custom-note</xsl:text>
  </xsl:template>

  <!-- Add custom header content -->
  <xsl:template name="custom-header">
    <div class="custom-header">
      <p>Generated with My Custom Plugin</p>
    </div>
  </xsl:template>

</xsl:stylesheet>
