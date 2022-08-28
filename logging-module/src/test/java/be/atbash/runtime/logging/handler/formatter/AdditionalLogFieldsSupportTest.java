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
package be.atbash.runtime.logging.handler.formatter;

import be.atbash.runtime.logging.testing.LoggingEvent;
import be.atbash.runtime.logging.testing.TestLogMessages;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.logging.Level;

class AdditionalLogFieldsSupportTest {

    @AfterEach
    public void teardown() {
        TestLogMessages.reset();
    }

    @Test
    void isSet() {
        AdditionalLogFieldsSupport fieldsSupport = new AdditionalLogFieldsSupport("");
        Assertions.assertThat(fieldsSupport.isSet(AdditionalLogFieldsSupport.SupplementalAttribute.TID)).isFalse();
        Assertions.assertThat(fieldsSupport.isSet(AdditionalLogFieldsSupport.SupplementalAttribute.TIME_MILLIS)).isFalse();
        Assertions.assertThat(fieldsSupport.isSet(AdditionalLogFieldsSupport.SupplementalAttribute.LEVEL_VALUE)).isFalse();

        List<LoggingEvent> loggingEvents = TestLogMessages.getLoggingEvents();
        Assertions.assertThat(loggingEvents).isEmpty();
    }

    @Test
    void isSet_wrongName() {
        TestLogMessages.init();
        AdditionalLogFieldsSupport fieldsSupport = new AdditionalLogFieldsSupport("Atbash");
        Assertions.assertThat(fieldsSupport.isSet(AdditionalLogFieldsSupport.SupplementalAttribute.TID)).isFalse();
        Assertions.assertThat(fieldsSupport.isSet(AdditionalLogFieldsSupport.SupplementalAttribute.TIME_MILLIS)).isFalse();
        Assertions.assertThat(fieldsSupport.isSet(AdditionalLogFieldsSupport.SupplementalAttribute.LEVEL_VALUE)).isFalse();

        List<LoggingEvent> loggingEvents = TestLogMessages.getLoggingEvents();
        Assertions.assertThat(loggingEvents).hasSize(1);
        Assertions.assertThat(loggingEvents.get(0).getLevel()).isEqualTo(Level.WARNING);
        Assertions.assertThat(loggingEvents.get(0).getMessage()).isEqualTo("LOG-011: Unknown Exclude Field provided : 'Atbash'");
    }
    @Test
    void isSet_correctValues() {
        TestLogMessages.init();
        AdditionalLogFieldsSupport fieldsSupport = new AdditionalLogFieldsSupport("tid,timeMillis,levelValue");
        Assertions.assertThat(fieldsSupport.isSet(AdditionalLogFieldsSupport.SupplementalAttribute.TID)).isTrue();
        Assertions.assertThat(fieldsSupport.isSet(AdditionalLogFieldsSupport.SupplementalAttribute.TIME_MILLIS)).isTrue();
        Assertions.assertThat(fieldsSupport.isSet(AdditionalLogFieldsSupport.SupplementalAttribute.LEVEL_VALUE)).isTrue();

        List<LoggingEvent> loggingEvents = TestLogMessages.getLoggingEvents();
        Assertions.assertThat(loggingEvents).isEmpty();

    }
}