package scw.transaction.synchronization;

/**
 * 事务的生命周期
 * 
 * @author shuchaowen
 *
 */
public interface TransactionLifeCycle {
	/**
	 * 在事务提交之前调用
	 */
	void beforeCommit() throws Throwable;

	/**
	 * 事务提交之后调用
	 */
	void afterCommit() throws Throwable;
	
	/**
	 * 在事务回滚前调用
	 * @throws Throwable
	 */
	void beforeRollback() throws Throwable;
	
	/**
	 * 在事务回滚后调用
	 * @throws Throwable
	 */
	void afterRollback() throws Throwable;

	/**
	 * 事务结束后调用
	 */
	void complete() throws Throwable;
}