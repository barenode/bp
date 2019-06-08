package org.barenode;

import org.apache.spark.api.java.JavaRDD;
import org.apache.spark.ml.recommendation.ALS.Rating;
import org.apache.spark.sql.Dataset;
import org.apache.spark.sql.Encoders;
import org.junit.Test;

public class CALSTestCase extends SharedSparkSession {

	@Test public void test() throws Exception {
		//Rating rating = new Rating();
		
		Dataset<String> rawArtistData = spark.read().textFile("/filmy/data/user_artist_data.txt");
		JavaRDD ratings = buildRDD(rawArtistData);
		new CALS().run(ratings);

	}
	
	JavaRDD<Rating> buildRDD(Dataset<String> rawArtistData) {
		return rawArtistData.map(line -> {
			String[] parts = line.split(" ");
			Integer userId = Integer.valueOf(parts[0]);
			Integer artistId = Integer.valueOf(parts[1]);
			//play count
			Float playCount = Float.valueOf(parts[2]);			
			return new Rating<Integer>(userId, artistId, playCount);
		}, Encoders.bean(Rating.class)).toJavaRDD();
	}
	
}
