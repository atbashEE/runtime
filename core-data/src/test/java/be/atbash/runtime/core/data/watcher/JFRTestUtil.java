/*
 * Copyright 2021 Rudy De Busscher (https://www.atbash.be)
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
package be.atbash.runtime.core.data.watcher;

import jdk.jfr.Configuration;
import jdk.jfr.EventType;
import jdk.jfr.FlightRecorder;
import jdk.jfr.Recording;
import jdk.jfr.consumer.RecordedEvent;
import jdk.jfr.consumer.RecordingFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.fail;

public final class JFRTestUtil {

    private static Recording recording;

    private JFRTestUtil() {
    }

    public static void startFlightRecorder() {
        try {
            if (recording == null) {
                Configuration conf = Configuration.getConfiguration("default");

                recording = new Recording(conf);

                // Disable all standard events
                for (EventType et : FlightRecorder.getFlightRecorder().getEventTypes()) {
                    if (!et.getName().startsWith("be.atbash")) {
                        recording.disable(et.getName());
                    }
                }

                // disable disk writes
                recording.setToDisk(false);
                recording.start();
            }
        } catch (IOException | ParseException e) {
            fail(e);
        }
    }

    public static List<RecordedEvent> stopAndReadEvents() {
        List<RecordedEvent> result = new ArrayList<>();
        Path tempFile;
        try {
            tempFile = Files.createTempFile("atbashRuntimeTest", ".jfr");

            recording.dump(tempFile);
            recording.stop();

            RecordingFile recordingFile = new RecordingFile(tempFile);
            while (recordingFile.hasMoreEvents()) {
                result.add(recordingFile.readEvent());
            }
            recordingFile.close();
            Files.delete(tempFile);
        } catch (IOException e) {
            fail(e);
        }

        recording = null;
        return result;
    }

}
