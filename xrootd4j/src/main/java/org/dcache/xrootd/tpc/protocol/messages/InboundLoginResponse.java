/**
 * Copyright (C) 2011-2018 dCache.org <support@dcache.org>
 *
 * This file is part of xrootd4j.
 *
 * xrootd4j is free software: you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as published
 * by the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * xrootd4j is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with xrootd4j.  If not, see http://www.gnu.org/licenses/.
 */
package org.dcache.xrootd.tpc.protocol.messages;

import io.netty.buffer.ByteBuf;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.dcache.xrootd.core.XrootdException;
import org.dcache.xrootd.core.XrootdSessionIdentifier;
import org.dcache.xrootd.security.SecurityInfo;

import static java.nio.charset.StandardCharsets.US_ASCII;
import static org.dcache.xrootd.protocol.XrootdProtocol.SESSION_ID_SIZE;
import static org.dcache.xrootd.protocol.XrootdProtocol.kXR_login;

/**
 * <p>Response from third-party source server.</p>
 */
public class InboundLoginResponse extends AbstractXrootdInboundResponse
{
    private final XrootdSessionIdentifier   sessionId;
    private final List<SecurityInfo>        protocols;
    private final Map<String, SecurityInfo> protocolMap;

    public InboundLoginResponse(ByteBuf buffer) throws XrootdException
    {
        super(buffer);

        if (buffer.readableBytes() > 8) {
            int slen = buffer.getInt(4) - SESSION_ID_SIZE;
            byte[] session = new byte[SESSION_ID_SIZE];
            buffer.getBytes(8, session);
            sessionId = new XrootdSessionIdentifier(session);
            if (slen > 0) {
                /*
                 * In the order given by the server.
                 */
                protocols = SecurityInfo.parse(buffer.toString(24,
                                                                     slen,
                                                                     US_ASCII));
            } else {
                protocols = Collections.EMPTY_LIST;
            }
        } else {
            sessionId = null;
            protocols = Collections.EMPTY_LIST;
        }

        protocolMap = protocols.stream()
                               .collect(Collectors.toMap((p) -> p.getProtocol(),
                                                         (p) -> p));
    }

    public List<SecurityInfo> getProtocols()
    {
        return protocols;
    }

    public SecurityInfo getInfo(String protocol)
    {
        return protocolMap.get(protocol);
    }

    public XrootdSessionIdentifier getSessionId() {
        return sessionId;
    }

    @Override
    public int getRequestId() {
        return kXR_login;
    }
}
