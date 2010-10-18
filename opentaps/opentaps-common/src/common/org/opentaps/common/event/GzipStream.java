/*
 * Copyright (c) Open Source Strategies, Inc.
 *
 * Opentaps is free software: you can redistribute it and/or modify it
 * under the terms of the GNU Affero General Public License as published
 * by the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Opentaps is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with Opentaps.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.opentaps.common.event;

import java.io.IOException;
import java.util.zip.GZIPOutputStream;

import javax.servlet.ServletOutputStream;

/**
 * Gzip stream for gzip the transfer data between client and server.
 */
public class GzipStream extends ServletOutputStream {
    private ServletOutputStream out;
    private GZIPOutputStream gzipStream;

    public GzipStream(ServletOutputStream out) throws IOException {
        this.out = out;
        reset();
    }

    /** {@inheritDoc} */
    public void close() throws IOException {
        gzipStream.close();
    }

    /** {@inheritDoc} */
    public void flush() throws IOException {
        gzipStream.flush();
    }

    /** {@inheritDoc} */
    public void write(byte[] b) throws IOException {
        write(b, 0, b.length);
    }

    /** {@inheritDoc} */
    public void write(byte[] b, int off, int len) throws IOException {
        gzipStream.write(b, off, len);
    }

    /** {@inheritDoc} */
    public void write(int b) throws IOException {
        gzipStream.write(b);
    }

    /** {@inheritDoc} */
    public void reset() throws IOException {
        gzipStream = new GZIPOutputStream(out);
    }
}
