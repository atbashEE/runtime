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
package be.atbash.runtime.logging.handler;

import java.io.IOException;
import java.io.OutputStream;

class MeteredStream extends OutputStream {

    private volatile boolean isOpen;

    private OutputStream out;
    private long written;

    MeteredStream(OutputStream out, long written) {
        this.out = out;
        this.written = written;
        isOpen = true;
    }

    @Override
    public void write(int b) throws IOException {
        out.write(b);
        written++;
    }

    @Override
    public void write(byte[] buff) throws IOException {
        out.write(buff);
        written += buff.length;
    }

    @Override
    public void write(byte[] buff, int off, int len) throws IOException {
        out.write(buff, off, len);
        written += len;
    }

    @Override
    public void flush() throws IOException {
        out.flush();
    }

    @Override
    public void close() throws IOException {
        if (isOpen) {
            isOpen = false;
            flush();
            out.close();
        }

    }

    public long getBytesWritten() {
        return written;
    }
}
