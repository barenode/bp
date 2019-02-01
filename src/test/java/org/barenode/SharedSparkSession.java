package org.barenode;

import java.io.IOException;
import java.io.Serializable;

import org.apache.spark.api.java.JavaSparkContext;
import org.apache.spark.sql.SparkSession;
import org.junit.After;
import org.junit.Before;

public class SharedSparkSession implements Serializable {
	private static final long serialVersionUID = 1L;
	
	protected transient SparkSession spark;
	protected transient JavaSparkContext jsc;

	@Before
	public void setUp() throws IOException {
		spark = SparkSession.builder()
			.master("local[2]")
			.appName(getClass().getSimpleName())
			.getOrCreate();
		jsc = new JavaSparkContext(spark.sparkContext());
	}

	@After
	public void tearDown() {
		try {
			spark.stop();
			spark = null;
		} finally {
			SparkSession.clearDefaultSession();
			SparkSession.clearActiveSession();
		}
	}
}
