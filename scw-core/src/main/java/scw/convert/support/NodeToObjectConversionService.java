package scw.convert.support;

import java.util.Collections;
import java.util.Set;

import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import scw.convert.ConversionService;
import scw.convert.TypeDescriptor;
import scw.dom.DomUtils;
import scw.value.StringValue;
import scw.value.ValueUtils;

class NodeToObjectConversionService extends ConditionalConversionService{
	private final ConversionService conversionService;
	
	public NodeToObjectConversionService(ConversionService conversionService){
		this.conversionService = conversionService;
	}
	
	public Object convert(Object source, TypeDescriptor sourceType,
			TypeDescriptor targetType) {
		if(source == null){
			return null;
		}
		
		if(Document.class.isAssignableFrom(sourceType.getType())){
			Node node = ((Document)source).getDocumentElement();
			return convert(node, TypeDescriptor.valueOf(NodeList.class), targetType);
		}else{
			return convert((Node)source, sourceType, targetType);
		}
	}
	
	public Object convert(Node node, TypeDescriptor sourceType, TypeDescriptor targetType){
		if(ValueUtils.isBaseType(targetType.getType())){
			StringValue value = new StringValue(node.getTextContent());
			return value.getAsObject(targetType.getResolvableType().getType());
		}
		
		NodeList nodeList = node.getChildNodes();
		int len = nodeList.getLength();
		if(len == 0){
			nodeList = DomUtils.toNodeList(node.getAttributes());
		}
		return conversionService.convert(nodeList, TypeDescriptor.valueOf(NodeList.class), targetType);
	}

	public Set<ConvertiblePair> getConvertibleTypes() {
		return Collections.singleton(new ConvertiblePair(Node.class, Object.class));
	}

}
