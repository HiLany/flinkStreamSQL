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

package com.dtstack.flink.sql.sink.hbase;

import com.dtstack.flink.sql.sink.hbase.enums.EReplaceOpType;
import com.dtstack.flink.sql.util.MD5Utils;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import static org.junit.Assert.assertEquals;

/**
 * @author: chuixue
 * @create: 2020-07-07 15:31
 * @description:
 **/
public class Md5ReplaceOperatorTest {

    private Md5ReplaceOperator md5ReplaceOperator;

    @Before
    public void setUp() {
        md5ReplaceOperator = new Md5ReplaceOperator(EReplaceOpType.MD5_FUNC);
    }

    @Test
    public void testDoFun() {
        String md5 = md5ReplaceOperator.doFunc("md5");
        assertEquals("1bc29b36f623ba82aaf6724fd3b16718", md5);
    }
}
