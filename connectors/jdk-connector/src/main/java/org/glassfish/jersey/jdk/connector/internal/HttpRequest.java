/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2015-2017 Oracle and/or its affiliates. All rights reserved.
 *
 * The contents of this file are subject to the terms of either the GNU
 * General Public License Version 2 only ("GPL") or the Common Development
 * and Distribution License("CDDL") (collectively, the "License").  You
 * may not use this file except in compliance with the License.  You can
 * obtain a copy of the License at
 * https://oss.oracle.com/licenses/CDDL+GPL-1.1
 * or LICENSE.txt.  See the License for the specific
 * language governing permissions and limitations under the License.
 *
 * When distributing the software, include this License Header Notice in each
 * file and include the License file at LICENSE.txt.
 *
 * GPL Classpath Exception:
 * Oracle designates this particular file as subject to the "Classpath"
 * exception as provided by Oracle in the GPL Version 2 section of the License
 * file that accompanied this code.
 *
 * Modifications:
 * If applicable, add the following below the License Header, with the fields
 * enclosed by brackets [] replaced by your own identifying information:
 * "Portions Copyright [year] [name of copyright owner]"
 *
 * Contributor(s):
 * If you wish your version of this file to be governed by only the CDDL or
 * only the GPL Version 2, indicate your decision by adding "[Contributor]
 * elects to include this software in this distribution under the [CDDL or GPL
 * Version 2] license."  If you don't indicate a single choice of license, a
 * recipient has the option to distribute your version of this file under
 * either the CDDL, the GPL Version 2 or to extend the choice of license to
 * its licensees as provided above.  However, if you add GPL Version 2 code
 * and therefore, elected the GPL Version 2 license, then the option applies
 * only if the new code is made subject to such option by the copyright
 * holder.
 */

package org.glassfish.jersey.jdk.connector.internal;

import java.net.URI;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author Petr Janouch (petr.janouch at oracle.com)
 */
class HttpRequest {

    private final String method;
    private final URI uri;
    private final Map<String, List<String>> headers = new HashMap<>();
    private final BodyMode bodyMode;
    private final BodyOutputStream bodyStream;

    private HttpRequest(String method, URI uri, BodyMode bodyMode, BodyOutputStream bodyStream) {
        this.method = method;
        this.uri = uri;
        this.bodyMode = bodyMode;
        this.bodyStream = bodyStream;
    }

    static HttpRequest createBodyless(String method, URI uri) {
        HttpRequest httpRequest = new HttpRequest(method, uri, BodyMode.NONE, null);
        return httpRequest;
    }

    static HttpRequest createChunked(String method, URI uri, int chunkSize) {
        ChunkedBodyOutputStream bodyStream = new ChunkedBodyOutputStream(chunkSize);
        return new HttpRequest(method, uri, BodyMode.CHUNKED, bodyStream);
    }

    static HttpRequest createBuffered(String method, URI uri) {
        BufferedBodyOutputStream bodyOutputStream = new BufferedBodyOutputStream();
        return new HttpRequest(method, uri, BodyMode.BUFFERED, bodyOutputStream);
    }

    String getMethod() {
        return method;
    }

    URI getUri() {
        return uri;
    }

    Map<String, List<String>> getHeaders() {
        return headers;
    }

    BodyMode getBodyMode() {
        return bodyMode;
    }

    BodyOutputStream getBodyStream() {
        if (BodyMode.NONE == bodyMode) {
            throw new IllegalStateException(LocalizationMessages.HTTP_REQUEST_NO_BODY());
        }

        return bodyStream;
    }

    void addHeaderIfNotPresent(String name, String value) {
        List<String> values = headers.get(name);
        if (values == null) {
            values = new ArrayList<>(1);
            headers.put(name, values);
            values.add(value);
        }
    }

    ByteBuffer getBufferedBody() {
        if (BodyMode.BUFFERED != bodyMode) {
            throw new IllegalStateException(LocalizationMessages.HTTP_REQUEST_NO_BUFFERED_BODY());
        }

        return ((BufferedBodyOutputStream) bodyStream).toBuffer();
    }

    int getBodySize() {
        if (bodyMode == BodyMode.CHUNKED) {
            throw new IllegalStateException(LocalizationMessages.HTTP_REQUEST_BODY_SIZE_NOT_AVAILABLE());
        }

        return ((BufferedBodyOutputStream) bodyStream).toBuffer().remaining();
    }

    enum BodyMode {
        NONE,
        CHUNKED,
        BUFFERED
    }
}
