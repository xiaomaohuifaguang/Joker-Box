package com.cat.simple.config.flowable.gateway;

import com.cat.common.entity.process.ProcessGatewayCondition;
import org.springframework.stereotype.Component;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.xml.sax.InputSource;

@Component
public class GatewayConditionXmlInjector {

    private static final String BPMN_NS = "http://www.omg.org/spec/BPMN/20100524/MODEL";
    private static final String XSI_NS = "http://www.w3.org/2001/XMLSchema-instance";

    /**
     * Injects gateway conditions into BPMN XML.
     *
     * @param xmlStr     the original BPMN XML string
     * @param conditions list of gateway conditions to inject
     * @return the modified BPMN XML string
     */
    public String inject(String xmlStr, List<ProcessGatewayCondition> conditions) {
        if (conditions == null || conditions.isEmpty()) {
            return xmlStr;
        }

        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setNamespaceAware(true);
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document doc = builder.parse(new InputSource(new StringReader(xmlStr)));

            Map<String, ProcessGatewayCondition> conditionMap = conditions.stream()
                    .collect(Collectors.toMap(ProcessGatewayCondition::getSequenceFlowId, c -> c));

            // Process sequenceFlows
            processSequenceFlows(doc, conditionMap);

            // Set gateway default attributes
            setGatewayDefaults(doc, conditionMap);

            // Serialize back to XML
            TransformerFactory transformerFactory = TransformerFactory.newInstance();
            Transformer transformer = transformerFactory.newTransformer();
            transformer.setOutputProperty(OutputKeys.INDENT, "yes");
            transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "no");
            transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");

            StringWriter writer = new StringWriter();
            transformer.transform(new DOMSource(doc), new StreamResult(writer));
            return writer.toString();

        } catch (Exception e) {
            throw new RuntimeException("Failed to inject gateway conditions into BPMN XML", e);
        }
    }

    private void processSequenceFlows(Document doc, Map<String, ProcessGatewayCondition> conditionMap) {
        List<Element> sequenceFlows = getElementsByTagName(doc, "sequenceFlow");

        for (Element sequenceFlow : sequenceFlows) {
            String flowId = sequenceFlow.getAttribute("id");
            ProcessGatewayCondition condition = conditionMap.get(flowId);
            if (condition == null) {
                continue;
            }

            // Remove existing conditionExpression
            removeExistingConditionExpression(sequenceFlow);

            if (Boolean.TRUE.equals(condition.getIsDefault())) {
                // DEFAULT: remove conditionExpression (already done above)
                continue;
            }

            // Create new conditionExpression
            Element condExpr = doc.createElementNS(BPMN_NS, "conditionExpression");
            condExpr.setAttributeNS(XSI_NS, "xsi:type", "tFormalExpression");

            String expression;
            if ("NATIVE".equals(condition.getConditionType())) {
                expression = condition.getNativeExpression();
            } else if ("CUSTOM".equals(condition.getConditionType())) {
                expression = "${gatewayConditionEvaluator.evaluate('" + flowId + "')}";
            } else {
                continue;
            }

            condExpr.setTextContent(expression);
            sequenceFlow.appendChild(condExpr);
        }
    }

    private void removeExistingConditionExpression(Element sequenceFlow) {
        NodeList children = sequenceFlow.getChildNodes();
        for (int i = children.getLength() - 1; i >= 0; i--) {
            Node child = children.item(i);
            if (child.getNodeType() == Node.ELEMENT_NODE) {
                String localName = child.getLocalName();
                if (localName != null && "conditionExpression".equals(localName)) {
                    sequenceFlow.removeChild(child);
                } else if (localName == null && "conditionExpression".equals(child.getNodeName())) {
                    sequenceFlow.removeChild(child);
                }
            }
        }
    }

    private void setGatewayDefaults(Document doc, Map<String, ProcessGatewayCondition> conditionMap) {
        for (ProcessGatewayCondition condition : conditionMap.values()) {
            if (!Boolean.TRUE.equals(condition.getIsDefault())) {
                continue;
            }

            String sourceNodeId = condition.getSourceNodeId();
            String sequenceFlowId = condition.getSequenceFlowId();

            // Try exclusiveGateway first, then inclusiveGateway
            boolean found = setGatewayDefault(doc, "exclusiveGateway", sourceNodeId, sequenceFlowId);
            if (!found) {
                setGatewayDefault(doc, "inclusiveGateway", sourceNodeId, sequenceFlowId);
            }
        }
    }

    private boolean setGatewayDefault(Document doc, String gatewayTag, String sourceNodeId, String sequenceFlowId) {
        List<Element> gateways = getElementsByTagName(doc, gatewayTag);
        for (Element gateway : gateways) {
            if (sourceNodeId.equals(gateway.getAttribute("id"))) {
                gateway.setAttribute("default", sequenceFlowId);
                return true;
            }
        }
        return false;
    }

    private List<Element> getElementsByTagName(Document doc, String tagName) {
        java.util.List<Element> result = new java.util.ArrayList<>();

        // Try namespaced first
        NodeList nodeList = doc.getElementsByTagNameNS(BPMN_NS, tagName);
        for (int i = 0; i < nodeList.getLength(); i++) {
            Node node = nodeList.item(i);
            if (node.getNodeType() == Node.ELEMENT_NODE) {
                result.add((Element) node);
            }
        }

        // Fall back to non-namespaced
        if (result.isEmpty()) {
            NodeList fallbackList = doc.getElementsByTagName(tagName);
            for (int i = 0; i < fallbackList.getLength(); i++) {
                Node node = fallbackList.item(i);
                if (node.getNodeType() == Node.ELEMENT_NODE) {
                    result.add((Element) node);
                }
            }
        }

        return result;
    }
}
