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
package org.dcache.xrootd.tpc;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelId;
import io.netty.channel.ChannelInboundHandlerAdapter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.channels.ClosedChannelException;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import org.dcache.xrootd.core.XrootdException;
import org.dcache.xrootd.tpc.protocol.messages.AbstractXrootdInboundResponse;
import org.dcache.xrootd.tpc.protocol.messages.InboundAttnResponse;
import org.dcache.xrootd.tpc.protocol.messages.InboundAuthenticationResponse;
import org.dcache.xrootd.tpc.protocol.messages.InboundChecksumResponse;
import org.dcache.xrootd.tpc.protocol.messages.InboundCloseResponse;
import org.dcache.xrootd.tpc.protocol.messages.InboundErrorResponse;
import org.dcache.xrootd.tpc.protocol.messages.InboundHandshakeResponse;
import org.dcache.xrootd.tpc.protocol.messages.InboundLoginResponse;
import org.dcache.xrootd.tpc.protocol.messages.InboundOpenReadOnlyResponse;
import org.dcache.xrootd.tpc.protocol.messages.InboundProtocolResponse;
import org.dcache.xrootd.tpc.protocol.messages.InboundReadResponse;
import org.dcache.xrootd.tpc.protocol.messages.InboundRedirectResponse;
import org.dcache.xrootd.tpc.protocol.messages.InboundWaitResponse;
import org.dcache.xrootd.tpc.protocol.messages.OutboundAuthenticationRequest;
import org.dcache.xrootd.tpc.protocol.messages.OutboundChecksumRequest;
import org.dcache.xrootd.tpc.protocol.messages.OutboundCloseRequest;
import org.dcache.xrootd.tpc.protocol.messages.OutboundLoginRequest;
import org.dcache.xrootd.tpc.protocol.messages.OutboundOpenReadOnlyRequest;
import org.dcache.xrootd.tpc.protocol.messages.OutboundReadRequest;
import org.dcache.xrootd.tpc.protocol.messages.XrootdInboundResponse;
import org.dcache.xrootd.tpc.protocol.messages.XrootdOutboundRequest;
import org.dcache.xrootd.util.ParseException;

import static org.dcache.xrootd.protocol.XrootdProtocol.*;

/**
 * <p>This handler is intended for implementation of only a very limited
 *    subset of the protocol in order to support third-party read requests.
 *    It exposes the handshake, login, protocol, auth, open, read, close
 *    and endsession exchanges.</p>
 */
