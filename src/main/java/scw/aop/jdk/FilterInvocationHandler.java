package scw.aop.jdk;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.util.Collection;

import scw.aop.DefaultFilterChain;
import scw.aop.Filter;
import scw.aop.FilterChain;
import scw.aop.ReflectInvoker;

public final class FilterInvocationHandler implements InvocationHandler{
	private final Collection<Filter> filters;
	private final Object obj;
	
	public FilterInvocationHandler(Object obj, Collection<Filter> filters){
		this.obj = obj;
		this.filters = filters;
	}
	
	public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
		FilterChain filterChain = new DefaultFilterChain(filters);
		return filterChain.doFilter(new ReflectInvoker(obj, method), proxy, method, args);
	}

}
