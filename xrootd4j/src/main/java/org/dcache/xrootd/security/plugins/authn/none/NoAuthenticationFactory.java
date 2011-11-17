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
package org.dcache.xrootd.security.plugins.authn.none;

import org.dcache.xrootd.security.AuthenticationFactory;
import org.dcache.xrootd.security.AuthenticationHandler;
import org.dcache.xrootd.security.plugins.authn.InvalidHandlerConfigurationException;

/**
 * Dummy authentication factory that creates an authentication handler which
 * accepts all AuthenticationRequests
 *
 * @author tzangerl
 *
 */
public class NoAuthenticationFactory implements AuthenticationFactory
{
    @Override
    public AuthenticationHandler createHandler()
            throws InvalidHandlerConfigurationException
    {
        return new NoAuthenticationHandler();
    }
}