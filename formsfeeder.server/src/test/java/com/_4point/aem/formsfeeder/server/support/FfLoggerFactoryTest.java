package com._4point.aem.formsfeeder.server.support;

import static org.junit.jupiter.api.Assertions.*;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.CoreMatchers.*;

import java.util.Objects;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.Marker;


class FfLoggerFactoryTest {

	private static final String TEST_CORRELATION_ID = "TestCorrelationId";
	private static final String TEST_MESSAGE = "Test Error Message";
	private static final Object TEST_OBJECT = null;
	private static final Throwable TEST_EXCEPTION = new IllegalStateException("Test Exception");

	MockLogger mockLogger = new MockLogger();
	Logger underTest = FfLoggerFactory.wrap(TEST_CORRELATION_ID, mockLogger);
	
	@Test
	void testgetName() {
		assertEquals(MockLogger.MOCK_LOGGER_NAME, underTest.getName());
	}

	@Test
	void testIsTraceEnabled() {
		assertTrue(underTest.isTraceEnabled());
	}

	@Test
	void testIsTraceEnabledMarker() {
		assertTrue(underTest.isTraceEnabled((Marker)TEST_OBJECT));
	}

	@Test
	void testIsDebugEnabled() {
		assertTrue(underTest.isDebugEnabled());
	}

	@Test
	void testIsDebugEnabledMarker() {
		assertTrue(underTest.isDebugEnabled((Marker)TEST_OBJECT));
	}

	@Test
	void testIsInfoEnabled() {
		assertTrue(underTest.isInfoEnabled());
	}

	@Test
	void testIsInfoEnabledMarker() {
		assertTrue(underTest.isInfoEnabled((Marker)TEST_OBJECT));
	}

	@Test
	void testIsWarnEnabled() {
		assertTrue(underTest.isWarnEnabled());
	}

	@Test
	void testIsWarnEnabledMarker() {
		assertTrue(underTest.isWarnEnabled((Marker)TEST_OBJECT));
	}

	@Test
	void testIsErrorEnabled() {
		assertTrue(underTest.isErrorEnabled());
	}

	@Test
	void testIsErrorEnabledMarker() {
		assertTrue(underTest.isErrorEnabled((Marker)TEST_OBJECT));
	}

	@Nested
	class TraceTests {
		private final MockLogger.Level expectedLevel = MockLogger.Level.Trace;  

		@AfterEach
		void checkTraceLevel() {
			assertAll(
					()->assertThat(mockLogger.getMsg(), containsString(TEST_MESSAGE)),
					()->assertThat(mockLogger.getMsg(), containsString(TEST_CORRELATION_ID)),
					()->assertEquals(expectedLevel, mockLogger.msgLevel)
					);
		}
		
		@Test
		void testTraceString() {
			underTest.trace(TEST_MESSAGE);
		}

		@Test
		void testTraceStringObject() {
			underTest.trace(TEST_MESSAGE, TEST_OBJECT);
		}

		@Test
		void testTraceStringObjectObject() {
			underTest.trace(TEST_MESSAGE, TEST_OBJECT, TEST_OBJECT);
		}

		@Test
		void testTraceStringObjectArray() {
			underTest.trace(TEST_MESSAGE, new Object[] { TEST_OBJECT, TEST_OBJECT });
		}

		@Test
		void testTraceStringThrowable() {
			underTest.trace(TEST_MESSAGE, TEST_EXCEPTION);
		}

		@Test
		void testTraceMarkerString() {
			underTest.trace((Marker)TEST_OBJECT, TEST_MESSAGE);
		}

		@Test
		void testTraceMarkerStringObject() {
			underTest.trace((Marker)TEST_OBJECT, TEST_MESSAGE, TEST_OBJECT);
		}

		@Test
		void testTraceMarkerStringObjectObject() {
			underTest.trace((Marker)TEST_OBJECT, TEST_MESSAGE, TEST_OBJECT, TEST_OBJECT);
		}

		@Test
		void testTraceMarkerStringObjectArray() {
			underTest.trace((Marker)TEST_OBJECT, TEST_MESSAGE, new Object[] { TEST_OBJECT, TEST_OBJECT });
		}

