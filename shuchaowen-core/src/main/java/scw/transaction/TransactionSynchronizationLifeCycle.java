package scw.transaction;

final class TransactionSynchronizationLifeCycle implements TransactionSynchronization {
	private final TransactionLifeCycle transactionLifeCycle;
	private final TransactionSynchronization transactionSynchronization;

	public TransactionSynchronizationLifeCycle(TransactionSynchronization transactionSynchronization,
			TransactionLifeCycle transactionLifeCycle) {
		this.transactionLifeCycle = transactionLifeCycle;
		this.transactionSynchronization = transactionSynchronization;
	}

	public void process() throws Throwable {
		if (transactionLifeCycle != null) {
			transactionLifeCycle.beforeProcess();
		}

		if (transactionSynchronization != null) {
			transactionSynchronization.process();
		}

		if (transactionLifeCycle != null) {
			transactionLifeCycle.afterProcess();
		}
	}

	public void rollback() {
		if (transactionLifeCycle != null) {
			transactionLifeCycle.beforeRollback();
		}

		if (transactionSynchronization != null) {
			transactionSynchronization.rollback();
		}

		if (transactionLifeCycle != null) {
			transactionLifeCycle.afterRollback();
		}
	}

	public void end() {
		try {
			if(transactionSynchronization != null){
				transactionSynchronization.end();
			}
		} finally {
			if (transactionLifeCycle != null) {
				transactionLifeCycle.complete();
			}
		}
	}
}