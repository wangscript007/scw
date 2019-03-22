package scw.beans.rpc.transaction.service;

import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.TimerTask;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import scw.beans.BeanFactory;
import scw.beans.annotaion.Autowrite;
import scw.beans.annotaion.Destroy;
import scw.beans.annotaion.InitMethod;
import scw.beans.rpc.transaction.InvokeInfo;
import scw.beans.rpc.transaction.StageType;
import scw.beans.rpc.transaction.TCCService;
import scw.common.Base64;
import scw.common.FileManager;
import scw.common.utils.ConfigUtils;
import scw.common.utils.FileUtils;
import scw.logger.Logger;
import scw.logger.LoggerFactory;
import scw.transaction.DefaultTransactionLifeCycle;
import scw.transaction.TransactionException;
import scw.transaction.TransactionManager;

public final class RetryTCCService implements TCCService {
	private static Logger logger = LoggerFactory.getLogger(RetryTCCService.class);

	@Autowrite
	private BeanFactory beanFactory;
	private FileManager fileManager;
	private final int retryTime;// 秒
	private final ScheduledExecutorService executorService = Executors.newScheduledThreadPool(4);

	public RetryTCCService() {
		this(30);
	}

	public RetryTCCService(int retryTime) {
		this.retryTime = retryTime;
	}

	@InitMethod
	public void init() throws UnsupportedEncodingException {
		String logPath = System.getProperty("java.io.tmpdir");
		String classPath = ConfigUtils.getClassPath();
		logPath += File.separator + "TCC_" + Base64.encode(classPath.getBytes("UTF-8"));
		fileManager = new FileManager(logPath);
		logger.debug("logPath=" + logPath);

		File file = new File(logPath);
		if (!file.exists()) {
			file.mkdirs();
		} else {
			File[] files = file.listFiles();
			if (files != null) {
				for (File f : files) {
					try {
						TransactionInfo info = FileUtils.readObject(f);
						new RetryInvoker(info, retryTime, f.getPath()).start();
					} catch (ClassNotFoundException e) {
						e.printStackTrace();
					} catch (IOException e) {
						e.printStackTrace();
					}
				}
			}
		}
	}

	@Destroy
	public void destory() {
		executorService.shutdownNow();
	}

	private void invoke(InvokeInfo invokeInfo, StageType stageType) {
		if (!invokeInfo.hasCanInvoke(stageType)) {
			return;
		}

		TransactionInfo info = new TransactionInfo(invokeInfo, stageType);
		try {
			File file = fileManager.createRandomFileWriteObject(info);
			new RetryInvoker(info, retryTime, file.getPath()).start();
		} catch (IOException e) {
			throw new TransactionException(e);
		}
	}

	public void service(final Object obj, final InvokeInfo invokeInfo) {
		TransactionManager.transactionLifeCycle(new DefaultTransactionLifeCycle() {
			@Override
			public void beforeProcess() {
				invoke(invokeInfo, StageType.Confirm);
			}

			@Override
			public void beforeRollback() {
				invoke(invokeInfo, StageType.Cancel);
			}

			@Override
			public void complete() {
				invoke(invokeInfo, StageType.Complete);
			}
		});
	}

	class RetryInvoker extends TimerTask {
		private final TransactionInfo transactionInfo;
		private final int retryTime;
		private final String fileId;

		/**
		 * 
		 * @param retryTime
		 *            重试时间 毫秒
		 * @param obj
		 * @param method
		 * @param args
		 */
		public RetryInvoker(TransactionInfo transactionInfo, int retryTime, String fileId) {
			this.transactionInfo = transactionInfo;
			this.retryTime = retryTime;
			this.fileId = fileId;
		}

		@Override
		public void run() {
			try {
				transactionInfo.invoke(beanFactory);
				File file = new File(fileId);
				if (file.exists()) {
					file.delete();
				}
			} catch (Exception e) {
				executorService.schedule(this, retryTime, TimeUnit.SECONDS);
				e.printStackTrace();
			}
		}

		public void start() {
			if (!transactionInfo.hasCanInvoke()) {
				return;
			}
			run();
		}
	}
}