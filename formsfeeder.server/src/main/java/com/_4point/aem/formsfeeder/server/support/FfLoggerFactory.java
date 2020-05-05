package com._4point.aem.formsfeeder.server.support;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.Marker;

public class FfLoggerFactory {

	public static Logger getLogger(String correlationId, Class<?> clazz) {
		return new FsLogger(correlationId, LoggerFactory.getLogger(clazz));
	}
	
	public static Logger wrap(String correlationId, Logger logger) {
		return new FsLogger(correlationId, logger);
	}
	
	public static class FsLogger implements Logger {

		private static final String CORRELATION_ID_MSG_PREFIX = "correlationId = ";
		
		final String msgPrefix;
		final String correlationId;
		final Logger myLogger;
		
		public FsLogger(String correlationId, Logger myLogger) {
			super();
			this.correlationId = correlationId;
			this.myLogger = myLogger;
			this.msgPrefix = CORRELATION_ID_MSG_PREFIX + correlationId + ": ";
		}

		@Override
		public String getName() {
			return myLogger.getName();
		}

		@Override
		public boolean isTraceEnabled() {
			return myLogger.isTraceEnabled();
		}

		@Override
		public void trace(String msg) {
			myLogger.trace(msgPrefix + msg);
		}

		@Override
		public void trace(String format, Object arg) {
			myLogger.trace(msgPrefix + format, arg);
		}

		@Override
		public void trace(String format, Object arg1, Object arg2) {
			myLogger.trace(msgPrefix + format, arg1, arg2);
		}

		@Override
		public void trace(String format, Object... arguments) {
			myLogger.trace(msgPrefix + format, arguments);
		}

		@Override
		public void trace(String msg, Throwable t) {
			myLogger.trace(msgPrefix + msg, t);
		}

		@Override
		public boolean isTraceEnabled(Marker marker) {
			return myLogger.isTraceEnabled(marker);
		}

		@Override
		public void trace(Marker marker, String msg) {
			myLogger.trace(marker, msgPrefix + msg);
		}

		@Override
		public void trace(Marker marker, String format, Object arg) {
			myLogger.trace(marker, msgPrefix + format, arg);
		}

		@Override
		public void trace(Marker marker, String format, Object arg1, Object arg2) {
			myLogger.trace(marker, msgPrefix + format, arg1, arg2);
		}

		@Override
		public void trace(Marker marker, String format, Object... argArray) {
			myLogger.trace(marker, msgPrefix + format, argArray);
		}

		@Override
		public void trace(Marker marker, String msg, Throwable t) {
			myLogger.trace(marker, msgPrefix + msg, t);
		}

		@Override
		public boolean isDebugEnabled() {
			return myLogger.isDebugEnabled();
		}

		@Override
		public void debug(String msg) {
			myLogger.debug(msgPrefix + msg);
		}

		@Override
		public void debug(String format, Object arg) {
			myLogger.debug(msgPrefix + format, arg);
		}

		@Override
		public void debug(String format, Object arg1, Object arg2) {
			myLogger.debug(msgPrefix + format, arg1, arg2);
		}

		@Override
		public void debug(String format, Object... arguments) {
			myLogger.debug(msgPrefix + format, arguments);
		}

		@Override
		public void debug(String msg, Throwable t) {
			myLogger.debug(msgPrefix + msg, t);
		}

		@Override
		public boolean isDebugEnabled(Marker marker) {
			return myLogger.isDebugEnabled(marker);
		}

		@Override
		public void debug(Marker marker, String msg) {
			myLogger.debug(marker, msgPrefix + msg);
		}

		@Override
		public void debug(Marker marker, String format, Object arg) {
			myLogger.debug(marker, msgPrefix + format, arg);
		}

		@Override
		public void debug(Marker marker, String format, Object arg1, Object arg2) {
			myLogger.debug(marker, msgPrefix + format, arg1, arg2);
		}

		@Override
		public void debug(Marker marker, String format, Object... arguments) {
			myLogger.debug(marker, msgPrefix + format, arguments);
		}

		@Override
		public void debug(Marker marker, String msg, Throwable t) {
			myLogger.debug(marker, msgPrefix + msg, t);
		}

		@Override
		public boolean isInfoEnabled() {
			return myLogger.isInfoEnabled();
		}

		@Override
		public void info(String msg) {
			myLogger.info(msgPrefix + msg);
		}

