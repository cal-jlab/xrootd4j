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
package org.dcache.xrootd.plugins.authn.gsi;

import eu.emi.security.authn.x509.CrlCheckingMode;
import eu.emi.security.authn.x509.NamespaceCheckingMode;
import eu.emi.security.authn.x509.OCSPCheckingMode;
import eu.emi.security.authn.x509.OCSPParametes;
import eu.emi.security.authn.x509.ProxySupport;
import eu.emi.security.authn.x509.RevocationParameters;
import eu.emi.security.authn.x509.X509CertChainValidator;
import eu.emi.security.authn.x509.impl.OpensslCertChainValidator;
import eu.emi.security.authn.x509.impl.PEMCredential;
import eu.emi.security.authn.x509.impl.ValidatorParams;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.security.KeyStoreException;
import java.security.cert.CertificateException;
import java.util.Properties;
import java.util.concurrent.TimeUnit;

/**
 *  <p>Shared functionality for various GSI factories.</p>
 *
 *  <p>Initializes the server-side certificate objects (host certificate, host key,
 *      trusted certificates and CRLs) needed for the handler to perform its tasks.</p>
 *
 *  <p>Thus the certificates and trust anchors can be cached for a configurable
 *      time period. The configuration option controlling this caching is the
 *      same as the one used in the SRM door.</p>
 */
abstract class BaseGSIAuthenticationFactory
{
    protected static final Logger LOGGER =
        LoggerFactory.getLogger(BaseGSIAuthenticationFactory.class);

    protected final String hostCertificatePath;
    protected final String hostKeyPath;
    protected final String caCertificatePath;
    protected final X509CertChainValidator validator;

    protected final long hostCertRefreshInterval;
    protected final long trustAnchorRefreshInterval;
    protected final boolean verifyHostCertificate;

    protected PEMCredential hostCredential;
    protected long hostCertRefreshTimestamp = 0;

    protected BaseGSIAuthenticationFactory(Properties properties)
    {
        hostKeyPath = properties.getProperty("xrootd.gsi.hostcert.key");
        hostCertificatePath = properties.getProperty("xrootd.gsi.hostcert.cert");
        hostCertRefreshInterval =
                TimeUnit.valueOf(properties.getProperty("xrootd.gsi.hostcert.refresh.unit"))
                        .toMillis(Integer.parseInt(properties.getProperty("xrootd.gsi.hostcert.refresh")));
        verifyHostCertificate =
            Boolean.parseBoolean(properties.getProperty("xrootd.gsi.hostcert.verify"));

        caCertificatePath = properties.getProperty("xrootd.gsi.ca.path");
        trustAnchorRefreshInterval =
                TimeUnit.valueOf(properties.getProperty("xrootd.gsi.ca.refresh.unit"))
                        .toMillis(Integer.parseInt(properties.getProperty("xrootd.gsi.ca.refresh")));
        NamespaceCheckingMode namespaceMode =
                NamespaceCheckingMode.valueOf(properties.getProperty("xrootd.gsi.ca.namespace-mode"));
        CrlCheckingMode crlCheckingMode =
                CrlCheckingMode.valueOf(properties.getProperty("xrootd.gsi.ca.crl-mode"));
        OCSPCheckingMode ocspCheckingMode =
                OCSPCheckingMode.valueOf(properties.getProperty("xrootd.gsi.ca.ocsp-mode"));
        ValidatorParams validatorParams = new ValidatorParams(
                new RevocationParameters(crlCheckingMode, new OCSPParametes(ocspCheckingMode)), ProxySupport.ALLOW);
        validator =
                new OpensslCertChainValidator(caCertificatePath, false, namespaceMode,
                                              trustAnchorRefreshInterval, validatorParams, false);
    }

    protected synchronized void loadServerCredentials()
                    throws CertificateException, KeyStoreException, IOException
    {
        if (shouldReloadServerCredentials()) {
            LOGGER.info("Loading server certificates. Current refresh interval: {} ms",
                      hostCertRefreshInterval);
            PEMCredential credential = new PEMCredential(hostKeyPath, hostCertificatePath, null);
            if (verifyHostCertificate) {
                LOGGER.info("Verifying host certificate");
                validator.validate(credential.getCertificateChain());
            }
            hostCredential = credential;
            hostCertRefreshTimestamp = System.currentTimeMillis();
        }
    }

    protected boolean shouldReloadServerCredentials()
    {
        long timeSinceLastServerRefresh = (System.currentTimeMillis() - hostCertRefreshTimestamp);
        LOGGER.info("Time since last server cert refresh {}", timeSinceLastServerRefresh);
        return hostCredential == null || timeSinceLastServerRefresh >= hostCertRefreshInterval;
    }
}
