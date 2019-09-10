package scw.session;

public interface SessionFactory {
	/**
	 * 获取session
	 * @param sessionId
	 * @return
	 */
	Session getSession(String sessionId);
	
	/**
	 * 获取session
	 * @param sessionId
	 * @param create 如果不存在是否创建
	 * @return
	 */
	Session getSession(String sessionId, boolean create);
}