		@Test
		void testTraceMarkerStringThrowable() {
			underTest.trace((Marker)TEST_OBJECT, TEST_MESSAGE, TEST_EXCEPTION);
		}
	}
	
	@Nested
	class DebugTests {
		private final MockLogger.Level expectedLevel = MockLogger.Level.Debug;  

		@AfterEach
		void checkDebugLevel() {
			assertAll(
					()->assertThat(mockLogger.getMsg(), containsString(TEST_MESSAGE)),
					()->assertThat(mockLogger.getMsg(), containsString(TEST_CORRELATION_ID)),
					()->assertEquals(expectedLevel, mockLogger.msgLevel)
					);
		}
		
		@Test
		void testDebugString() {
			underTest.debug(TEST_MESSAGE);
		}

		@Test
		void testDebugStringObject() {
			underTest.debug(TEST_MESSAGE, TEST_OBJECT);
		}

		@Test
		void testDebugStringObjectObject() {
			underTest.debug(TEST_MESSAGE, TEST_OBJECT, TEST_OBJECT);
		}

		@Test
		void testDebugStringObjectArray() {
			underTest.debug(TEST_MESSAGE, new Object[] { TEST_OBJECT, TEST_OBJECT });
		}

		@Test
		void testDebugStringThrowable() {
			underTest.debug(TEST_MESSAGE, TEST_EXCEPTION);
		}

		@Test
		void testDebugMarkerString() {
			underTest.debug((Marker)TEST_OBJECT, TEST_MESSAGE);
		}

		@Test
		void testDebugMarkerStringObject() {
			underTest.debug((Marker)TEST_OBJECT, TEST_MESSAGE, TEST_OBJECT);
		}

		@Test
		void testDebugMarkerStringObjectObject() {
			underTest.debug((Marker)TEST_OBJECT, TEST_MESSAGE, TEST_OBJECT, TEST_OBJECT);
		}

		@Test
		void testDebugMarkerStringObjectArray() {
			underTest.debug((Marker)TEST_OBJECT, TEST_MESSAGE, new Object[] { TEST_OBJECT, TEST_OBJECT });
		}

		@Test
		void testDebugMarkerStringThrowable() {
			underTest.debug((Marker)TEST_OBJECT, TEST_MESSAGE, TEST_EXCEPTION);
		}
	}
	
	@Nested
	class InfoTests {
		private final MockLogger.Level expectedLevel = MockLogger.Level.Info;  

		@AfterEach
		void checkInfoLevel() {
			assertAll(
					()->assertThat(mockLogger.getMsg(), containsString(TEST_MESSAGE)),
					()->assertThat(mockLogger.getMsg(), containsString(TEST_CORRELATION_ID)),
					()->assertEquals(expectedLevel, mockLogger.msgLevel)
					);
		}
		
		@Test
		void testInfoString() {
			underTest.info(TEST_MESSAGE);
		}

		@Test
		void testInfoStringObject() {
			underTest.info(TEST_MESSAGE, TEST_OBJECT);
		}

		@Test
		void testInfoStringObjectObject() {
			underTest.info(TEST_MESSAGE, TEST_OBJECT, TEST_OBJECT);
		}

		@Test
		void testInfoStringObjectArray() {
			underTest.info(TEST_MESSAGE, new Object[] { TEST_OBJECT, TEST_OBJECT });
		}

		@Test
		void testInfoStringThrowable() {
			underTest.info(TEST_MESSAGE, TEST_EXCEPTION);
		}

		@Test
		void testInfoMarkerString() {
			underTest.info((Marker)TEST_OBJECT, TEST_MESSAGE);
		}

		@Test
		void testInfoMarkerStringObject() {
			underTest.info((Marker)TEST_OBJECT, TEST_MESSAGE, TEST_OBJECT);
		}

		@Test
		void testInfoMarkerStringObjectObject() {
			underTest.info((Marker)TEST_OBJECT, TEST_MESSAGE, TEST_OBJECT, TEST_OBJECT);
		}

		@Test
		void testInfoMarkerStringObjectArray() {
			underTest.info((Marker)TEST_OBJECT, TEST_MESSAGE, new Object[] { TEST_OBJECT, TEST_OBJECT });
		}

