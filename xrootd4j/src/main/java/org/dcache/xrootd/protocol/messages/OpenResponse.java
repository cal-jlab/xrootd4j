/**
 * Copyright (C) 2011 dCache.org <support@dcache.org>
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
 * License along with xrootd4j.  If not, see
 * <http://www.gnu.org/licenses/>.
 */
package org.dcache.xrootd.protocol.messages;

import org.dcache.xrootd.protocol.XrootdProtocol;
import org.dcache.xrootd.util.FileStatus;

import java.io.UnsupportedEncodingException;

public class OpenResponse extends AbstractResponseMessage
{
    public OpenResponse(int sId, long fileHandle,
                        Integer cpsize, String cptype, FileStatus fs)
    {
        /* The length is an upper bound.
         */
        super(sId, XrootdProtocol.kXR_ok, 256);

        try {
            putSignedInt((int) fileHandle);

            if (cpsize != null && cptype != null) {
                putSignedInt(cpsize);
                int len = Math.min(cptype.length(), 4);
                _buffer.writeBytes(cptype.getBytes("ASCII"), 0, len);
                _buffer.writeZero(4 - len);
            } else if (fs != null) {
                _buffer.writeZero(8);
            }

            if (fs != null) {
                putCharSequence(fs.toString());
            }
        } catch (UnsupportedEncodingException e) {
            /* We cannot possibly recover from this option, so
             * escalate it.
             */
            throw new RuntimeException("Failed to construct xrootd message", e);
        }
    }
}