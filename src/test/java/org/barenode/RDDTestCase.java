package org.barenode;

import org.apache.spark.api.java.JavaRDD;
import org.apache.spark.mllib.recommendation.ALS;
import org.apache.spark.mllib.recommendation.MatrixFactorizationModel;
import org.apache.spark.mllib.recommendation.Rating;
import org.apache.spark.sql.Dataset;
import org.apache.spark.sql.Encoders;
import org.junit.Test;

public class RDDTestCase extends SharedSparkSession {

	@Test public void test() throws Exception {
		//Rating rating = new Rating();
		
		Dataset<String> rawArtistData = spark.read().textFile("/filmy/data/user_artist_data.txt");
		JavaRDD<Rating> ratings = buildRDD(rawArtistData);
		System.out.println("COUNT: " + ratings.count());
		
		MatrixFactorizationModel model = new ALS().setIterations(0).setRank(1).run(ratings);
		model.save(jsc.sc(), "/filmy/data/1/");

	}
	
	JavaRDD<Rating> buildRDD(Dataset<String> rawArtistData) {
		return rawArtistData.map(line -> {
			String[] parts = line.split(" ");
			Integer userId = Integer.valueOf(parts[0]);
			Integer artistId = Integer.valueOf(parts[1]);
			//play count
			Double playCount = Double.valueOf(parts[2]);			
			return new Rating(userId, artistId, playCount);
		}, Encoders.bean(Rating.class)).toJavaRDD();
	}
}
