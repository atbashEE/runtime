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
package be.atbash.runtime.common.command.util;

import be.atbash.runtime.core.data.exception.UnexpectedException;

import java.io.IOException;
import java.io.InputStream;
import java.net.http.HttpRequest;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;

// Based on code from https://github.com/softwaremill/sttp
public class MultipartBodyPublisher {

    public static final String BOUNDARY = "AtbashRuntimeUploadBoundary";
    private final List<PartsSpecification> partsSpecificationList = new ArrayList<>();

    public HttpRequest.BodyPublisher build() {
        if (partsSpecificationList.size() == 0) {
            // This is a developer error, so ok to throw the IllegalStateException
            throw new IllegalStateException("Must have at least one part to build multipart message.");
        }
        addFinalBoundaryPart();
        return HttpRequest.BodyPublishers.ofByteArrays(PartsIterator::new);
    }

    public void addPart(String name, Path value) {
        PartsSpecification newPart = new PartsSpecification();
        newPart.type = PartsSpecification.TYPE.FILE;
        newPart.name = name;
        newPart.path = value;
        partsSpecificationList.add(newPart);
    }

    private void addFinalBoundaryPart() {
        PartsSpecification newPart = new PartsSpecification();
        newPart.type = PartsSpecification.TYPE.FINAL_BOUNDARY;
        newPart.value = "--" + BOUNDARY + "--";
        partsSpecificationList.add(newPart);
    }

    private static class PartsSpecification {

        public enum TYPE {
            FILE, FINAL_BOUNDARY
        }

        private PartsSpecification.TYPE type;
        private String name;
        private String value;
        private Path path;

    }

    private class PartsIterator implements Iterator<byte[]> {

        private final Iterator<PartsSpecification> iterator;
        private InputStream currentFileInput;

        private boolean done;
        private byte[] next;

        PartsIterator() {
            iterator = partsSpecificationList.iterator();
        }

        @Override
        public boolean hasNext() {
            if (done) {
                return false;
            }
            if (next != null) {
                return true;
            }
            try {
                next = computeNext();
            } catch (IOException e) {
                throw new UnexpectedException(UnexpectedException.UnexpectedExceptionCode.UE002, e);
            }
            if (next == null) {
                done = true;
                return false;
            }
            return true;
        }

        @Override
        public byte[] next() {
            if (!hasNext()) {
                throw new NoSuchElementException();
            }
            byte[] res = next;
            next = null;
            return res;
        }

        private byte[] computeNext() throws IOException {
            if (currentFileInput == null) {
                if (!iterator.hasNext()) {
                    return null;
                }
                PartsSpecification nextPart = iterator.next();
                if (PartsSpecification.TYPE.FINAL_BOUNDARY.equals(nextPart.type)) {
                    return nextPart.value.getBytes(StandardCharsets.UTF_8);
                }
                String filename = null;
                String contentType = null;
                if (PartsSpecification.TYPE.FILE.equals(nextPart.type)) {
                    Path path = nextPart.path;
                    filename = path.getFileName().toString();
                    contentType = Files.probeContentType(path);
                    if (contentType == null) {
                        // This Maven Module doesn not have access to JAX-RS api where mime types ar declared.
                        contentType = "application/octet-stream";
                    }
                    currentFileInput = Files.newInputStream(path);
                }
                String partHeader =
                        "--" + BOUNDARY + "\r\n" +
                                "Content-Disposition: form-data; name=" + nextPart.name + "; filename=" + filename + "\r\n" +
                                "Content-Type: " + contentType + "\r\n\r\n";
                return partHeader.getBytes(StandardCharsets.UTF_8);
            } else {
                byte[] buf = new byte[8192];
                int r = currentFileInput.read(buf);
                if (r > 0) {
                    byte[] actualBytes = new byte[r];
                    System.arraycopy(buf, 0, actualBytes, 0, r);
                    return actualBytes;
                } else {
                    currentFileInput.close();
                    currentFileInput = null;
                    return "\r\n".getBytes(StandardCharsets.UTF_8);
                }
            }
        }
    }
}
