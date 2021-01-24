package scw.serializer.fastjson;

import scw.aop.ProxyUtils;
import scw.aop.support.FieldSetterListen;
import scw.aop.support.FieldSetterListenImpl;
import scw.json.JSONAware;
import scw.mapper.Copy;
import scw.value.AnyValue;
import scw.value.Value;

import com.alibaba.fastjson.serializer.ValueFilter;

public class ExtendFastJsonValueFilter implements ValueFilter {
	public static final ValueFilter INSTANCE = new ExtendFastJsonValueFilter();

	private ExtendFastJsonValueFilter() {
	};

	public Object process(Object object, String name, Object value) {
		if (value == null) {
			return value;
		}
		
		if(value instanceof JSONAware){
			return ((JSONAware) value).toJSONString();
		}
		
		if(value instanceof AnyValue){
			return ((AnyValue) value).getValue();
		}
		
		if(value instanceof Value){
			return ((Value) value).getAsString();
		}

		//这是应该还想办法屏蔽Gson的Factory对象
		if ("callbacks".equals(name)) {
			return null;
		}

		if (object instanceof FieldSetterListen && FieldSetterListenImpl.FIELD_SETTER_MAP_FIELD_NAME.equals(name)) {
			return null;
		}

		if (ProxyUtils.getProxyFactory().isProxy(value.getClass())) {
			return Copy.copy(ProxyUtils.getProxyFactory().getUserClass(value.getClass()), value);
		}
		
		return value;
	}
}
