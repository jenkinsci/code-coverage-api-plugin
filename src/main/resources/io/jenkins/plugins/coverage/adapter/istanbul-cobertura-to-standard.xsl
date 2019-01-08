<xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform">

    <xsl:output method="xml"/>

    <xsl:template match="/">
        <report>
            <xsl:attribute name="name">istanbul</xsl:attribute>
            <xsl:apply-templates select="coverage/packages/package"/>
        </report>
    </xsl:template>


    <xsl:template match="coverage/packages/package">
        <directory>
            <xsl:attribute name="name">
                <xsl:value-of select="@name"/>
            </xsl:attribute>
            <xsl:apply-templates select="classes/class"/>
        </directory>
    </xsl:template>

    <xsl:template match="classes/class">
        <file name="{@filename}">
            <xsl:apply-templates select="methods/method"/>
            <xsl:apply-templates select="lines/line"/>
        </file>
    </xsl:template>

    <xsl:template match="methods/method">
        <function>
            <xsl:attribute name="name">
                <xsl:value-of select="@name"/>
            </xsl:attribute>
            <xsl:attribute name="signature">
                <xsl:value-of select="@signature"/>
            </xsl:attribute>
            <xsl:apply-templates select="lines/line"/>
        </function>
    </xsl:template>


    <xsl:template match="lines/line">
        <xsl:copy>
            <xsl:copy-of select="@*"/>
        </xsl:copy>
    </xsl:template>
</xsl:stylesheet>