		@Test
		void testInfoMarkerStringThrowable() {
			underTest.info((Marker)TEST_OBJECT, TEST_MESSAGE, TEST_EXCEPTION);
		}
	}
	
	@Nested
	class WarnTests {
		private final MockLogger.Level expectedLevel = MockLogger.Level.Warn;  

		@AfterEach
		void checkWarnLevel() {
			assertAll(
					()->assertThat(mockLogger.getMsg(), containsString(TEST_MESSAGE)),
					()->assertThat(mockLogger.getMsg(), containsString(TEST_CORRELATION_ID)),
					()->assertEquals(expectedLevel, mockLogger.msgLevel)
					);
		}
		
		@Test
		void testWarnString() {
			underTest.warn(TEST_MESSAGE);
		}

		@Test
		void testWarnStringObject() {
			underTest.warn(TEST_MESSAGE, TEST_OBJECT);
		}

		@Test
		void testWarnStringObjectObject() {
			underTest.warn(TEST_MESSAGE, TEST_OBJECT, TEST_OBJECT);
		}

		@Test
		void testWarnStringObjectArray() {
			underTest.warn(TEST_MESSAGE, new Object[] { TEST_OBJECT, TEST_OBJECT });
		}

		@Test
		void testWarnStringThrowable() {
			underTest.warn(TEST_MESSAGE, TEST_EXCEPTION);
		}

		@Test
		void testWarnMarkerString() {
			underTest.warn((Marker)TEST_OBJECT, TEST_MESSAGE);
		}

		@Test
		void testWarnMarkerStringObject() {
			underTest.warn((Marker)TEST_OBJECT, TEST_MESSAGE, TEST_OBJECT);
		}

		@Test
		void testWarnMarkerStringObjectObject() {
			underTest.warn((Marker)TEST_OBJECT, TEST_MESSAGE, TEST_OBJECT, TEST_OBJECT);
		}

		@Test
		void testWarnMarkerStringObjectArray() {
			underTest.warn((Marker)TEST_OBJECT, TEST_MESSAGE, new Object[] { TEST_OBJECT, TEST_OBJECT });
		}

		@Test
		void testWarnMarkerStringThrowable() {
			underTest.warn((Marker)TEST_OBJECT, TEST_MESSAGE, TEST_EXCEPTION);
		}
	}
	
	@Nested
	class ErrorTests {
		private final MockLogger.Level expectedLevel = MockLogger.Level.Error;  

		@AfterEach
		void checkErrorLevel() {
			assertAll(
					()->assertThat(mockLogger.getMsg(), containsString(TEST_MESSAGE)),
					()->assertThat(mockLogger.getMsg(), containsString(TEST_CORRELATION_ID)),
					()->assertEquals(expectedLevel, mockLogger.msgLevel)
					);
		}
		
		@Test
		void testErrorString() {
			underTest.error(TEST_MESSAGE);
		}

		@Test
		void testErrorStringObject() {
			underTest.error(TEST_MESSAGE, TEST_OBJECT);
		}

		@Test
		void testErrorStringObjectObject() {
			underTest.error(TEST_MESSAGE, TEST_OBJECT, TEST_OBJECT);
		}

		@Test
		void testErrorStringObjectArray() {
			underTest.error(TEST_MESSAGE, new Object[] { TEST_OBJECT, TEST_OBJECT });
		}

		@Test
		void testErrorStringThrowable() {
			underTest.error(TEST_MESSAGE, TEST_EXCEPTION);
		}

		@Test
		void testErrorMarkerString() {
			underTest.error((Marker)TEST_OBJECT, TEST_MESSAGE);
		}

		@Test
		void testErrorMarkerStringObject() {
			underTest.error((Marker)TEST_OBJECT, TEST_MESSAGE, TEST_OBJECT);
		}

		@Test
		void testErrorMarkerStringObjectObject() {
			underTest.error((Marker)TEST_OBJECT, TEST_MESSAGE, TEST_OBJECT, TEST_OBJECT);
		}

		@Test
		void testErrorMarkerStringObjectArray() {
			underTest.error((Marker)TEST_OBJECT, TEST_MESSAGE, new Object[] { TEST_OBJECT, TEST_OBJECT });
		}

