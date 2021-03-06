package scw.mvc.security;

import java.lang.reflect.AnnotatedElement;
import java.util.HashMap;
import java.util.Map;

import scw.context.annotation.Provider;
import scw.core.Constants;
import scw.core.annotation.KeyValuePair;
import scw.core.annotation.MultiAnnotatedElement;
import scw.core.utils.StringUtils;
import scw.event.EventListener;
import scw.event.ObjectEvent;
import scw.http.HttpMethod;
import scw.http.server.HttpControllerDescriptor;
import scw.lang.NotSupportedException;
import scw.mvc.action.Action;
import scw.mvc.action.ActionManager;
import scw.mvc.annotation.ActionAuthority;
import scw.mvc.annotation.ActionAuthorityParent;
import scw.security.authority.http.DefaultHttpAuthority;
import scw.security.authority.http.DefaultHttpAuthorityManager;
import scw.security.authority.http.HttpAuthority;
import scw.util.Base64;

@Provider(order = Integer.MIN_VALUE, value = HttpActionAuthorityManager.class)
public class DefaultHttpActionAuthorityManager extends DefaultHttpAuthorityManager<HttpAuthority>
		implements HttpActionAuthorityManager {

	public DefaultHttpActionAuthorityManager(ActionManager actionManager) {
		for (Action action : actionManager) {
			register(action);
		}
		
		actionManager.registerListener(new EventListener<ObjectEvent<Action>>() {
			
			public void onEvent(ObjectEvent<Action> event) {
				register(event.getSource());
			}
		});
	}

	private String getParentId(AnnotatedElement annotatedElement, String defaultId) {
		ActionAuthorityParent actionAuthorityParent = annotatedElement.getAnnotation(ActionAuthorityParent.class);
		String parentId = actionAuthorityParent == null ? defaultId : actionAuthorityParent.value().getName();
		if (parentId != null) {
			parentId = Base64.encode(StringUtils.getStringOperations().getBytes(parentId, Constants.ISO_8859_1));
		}
		return parentId;
	}

	public void register(Action action) {
		ActionAuthority classAuthority = action.getSourceClass().getAnnotation(ActionAuthority.class);
		if (classAuthority != null) {// 如果在类上存在此注解说明这是一个菜单
			String id = action.getSourceClass().getName();
			id = Base64.encode(StringUtils.getStringOperations().getBytes(id, Constants.ISO_8859_1));
			HttpAuthority authority = getAuthority(id);
			if (authority == null) {
				String parentId = getParentId(action.getSourceClass(), null);
				boolean isMenu = classAuthority.menu();
				if (isMenu) {
					checkIsMenu(parentId, action);
				}

				register(new DefaultHttpAuthority(id, parentId, classAuthority.value(), getAttributeMap(classAuthority),
						isMenu, null, null));
			}
		}

		ActionAuthority methodAuthority = action.getAnnotatedElement().getAnnotation(ActionAuthority.class);
		if (methodAuthority == null) {
			return;
		}

		HttpControllerDescriptor descriptor = getAuthorityControllerDescriptor(action);
		if (descriptor == null) {
			logger.warn("not found controller descriptor: {}", action);
			return;
		}

		String parentId = getParentId(new MultiAnnotatedElement(action.getSourceClass(), action.getAnnotatedElement()),
				action.getSourceClass().getName());
		boolean isMenu = methodAuthority.menu();
		if (isMenu) {
			checkIsMenu(parentId, action);
		}

		String id = descriptor.getMethod() + "&" + descriptor.getPath();
		id = Base64.encode(StringUtils.getStringOperations().getBytes(id, Constants.ISO_8859_1));

		register(new DefaultHttpAuthority(id, parentId, methodAuthority.value(),
				getAttributeMap(classAuthority, methodAuthority), isMenu, descriptor.getPath(),
				descriptor.getMethod()));
	}

	private void checkIsMenu(String parentId, Action action) {
		if (parentId != null) {
			HttpAuthority parent = getAuthority(parentId);
			if (parent != null && !parent.isMenu()) {
				throw new NotSupportedException("标注为一个菜单,但父级并不是一个菜单: " + action);
			}
		}
	}

	public HttpAuthority getAuthority(Action action) {
		for (HttpControllerDescriptor descriptor : action.getHttpControllerDescriptors()) {
			HttpAuthority authority = getAuthority(descriptor.getPath(), descriptor.getMethod());
			if (authority != null) {
				return authority;
			}
		}
		return null;
	}

	protected final Map<String, String> getAttributeMap(ActionAuthority... authoritys) {
		Map<String, String> attributeMap = new HashMap<String, String>();
		if (authoritys != null) {
			for (ActionAuthority actionAuthority : authoritys) {
				if (actionAuthority == null) {
					continue;
				}

				for (KeyValuePair pair : actionAuthority.attributes()) {
					attributeMap.put(pair.key(), pair.value());
				}
			}
		}
		return attributeMap.isEmpty() ? null : attributeMap;
	}

	protected HttpControllerDescriptor getAuthorityControllerDescriptor(Action action) {
		for (HttpControllerDescriptor descriptor : action.getHttpControllerDescriptors()) {
			if (descriptor.getMethod() == HttpMethod.GET) {
				return descriptor;
			}
		}

		for (HttpControllerDescriptor descriptor : action.getHttpControllerDescriptors()) {
			return descriptor;
		}
		return null;
	}
}
