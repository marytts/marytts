/**
 * Copyright 2009 DFKI GmbH.
 * All Rights Reserved.  Use is subject to license terms.
 *
 * This file is part of MARY TTS.
 *
 * MARY TTS is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, version 3 of the License.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 */
package marytts.tools.perceptiontest;


import java.io.IOException;
import java.io.InterruptedIOException;
import java.net.InetSocketAddress;

import marytts.util.MaryUtils;

import org.apache.http.HttpException;
import org.apache.http.impl.DefaultConnectionReuseStrategy;
import org.apache.http.impl.DefaultHttpResponseFactory;
import org.apache.http.impl.nio.DefaultServerIOEventDispatch;
import org.apache.http.impl.nio.reactor.DefaultListeningIOReactor;
import org.apache.http.nio.NHttpConnection;
import org.apache.http.nio.protocol.BufferingHttpServiceHandler;
import org.apache.http.nio.protocol.EventListener;
import org.apache.http.nio.reactor.IOEventDispatch;
import org.apache.http.nio.reactor.ListeningIOReactor;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.CoreConnectionPNames;
import org.apache.http.params.CoreProtocolPNames;
import org.apache.http.params.HttpParams;
import org.apache.http.protocol.BasicHttpProcessor;
import org.apache.http.protocol.ExecutionContext;
import org.apache.http.protocol.HttpRequestHandlerRegistry;
import org.apache.http.protocol.ResponseConnControl;
import org.apache.http.protocol.ResponseContent;
import org.apache.http.protocol.ResponseDate;
import org.apache.http.protocol.ResponseServer;
import org.apache.log4j.Logger;


/**
 * Class for Perception Test Http Server
 * @author Sathish Pammi
 *
 */
public class PerceptionTestHttpServer  extends Thread {
    private static Logger logger;
    private String testXmlName; 
    private String userRatingsDirectory;
    private int serverPort = 44547; // default
    
    public PerceptionTestHttpServer(String testXmlName, String userRatingsDirectory) {
        logger = MaryUtils.getLogger("server");
        this.testXmlName =  testXmlName;
        this.userRatingsDirectory = userRatingsDirectory;
    }

    public PerceptionTestHttpServer(String testXmlName, String userRatingsDirectory, int serverPort) {
        logger = MaryUtils.getLogger("server");
        this.testXmlName =  testXmlName;
        this.userRatingsDirectory = userRatingsDirectory;
        this.serverPort = serverPort;
    }

    public void run()
    {
        logger.info("Starting server.");
        System.out.println("Starting server....");
        //int localPort = MaryProperties.needInteger("socket.port");
        int localPort = serverPort;
        
        HttpParams params = new BasicHttpParams();
        params
            .setIntParameter(CoreConnectionPNames.SO_TIMEOUT, 0) // 0 means no timeout, any positive value means time out in miliseconds (i.e. 50000 for 50 seconds)
            .setIntParameter(CoreConnectionPNames.SOCKET_BUFFER_SIZE, 8 * 1024)
            .setBooleanParameter(CoreConnectionPNames.STALE_CONNECTION_CHECK, false)
            .setBooleanParameter(CoreConnectionPNames.TCP_NODELAY, true)
            .setParameter(CoreProtocolPNames.ORIGIN_SERVER, "HttpComponents/1.1");

        BasicHttpProcessor httpproc = new BasicHttpProcessor();
        httpproc.addInterceptor(new ResponseDate());
        httpproc.addInterceptor(new ResponseServer());
        httpproc.addInterceptor(new ResponseContent());
        httpproc.addInterceptor(new ResponseConnControl());

        BufferingHttpServiceHandler handler = new BufferingHttpServiceHandler(
                httpproc,
                new DefaultHttpResponseFactory(),
                new DefaultConnectionReuseStrategy(),
                params);

        // Set up request handlers
        HttpRequestHandlerRegistry registry = new HttpRequestHandlerRegistry();
        //registry.register("/perceptionTest", new FileDataRequestHandler("perception.html"));
        //registry.register("/process", new FileDataRequestHandler("perception.html"));
        //registry.register("/perceptionTest", new UtterancePlayRequestHandler());
        
        DataRequestHandler infoRH = new DataRequestHandler(this.testXmlName);
        UserRatingStorer userRatingRH = new UserRatingStorer(this.userRatingsDirectory, infoRH);
        registry.register("/options", infoRH);
        registry.register("/queryStatement", infoRH);
        registry.register("/process", new UtterancePlayRequestHandler(infoRH));
        registry.register("/perceptionTest", new PerceptionRequestHandler(infoRH, userRatingRH));
        registry.register("/userRating", new StoreRatingRequestHandler(infoRH, userRatingRH));
        registry.register("*", new FileDataRequestHandler());
        
        handler.setHandlerResolver(registry);

        // Provide an event logger
        handler.setEventListener(new EventLogger());

        IOEventDispatch ioEventDispatch = new DefaultServerIOEventDispatch(handler, params);
        
        //int numParallelThreads = MaryProperties.getInteger("server.http.parallelthreads", 5);
        int numParallelThreads = 5;
        
        logger.info("Waiting for client to connect on port " + localPort);
        System.out.println("Waiting for client to connect on port " + localPort);
        
        try {
            ListeningIOReactor ioReactor = new DefaultListeningIOReactor(numParallelThreads, params);
            ioReactor.listen(new InetSocketAddress(localPort));
            ioReactor.execute(ioEventDispatch);
        } catch (InterruptedIOException ex) {
            logger.info("Interrupted", ex);
            System.out.println("Interrupted"+ ex.toString());
        } catch (IOException e) {
            logger.info("Problem with HTTP connection ", e);
            System.out.println("Problem with HTTP connection "+ e.toString());
        }
        logger.debug("Shutdown");
        System.out.println("Shutdown");
    }
    
    
    static class EventLogger implements EventListener
    {
        public void connectionOpen(final NHttpConnection conn) 
        {
            logger.info(
                    "Connection from "
                    + conn.getContext().getAttribute(ExecutionContext.HTTP_TARGET_HOST) //conn.getInetAddress().getHostName()
                    );
        }

        public void connectionTimeout(final NHttpConnection conn) 
        {
            logger.info("Connection timed out: " + conn);
        }

        public void connectionClosed(final NHttpConnection conn) 
        {
            logger.info("Connection closed: " + conn);
        }

        public void fatalIOException(final IOException ex, final NHttpConnection conn) 
        {
            logger.info("I/O error: " + ex.getMessage());
        }

        public void fatalProtocolException(final HttpException ex, final NHttpConnection conn) 
        {
            logger.info("HTTP error: " + ex.getMessage());
        }
    }
}

