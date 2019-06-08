package org.barenode;

import org.apache.spark.HashPartitioner;
import org.apache.spark.Partitioner;
import org.apache.spark.api.java.JavaRDD;
import org.apache.spark.ml.recommendation.ALS.Rating;

public class CALS {

	public void run(JavaRDD<Rating<Long>> ratings) {
	    Partitioner userPart = new HashPartitioner(10);
	    Partitioner itemPart = new HashPartitioner(10);
	    partitionRatings(ratings, userPart, itemPart);
	}
	
	
	private void partitionRatings(
		JavaRDD<Rating<Long>> ratings,
		Partitioner srcPart,
		Partitioner dstPart) 
	{
		Integer numPartitions = srcPart.numPartitions() * dstPart.numPartitions();
		ratings.mapPartitions(iter->{
			
		});
	}						
}
