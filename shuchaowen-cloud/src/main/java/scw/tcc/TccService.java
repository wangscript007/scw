package scw.tcc;

import scw.aop.ProxyInvoker;
import scw.complete.Complete;
import scw.tcc.annotation.Tcc;

public interface TccService {
	Stage createConfirm(ProxyInvoker invoker, Object[] args, Object tryResult, Tcc tcc);

	Stage createCancel(ProxyInvoker invoker, Object[] args, Object tryResult, Tcc tcc);

	Complete registerComplete(Stage stage) throws Exception;
}