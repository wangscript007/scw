package scw.net.message;

import scw.net.MimeType;

/**
 * 对于文本的定义，这里只是一个定义，不做具体实现
 * @author shuchaowen
 *
 */
public interface Text {
	/**
	 * 转化为文本
	 * @return
	 */
	String toTextContent();

	/**
	 * 数据类型
	 * @return
	 */
	MimeType getMimeType();
}
