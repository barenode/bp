package org.barenode;

import java.util.Arrays;

import org.apache.spark.api.java.JavaRDD;
import org.apache.spark.mllib.linalg.distributed.BlockMatrix;
import org.apache.spark.mllib.linalg.distributed.CoordinateMatrix;
import org.apache.spark.mllib.linalg.distributed.MatrixEntry;
import org.junit.Test;

public class FooTestCase extends SharedSparkSession {
	
	@Test 
	public void test() throws Exception {
		JavaRDD<MatrixEntry> rdd = jsc.parallelize(Arrays.asList(
			new MatrixEntry(0, 0, 0.0),
			new MatrixEntry(1, 0, 0.0),
			new MatrixEntry(0, 1, 0.0),
			new MatrixEntry(1, 1, 0.0)
		));
		CoordinateMatrix coordMat = new CoordinateMatrix(rdd.rdd());
		BlockMatrix matA = coordMat.toBlockMatrix().cache();		
		matA.validate();

		// Calculate A^T A.
		BlockMatrix ata = matA.transpose().multiply(matA);		
		
		//BlockMatrix m = new BlockMatrix();
	}
}
