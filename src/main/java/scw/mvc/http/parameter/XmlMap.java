package scw.mvc.http.parameter;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.LinkedHashMap;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import scw.beans.annotation.Bean;
import scw.core.utils.XMLUtils;
import scw.mvc.http.HttpRequest;

@Bean(singleton=false)
public final class XmlMap extends LinkedHashMap<String, String> {
	private static final long serialVersionUID = 1L;

	/**
	 * 用于序列化
	 */
	protected XmlMap() {
	}

	public XmlMap(HttpRequest request) throws IOException {
		BufferedReader reader = request.getReader();
		Document document = XMLUtils.parse(new InputSource(reader));
		Element element = document.getDocumentElement();
		NodeList nodeList = element.getChildNodes();
		for (int i = 0; i < nodeList.getLength(); i++) {
			Node n = nodeList.item(i);
			if (n == null) {
				continue;
			}

			String nodeName = n.getNodeName();
			if (!XMLUtils.checkNodeName(nodeName)) {
				continue;
			}

			String value = n.getTextContent();
			put(nodeName, value);
		}
	}
}