		@Test
		void testErrorMarkerStringThrowable() {
			underTest.error((Marker)TEST_OBJECT, TEST_MESSAGE, TEST_EXCEPTION);
		}
	}
	


	private static class MockLogger implements Logger {
		
		private static final String MOCK_LOGGER_NAME = "MockLogger";

		public enum Level { Trace, Debug, Info, Warn, Error };
		
		private String msg = null;
		private Level msgLevel = null;

		public String getMsg() {
			return Objects.requireNonNull(msg, "Mock Logger message has not been initialized.");
		}

		public Level getMsgLevel() {
			return msgLevel;
		}

		@Override
		public String getName() {
			return MOCK_LOGGER_NAME;
		}

		@Override
		public boolean isTraceEnabled() {
			return true;
		}

		@Override
		public void trace(String msg) {
			this.msg = msg;
			this.msgLevel = Level.Trace;
		}

		@Override
		public void trace(String format, Object arg) {
			this.msg = format;
			this.msgLevel = Level.Trace;
		}

		@Override
		public void trace(String format, Object arg1, Object arg2) {
			this.msg = format;
			this.msgLevel = Level.Trace;
		}

		@Override
		public void trace(String format, Object... arguments) {
			this.msg = format;
			this.msgLevel = Level.Trace;
		}

		@Override
		public void trace(String msg, Throwable t) {
			this.msg = msg;
			this.msgLevel = Level.Trace;
		}

		@Override
		public boolean isTraceEnabled(Marker marker) {
			return true;
		}

		@Override
		public void trace(Marker marker, String msg) {
			this.msg = msg;
			this.msgLevel = Level.Trace;
		}

		@Override
		public void trace(Marker marker, String format, Object arg) {
			this.msg = format;
			this.msgLevel = Level.Trace;
		}

		@Override
		public void trace(Marker marker, String format, Object arg1, Object arg2) {
			this.msg = format;
			this.msgLevel = Level.Trace;
		}

		@Override
		public void trace(Marker marker, String format, Object... argArray) {
			this.msg = format;
			this.msgLevel = Level.Trace;
		}

		@Override
		public void trace(Marker marker, String msg, Throwable t) {
			this.msg = msg;
			this.msgLevel = Level.Trace;
		}

		@Override
		public boolean isDebugEnabled() {
			return true;
		}

		@Override
		public void debug(String msg) {
			this.msg = msg;
			this.msgLevel = Level.Debug;
		}

		@Override
		public void debug(String format, Object arg) {
			this.msg = format;
			this.msgLevel = Level.Debug;
		}

		@Override
		public void debug(String format, Object arg1, Object arg2) {
			this.msg = format;
			this.msgLevel = Level.Debug;
		}

		@Override
		public void debug(String format, Object... arguments) {
			this.msg = format;
			this.msgLevel = Level.Debug;
		}

		@Override
		public void debug(String msg, Throwable t) {
			this.msg = msg;
			this.msgLevel = Level.Debug;
		}

		@Override
		public boolean isDebugEnabled(Marker marker) {
			return true;
		}

		@Override
		public void debug(Marker marker, String msg) {
			this.msg = msg;
			this.msgLevel = Level.Debug;
		}

		@Override
		public void debug(Marker marker, String format, Object arg) {
			this.msg = format;
			this.msgLevel = Level.Debug;
		}

		@Override
		public void debug(Marker marker, String format, Object arg1, Object arg2) {
			this.msg = format;
			this.msgLevel = Level.Debug;
		}

		@Override
		public void debug(Marker marker, String format, Object... arguments) {
			this.msg = format;
			this.msgLevel = Level.Debug;
		}

		@Override
		public void debug(Marker marker, String msg, Throwable t) {
			this.msg = msg;
			this.msgLevel = Level.Debug;
		}

		@Override
		public boolean isInfoEnabled() {
			return true;
		}

		@Override
		public void info(String msg) {
			this.msg = msg;
			this.msgLevel = Level.Info;
		}

		@Override
		public void info(String format, Object arg) {
			this.msg = format;
			this.msgLevel = Level.Info;
		}

