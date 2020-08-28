package scw.hystrix;

import com.netflix.hystrix.HystrixCommand;

import scw.aop.MethodInterceptorChain;
import scw.aop.MethodInvoker;
import scw.beans.annotation.AopEnable;
import scw.beans.annotation.AutoImpl;

@AutoImpl(DefaultHystrixCommandFactory.class)
@AopEnable(false)
public interface HystrixCommandFactory {
	HystrixCommand<?> getHystrixCommandFactory(MethodInvoker invoker, Object[] args, MethodInterceptorChain chain)
			throws Exception;
}