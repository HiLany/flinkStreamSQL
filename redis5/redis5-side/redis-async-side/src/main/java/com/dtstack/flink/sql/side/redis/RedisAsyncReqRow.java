/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.dtstack.flink.sql.side.redis;

import com.dtstack.flink.sql.enums.ECacheContentType;
import com.dtstack.flink.sql.side.AbstractSideTableInfo;
import com.dtstack.flink.sql.side.BaseAsyncReqRow;
import com.dtstack.flink.sql.side.CacheMissVal;
import com.dtstack.flink.sql.side.FieldInfo;
import com.dtstack.flink.sql.side.JoinInfo;
import com.dtstack.flink.sql.side.cache.CacheObj;
import com.dtstack.flink.sql.side.redis.enums.RedisType;
import com.dtstack.flink.sql.side.redis.table.RedisSideReqRow;
import com.dtstack.flink.sql.side.redis.table.RedisSideTableInfo;
import com.dtstack.flink.sql.util.RowDataComplete;
import io.lettuce.core.RedisClient;
import io.lettuce.core.RedisFuture;
import io.lettuce.core.RedisURI;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.api.async.RedisHashAsyncCommands;
import io.lettuce.core.api.async.RedisKeyAsyncCommands;
import io.lettuce.core.cluster.RedisClusterClient;
import io.lettuce.core.cluster.api.StatefulRedisClusterConnection;
import org.apache.commons.collections.MapUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.flink.api.java.typeutils.RowTypeInfo;
import org.apache.flink.configuration.Configuration;
import org.apache.flink.streaming.api.functions.async.ResultFuture;
import org.apache.flink.table.dataformat.BaseRow;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.regex.Matcher;

/**
 * @author yanxi
 */
public class RedisAsyncReqRow extends BaseAsyncReqRow {

    private static final long serialVersionUID = -2079908694523987738L;

    private RedisClient redisClient;

    private StatefulRedisConnection<String, String> connection;

    private RedisClusterClient clusterClient;

    private StatefulRedisClusterConnection<String, String> clusterConnection;

    private RedisKeyAsyncCommands<String, String> async;

    private RedisSideTableInfo redisSideTableInfo;

    private RedisSideReqRow redisSideReqRow;

    public RedisAsyncReqRow(RowTypeInfo rowTypeInfo, JoinInfo joinInfo, List<FieldInfo> outFieldInfoList, AbstractSideTableInfo sideTableInfo) {
        super(new RedisAsyncSideInfo(rowTypeInfo, joinInfo, outFieldInfoList, sideTableInfo));
        redisSideReqRow = new RedisSideReqRow(super.sideInfo, (RedisSideTableInfo) sideTableInfo);
    }

    @Override
    public void open(Configuration parameters) throws Exception {
        super.open(parameters);
        redisSideTableInfo = (RedisSideTableInfo) sideInfo.getSideTableInfo();
        buildRedisClient(redisSideTableInfo);
    }

    private void buildRedisClient(RedisSideTableInfo tableInfo){
        String url = redisSideTableInfo.getUrl();
        String password = redisSideTableInfo.getPassword();

        String database = redisSideTableInfo.getDatabase();
        if (database == null){
            database = "0";
        }
        switch (RedisType.parse(tableInfo.getRedisType())){
            case STANDALONE:
                RedisURI redisURI = RedisURI.create("redis://" + url);
                redisURI.setPassword(password);
                redisURI.setDatabase(Integer.valueOf(database));
                redisClient = RedisClient.create(redisURI);
                connection = redisClient.connect();
                async = connection.async();
                break;
            case SENTINEL:
                String[] urlSplit = StringUtils.split(url, ",");
                RedisURI.Builder builder = null;
                for (String item : urlSplit) {
                    Matcher mather = RedisSideReqRow.HOST_PORT_PATTERN.matcher(item);
                    if (mather.find()) {
                        builder = buildSentinelUri(
                                mather.group("host"),
                                mather.group("port"),
                                builder
                        );
                    } else {
                        throw new IllegalArgumentException(
                                String.format("Illegal format with redis url [%s]", item)
                        );
                    }
                }

                if (Objects.nonNull(builder)) {
                    builder
                            .withPassword(tableInfo.getPassword())
                            .withDatabase(Integer.parseInt(tableInfo.getDatabase()))
                            .withSentinelMasterId(tableInfo.getMasterName());
                } else {
                    throw new NullPointerException("build redis uri error!");
                }

                RedisURI uri = builder.build();
                redisClient = RedisClient.create(uri);
                connection = redisClient.connect();
                async = connection.async();
                break;
            case CLUSTER:
                RedisURI clusterURI = RedisURI.create("redis://" + url);
                clusterURI.setPassword(password);
                clusterClient = RedisClusterClient.create(clusterURI);
                clusterConnection = clusterClient.connect();
                async = clusterConnection.async();
            default:
                break;
        }
    }

    private RedisURI.Builder buildSentinelUri(
            String host,
            String port,
            RedisURI.Builder builder) {
        if (Objects.nonNull(builder)) {
            builder.withSentinel(host, Integer.parseInt(port));
        } else {
            builder = RedisURI.Builder.sentinel(host, Integer.parseInt(port));
        }
        return builder;
    }

    @Override
    public BaseRow fillData(BaseRow input, Object sideInput) {
        return redisSideReqRow.fillData(input, sideInput);
    }

    @Override
    public void handleAsyncInvoke(Map<String, Object> inputParams, BaseRow input, ResultFuture<BaseRow> resultFuture) throws Exception {
        String key = buildCacheKey(inputParams);
        if(StringUtils.isBlank(key)){
            return;
        }
        RedisFuture<Map<String, String>> future = ((RedisHashAsyncCommands) async).hgetall(key);
        future.thenAccept(new Consumer<Map<String, String>>() {
            @Override
            public void accept(Map<String, String> values) {
                if (MapUtils.isNotEmpty(values)) {
                    try {
                        BaseRow row = fillData(input, values);
                        dealCacheData(key,CacheObj.buildCacheObj(ECacheContentType.SingleLine, row));
                        RowDataComplete.completeBaseRow(resultFuture, row);
                    } catch (Exception e) {
                        dealFillDataError(input, resultFuture, e);
                    }
                } else {
                    dealMissKey(input, resultFuture);
                    dealCacheData(key, CacheMissVal.getMissKeyObj());
                }
            }
        });
    }

    @Override
    public String buildCacheKey(Map<String, Object> refData) {
        return redisSideReqRow.buildCacheKey(refData);
    }

    @Override
    public void close() throws Exception {
        super.close();
        if (connection != null){
            connection.close();
        }
        if (redisClient != null){
            redisClient.shutdown();
        }
        if (clusterConnection != null){
            clusterConnection.close();
        }
        if (clusterClient != null){
            clusterClient.shutdown();
        }
    }

}
