package scw.timer.support;

import java.util.Calendar;
import java.util.Date;
import java.util.Map.Entry;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;

import scw.context.Destroy;
import scw.core.IteratorCallback;
import scw.core.utils.StringUtils;
import scw.core.utils.XTime;
import scw.lang.AlreadyExistsException;
import scw.logger.Logger;
import scw.logger.LoggerUtils;
import scw.timer.CrontabTaskConfig;
import scw.timer.ScheduleTaskConfig;
import scw.timer.Task;
import scw.timer.TaskConfig;
import scw.timer.TaskContext;
import scw.timer.TaskFactory;
import scw.timer.TaskLockFactory;
import scw.util.DefaultStringMatcher;
import scw.util.StringMatcher;

/**
 * 默认的Timer实现
 * 
 * @author shuchaowen
 *
 */
public final class DefaultTimer implements scw.timer.Timer, Destroy {
	private static Logger logger = LoggerUtils.getLogger(DefaultTimer.class);
	private final ConcurrentHashMap<String, TaskContext> contextMap = new ConcurrentHashMap<String, TaskContext>();
	private final TaskLockFactory taskLockFactory;
	private final java.util.Timer timer;
	private final ExecutorService executorService;
	private final TaskFactory taskFactory;
	private StringMatcher matcher = DefaultStringMatcher.getInstance();

	public DefaultTimer(TaskLockFactory taskLockFactory, ExecutorService executorService, TaskFactory taskFactory) {
		this.taskLockFactory = taskLockFactory;
		this.timer = createTimer();
		this.executorService = executorService;
		this.taskFactory = taskFactory;
		Calendar calendar = Calendar.getInstance();
		calendar.add(Calendar.MINUTE, 1);
		calendar.set(Calendar.SECOND, 0);
		calendar.set(Calendar.MILLISECOND, 0);
		timer.schedule(new CrontabTimerTask(), new Date(calendar.getTimeInMillis()), XTime.ONE_MINUTE);
		timer.schedule(new ScanningTaskConfigTask(), 0, 1);
		timer.schedule(new PurgeTimerTask(), XTime.ONE_MINUTE, XTime.ONE_MINUTE);
	}

	protected Timer createTimer() {
		return new Timer(getClass().getName(), true);
	}

	public final StringMatcher getMatcher() {
		return matcher;
	}

	public void setMatcher(StringMatcher matcher) {
		this.matcher = matcher;
	}

	public TaskLockFactory getTimerLockFactory() {
		return taskLockFactory;
	}

	public TaskContext getTaskContext(String taskId) {
		TaskContext taskContext = contextMap.get(taskId);
		if (taskContext != null) {
			return taskContext;
		}

		if (taskContext == null) {
			TaskConfig taskConfig = taskFactory.getTaskConfig(taskId);
			if (taskConfig != null) {// 一个本地未注册的任务
				return register(taskConfig, true);
			}
		}
		return null;
	}

	public TaskContext register(TaskConfig taskConfig, boolean throwError) {
		if (taskConfig instanceof ScheduleTaskConfig) {
			return privateSchedule((ScheduleTaskConfig) taskConfig, throwError);
		} else if (taskConfig instanceof CrontabTaskConfig) {
			return privateCrontab((CrontabTaskConfig) taskConfig, throwError);
		}
		return null;
	}

	private TaskContext privateSchedule(ScheduleTaskConfig config, boolean throwError) {
		DefaultTimerTask defaultTimerTask = new DefaultTimerTask(taskLockFactory, config);
		java.util.TimerTask timerTask = new DefaultTimerTaskWrapper(defaultTimerTask);
		TaskContext context = new SimpleTaskContext(timerTask, config);
		if (contextMap.putIfAbsent(config.getTaskId(), context) != null) {
			if (throwError) {
				throw new AlreadyExistsException("已经存在此任务:" + config.getTaskId());
			}
			return null;
		}

		if (config.getPeriod() < 0) {
			timer.schedule(timerTask, config.getTimeUnit().toMillis(config.getDelay()));
		} else {
			timer.schedule(timerTask, config.getTimeUnit().toMillis(config.getDelay()),
					config.getTimeUnit().toMillis(config.getPeriod()));
		}
		return context;
	}

	public TaskContext schedule(ScheduleTaskConfig config) {
		if (!taskFactory.register(config)) {
			throw new AlreadyExistsException("已经存在此任务:" + config.getTaskId());
		}

		logger.info("add Schedule：name={},delay={},period={},timeunit={}", config.getTaskId(), config.getDelay(),
				config.getPeriod(), config.getTimeUnit());
		return privateSchedule(config, true);
	}

	private TaskContext privateCrontab(CrontabTaskConfig config, boolean throwError) {
		CrontabTaskContext context = new CrontabTaskContext(config, new DefaultTimerTask(taskLockFactory, config));
		if (contextMap.putIfAbsent(config.getTaskId(), context) != null) {
			if (throwError) {
				throw new AlreadyExistsException("任务已经存在:" + config.getTaskId());
			}
			return null;
		}

		return context;
	}

