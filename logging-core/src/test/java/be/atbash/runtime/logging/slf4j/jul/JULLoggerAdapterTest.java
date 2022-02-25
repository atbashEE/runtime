/*
 * Copyright 2021-2022 Rudy De Busscher (https://www.atbash.be)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package be.atbash.runtime.logging.slf4j.jul;

import be.atbash.runtime.logging.EnhancedLogRecord;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.MDC;
import org.slf4j.event.Level;

import java.util.ResourceBundle;
import java.util.logging.Logger;

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class JULLoggerAdapterTest {

    @Mock
    private Logger loggerMock;

    @Mock
    private ResourceBundle resourceBundleMock;

    @Captor
    private ArgumentCaptor<EnhancedLogRecord> enhancedLogRecordCaptor;

    @AfterEach
    public void teardown() {
        MDC.clear();
    }

    @Test
    void handleNormalizedLoggingCall() {

        JULLoggerAdapter adapter = new JULLoggerAdapter(loggerMock);

        adapter.handleNormalizedLoggingCall(Level.INFO, null, "Basic message", new Object[]{}, null);
        Mockito.verify(loggerMock).log(enhancedLogRecordCaptor.capture());

        EnhancedLogRecord capture = enhancedLogRecordCaptor.getValue();
        Assertions.assertThat(capture.getLevel()).isEqualTo(java.util.logging.Level.INFO);
        Assertions.assertThat(capture.getMessage()).isEqualTo("Basic message");
        Assertions.assertThat(capture.getParameters()).isEmpty();
        Assertions.assertThat(capture.getResourceBundle()).isNull();
    }

    @Test
    void log() {
        when(loggerMock.isLoggable(java.util.logging.Level.INFO)).thenReturn(true);
        when(loggerMock.getName()).thenReturn("loggerName");
        JULLoggerAdapter adapter = new JULLoggerAdapter(loggerMock);

        adapter.log(null, this.getClass().getName(), 20, "Basic message", new Object[]{}, null);
        Mockito.verify(loggerMock).log(enhancedLogRecordCaptor.capture());

        EnhancedLogRecord capture = enhancedLogRecordCaptor.getValue();
        Assertions.assertThat(capture.getLevel()).isEqualTo(java.util.logging.Level.INFO);
        Assertions.assertThat(capture.getMessage()).isEqualTo("Basic message");
        Assertions.assertThat(capture.getParameters()).isEmpty();
        Assertions.assertThat(capture.getResourceBundle()).isNull();
        Assertions.assertThat(capture.getLoggerName()).isEqualTo("loggerName");
        Assertions.assertThat(capture.getSourceClassName()).isEqualTo(this.getClass().getName());
        Assertions.assertThat(capture.getSourceMethodName()).isEqualTo("log");
        Assertions.assertThat(capture.getMdc()).isNull();
    }

    @Test
    void log_notLoggable() {
        when(loggerMock.isLoggable(java.util.logging.Level.FINE)).thenReturn(false);
        when(loggerMock.getName()).thenReturn("loggerName");
        JULLoggerAdapter adapter = new JULLoggerAdapter(loggerMock);

        adapter.log(null, this.getClass().getName(), 10, "Debug message", new Object[]{}, null);
        Mockito.verify(loggerMock, never()).log(enhancedLogRecordCaptor.capture());

    }

    @Test
    void log_msgWithSLF4JTemplate() {
        when(loggerMock.isLoggable(java.util.logging.Level.INFO)).thenReturn(true);
        when(loggerMock.getName()).thenReturn("loggerName");
        JULLoggerAdapter adapter = new JULLoggerAdapter(loggerMock);

        adapter.log(null, this.getClass().getName(), 20, "template message, parameter '{}'", new Object[]{"param"}, null);
        Mockito.verify(loggerMock).log(enhancedLogRecordCaptor.capture());

        EnhancedLogRecord capture = enhancedLogRecordCaptor.getValue();
        Assertions.assertThat(capture.getLevel()).isEqualTo(java.util.logging.Level.INFO);
        Assertions.assertThat(capture.getMessage()).isEqualTo("template message, parameter 'param'");
        Assertions.assertThat(capture.getParameters()).isNull();  // since already used for message
        Assertions.assertThat(capture.getResourceBundle()).isNull();
        Assertions.assertThat(capture.getLoggerName()).isEqualTo("loggerName");
        Assertions.assertThat(capture.getSourceClassName()).isEqualTo(this.getClass().getName());
        Assertions.assertThat(capture.getSourceMethodName()).isEqualTo("log_msgWithSLF4JTemplate");
    }

    @Test
    void log_captureMDC() {
        when(loggerMock.isLoggable(java.util.logging.Level.INFO)).thenReturn(true);
        when(loggerMock.getName()).thenReturn("loggerName");
        JULLoggerAdapter adapter = new JULLoggerAdapter(loggerMock);

        MDC.put("key", "value");

        adapter.log(null, this.getClass().getName(), 20, "Basic message", new Object[]{}, null);
        Mockito.verify(loggerMock).log(enhancedLogRecordCaptor.capture());

        EnhancedLogRecord capture = enhancedLogRecordCaptor.getValue();
        Assertions.assertThat(capture.getLevel()).isEqualTo(java.util.logging.Level.INFO);
        Assertions.assertThat(capture.getMessage()).isEqualTo("Basic message");
        Assertions.assertThat(capture.getParameters()).isEmpty();
        Assertions.assertThat(capture.getResourceBundle()).isNull();
        Assertions.assertThat(capture.getLoggerName()).isEqualTo("loggerName");
        Assertions.assertThat(capture.getSourceClassName()).isEqualTo(this.getClass().getName());
        Assertions.assertThat(capture.getSourceMethodName()).isEqualTo("log_captureMDC");
        Assertions.assertThat(capture.getMdc()).containsEntry("key", "value");
    }

    @Test
    void log_propagateParameters() {
        when(loggerMock.isLoggable(java.util.logging.Level.INFO)).thenReturn(true);
        when(loggerMock.getName()).thenReturn("loggerName");
        when(loggerMock.getResourceBundle()).thenReturn(resourceBundleMock);
        JULLoggerAdapter adapter = new JULLoggerAdapter(loggerMock);

        adapter.log(null, this.getClass().getName(), 20, "KEY-001", new Object[]{"Param"}, null);
        Mockito.verify(loggerMock).log(enhancedLogRecordCaptor.capture());

        EnhancedLogRecord capture = enhancedLogRecordCaptor.getValue();
        Assertions.assertThat(capture.getLevel()).isEqualTo(java.util.logging.Level.INFO);
        Assertions.assertThat(capture.getMessage()).isEqualTo("KEY-001");
        Assertions.assertThat(capture.getParameters()).containsExactly("Param");  // propagated ?
        Assertions.assertThat(capture.getResourceBundle()).isEqualTo(resourceBundleMock);  // propagated?
        Assertions.assertThat(capture.getLoggerName()).isEqualTo("loggerName");
        Assertions.assertThat(capture.getSourceClassName()).isEqualTo(this.getClass().getName());
        Assertions.assertThat(capture.getSourceMethodName()).isEqualTo("log_propagateParameters");
        Assertions.assertThat(capture.getMdc()).isNull();
    }
}