mvn -pl spring-ai/spring-ai-agent clean package org.apache.maven.plugins:maven-archetype-plugin:3.4.1:create-from-project \
-Darchetype.keepParent=false \
-Dinteractive \
-Darchetype.properties=archetype.properties

# copy src directory
rm -rf spring-ai/spring-ai-agent-archetype/src/main/resources/archetype-resources/src
cp -r spring-ai/spring-ai-agent/target/generated-sources/archetype/src/main/resources/archetype-resources/src \
spring-ai/spring-ai-agent-archetype/src/main/resources/archetype-resources/src
# copy pom.xml
cp -r spring-ai/spring-ai-agent/target/generated-sources/archetype/src/main/resources/archetype-resources/pom.xml \
spring-ai/spring-ai-agent-archetype/src/main/resources/archetype-resources/
# copy README.md
cp -r spring-ai/spring-ai-agent/target/generated-sources/archetype/src/main/resources/archetype-resources/README.md \
spring-ai/spring-ai-agent-archetype/src/main/resources/archetype-resources/
# replace wrong import
find spring-ai/spring-ai-agent-archetype/src/main/resources/archetype-resources/src -type f -name "*.java" \
-exec sed -i '' \
-e 's/import static \${package}\./import static org.bsc.langgraph4j./g' \
-e 's/import \${package}\./import org.bsc.langgraph4j./g' {} +