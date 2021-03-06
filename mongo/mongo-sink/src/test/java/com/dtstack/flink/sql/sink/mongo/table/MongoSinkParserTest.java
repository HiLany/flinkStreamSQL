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

package com.dtstack.flink.sql.sink.mongo.table;

import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

/**
 * @author: chuixue
 * @create: 2020-07-24 16:52
 * @description:
 **/
public class MongoSinkParserTest {
    private MongoSinkParser mongoSinkParser;

    @Test
    public void testGetTableInfo() {
        mongoSinkParser = new MongoSinkParser();
        Map<String, Object> prop = new HashMap();

        prop.put("type", "mongo");
        prop.put("address", "172.16.8.193:27017");
        prop.put("database", "dtstack");
        prop.put("tablename", "userInfo");


        mongoSinkParser.getTableInfo("userInfo", "id int,\n" +
                "    name VARCHAR,\n" +
                "    address VARCHAR,\n" +
                "    primary key (id)\n", prop);
    }
}
