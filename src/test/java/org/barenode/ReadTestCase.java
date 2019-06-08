package org.barenode;

import org.apache.spark.sql.Dataset;
import org.apache.spark.sql.Row;
import org.junit.Test;

public class ReadTestCase extends SharedSparkSession {

	@Test public void test() throws Exception {
		Dataset<Row> ds = spark.read().parquet("/filmy/data/1/data/product");
		Row[] rows = (Row[])ds.take(10);
		for (Row row : rows) {
			System.out.println(row);
		}
	}
}
