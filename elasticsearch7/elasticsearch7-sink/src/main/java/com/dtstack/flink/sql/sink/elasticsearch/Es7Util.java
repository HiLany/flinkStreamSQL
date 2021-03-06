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


package com.dtstack.flink.sql.sink.elasticsearch;

import com.dtstack.flink.sql.util.DtStringUtil;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;
import org.apache.commons.lang3.StringUtils;
import org.apache.flink.shaded.guava18.com.google.common.collect.Maps;
import org.apache.flink.types.Row;
import org.apache.flink.util.Preconditions;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @description: parse utils
 * @program: flink.sql
 * @author: lany
 * @create: 2021/01/04 17:20
 */
public class Es7Util {

    public static Map<String, Object> rowToJsonMap(Row row, List<String> fields, List<String> types) {
        Preconditions.checkArgument(row.getArity() == fields.size());
        Map<String,Object> jsonMap = Maps.newHashMap();
        int i = 0;
        for(; i < fields.size(); ++i) {
            String field = fields.get(i);
            String[] parts = field.split("\\.");
            Map<String, Object> currMap = jsonMap;
            for(int j = 0; j < parts.length - 1; ++j) {
                String key = parts[j];
                if(currMap.get(key) == null) {
                    HashMap<String, Object> hashMap = Maps.newHashMap();
                    currMap.put(key, hashMap);
                }
                currMap = (Map<String, Object>) currMap.get(key);
            }
            String key = parts[parts.length - 1];
            Object col = row.getField(i);
            if(col != null) {
                Object value = DtStringUtil.col2string(col, types.get(i));
                currMap.put(key, value);
            }

        }

        return jsonMap;
    }

    /**
     * check whether str is json format
     * @param json
     * @return
     */
    public static boolean isJson(String json) {
        if (StringUtils.isEmpty(json)) {
            return false;
        }
        try {
            new JsonParser().parse(json);
            return true;
        } catch (JsonSyntaxException e) {
            return false;
        }
    }

}
