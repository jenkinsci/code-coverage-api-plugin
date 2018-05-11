<xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform">

    <xsl:output method="xml"/>

    <xsl:key name="filename" match="class" use="@filename"/>

    <xsl:template match="/">
        <report>
            <xsl:attribute name="name">cobertura</xsl:attribute>
            <group>
                <xsl:apply-templates select="coverage/packages/package"/>
            </group>
        </report>
    </xsl:template>


    <xsl:template match="coverage/packages/package">
        <package>
            <xsl:attribute name="name">
                <xsl:value-of select="@name"/>
            </xsl:attribute>
            <xsl:apply-templates select="classes/class[generate-id(.)=generate-id(key('filename',@filename)[1])]"/>
        </package>
    </xsl:template>

    <xsl:template match="classes/class">
        <file name="{@filename}">
            <xsl:for-each select="key('filename', @filename)">
                <class>
                    <xsl:attribute name="name">
                        <xsl:value-of select="@name"/>
                    </xsl:attribute>
                    <xsl:apply-templates select="methods/method"/>
                    <xsl:apply-templates select="lines/line"/>
                </class>
            </xsl:for-each>
        </file>
    </xsl:template>

    <xsl:template match="methods/method">
        <method>
            <xsl:attribute name="name">
                <xsl:value-of select="@name"/>
            </xsl:attribute>
            <xsl:attribute name="signature">
                <xsl:value-of select="@signature"/>
            </xsl:attribute>
            <xsl:apply-templates select="lines/line"/>
        </method>
    </xsl:template>


    <xsl:template match="lines/line">
        <xsl:copy>
            <xsl:copy-of select="@*"/>
        </xsl:copy>
    </xsl:template>
</xsl:stylesheet>