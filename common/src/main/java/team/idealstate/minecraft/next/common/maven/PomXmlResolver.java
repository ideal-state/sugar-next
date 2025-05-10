/*
 *    Copyright 2025 ideal-state
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */

package team.idealstate.minecraft.next.common.maven;

import static team.idealstate.minecraft.next.common.function.Functional.functional;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.StringJoiner;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import lombok.Getter;
import lombok.NonNull;
import org.eclipse.aether.artifact.DefaultArtifact;
import org.eclipse.aether.graph.Dependency;
import org.eclipse.aether.util.artifact.JavaScopes;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;
import team.idealstate.minecraft.next.common.maven.exception.MavenException;
import team.idealstate.minecraft.next.common.validate.Validation;
import team.idealstate.minecraft.next.common.validate.annotation.NotNull;

public class PomXmlResolver extends DefaultHandler {

    @NotNull @SuppressWarnings("unchecked")
    public static List<Dependency> resolve(@NotNull InputStream inputStream) {
        Validation.notNull(inputStream, "inputStream must not be null.");
        return functional(inputStream)
                .use(
                        List.class,
                        input -> {
                            SAXParser parser = SAXParserFactory.newInstance().newSAXParser();
                            PomXmlResolver handler = new PomXmlResolver();
                            parser.parse(input, handler);
                            return handler.getDependencies();
                        });
    }

    public static final int DEPTH_ON_ROOT = 0;
    public static final int DEPTH_ON_PROJECT = 1;
    public static final int DEPTH_ON_DEPENDENCIES = 2;
    public static final int DEPTH_ON_DEPENDENCY = 3;
    public static final int DEPTH_ON_DEPENDENCY_ELEMENT = 4;
    public static final int INDEX_DEPENDENCY_GROUP_ID = 0;
    public static final int INDEX_DEPENDENCY_ARTIFACT_ID = 1;
    public static final int INDEX_DEPENDENCY_EXTENSION = 2;
    public static final int INDEX_DEPENDENCY_CLASSIFIER = 3;
    public static final int INDEX_DEPENDENCY_VERSION = 4;

    @SuppressWarnings("Java9CollectionFactory")
    public static final Set<Integer> OPTIONAL_INDEX_DEPENDENCY_ELEMENT =
            Collections.unmodifiableSet(
                    new HashSet<>(
                            Arrays.asList(
                                    INDEX_DEPENDENCY_EXTENSION, INDEX_DEPENDENCY_CLASSIFIER)));

    private int depth = 0;
    private final Boolean[] status = {false, false, false};
    private final String[] builder = new String[5];
    private String scope = null;
    private String currentQName = null;
    @NonNull @Getter private List<Dependency> dependencies = Collections.emptyList();

    protected PomXmlResolver() {}

    public void startElement(String uri, String localName, String qName, Attributes attributes)
            throws SAXException {
        switch (depth) {
            case DEPTH_ON_ROOT:
                if ("project".equals(qName)) {
                    status[DEPTH_ON_ROOT] = true;
                }
                break;
            case DEPTH_ON_PROJECT:
                if ("dependencies".equals(qName)) {
                    status[DEPTH_ON_PROJECT] = true;
                }
                break;
            case DEPTH_ON_DEPENDENCIES:
                if ("dependency".equals(qName)) {
                    status[DEPTH_ON_DEPENDENCIES] = true;
                }
                break;
            case DEPTH_ON_DEPENDENCY:
                switch (qName) {
                    case "groupId":
                    case "artifactId":
                    case "extension":
                    case "classifier":
                    case "version":
                    case "scope":
                        this.currentQName = qName;
                        break;
                }
                break;
        }
        depth++;
    }

    public void endElement(String uri, String localName, String qName) throws SAXException {
        depth--;
        switch (depth) {
            case DEPTH_ON_ROOT:
                if ("project".equals(qName)) {
                    status[DEPTH_ON_ROOT] = false;
                }
                break;
            case DEPTH_ON_PROJECT:
                if ("dependencies".equals(qName)) {
                    status[DEPTH_ON_PROJECT] = false;
                }
                break;
            case DEPTH_ON_DEPENDENCIES:
                if ("dependency".equals(qName)) {
                    status[DEPTH_ON_DEPENDENCIES] = false;
                    if (Arrays.stream(builder).anyMatch(Objects::nonNull)) {
                        Dependency dependency = makeDependency();
                        if (dependencies.isEmpty()) {
                            this.dependencies = new ArrayList<>();
                        }
                        this.dependencies.add(dependency);
                        Arrays.fill(builder, null);
                        this.scope = null;
                    }
                }
                break;
        }
    }

    @NotNull private Dependency makeDependency() {
        StringJoiner joiner = new StringJoiner(":");
        for (int i = 0; i < builder.length; i++) {
            String content = builder[i];
            if (content == null) {
                if (OPTIONAL_INDEX_DEPENDENCY_ELEMENT.contains(i)) {
                    continue;
                }
                throw new MavenException(
                        String.format("invalid dependency id %s", Arrays.toString(builder)));
            }
            joiner.add(content);
        }
        String scope = this.scope;
        if (scope == null) {
            scope = JavaScopes.COMPILE;
        }
        String dependencyId = joiner.toString();
        DefaultArtifact defaultArtifact = new DefaultArtifact(dependencyId);
        return new Dependency(defaultArtifact, scope);
    }

    public void characters(char[] ch, int start, int length) throws SAXException {
        if (depth == DEPTH_ON_DEPENDENCY_ELEMENT
                && currentQName != null
                && Arrays.stream(status).allMatch(Boolean::booleanValue)) {
            int index = -1;
            //noinspection EnhancedSwitchMigration
            switch (currentQName) {
                case "groupId":
                    index = INDEX_DEPENDENCY_GROUP_ID;
                    break;
                case "artifactId":
                    index = INDEX_DEPENDENCY_ARTIFACT_ID;
                    break;
                case "extension":
                    index = INDEX_DEPENDENCY_EXTENSION;
                    break;
                case "classifier":
                    index = INDEX_DEPENDENCY_CLASSIFIER;
                    break;
                case "version":
                    index = INDEX_DEPENDENCY_VERSION;
                    break;
                case "scope":
                    this.scope = new String(ch, start, length);
                    return;
            }
            if (index >= 0) {
                builder[index] = new String(ch, start, length);
            }
        }
    }
}
