package scw.feign;

import scw.beans.BeanFactory;
import scw.beans.BeansException;
import scw.beans.support.DefaultBeanDefinition;
import scw.core.utils.StringUtils;
import scw.net.InetUtils;
import feign.Feign;
import feign.codec.Decoder;
import feign.codec.Encoder;

public class FeignBeanDefinition extends DefaultBeanDefinition {
	private scw.feign.annotation.FeignClient feignClient;

	public FeignBeanDefinition(BeanFactory beanFactory, Class<?> sourceClass, scw.feign.annotation.FeignClient feignClient) {
		super(beanFactory, sourceClass);
		this.feignClient = feignClient;
	}

	private String getHost(){
		String host = feignClient.host();
		if(StringUtils.isEmpty(host)){
			host = beanFactory.getEnvironment().getString("feign.host");
		}else{
			host = beanFactory.getEnvironment().resolvePlaceholders(host);
		}
		return host;
	}

	public boolean isInstance() {
		return StringUtils.isNotEmpty(getHost());
	}

	@Override
	public Object create() throws BeansException {
		FeignCodec codec = new FeignCodec(InetUtils.getMessageConverter());
		Encoder encoder = beanFactory.isInstance(Encoder.class) ? beanFactory.getInstance(Encoder.class) : codec;
		Decoder decoder = beanFactory.isInstance(Decoder.class) ? beanFactory.getInstance(Decoder.class) : codec;
		Object proxy = Feign.builder().encoder(encoder).decoder(decoder).target(getTargetClass(), getHost());
		return beanFactory.getAop().getProxy(getTargetClass(), proxy).create();
	}
}