public abstract class AbstractClientRequestHandler extends
                ChannelInboundHandlerAdapter
{
    protected static final Logger LOGGER
                    = LoggerFactory.getLogger(AbstractClientRequestHandler.class);

    protected XrootdTpcClient client;
    protected ScheduledFuture future;

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg)
    {
        if (msg instanceof XrootdInboundResponse) {
            responseReceived(ctx, (XrootdInboundResponse) msg);
            return;
        }
        ctx.fireChannelRead(msg);
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable t)
    {
        if (t instanceof ClosedChannelException) {
            LOGGER.warn("ClosedChannelException caught on channel {}.",
                        ctx.channel().id());
        } else if (t instanceof IOException) {
            LOGGER.error("IOException caught on channel {}: {}.",
                         ctx.channel().id(), t.toString());
        } else if (t instanceof XrootdException) {
            LOGGER.error("Exception caught on channel {}: {}.",
                         ctx.channel().id(),
                         t.toString());
        } else {
            LOGGER.error("Exception caught on channel {}: {}",
                         ctx.channel().id(),
                         t.getMessage());
            Thread me = Thread.currentThread();
            me.getUncaughtExceptionHandler().uncaughtException(me, t);
        }

        if (client != null) {
            client.setError(t);
            try {
                client.shutDown(ctx);
            } catch (InterruptedException e) {
                LOGGER.warn("Client shutdown interrupted.");
            }
        }
    }

    public void setClient(XrootdTpcClient client)
    {
        this.client = client;
    }

    protected void doOnAsynResponse(ChannelHandlerContext ctx,
                                    InboundAttnResponse response)
    {
        switch (response.getRequestId()) {
            case kXR_endsess:
                client.doEndsession(ctx);
                break;
            default:
                ctx.fireChannelRead(response);
        }
    }

    protected void doOnAuthenticationResponse(ChannelHandlerContext ctx,
                                              InboundAuthenticationResponse response)
    {
        LOGGER.trace("doOnAuthenticationResponse, channel {}, stream {}"
                                     + " –– passing to next in chain.",
                     ctx.channel().id(), response.getStreamId());
        ctx.fireChannelRead(response);
    }

    protected void doOnChecksumResponse(ChannelHandlerContext ctx,
                                        InboundChecksumResponse response)
    {
        LOGGER.trace("doOnChecksumResponse, channel {}, stream {}"
                                     + " –– passing to next in chain.",
                     ctx.channel().id(), response.getStreamId());
        ctx.fireChannelRead(response);
    }

    protected void doOnCloseResponse(ChannelHandlerContext ctx,
                                     InboundCloseResponse response)
    {
        LOGGER.trace("doOnCloseResponse, channel {}, stream {}"
                                     + " –– passing to next in chain.",
                     ctx.channel().id(), response.getStreamId());
        ctx.fireChannelRead(response);
    }

    protected void doOnErrorResponse(ChannelHandlerContext ctx,
                                     InboundErrorResponse response)
    {
        exceptionCaught(ctx,
                        new XrootdException(response.getError(),
                                            response.getErrorMessage()));
    }

    protected void doOnHandshakeResponse(ChannelHandlerContext ctx,
                                         InboundHandshakeResponse response)
    {
        LOGGER.trace("doOnHandshakeResponse, channel {}"
                                     + " –– passing to next in chain.",
                     ctx.channel().id());
        ctx.fireChannelRead(response);
    }

    protected void doOnLoginResponse(ChannelHandlerContext ctx,
                                   InboundLoginResponse response)
    {
        LOGGER.trace("doOnLoginResponse, channel {}, stream {}"
                                     + " –– passing to next in chain.",
                     ctx.channel().id(), response.getStreamId());
        ctx.fireChannelRead(response);
    }

    protected void doOnOpenResponse(ChannelHandlerContext ctx,
                                  InboundOpenReadOnlyResponse response)
    {
        LOGGER.trace("doOnOpenResponse, channel {}, stream {}"
                                     + " –– passing to next in chain.",
                     ctx.channel().id(), response.getStreamId());
        ctx.fireChannelRead(response);
    }

    protected void doOnProtocolResponse(ChannelHandlerContext ctx,
                                        InboundProtocolResponse response)
    {
        LOGGER.trace("doOnProtocolResponse, channel {}, stream {}"
                                     + " –– passing to next in chain.",
                     ctx.channel().id(), response.getStreamId());
        ctx.fireChannelRead(response);
    }

    protected void doOnReadResponse(ChannelHandlerContext ctx,
                                    InboundReadResponse response)
    {
        LOGGER.trace("doOnReadResponse, channel {}, stream {}"
                                     + " –– passing to next in chain.",
                     ctx.channel().id(), response.getStreamId());
        ctx.fireChannelRead(response);
    }

    protected void doOnRedirectResponse(ChannelHandlerContext ctx,
                                        InboundRedirectResponse response)
    {
        ChannelId id = ctx.channel().id();
        LOGGER.trace("redirecting client from {} to {}:{}, channel {}, "
                                     + "stream {}; [info {}].",
                     client.getInfo().getSrc(),
                     response.getHost(),
                     response.getPort(),
                     id, client.getStreamId(),
                     client.getInfo());
        try {
            client.getWriteHandler().redirect(ctx, response);
        } catch (XrootdException e) {
            exceptionCaught(ctx, e);
        }
    }

    protected void doOnAttnResponse(ChannelHandlerContext ctx,
                                    InboundAttnResponse response)
    {
        String message;

        switch (response.getActnum()) {
            case kXR_asyncab:
                /*
                 * The client should immediately disconnect (i.e., close
                 * the socket connection) from the server and abort
                 * further execution.
                 */
                message = "Received abort from source server: "
                                + response.getMessage();
                exceptionCaught(ctx,
                                new XrootdException(kXR_ServerError,
                                                    message));
                break;
            case kXR_asyncms:
                /*
                 * The client should send the indicated message to the console.
                 * The parameters contain the message text.
                 */
                LOGGER.info("Received from source server: {}.",
                            response.getMessage());
                break;
            case kXR_asyncgo:
                /*
                 * The client may start sending requests. This code is sent to
                 * cancel the effects of a previous kXR_asyncwt code.
                 *
                 * Fall through to handle future cancellation.
                 */
            case kXR_asynresp:
                /*
                 * The client should use the response data in the message to
                 * complete the request associated with the indicated streamid.
                 *
                 * For this to be valid, there has to be a waiting request
                 * for this pipeline.  If future is null here, we skip it.
                 */
                synchronized (this) {
                    if (future != null) {
                        future.cancel(true);
                        doOnAsynResponse(ctx, response);
                        future = null;
                    }
                }
                break;
            case kXR_asyncwt:
                /*
                 * The client should hold off sending any new requests until the
                 * indicated amount of time has passed or until receiving a
                 * kXR_asyncgo action code.
                 */
                doOnWaitResponse(ctx, response);
                break;
            case kXR_asyncrd:
                /*
                 * The client should immediately disconnect (i.e., close the
                 * socket connection) and reconnect to the indicated server.
                 *
                 * NOTE:  without opaque data in the parameters, this redirect
                 * probably will not work here, but we will allow this to fail
                 * downstream.
                 *
                 * Fall through to redirect.
                 */
            case kXR_asyncdi:
                /*
                 * The client should immediately disconnect
                 * (i.e., close the socket connection) from the server.
                 * Parameters indicate when a reconnect may be attempted.
                 *
                 * This is essentially a delayed redirect to the same
                 * endpoint.
                 */
                try {
                    doOnRedirectResponse(ctx,
                                         new InboundRedirectResponse(response));
                } catch (ParseException e) {
                    exceptionCaught(ctx,
                                    new XrootdException(kXR_ServerError,
                                                        "bad redirect data from kXR_asyncdi"));
                }
                break;
            case kXR_asyncav:
                /*
                 * The file or file(s) the client previously
                 * requested to be prepared are now available.
                 *
                 * We do not issue prepare requests.  NR.
                 */
            case kXR_asynunav:
                /*
                 * The file or file(s) the client previously requested to
                 * be prepared cannot be made available.
                 *
                 * We do not issue prepare requests.  NR.
                 */
                exceptionCaught(ctx,
                                new XrootdException(kXR_ServerError,
                                                "tpc client does not support"
                                                                + "this option: "
                                                    + response.getActnum()));
                break;
            default:
                exceptionCaught(ctx,
                                new XrootdException(kXR_ServerError,
                                                "unrecognized kXR_attn action: "
                                                    + response.getActnum()));
                return;
        }
    }

    /*
     * Do not wait on the event thread.
     */
    protected synchronized void doOnWaitResponse(final ChannelHandlerContext ctx,
                                                 AbstractXrootdInboundResponse response)
    {
        switch (response.getRequestId()) {
            case kXR_endsess:
                future = client.getExecutor().schedule(new Runnable() {
                    @Override
                    public void run() {
                        client.doEndsession(ctx);
                        synchronized (AbstractClientRequestHandler.this) {
                            future = null;
                        }
                    }
                }, getWaitInSeconds(response), TimeUnit.SECONDS);
                break;
            default:
                ctx.fireChannelRead(response);
        }
    }

    protected int getWaitInSeconds(AbstractXrootdInboundResponse response)
    {
        int wsec = 0;
        int msec = 0;
        if (response instanceof InboundWaitResponse) {
            msec = ((InboundWaitResponse)response).getMaxWaitInSeconds();
            wsec = 10;
        } else if (response instanceof InboundAttnResponse) {
            InboundAttnResponse attnResponse = (InboundAttnResponse)response;
            wsec = attnResponse.getWsec();
            msec = wsec;
        }
        return Math.min(wsec, msec);
    }

    protected void responseReceived(ChannelHandlerContext ctx,
                                    XrootdInboundResponse response)
    {
        if (response instanceof InboundWaitResponse) {
            doOnWaitResponse(ctx, (InboundWaitResponse)response);
            return;
        }

        if (response instanceof InboundErrorResponse) {
            doOnErrorResponse(ctx, (InboundErrorResponse)response);
            return;
        }

        if (response instanceof InboundRedirectResponse) {
            doOnRedirectResponse(ctx, (InboundRedirectResponse)response);
            return;
        }

        if (response instanceof InboundAttnResponse) {
            doOnAttnResponse(ctx, (InboundAttnResponse)response);
            return;
        }

        int streamId = response.getStreamId();
        ChannelId id = ctx.channel().id();
        int requestId = response.getRequestId();

        switch (requestId) {
            case kXR_auth:
                LOGGER.trace("responseReceived, channel {}, stream {}, "
                                             + "requestId = kXR_auth.",
                             id, streamId);
                doOnAuthenticationResponse(ctx, (InboundAuthenticationResponse)response);
                break;
            case kXR_close:
                LOGGER.trace("responseReceived, channel {}, stream {}, "
                                             + "requestId = kXR_close.",
                             id, streamId);
                doOnCloseResponse(ctx, (InboundCloseResponse) response);
                break;
            case kXR_endsess:
                LOGGER.trace("responseReceived, channel {}, stream {}, "
                                             + "requestId = kXR_endsess.",
                             id, streamId);
                LOGGER.trace("endsession response received.");
                client.disconnect(); // will not attempt disconnect twice
                break;
            case kXR_handshake:
                LOGGER.trace("responseReceived, channel {}, stream {}, "
                                             + "requestId = kXR_handshake.",
                             id, streamId);
                doOnHandshakeResponse(ctx, (InboundHandshakeResponse) response);
                break;
            case kXR_login:
                LOGGER.trace("responseReceived, channel {}, stream {}, "
                                             + "requestId = kXR_login.",
                             id, streamId);
                doOnLoginResponse(ctx, (InboundLoginResponse) response);
                break;
            case kXR_open:
                LOGGER.trace("responseReceived, channel {}, stream {}, "
                                             + "requestId = kXR_open.",
                             id, streamId);
                doOnOpenResponse(ctx, (InboundOpenReadOnlyResponse) response);
                break;
            case kXR_protocol:
                LOGGER.trace("responseReceived, channel {}, stream {}, "
                                             + "requestId = kXR_protocol.",
                             id, streamId);
                doOnProtocolResponse(ctx, (InboundProtocolResponse) response);
                break;
            case kXR_query:
                LOGGER.trace("responseReceived, channel {}, stream {}, "
                                             + "requestId = kXR_query.",
                             id, streamId);
                doOnChecksumResponse(ctx, (InboundChecksumResponse) response);
                break;
            case kXR_read:
                LOGGER.trace("responseReceived, channel {}, stream {}, "
                                             + "requestId = kXR_read.",
                             id, streamId);
                doOnReadResponse(ctx, (InboundReadResponse) response);
                break;
            default:
                String error = String.format("Response (channel %s, stream %d, "
                                                             + "request %s) "
                                                             + "should not have "
                                                             + "been received "
                                                             + "by tpc client",
                                             id, streamId, requestId);
                exceptionCaught(ctx,
                                new RuntimeException(error));
        }
    }

    protected void sendAuthenticationRequest(ChannelHandlerContext ctx)
                    throws XrootdException
    {
        unsupported(OutboundAuthenticationRequest.class);
    }

    protected void sendChecksumRequest(ChannelHandlerContext ctx)
                    throws XrootdException
    {
        unsupported(OutboundChecksumRequest.class);
    }

    protected void sendCloseRequest(ChannelHandlerContext ctx)
                    throws XrootdException
    {
        unsupported(OutboundCloseRequest.class);
    }

    protected void sendLoginRequest(ChannelHandlerContext ctx)
                    throws XrootdException
    {
        unsupported(OutboundLoginRequest.class);
    }

    protected void sendOpenRequest(ChannelHandlerContext ctx)
                    throws XrootdException
    {
        unsupported(OutboundOpenReadOnlyRequest.class);
    }

    protected void sendReadRequest(ChannelHandlerContext ctx)
                    throws XrootdException
    {
        unsupported(OutboundReadRequest.class);
    }

    protected <T extends XrootdOutboundRequest> void unsupported(Class<T> msg)
                    throws XrootdException
    {
        LOGGER.warn("Unsupported request: " + msg.getSimpleName());
        throw new XrootdException(kXR_Unsupported, "request "
                        + msg.getSimpleName() + " not supported");
    }
}
