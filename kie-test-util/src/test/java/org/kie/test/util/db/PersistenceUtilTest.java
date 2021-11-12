/*
 * Copyright 2021 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.kie.test.util.db;

import static org.junit.Assert.assertEquals;
import org.junit.Test;

public class PersistenceUtilTest {

    @Test
    public void verifyH2JdbcUrlWithMemWithParams() {
        final String RESULT_JDBC_URL = "jdbc:h2:mem:test;MVCC=TRUE;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE";
        String url = "jdbc:h2:mem:test;MVCC=TRUE;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE";
        assertEquals(RESULT_JDBC_URL, PersistenceUtil.prepareH2JdbcUrl(url));
    }

    @Test
    public void verifyH2JdbcUrlWithoutTcpWithoutParams() {
        final String RESULT_JDBC_URL = "jdbc:h2:tcp://localhost/target/./persistence-test";
        String url = "jdbc:h2:";
        assertEquals(RESULT_JDBC_URL, PersistenceUtil.prepareH2JdbcUrl(url));
    }

    @Test
    public void verifyH2JdbcUrlWithTcpWithoutParams() {
        final String RESULT_JDBC_URL = "jdbc:h2:tcp://localhost/target/./persistence-test";
        String url = "jdbc:h2:tcp://localhost/target/./persistence-test";
        assertEquals(RESULT_JDBC_URL, PersistenceUtil.prepareH2JdbcUrl(url));
    }

    @Test
    public void verifyH2JdbcUrlWithMemWithoutParams() {
        final String RESULT_JDBC_URL = "jdbc:h2:mem:test";
        String url = "jdbc:h2:mem:test";
        assertEquals(RESULT_JDBC_URL, PersistenceUtil.prepareH2JdbcUrl(url));
    }

}
