package org.barenode;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.spark.ml.linalg.Vectors;
import org.apache.spark.ml.stat.Correlation;
import org.apache.spark.ml.linalg.Vector;
import org.apache.spark.ml.linalg.VectorUDT;
import org.apache.spark.sql.Dataset;
import org.apache.spark.sql.Row;
import org.apache.spark.sql.RowFactory;
import org.apache.spark.sql.catalyst.expressions.GenericRowWithSchema;
import org.apache.spark.sql.types.Metadata;
import org.apache.spark.sql.types.StructField;
import org.apache.spark.sql.types.StructType;
import org.junit.Test;

@SuppressWarnings("serial")
public class CorrelationTestCase extends SharedSparkSession {

	List<Row> data = Arrays.asList(
//	  RowFactory.create(Vectors.sparse(4, new int[]{0, 3}, new double[]{1.0, -2.0})),
	  RowFactory.create(Vectors.dense(4.0, 5.0, 0.0)),
	  RowFactory.create(Vectors.dense(6.0, 7.0, 0.0)),
	  RowFactory.create(Vectors.dense(1.0, 1.0, 1.0)),
	  RowFactory.create(Vectors.dense(5.0, 6.0, 7.0))
//	  ,
//	  RowFactory.create(Vectors.sparse(4, new int[]{0, 3}, new double[]{9.0, 1.0}))
	);
	
	StructType schema = new StructType(new StructField[]{
			new StructField("features", new VectorUDT(), false, Metadata.empty()),
	});
	
	@Test public void testCorrelation() throws Exception {
		Dataset<Row> df = spark.createDataFrame(data, schema);
		System.out.println(df);
		System.out.println(df.count());
		Row row = (Row)df.first();
		System.out.println(row.size());
		Vector vector = (Vector)row.get(0);
		print(vector.toArray());
		Row r1 = Correlation.corr(df, "features").head();
		System.out.println("Pearson correlation matrix:\n" + r1.get(0).toString());

//		Row r2 = Correlation.corr(df, "features", "spearman").head();
//		System.out.println("Spearman correlation matrix:\n" + r2.get(0).toString());
	}
	
	private void print(double[] values) {
		for (int i=0; i<values.length; i++) {
			System.out.print(values[i]);
			System.out.print(", ");
		}
	}
}
