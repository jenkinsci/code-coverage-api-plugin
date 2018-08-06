<xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform">

    <xsl:output method="xml"/>

    <xsl:key name="filename" match="class" use="substring-before(concat(@name, '$'), '$')"/>


    <xsl:template match="/">
        <xsl:if test="/report">
            <report>
                <xsl:attribute name="name">jacoco</xsl:attribute>
                <xsl:choose>
                    <xsl:when test="/report/group">
                        <xsl:apply-templates select="report/group"/>
                    </xsl:when>
                    <xsl:otherwise>
                        <group>
                            <xsl:attribute name="name">project</xsl:attribute>
                            <xsl:apply-templates select="report/package"/>
                        </group>
                    </xsl:otherwise>
                </xsl:choose>
            </report>
        </xsl:if>
    </xsl:template>

    <xsl:template match="/report/group">
        <group>
            <xsl:attribute name="name">
                <xsl:value-of select="@name"/>
            </xsl:attribute>
            <xsl:apply-templates select="package"/>
        </group>
    </xsl:template>

    <xsl:template match="package">
        <package>
            <xsl:attribute name="name">
                <xsl:value-of select="@name"/>
            </xsl:attribute>
            <xsl:apply-templates
                    select="class[generate-id(.)=generate-id(key('filename',substring-before(concat(@name, '$'), '$'))[1])]"/>
        </package>
    </xsl:template>


    <xsl:template match="class">
        <file name="{substring-before(concat(@name, '$'), '$')}.java">
            <xsl:for-each select="key('filename',substring-before(concat(@name, '$'), '$'))">
                <class>
                    <xsl:attribute name="name">
                        <xsl:value-of select="@name"/>
                    </xsl:attribute>


                    <xsl:apply-templates select="method"/>

                    <xsl:variable name="filename"
                                  select="substring-after(concat(substring-before(concat(@name, '$'), '$'),'.java'), concat(parent::package/@name, '/'))"/>
                    <xsl:if test="count(child::method) != 0">
                        <xsl:variable name="start" select="method/@line[not(. > ../method/@line)][1]"/>
                        <xsl:variable name="lines"
                                      select="number(counter[type = LINE]/@missed) + number(counter[type = LINE]/@covered)"/>
                        <xsl:for-each select="../sourcefile[@name = $filename]/line[@nr >= $start]">
                            <xsl:if test="not (position() > $lines)">
                                <line>
                                    <xsl:attribute name="number">
                                        <xsl:value-of select="./@nr"/>
                                    </xsl:attribute>
                                    <xsl:attribute name="hits">
                                        <xsl:choose>
                                            <xsl:when test="./@ci > 0">1</xsl:when>
                                            <xsl:otherwise>0</xsl:otherwise>
                                        </xsl:choose>
                                    </xsl:attribute>
                                    <xsl:choose>
                                        <xsl:when test="number(./@mb) + number(./@cb) > 0 ">
                                            <xsl:attribute name="branch">true</xsl:attribute>
                                            <xsl:variable name="percentage"
                                                          select="number(./@cb) div (number(./@cb) + number(./@mb))"/>
                                            <xsl:attribute name="condition-coverage">
                                                <xsl:value-of select="concat($percentage, ' (')"/><xsl:value-of
                                                    select="concat(./@cb, '/', ./@mb,')')"/>
                                            </xsl:attribute>

                                        </xsl:when>
                                        <xsl:otherwise>
                                            <xsl:attribute name="branch">false</xsl:attribute>
                                        </xsl:otherwise>
                                    </xsl:choose>
                                </line>
                            </xsl:if>
                        </xsl:for-each>
                    </xsl:if>
                </class>
            </xsl:for-each>
        </file>
    </xsl:template>


    <xsl:template match="method">
        <method>
            <xsl:attribute name="name">
                <xsl:value-of select="@name"/>
            </xsl:attribute>
            <xsl:attribute name="signature">
                <xsl:value-of select="@desc"/>
            </xsl:attribute>
            <xsl:variable name="filename"
                          select="substring-after(concat(substring-before(concat(parent::class/@name, '$'), '$'),'.java'), concat(../../@name, '/'))"/>


            <xsl:variable name="start" select="@line"/>
            <xsl:variable name="lines"
                          select="number(counter[type = LINE]/@missed) + number(counter[type = LINE]/@covered)"/>
            <xsl:for-each select="../../sourcefile[@name = $filename]/line[@nr >= $start]">
                <xsl:if test="not (position() > $lines)">
                    <line>
                        <xsl:attribute name="number">
                            <xsl:value-of select="./@nr"/>
                        </xsl:attribute>
                        <xsl:attribute name="hits">
                            <xsl:choose>
                                <xsl:when test="./@ci > 0">1</xsl:when>
                                <xsl:otherwise>0</xsl:otherwise>
                            </xsl:choose>
                        </xsl:attribute>
                        <xsl:choose>
                            <xsl:when test="number(./@mb) + number(./@cb) > 0 ">
                                <xsl:attribute name="branch">true</xsl:attribute>
                                <xsl:variable name="percentage"
                                              select="number(./@cb) div (number(./@cb) + number(./@mb))"/>
                                <xsl:attribute name="condition-coverage">
                                    <xsl:value-of select="concat($percentage * 100, '% (')"/><xsl:value-of
                                        select="concat(./@cb, '/', ./@mb,')')"/>
                                </xsl:attribute>

                            </xsl:when>
                            <xsl:otherwise>
                                <xsl:attribute name="branch">false</xsl:attribute>
                            </xsl:otherwise>
                        </xsl:choose>
                    </line>
                </xsl:if>
            </xsl:for-each>
        </method>
    </xsl:template>


</xsl:stylesheet>