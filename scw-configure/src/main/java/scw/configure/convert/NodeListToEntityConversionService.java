package scw.configure.convert;

import java.util.Collections;
import java.util.Enumeration;
import java.util.Set;

import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import scw.convert.ConversionService;
import scw.convert.support.ConvertiblePair;
import scw.core.instance.NoArgsInstanceFactory;
import scw.util.Accept;
import scw.xml.XMLUtils;

public class NodeListToEntityConversionService extends EntityConversionService {

	public NodeListToEntityConversionService(
			ConversionService conversionService,
			NoArgsInstanceFactory instanceFactory) {
		super(conversionService, instanceFactory);
	}

	public Set<ConvertiblePair> getConvertibleTypes() {
		return Collections.singleton(new ConvertiblePair(NodeList.class, Object.class));
	}

	@Override
	protected Enumeration<String> keys(Object source) {
		return new NodeNameEnumeration((NodeList) source);
	}

	@Override
	protected Object getProperty(Object source, final String key) {
		return XMLUtils.findNode((NodeList) source, new Accept<Node>() {

			public boolean accept(Node e) {
				return e.getNodeName().equals(key);
			}
		});
	}

	private static final class NodeNameEnumeration implements
			Enumeration<String> {
		private final NodeList nodeList;
		private int i = 0;
		private int len;

		public NodeNameEnumeration(NodeList nodeList) {
			this.nodeList = nodeList;
			this.len = nodeList.getLength();
		}

		public boolean hasMoreElements() {
			return i < len;
		}

		public String nextElement() {
			Node node = nodeList.item(i++);
			return node.getNodeName();
		}

	}
}