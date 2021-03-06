package scw.security.limit;

import scw.beans.annotation.AutoImpl;
import scw.core.reflect.MethodInvoker;
import scw.security.limit.annotation.CountLimitSecurity;

@AutoImpl({ DefaultCountLimitFactory.class })
public interface CountLimitFactory {
	String getKey(CountLimitSecurity countLimitSecurity, MethodInvoker invoker, Object[] args);
}