	public TaskContext crontab(CrontabTaskConfig config) {
		if (!taskFactory.register(config)) {
			throw new AlreadyExistsException("已经存在此任务:" + config.getTaskId());
		}

		logger.info("add crontab： name={},dayOfWeek={},month={},dayOfMonth={},hour={},minute={}", config.getTaskId(),
				config.getDayOfWeek(), config.getMonth(), config.getDayOfMonth(), config.getHour(), config.getMinute());
		return privateCrontab(config, true);
	}

	public void destroy() {
		timer.cancel();
	}

	private final class PurgeTimerTask extends TimerTask {

		@Override
		public void run() {
			timer.purge();
		}
	}

	private final class CrontabTimerTask extends TimerTask {

		@Override
		public void run() {
			executorService.execute(new CrontabRun(scheduledExecutionTime()));
		}
	}

	private final class DefaultTimerTaskWrapper extends TimerTask {
		private final scw.timer.Task task;

		public DefaultTimerTaskWrapper(scw.timer.Task task) {
			this.task = task;
		}

		@Override
		public void run() {
			executorService.execute(new TaskInvoker(scheduledExecutionTime(), task));
		}
	}

	private final class TaskInvoker implements Runnable {
		private long cts;
		private Task task;

		public TaskInvoker(long cts, Task task) {
			this.cts = cts;
			this.task = task;
		}

		public void run() {
			try {
				task.run(cts);
			} catch (Throwable e) {
				e.printStackTrace();
			}
		}
	}

	private final class CrontabRun implements Runnable {
		private final long cts;

		public CrontabRun(long cts) {
			this.cts = cts;
		}

		public void run() {
			Calendar calendar = Calendar.getInstance();
			calendar.setTimeInMillis(cts);

			for (Entry<String, TaskContext> entry : contextMap.entrySet()) {
				TaskContext context = entry.getValue();
				if (context instanceof CrontabTaskContext) {
					if (((CrontabTaskContext) context).checkTime(calendar)) {
						executorService.execute(new TaskInvoker(cts, ((CrontabTaskContext) context).getTask()));
					}
				}
			}
		}
	}

	private final class ScanningTaskConfigTask extends TimerTask {

		@Override
		public void run() {
			taskFactory.iteratorRegisteredTaskConfig(new IteratorCallback<TaskConfig>() {

				public boolean iteratorCallback(TaskConfig config) {
					TaskContext cacheContext = contextMap.get(config.getTaskId());
					if (cacheContext == null) {
						TaskContext taskContext = register(config, false);
						if (taskContext != null) {
							logger.debug("动态添加任务：" + config.getTaskId());
						}
					}
					return true;
				}
			});
		}

	}

	private final class CrontabTaskContext implements TaskContext {
		private CrontabTaskConfig crontabTaskConfig;
		private final String[] dayOfWeek;
		private final String[] month;
		private final String[] dayOfMonth;
		private final String[] hour;
		private final String[] minute;
		private final Task task;

		public CrontabTaskContext(CrontabTaskConfig crontabTaskConfig, Task task) {
			this.crontabTaskConfig = crontabTaskConfig;
			this.dayOfWeek = StringUtils.commonSplit(crontabTaskConfig.getDayOfWeek());
			this.month = StringUtils.commonSplit(crontabTaskConfig.getMonth());
			this.dayOfMonth = StringUtils.commonSplit(crontabTaskConfig.getDayOfMonth());
			this.hour = StringUtils.commonSplit(crontabTaskConfig.getHour());
			this.minute = StringUtils.commonSplit(crontabTaskConfig.getMinute());
			this.task = task;
		}

		public Task getTask() {
			return task;
		}

		private boolean checkBySplit(int value, String[] check) {
			for (String v : check) {
				if (StringUtils.isEmpty(v)) {
					return true;
				}

				if (matcher.match(v, value + "")) {
					return true;
				}
			}
			return false;
		}

		public boolean checkTime(Calendar calendar) {
			return checkBySplit(calendar.get(Calendar.DAY_OF_WEEK), dayOfWeek)
					&& checkBySplit(calendar.get(Calendar.MONTH), month)
					&& checkBySplit(calendar.get(Calendar.DAY_OF_MONTH), dayOfMonth)
					&& checkBySplit(calendar.get(Calendar.HOUR_OF_DAY), hour)
					&& checkBySplit(calendar.get(Calendar.MINUTE), minute);
		}

		public boolean cancel() {
			if (taskFactory.unregister(crontabTaskConfig)) {
				contextMap.remove(crontabTaskConfig.getTaskId());
				return true;
			}
			return false;
		}

		public TaskConfig getTaskConfig() {
			return crontabTaskConfig;
		}
	}

	private final class SimpleTaskContext implements TaskContext {
		private final TimerTask timerTask;
		private final TaskConfig taskConfig;

		public SimpleTaskContext(TimerTask timerTask, TaskConfig taskConfig) {
			this.timerTask = timerTask;
			this.taskConfig = taskConfig;
		}

		public boolean cancel() {
			if (taskFactory.unregister(taskConfig)) {
				timerTask.cancel();
				return true;
			}
			return false;
		}

		public TaskConfig getTaskConfig() {
			return taskConfig;
		}
	}
}
