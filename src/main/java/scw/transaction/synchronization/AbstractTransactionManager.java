package scw.transaction.synchronization;

import java.util.LinkedList;

import scw.transaction.Transaction;
import scw.transaction.TransactionDefinition;
import scw.transaction.TransactionException;
import scw.transaction.TransactionManager;

public abstract class AbstractTransactionManager implements TransactionManager {
	private static final ThreadLocal<LinkedList<TransactionInfo>> LOCAL = new ThreadLocal<LinkedList<TransactionInfo>>();

	public abstract AbstractTransaction newTransaction(AbstractTransaction parent,
			TransactionDefinition transactionDefinition, boolean active) throws TransactionException;

	public Transaction getTransaction(TransactionDefinition transactionDefinition) throws TransactionException {
		LinkedList<TransactionInfo> linkedList = LOCAL.get();
		TransactionInfo transactionInfo;
		if (linkedList == null) {
			linkedList = new LinkedList<TransactionInfo>();
			LOCAL.set(linkedList);
			transactionInfo = new TransactionInfo(null, this);
		} else {
			transactionInfo = new TransactionInfo(linkedList.getLast(), this);
		}
		linkedList.add(transactionInfo);
		return transactionInfo.getTransaction(transactionDefinition);
	}

	public void rollback(Transaction transaction) throws TransactionException {
		AbstractTransaction tx = (AbstractTransaction) transaction;
		LinkedList<TransactionInfo> linkedList = LOCAL.get();
		TransactionInfo transactionInfo = linkedList.getLast();
		if (transactionInfo.hasSavepoint()) {
			try {
				tx.rollbackToSavepoint(transactionInfo.getSavepoint());
			} finally {
				try {
					if (tx.isNewTransaction()) {
						tx.rollback();
					}
				} finally {
					linkedList.removeLast();
				}
			}
		} else {
			try {
				if (tx.isNewTransaction()) {
					tx.rollback();
				}
			} finally {
				linkedList.removeLast();
			}
		}
	}

	public void commit(Transaction transaction) throws TransactionException {
		try {
			((AbstractTransaction) transaction).commit();
		} finally {
			LOCAL.get().removeLast();
		}
	}
}
