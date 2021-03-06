package scw.lucene;

import java.io.IOException;

import org.apache.lucene.index.IndexWriter;

import scw.logger.Logger;
import scw.logger.LoggerUtils;
import scw.transaction.Savepoint;
import scw.transaction.TransactionException;
import scw.transaction.TransactionResource;

public class IndexWriterResource implements TransactionResource {
	private static Logger logger = LoggerUtils.getLogger(IndexWriterResource.class);

	private final IndexWriter indexWriter;

	public IndexWriterResource(IndexWriter indexWriter) {
		this.indexWriter = indexWriter;
	}

	public void commit() throws Throwable {
		indexWriter.commit();
	}

	public void rollback() {
		try {
			indexWriter.rollback();
		} catch (IOException e) {
			logger.error(e, "rollback error");
		}
	}

	public void completion() {
		try {
			indexWriter.close();
		} catch (IOException e) {
			logger.error(e, "close error");
		}
	}

	public Savepoint createSavepoint() throws TransactionException {
		return null;
	}

	public IndexWriter getIndexWriter() {
		return indexWriter;
	}
}
