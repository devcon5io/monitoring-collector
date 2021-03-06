/*
 *     Universal Collector for Metrics
 *     Copyright (C) 2017-2018 DevCon5 GmbH, Switzerland
 *
 *     This program is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 *
 *     This program is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU General Public License for more details.
 *
 *     You should have received a copy of the GNU General Public License
 *     along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package io.devcon5.digester.influx;

import java.util.Collection;
import java.util.Collections;

import io.devcon5.measure.Encoder;
import io.devcon5.measure.Measurement;
import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.ext.web.client.HttpResponse;
import io.vertx.ext.web.client.WebClient;
import io.vertx.ext.web.client.WebClientOptions;

public class InfluxClient {

    public static final int DEFAULT_PORT = 8086;
    public static final String DEFAULT_HOST = "localhost";

    private final WebClient webclient;
    private final Encoder<Buffer> encoder;
    private String database;

    InfluxClient(WebClient webclient) {
        this.webclient = webclient;
        this.encoder = LineProtocol.encoder();
    }

    public InfluxClient send(Measurement m,Handler<AsyncResult<Void>> handler) {

        return send(m, getDatabase(), handler);
    }

    public InfluxClient send(Measurement m, String database, Handler<AsyncResult<Void>> handler) {
        return send(Collections.singleton(m), database, handler);
    }

    public InfluxClient send(Collection<Measurement> m, Handler<AsyncResult<Void>> handler) {
        return send(m, getDatabase(), handler);

    }

    public InfluxClient send(Collection<Measurement> m, String database, Handler<AsyncResult<Void>> handler) {
        this.webclient.post("/write")
                      .addQueryParam("db", database)
                      .putHeader("Content-Type", "application/x-www-form-urlencoded")
                      .sendBuffer(encoder.encode(m), resultHandler(handler));
        return this;
    }

    private String getDatabase() {
        if(this.database == null){
            throw new IllegalStateException("no database set");
        }
        return this.database;
    }

    private Handler<AsyncResult<HttpResponse<Buffer>>> resultHandler(Handler<AsyncResult<Void>> handler) {
        return result -> {
            if(result.succeeded()){
                if(result.result().statusCode() == 204){
                    handler.handle(Future.succeededFuture());
                } else {
                    handler.handle(Future.failedFuture(result.result().bodyAsString()));
                }
            } else {
                handler.handle(Future.failedFuture(result.cause()));
            }
        };
    }


    public static InfluxClient create(Vertx vertx) {
        return create(vertx, DEFAULT_HOST);
    }

    public static InfluxClient create(Vertx vertx, String host) {
        return create(vertx, host, DEFAULT_PORT);
    }

    public static InfluxClient create(Vertx vertx, String host, int port) {
        final WebClient webclient = WebClient.create(vertx, new WebClientOptions().setDefaultHost(host)
                                                                                  .setDefaultPort(port));
        return new InfluxClient(webclient);
    }


    public InfluxClient useDatabase(String db) {
        InfluxClient client = new InfluxClient(this.webclient);
        client.database = db;
        return client;
    }
}
