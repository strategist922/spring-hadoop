/*
 * Copyright 2011-2012 the original author or authors.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.springframework.data.hadoop.hbase;

import java.io.IOException;
import java.nio.charset.Charset;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.hbase.client.HTable;
import org.apache.hadoop.hbase.client.HTableInterface;
import org.apache.hadoop.hbase.client.HTableInterfaceFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

/**
 * Helper class featuring methods for Hbase table handling and exception translation. 
 * 
 * @author Costin Leau
 */
public class HbaseUtils {

	/**
	 * Converts the given (Hbase) exception to an appropriate exception from <tt>org.springframework.dao</tt> hierarchy.
	 * 
	 * @param ex Hbase exception that occurred
	 * @return the corresponding DataAccessException instance
	 */
	public static DataAccessException convertHbaseException(Exception ex) {
		return new HbaseSystemException(ex);
	}

	/**
	 * Retrieves an Hbase table instance identified by its name.
	 * 
	 * @param configuration Hbase configuration object
	 * @param tableName table name
	 * @return table instance
	 */
	public static HTable getHTable(Configuration configuration, String tableName) {
		return getHTable(null, getCharset(null), configuration, tableName);
	}

	/**
	 * Retrieves an Hbase table instance identified by its name and charset using the given table factory.
	 * 
	 * @param tableFactory table factory (may be null)
	 * @param charset name charset (may be null)
	 * @param configuration Hbase configuration object
	 * @param tableName table name
	 * @return table instance
	 */
	public static HTable getHTable(HTableInterfaceFactory tableFactory, Charset charset, Configuration configuration, String tableName) {
		if (HbaseSynchronizationManager.hasResource(tableName)) {
			return (HTable) HbaseSynchronizationManager.getResource(tableName);
		}

		HTable t = null;
		try {
			if (tableFactory != null) {
				HTableInterface table = tableFactory.createHTableInterface(configuration, tableName.getBytes(charset));
				Assert.isInstanceOf(HTable.class, table, "The table factory needs to create HTable instances");
				t = (HTable) table;
			}
			else {
				t = new HTable(configuration, tableName.getBytes(charset));
			}

			return t;

		} catch (Exception ex) {
			throw convertHbaseException(ex);
		}
	}

	static Charset getCharset(String encoding) {
		return (StringUtils.hasText(encoding) ? Charset.forName(encoding) : Charset.forName("UTF-8"));
	}

	/**
	 * Releases (and closes) the given table, created via the given configuration if it is not managed externally.
	 * 
	 * @param tableName
	 * @param table
	 */
	public static void releaseTable(String tableName, HTableInterface table) {
		try {
			doReleaseTable(tableName, table);
		} catch (IOException ex) {
			throw HbaseUtils.convertHbaseException(ex);
		}
	}

	private static void doReleaseTable(String tableName, HTableInterface table) throws IOException {
		if (table == null) {
			return;
		}

		if (!isBoundToThread(tableName)) {
			table.close();
		}
	}

	private static boolean isBoundToThread(String tableName) {
		return HbaseSynchronizationManager.hasResource(tableName);
	}
}