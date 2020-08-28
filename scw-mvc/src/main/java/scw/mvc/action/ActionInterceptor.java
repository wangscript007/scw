package scw.mvc.action;

import scw.beans.annotation.AopEnable;
import scw.mvc.HttpChannel;

@AopEnable(false)
public interface ActionInterceptor {
	Object intercept(HttpChannel httpChannel, Action action, ActionParameters parameters, ActionInterceptorChain chain)
			throws Throwable;
}