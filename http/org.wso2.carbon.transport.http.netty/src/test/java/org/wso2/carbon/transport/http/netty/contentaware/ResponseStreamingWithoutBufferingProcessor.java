/*
 * Copyright (c) 2017, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 * WSO2 Inc. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.wso2.carbon.transport.http.netty.contentaware;

import io.netty.handler.codec.http.HttpHeaders;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wso2.carbon.messaging.CarbonCallback;
import org.wso2.carbon.messaging.CarbonMessage;
import org.wso2.carbon.messaging.CarbonMessageProcessor;
import org.wso2.carbon.messaging.ClientConnector;
import org.wso2.carbon.messaging.DefaultCarbonMessage;
import org.wso2.carbon.messaging.TransportSender;
import org.wso2.carbon.transport.http.netty.common.Constants;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * A Message Processor which respond in streaming manner without buffering.
 */
public class ResponseStreamingWithoutBufferingProcessor implements CarbonMessageProcessor {
    private static final Logger logger = LoggerFactory.getLogger(RequestResponseTransformStreamingProcessor.class);
    private ExecutorService executor = Executors.newSingleThreadExecutor();

    @Override
    public boolean receive(CarbonMessage carbonMessage, CarbonCallback callback) throws Exception {
        executor.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    if (carbonMessage.getProperty(org.wso2.carbon.messaging.Constants.DIRECTION) != null &&
                            carbonMessage.getProperty(org.wso2.carbon.messaging.Constants.DIRECTION)
                            .equals(org.wso2.carbon.messaging.Constants.DIRECTION_RESPONSE)) {

                        callback.done(carbonMessage);
                    } else {
                        CarbonMessage cMsg = new DefaultCarbonMessage(false);
                        cMsg.setHeader(HttpHeaders.Names.CONNECTION, HttpHeaders.Values.KEEP_ALIVE);
                        cMsg.setHeader(HttpHeaders.Names.TRANSFER_ENCODING, HttpHeaders.Values.CHUNKED);
                        cMsg.setHeader(HttpHeaders.Names.CONTENT_TYPE, Constants.TEXT_PLAIN);
                        cMsg.setProperty(Constants.HTTP_STATUS_CODE, 200);
                        callback.done(cMsg);
                        while (!(carbonMessage.isEmpty() && carbonMessage.isEndOfMsgAdded())) {
                            cMsg.addMessageBody(carbonMessage.getMessageBody());
                        }
                        cMsg.setEndOfMsgAdded(true);
                    }
                } finally {
                    // Calling the release method to make sure that there won't be any memory leaks from netty
                    carbonMessage.release();
                }
            }
        });

        return false;
    }

    @Override
    public void setTransportSender(TransportSender sender) {

    }

    @Override
    public void setClientConnector(ClientConnector clientConnector) {

    }

    @Override
    public String getId() {
        return "ResponseWritingWithoutBufferingProcessor";
    }
}
