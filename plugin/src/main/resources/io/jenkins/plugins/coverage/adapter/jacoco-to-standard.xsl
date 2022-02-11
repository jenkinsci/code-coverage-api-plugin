<xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform">

    <xsl:output method="xml"/>

    <xsl:key name="classname" match="class" use="substring-before(concat(@name, '$'), '$')"/>
    <xsl:key name="sourcefilename" match="class" use="@sourcefilename"/>

    <xsl:template match="/">
        <xsl:if test="/report">
            <report>
                <xsl:attribute name="name">
                    <xsl:value-of select="report/@name"/>
                </xsl:attribute>
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

    <!--template apply in package element -->
    <xsl:template match="package">
        <package>
            <xsl:attribute name="name">
                <xsl:value-of select="@name"/>
            </xsl:attribute>

            <xsl:attribute name="attr-mode">true</xsl:attribute>
            <xsl:attribute name="instruction-covered">
                <xsl:value-of select="counter[@type = 'INSTRUCTION']/@covered"/>
            </xsl:attribute>
            <xsl:attribute name="instruction-missed">
                <xsl:value-of select="counter[@type = 'INSTRUCTION']/@missed"/>
            </xsl:attribute>
            <xsl:attribute name="line-covered">
                <xsl:value-of select="counter[@type = 'LINE']/@covered"/>
            </xsl:attribute>
            <xsl:attribute name="line-missed">
                <xsl:value-of select="counter[@type = 'LINE']/@missed"/>
            </xsl:attribute>
            <xsl:if test="counter[@type = 'BRANCH']">
                <xsl:attribute name="br-covered">
                    <xsl:value-of select="counter[@type = 'BRANCH']/@covered"/>
                </xsl:attribute>
                <xsl:attribute name="br-missed">
                    <xsl:value-of select="counter[@type = 'BRANCH']/@missed"/>
                </xsl:attribute>
            </xsl:if>

            <xsl:apply-templates
                    select="class[generate-id(.)=generate-id(key('classname',substring-before(concat(@name, '$'), '$'))[1])]"/>
        </package>
    </xsl:template>


    <!--template apply in class element -->
    <xsl:template match="class">
        <xsl:choose>
            <!--if element has sourcefilename attribute, they will be grouped by this attribute-->
            <xsl:when test="@sourcefilename">
                <xsl:variable name="sourcefilename" select="@sourcefilename"/>

                <file name="{$sourcefilename}">
                    <xsl:for-each select="key('sourcefilename', $sourcefilename)">
                        <class>
                            <xsl:attribute name="name">
                                <xsl:value-of select="@name"/>
                            </xsl:attribute>

                            <xsl:attribute name="attr-mode">true</xsl:attribute>
                            <xsl:attribute name="instruction-covered">
                                <xsl:value-of select="counter[@type = 'INSTRUCTION']/@covered"/>
                            </xsl:attribute>
                            <xsl:attribute name="instruction-missed">
                                <xsl:value-of select="counter[@type = 'INSTRUCTION']/@missed"/>
                            </xsl:attribute>
                            <xsl:attribute name="line-covered">
                                <xsl:value-of select="counter[@type = 'LINE']/@covered"/>
                            </xsl:attribute>
                            <xsl:attribute name="line-missed">
                                <xsl:value-of select="counter[@type = 'LINE']/@missed"/>
                            </xsl:attribute>
                            <xsl:if test="counter[@type = 'BRANCH']">
                                <xsl:attribute name="br-covered">
                                    <xsl:value-of select="counter[@type = 'BRANCH']/@covered"/>
                                </xsl:attribute>
                                <xsl:attribute name="br-missed">
                                    <xsl:value-of select="counter[@type = 'BRANCH']/@missed"/>
                                </xsl:attribute>
                            </xsl:if>

                            <xsl:apply-templates select="method"/>

                        </class>
                    </xsl:for-each>

                    <xsl:for-each select="../sourcefile[@name = $sourcefilename]/line">
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
                                            select="concat(./@cb, '/', number(./@mb) + number(./@cb),')')"/>
                                    </xsl:attribute>
                                </xsl:when>
                                <xsl:otherwise>
                                    <xsl:attribute name="branch">false</xsl:attribute>
                                </xsl:otherwise>
                            </xsl:choose>
                        </line>
                    </xsl:for-each>
                </file>
            </xsl:when>
            <!--otherwise they will be grouped by their class name-->
            <xsl:otherwise>
                <xsl:variable name="classname"
                              select="substring-after(substring-before(concat(@name, '$'), '$'), concat(../@name, '/'))"/>

                <xsl:variable name="sourcefile" select="../sourcefile[starts-with(@name, concat($classname, '.'))]"/>
                <xsl:choose>
                    <xsl:when test="$sourcefile">
                        <file name="{substring-before(concat(@name, '$'), '$')}.java">
                            <xsl:attribute name="name">
                                <xsl:value-of select="$sourcefile/@name"/>
                            </xsl:attribute>

                            <xsl:for-each select="key('classname',substring-before(concat(@name, '$'), '$'))">
                                <class>
                                    <xsl:attribute name="name">
                                        <xsl:value-of select="@name"/>
                                    </xsl:attribute>

                                    <xsl:attribute name="attr-mode">true</xsl:attribute>
                                    <xsl:attribute name="instruction-covered">
                                        <xsl:value-of select="counter[@type = 'INSTRUCTION']/@covered"/>
                                    </xsl:attribute>
                                    <xsl:attribute name="instruction-missed">
                                        <xsl:value-of select="counter[@type = 'INSTRUCTION']/@missed"/>
                                    </xsl:attribute>
                                    <xsl:attribute name="line-covered">
                                        <xsl:value-of select="counter[@type = 'LINE']/@covered"/>
                                    </xsl:attribute>
                                    <xsl:attribute name="line-missed">
                                        <xsl:value-of select="counter[@type = 'LINE']/@missed"/>
                                    </xsl:attribute>
                                    <xsl:if test="counter[@type = 'BRANCH']">
                                        <xsl:attribute name="br-covered">
                                            <xsl:value-of select="counter[@type = 'BRANCH']/@covered"/>
                                        </xsl:attribute>
                                        <xsl:attribute name="br-missed">
                                            <xsl:value-of select="counter[@type = 'BRANCH']/@missed"/>
                                        </xsl:attribute>
                                    </xsl:if>

                                    <xsl:apply-templates select="method"/>
                                </class>
                            </xsl:for-each>

                            <xsl:for-each select="$sourcefile/line">
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
                                                    select="concat(./@cb, '/', number(./@mb) + number(./@cb),')')"/>
                                            </xsl:attribute>
                                        </xsl:when>
                                        <xsl:otherwise>
                                            <xsl:attribute name="branch">false</xsl:attribute>
                                        </xsl:otherwise>
                                    </xsl:choose>
                                </line>
                            </xsl:for-each>
                        </file>
                    </xsl:when>
                    <xsl:otherwise>
                        <file name="{$classname}.java">
                            <xsl:for-each select="key('classname',substring-before(concat(@name, '$'), '$'))">
                                <class>
                                    <xsl:attribute name="name">
                                        <xsl:value-of select="@name"/>
                                    </xsl:attribute>

                                    <xsl:attribute name="attr-mode">true</xsl:attribute>
                                    <xsl:attribute name="instruction-covered">
                                        <xsl:value-of select="counter[@type = 'INSTRUCTION']/@covered"/>
                                    </xsl:attribute>
                                    <xsl:attribute name="instruction-missed">
                                        <xsl:value-of select="counter[@type = 'INSTRUCTION']/@missed"/>
                                    </xsl:attribute>
                                    <xsl:attribute name="line-covered">
                                        <xsl:value-of select="counter[@type = 'LINE']/@covered"/>
                                    </xsl:attribute>
                                    <xsl:attribute name="line-missed">
                                        <xsl:value-of select="counter[@type = 'LINE']/@missed"/>
                                    </xsl:attribute>
                                    <xsl:if test="counter[@type = 'BRANCH']">
                                        <xsl:attribute name="br-covered">
                                            <xsl:value-of select="counter[@type = 'BRANCH']/@covered"/>
                                        </xsl:attribute>
                                        <xsl:attribute name="br-missed">
                                            <xsl:value-of select="counter[@type = 'BRANCH']/@missed"/>
                                        </xsl:attribute>
                                    </xsl:if>

                                    <xsl:apply-templates select="method"/>
                                </class>
                            </xsl:for-each>
                        </file>
                    </xsl:otherwise>
                </xsl:choose>
            </xsl:otherwise>
        </xsl:choose>
    </xsl:template>

    <!--template apply in method element -->
    <xsl:template match="method">
        <method>
            <xsl:attribute name="name">
                <xsl:value-of select="@name"/>
            </xsl:attribute>
            <xsl:attribute name="signature">
                <xsl:value-of select="@desc"/>
            </xsl:attribute>
            <xsl:attribute name="attr-mode">true</xsl:attribute>
            <xsl:attribute name="instruction-covered">
                <xsl:value-of select="counter[@type = 'INSTRUCTION']/@covered"/>
            </xsl:attribute>
            <xsl:attribute name="instruction-missed">
                <xsl:value-of select="counter[@type = 'INSTRUCTION']/@missed"/>
            </xsl:attribute>
            <xsl:attribute name="line-covered">
                <xsl:value-of select="counter[@type = 'LINE']/@covered"/>
            </xsl:attribute>
            <xsl:attribute name="line-missed">
                <xsl:value-of select="counter[@type = 'LINE']/@missed"/>
            </xsl:attribute>
            <xsl:if test="counter[@type = 'BRANCH']">
                <xsl:attribute name="br-covered">
                    <xsl:value-of select="counter[@type = 'BRANCH']/@covered"/>
                </xsl:attribute>
                <xsl:attribute name="br-missed">
                    <xsl:value-of select="counter[@type = 'BRANCH']/@missed"/>
                </xsl:attribute>
            </xsl:if>

        </method>
    </xsl:template>

</xsl:stylesheet>