/**
 * Copyright 2011 Alexandre Dutra
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
package fr.dutra.tools.maven.deptree.core;

import java.io.IOException;
import java.io.Reader;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

public class GraphmlParser extends AbstractParser {

    private static final Pattern ACTIVE_ARTIFACT_PATTERN = Pattern.compile("artifact = (?!active project artifact:)(.+);");


    public Node parse(Reader reader) throws ParseException {
        SAXParserFactory factory = SAXParserFactory.newInstance();
        factory.setNamespaceAware(true);
        factory.setValidating(true);
        try {
            SAXParser saxParser = factory.newSAXParser();
            EventHandler handler = new EventHandler();
            saxParser.parse(new InputSource(reader), handler);
            return handler.getRootNode();
        } catch (ParserConfigurationException e) {
            throw new ParseException(e);
        } catch (SAXException e) {
            throw new ParseException(e);
        } catch (IOException e) {
            throw new ParseException(e);
        }
    }

    protected class EventHandler extends DefaultHandler {

        private Map<String, Node> nodes = new HashMap<String, Node>();

        private String currentNodeId;

        private boolean insideNodeLabel;

        private Node root;

        public Node getRootNode() {
            return root;
        }

        @Override
        public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException {

            if("node".equals(localName)) {
                currentNodeId = attributes.getValue("", "id");
            }

            if ("edge".equals(localName)){
                String parentNodeId = attributes.getValue("", "source");
                String childNodeId = attributes.getValue("", "target");
                Node parent = nodes.get(parentNodeId);
                Node child = nodes.get(childNodeId);
                parent.addChildNode(child);
            }

            insideNodeLabel = "NodeLabel".equals(localName);

        }

        @Override
        public void endElement(String uri, String localName, String qName) throws SAXException {
            insideNodeLabel = false;
        }

        @Override
        public void characters(char[] ch, int start, int length) throws SAXException {
            if(insideNodeLabel) {
                String artifact = String.valueOf(ch, start, length).trim();
                if(artifact.contains("active project artifact")){
                    artifact = extractActiveProjectArtifact(artifact);
                }
                Node node = parseArtifactString(artifact);
                nodes.put(currentNodeId, node);
                if(root == null) {
                    root = node;
                }
                currentNodeId = null;
            }
        }

    }


    /**
     * When doing an install at the same time on a multi-module project, one can get this kind of output:
     * <pre>
     * +- active project artifact:
     *     artifact = active project artifact:
     *     artifact = active project artifact:
     *     artifact = active project artifact:
     *     artifact = active project artifact:
     *     artifact = active project artifact:
     *     artifact = active project artifact:
     *     artifact = active project artifact:
     *     artifact = com.acme.org:foobar:jar:1.0.41-SNAPSHOT:compile;
     *     project: MavenProject: com.acme.org:foobar:1.0.41-SNAPSHOT @ /opt/jenkins/home/jobs/foobar/workspace/trunk/foobar/pom.xml;
     *     project: MavenProject: com.acme.org:foobar:1.0.41-SNAPSHOT @ /opt/jenkins/home/jobs/foobar/workspace/trunk/foobar/pom.xml;
     *     project: MavenProject: com.acme.org:foobar:1.0.41-SNAPSHOT @ /opt/jenkins/home/jobs/foobar/workspace/trunk/foobar/pom.xml;
     *     project: MavenProject: com.acme.org:foobar:1.0.41-SNAPSHOT @ /opt/jenkins/home/jobs/foobar/workspace/trunk/foobar/pom.xml;
     *     project: MavenProject: com.acme.org:foobar:1.0.41-SNAPSHOT @ /opt/jenkins/home/jobs/foobar/workspace/trunk/foobar/pom.xml;
     *     project: MavenProject: com.acme.org:foobar:1.0.41-SNAPSHOT @ /opt/jenkins/home/jobs/foobar/workspace/trunk/foobar/pom.xml;
     *     project: MavenProject: com.acme.org:foobar:1.0.41-SNAPSHOT @ /opt/jenkins/home/jobs/foobar/workspace/trunk/foobar/pom.xml;
     *     project: MavenProject: com.acme.org:foobar:1.0.41-SNAPSHOT @ /opt/jenkins/home/jobs/foobar/workspace/trunk/foobar/pom.xml
     * </pre>
     */
    protected String extractActiveProjectArtifact(String content) {
        Matcher matcher = ACTIVE_ARTIFACT_PATTERN.matcher(content);
        if(matcher.find()){
            return matcher.group(1);
        }
        return null;
    }

}