# code-coverage-api-plugin
(This plugin is now under development)

This plugin serves as API to integrate and publish multiple coverage report types.

## How to use it

Code Coverage API plugin now supports Cobertura and Jacoco.

#### Config maven to generate coverage reports.

For **Cobertura**
```xml
<build>
    <plugins>
        <plugin>
            <groupId>org.codehaus.mojo</groupId>
            <artifactId>cobertura-maven-plugin</artifactId>
            <version>2.7</version>
            <configuration>
                <formats>
                    <format>xml</format>
                </formats>
                <check/>
            </configuration>
            <executions>
                <execution>
                    <phase>package</phase>
                    <goals>
                        <goal>cobertura</goal>
                    </goals>
                </execution>
            </executions>
        </plugin>
    </plugins>
</build>
```

For **Jacoco**
```xml
<build>
    <plugins>
        <plugin>
            <groupId>org.jacoco</groupId>
            <artifactId>jacoco-maven-plugin</artifactId>
            <version>0.8.1</version>
            <executions>
                <execution>
                    <goals>
                        <goal>prepare-agent</goal>
                    </goals>
                </execution>
                <execution>
                    <id>report</id>
                    <phase>package</phase>
                    <goals>
                        <goal>report</goal>
                    </goals>
                </execution>
            </executions>
        </plugin>
    </plugins>
</build>
```

Then plugin will automatically find reports (Only Support Cobertura now) according to your auto detect path.

Also, we can specify report path for each coverage tool.

For more details, see the [Introduction Blogpost](https://jenkins.io/blog/2018/06/13/code-coverage-api-plugin/).