		@Override
		public void info(String format, Object arg1, Object arg2) {
			this.msg = format;
			this.msgLevel = Level.Info;
		}

		@Override
		public void info(String format, Object... arguments) {
			this.msg = format;
			this.msgLevel = Level.Info;
		}

		@Override
		public void info(String msg, Throwable t) {
			this.msg = msg;
			this.msgLevel = Level.Info;
		}

		@Override
		public boolean isInfoEnabled(Marker marker) {
			return true;
		}

		@Override
		public void info(Marker marker, String msg) {
			this.msg = msg;
			this.msgLevel = Level.Info;
		}

		@Override
		public void info(Marker marker, String format, Object arg) {
			this.msg = format;
			this.msgLevel = Level.Info;
		}

		@Override
		public void info(Marker marker, String format, Object arg1, Object arg2) {
			this.msg = format;
			this.msgLevel = Level.Info;
		}

		@Override
		public void info(Marker marker, String format, Object... arguments) {
			this.msg = format;
			this.msgLevel = Level.Info;
		}

		@Override
		public void info(Marker marker, String msg, Throwable t) {
			this.msg = msg;
			this.msgLevel = Level.Info;
		}

		@Override
		public boolean isWarnEnabled() {
			return true;
		}

		@Override
		public void warn(String msg) {
			this.msg = msg;
			this.msgLevel = Level.Warn;
		}

		@Override
		public void warn(String format, Object arg) {
			this.msg = format;
			this.msgLevel = Level.Warn;
		}

		@Override
		public void warn(String format, Object... arguments) {
			this.msg = format;
			this.msgLevel = Level.Warn;
		}

		@Override
		public void warn(String format, Object arg1, Object arg2) {
			this.msg = format;
			this.msgLevel = Level.Warn;
		}

		@Override
		public void warn(String msg, Throwable t) {
			this.msg = msg;
			this.msgLevel = Level.Warn;
		}

		@Override
		public boolean isWarnEnabled(Marker marker) {
			return true;
		}

		@Override
		public void warn(Marker marker, String msg) {
			this.msg = msg;
			this.msgLevel = Level.Warn;
		}

		@Override
		public void warn(Marker marker, String format, Object arg) {
			this.msg = format;
			this.msgLevel = Level.Warn;
		}

		@Override
		public void warn(Marker marker, String format, Object arg1, Object arg2) {
			this.msg = format;
			this.msgLevel = Level.Warn;
		}

		@Override
		public void warn(Marker marker, String format, Object... arguments) {
			this.msg = format;
			this.msgLevel = Level.Warn;
		}

		@Override
		public void warn(Marker marker, String msg, Throwable t) {
			this.msg = msg;
			this.msgLevel = Level.Warn;
		}

		@Override
		public boolean isErrorEnabled() {
			return true;
		}

		@Override
		public void error(String msg) {
			this.msg = msg;
			this.msgLevel = Level.Error;
		}

		@Override
		public void error(String format, Object arg) {
			this.msg = format;
			this.msgLevel = Level.Error;
		}

		@Override
		public void error(String format, Object arg1, Object arg2) {
			this.msg = format;
			this.msgLevel = Level.Error;
		}

		@Override
		public void error(String format, Object... arguments) {
			this.msg = format;
			this.msgLevel = Level.Error;
		}

		@Override
		public void error(String msg, Throwable t) {
			this.msg = msg;
			this.msgLevel = Level.Error;
		}

		@Override
		public boolean isErrorEnabled(Marker marker) {
			return true;
		}

		@Override
		public void error(Marker marker, String msg) {
			this.msg = msg;
			this.msgLevel = Level.Error;
		}

		@Override
		public void error(Marker marker, String format, Object arg) {
			this.msg = format;
			this.msgLevel = Level.Error;
		}

		@Override
		public void error(Marker marker, String format, Object arg1, Object arg2) {
			this.msg = format;
			this.msgLevel = Level.Error;
		}

		@Override
		public void error(Marker marker, String format, Object... arguments) {
			this.msg = format;
			this.msgLevel = Level.Error;
		}

		@Override
		public void error(Marker marker, String msg, Throwable t) {
			this.msg = msg;
			this.msgLevel = Level.Error;
		}
		
	}
}