		@Override
		public void info(String format, Object arg) {
			myLogger.info(msgPrefix + format, arg);
		}

		@Override
		public void info(String format, Object arg1, Object arg2) {
			myLogger.info(msgPrefix + format, arg1, arg2);
		}

		@Override
		public void info(String format, Object... arguments) {
			myLogger.info(msgPrefix + format, arguments);
		}

		@Override
		public void info(String msg, Throwable t) {
			myLogger.info(msgPrefix + msg, t);
		}

		@Override
		public boolean isInfoEnabled(Marker marker) {
			return myLogger.isInfoEnabled(marker);
		}

		@Override
		public void info(Marker marker, String msg) {
			myLogger.info(marker, msgPrefix + msg);
		}

		@Override
		public void info(Marker marker, String format, Object arg) {
			myLogger.info(marker, msgPrefix + format, arg);
		}

		@Override
		public void info(Marker marker, String format, Object arg1, Object arg2) {
			myLogger.info(marker, msgPrefix + format, arg1, arg2);
		}

		@Override
		public void info(Marker marker, String format, Object... arguments) {
			myLogger.info(marker, msgPrefix + format, arguments);
		}

		@Override
		public void info(Marker marker, String msg, Throwable t) {
			myLogger.info(marker, msgPrefix + msg, t);
		}

		@Override
		public boolean isWarnEnabled() {
			return myLogger.isWarnEnabled();
		}

		@Override
		public void warn(String msg) {
			myLogger.warn(msgPrefix + msg);
		}

		@Override
		public void warn(String format, Object arg) {
			myLogger.warn(msgPrefix + format, arg);
		}

		@Override
		public void warn(String format, Object... arguments) {
			myLogger.warn(msgPrefix + format, arguments);
		}

		@Override
		public void warn(String format, Object arg1, Object arg2) {
			myLogger.warn(msgPrefix + format, arg1, arg2);
		}

		@Override
		public void warn(String msg, Throwable t) {
			myLogger.warn(msgPrefix + msg, t);
		}

		@Override
		public boolean isWarnEnabled(Marker marker) {
			return myLogger.isWarnEnabled(marker);
		}

		@Override
		public void warn(Marker marker, String msg) {
			myLogger.warn(marker, msgPrefix + msg);
		}

		@Override
		public void warn(Marker marker, String format, Object arg) {
			myLogger.warn(marker, msgPrefix + format, arg);
		}

		@Override
		public void warn(Marker marker, String format, Object arg1, Object arg2) {
			myLogger.warn(marker, msgPrefix + format, arg1, arg2);
		}

		@Override
		public void warn(Marker marker, String format, Object... arguments) {
			myLogger.warn(marker, msgPrefix + format, arguments);
		}

		@Override
		public void warn(Marker marker, String msg, Throwable t) {
			myLogger.warn(marker, msgPrefix + msg, t);
		}

		@Override
		public boolean isErrorEnabled() {
			return myLogger.isErrorEnabled();
		}

		@Override
		public void error(String msg) {
			myLogger.error(msgPrefix + msg);
		}

		@Override
		public void error(String format, Object arg) {
			myLogger.error(msgPrefix + format, arg);
		}

		@Override
		public void error(String format, Object arg1, Object arg2) {
			myLogger.error(msgPrefix + format, arg1, arg2);
		}

		@Override
		public void error(String format, Object... arguments) {
			myLogger.error(msgPrefix + format, arguments);
		}

		@Override
		public void error(String msg, Throwable t) {
			myLogger.error(msgPrefix + msg, t);
		}

		@Override
		public boolean isErrorEnabled(Marker marker) {
			return myLogger.isErrorEnabled(marker);
		}

		@Override
		public void error(Marker marker, String msg) {
			myLogger.error(marker, msgPrefix + msg);
		}

		@Override
		public void error(Marker marker, String format, Object arg) {
			myLogger.error(marker, msgPrefix + format, arg);
		}

		@Override
		public void error(Marker marker, String format, Object arg1, Object arg2) {
			myLogger.error(marker, msgPrefix + format, arg1, arg2);
		}

		@Override
		public void error(Marker marker, String format, Object... arguments) {
			myLogger.error(marker, msgPrefix + format, arguments);
		}

		@Override
		public void error(Marker marker, String msg, Throwable t) {
			myLogger.error(marker, msgPrefix + msg, t);
		}
		
	}
}
