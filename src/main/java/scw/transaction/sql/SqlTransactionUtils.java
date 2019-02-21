package scw.transaction.sql;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.LinkedList;

import scw.sql.ConnectionFactory;
import scw.sql.Sql;
import scw.transaction.TransactionDefinition;
import scw.transaction.TransactionException;
import scw.transaction.support.TransactionLifeCycle;
import scw.transaction.support.TransactionSynchronization;

/**
 * 这是默认的事务管理器
 * 
 * @author shuchaowen
 *
 */
public abstract class SqlTransactionUtils {
	private static final ThreadLocal<LinkedList<MultipleConnectionTransactionSynchronization>> LOCAL = new ThreadLocal<LinkedList<MultipleConnectionTransactionSynchronization>>();

	/**
	 * 当前上下文是否存在事务
	 * 
	 * @return
	 */
	public static boolean hasTransaction() {
		LinkedList<MultipleConnectionTransactionSynchronization> list = LOCAL.get();
		if (list == null) {
			return false;
		}

		return !list.isEmpty();
	}

	/**
	 * 如果当前没有事务就是空的
	 * 
	 * @param connectionFactory
	 * @return
	 * @throws SQLException
	 */
	public static Connection getCurrentConnection(ConnectionFactory connectionFactory) throws SQLException {
		LinkedList<MultipleConnectionTransactionSynchronization> list = LOCAL.get();
		if (list == null) {
			return connectionFactory.getConnection();
		}

		MultipleConnectionTransactionSynchronization mcts = list.getLast();
		if (mcts == null) {
			return connectionFactory.getConnection();
		}

		return mcts.getConnectionTransaction(connectionFactory).getConnection();
	}

	public static void transactionSynchronization(TransactionSynchronization ts) throws TransactionException {
		LinkedList<MultipleConnectionTransactionSynchronization> list = LOCAL.get();
		if (list == null) {
			execute(ts);
			return;
		}

		MultipleConnectionTransactionSynchronization mcts = list.getLast();
		if (mcts == null) {
			execute(ts);
			return;
		}

		mcts.transactionSynchronization(ts);
	}

	/*
	 * 监听当前事务的生命周期，如果不存在事务则无效
	 */
	public static void transactionLifeCycle(TransactionLifeCycle tlc) {
		LinkedList<MultipleConnectionTransactionSynchronization> list = LOCAL.get();
		if (list == null) {
			return;
		}

		MultipleConnectionTransactionSynchronization mcts = list.getLast();
		if (mcts == null) {
			return;
		}

		mcts.transactionLifeCycle(tlc);
	}

	public static boolean executeSql(ConnectionFactory connectionFactory, Sql sql) {
		LinkedList<MultipleConnectionTransactionSynchronization> list = LOCAL.get();
		if (list == null) {
			return false;
		}

		MultipleConnectionTransactionSynchronization mcts = list.getLast();
		if (mcts == null) {
			return false;
		}

		mcts.getConnectionTransaction(connectionFactory).addSql(sql);
		return true;
	}

	protected static MultipleConnectionTransactionSynchronization getTransaction(
			TransactionDefinition transactionDefinition) {
		LinkedList<MultipleConnectionTransactionSynchronization> list = LOCAL.get();
		MultipleConnectionTransactionSynchronization mcts = null;
		if (list == null) {
			list = new LinkedList<MultipleConnectionTransactionSynchronization>();
			LOCAL.set(list);
		} else {
			mcts = list.getLast();
			if (mcts != null) {
				mcts = new MultipleConnectionTransactionSynchronization(mcts);
			}
		}

		switch (transactionDefinition.getPropagation()) {
		case REQUIRED:
			if (mcts == null) {
				mcts = new MultipleConnectionTransactionSynchronization(transactionDefinition, true);
			}
			break;
		case SUPPORTS:
			if (mcts == null) {
				mcts = new MultipleConnectionTransactionSynchronization(transactionDefinition, false);
			}
			break;
		case MANDATORY:
			if (mcts == null || !mcts.isActive()) {
				throw new TransactionException(transactionDefinition.getPropagation().name());
			}
			break;
		case REQUIRES_NEW:
			mcts = new MultipleConnectionTransactionSynchronization(transactionDefinition, true);
			break;
		case NOT_SUPPORTED:
			mcts = new MultipleConnectionTransactionSynchronization(transactionDefinition, false);
			break;
		case NEVER:
			if (mcts != null && mcts.isActive()) {
				throw new TransactionException(transactionDefinition.getPropagation().name());
			}
			break;
		case NESTED:
			if (mcts != null && mcts.isActive()) {
				mcts.createTempSavePoint();
			} else if (mcts == null) {
				mcts = new MultipleConnectionTransactionSynchronization(transactionDefinition, true);
			}
			break;
		}
		list.addLast(mcts);
		return mcts;
	}

	protected static void commit(MultipleConnectionTransactionSynchronization mcts) throws TransactionException {
		LinkedList<MultipleConnectionTransactionSynchronization> list = LOCAL.get();
		if (list == null) {
			throw new TransactionException("不存在事务");
		}

		MultipleConnectionTransactionSynchronization currentMcts = list.getLast();
		if (mcts != currentMcts) {
			throw new TransactionException("事务需要顺序关闭，请先关闭子事务");
		}

		mcts.process();
		try {
			mcts.end();
		} finally {
			list.removeLast();
			if (list.isEmpty()) {
				LOCAL.remove();
			}
		}
	}

	protected static void rollback(MultipleConnectionTransactionSynchronization mcts) throws TransactionException {
		LinkedList<MultipleConnectionTransactionSynchronization> list = LOCAL.get();
		if (list == null) {
			return;
		}

		MultipleConnectionTransactionSynchronization currentMcts = list.getLast();
		if (mcts != currentMcts) {
			return;
		}

		try {
			mcts.rollback();
		} finally {
			try {
				mcts.end();
			} finally {
				list.removeLast();
				if (list.isEmpty()) {
					LOCAL.remove();
				}
			}
		}
	}

	private static void execute(TransactionSynchronization synchronization) {
		try {
			synchronization.process();
		} catch (Throwable e) {
			try {
				synchronization.rollback();
			} finally {
				synchronization.end();
			}
			throw throwTransactionExpetion(e);
		} finally {
			synchronization.end();
		}
	}

	private static TransactionException throwTransactionExpetion(Throwable e) {
		if (e instanceof TransactionException) {
			return (TransactionException) e;
		}
		return new TransactionException(e);
	}
}
