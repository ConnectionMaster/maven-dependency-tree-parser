/**
 * Copyright 2011 Alexandre Dutra
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package fr.dutra.tools.maven.deptree.core;

import java.util.ArrayList;
import java.util.List;

/**
 * Parent class for all parsers.
 * @author Alexandre Dutra
 *
 */
public abstract class AbstractParser implements Parser {

    /**
     * Parses a string representing a Maven artifact in standard notation.
     * @param artifact
     * @return an instance of {@link Node} representing the artifact.
     */
    protected Node parseArtifactString(final String artifact) {
        final List<String> tokens = new ArrayList<String>(7);
        int tokenStart = 0;
        boolean tokenStarted = false;
        boolean hasDescription = false;
        boolean omitted = false;
        int tokenEnd = 0;
        for (; tokenEnd < artifact.length(); tokenEnd++) {
            final char c = artifact.charAt(tokenEnd);
            switch (c) {
                case ' ': // in descriptions only
                    if (tokenStarted && !hasDescription) {
                        tokens.add(artifact.substring(tokenStart, tokenEnd));
                        tokenStarted = false;
                        hasDescription = true;
                    }
                    continue;

                case ':':
                    tokens.add(artifact.substring(tokenStart, tokenEnd));
                    tokenStarted = false;
                    continue;
                case ')': //end of descriptions and omitted artifacts,
                    // check length in case of "com.bugsnag:bugsnag:jar:3.1.4:compile (version selected from constraint [3.0,4.0))"
                    if (tokenEnd == artifact.length() - 1) {
                        tokens.add(artifact.substring(tokenStart, tokenEnd));
                        tokenStarted = false;
                    }
                    continue;

                case '-': // in omitted artifacts descriptions
                    continue;

                case '(': // in omitted artifacts
                    if (tokenEnd == 0) {
                        omitted = true;
                    }
                    continue;

                default:
                    if (!tokenStarted) {
                        tokenStart = tokenEnd;
                        tokenStarted = true;
                    }
                    continue;
            }
        }

        //last token
        if (tokenStarted) {
            tokens.add(artifact.substring(tokenStart, tokenEnd));
        }

        String groupId;
        String artifactId;
        String packaging;
        String classifier;
        String version;
        String scope;
        String description;

        if (tokens.size() == 4) {

            groupId = tokens.get(0);
            artifactId = tokens.get(1);
            packaging = tokens.get(2);
            version = tokens.get(3);
            scope = null;
            description = null;
            classifier = null;

        } else if (tokens.size() == 5) {

            groupId = tokens.get(0);
            artifactId = tokens.get(1);
            packaging = tokens.get(2);
            version = tokens.get(3);
            scope = tokens.get(4);
            description = null;
            classifier = null;

        } else if (tokens.size() == 6) {

            if (hasDescription) {
                groupId = tokens.get(0);
                artifactId = tokens.get(1);
                packaging = tokens.get(2);
                version = tokens.get(3);
                scope = tokens.get(4);
                description = tokens.get(5);
                classifier = null;
            } else {
                groupId = tokens.get(0);
                artifactId = tokens.get(1);
                packaging = tokens.get(2);
                classifier = tokens.get(3);
                version = tokens.get(4);
                scope = tokens.get(5);
                description = null;
            }

        } else if (tokens.size() == 7) {

            groupId = tokens.get(0);
            artifactId = tokens.get(1);
            packaging = tokens.get(2);
            classifier = tokens.get(3);
            version = tokens.get(4);
            scope = tokens.get(5);
            description = tokens.get(6);

        } else {
            throw new IllegalStateException("Wrong number of tokens: " + tokens.size() + " for artifact: " + artifact);
        }

        final Node node = new Node(
                groupId,
                artifactId,
                packaging,
                classifier,
                version,
                scope,
                description,
                omitted
        );
        return node;
    }
}