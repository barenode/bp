package org.barenode;

import static org.apache.spark.sql.functions.*;
import static org.apache.spark.sql.Encoders.*;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Random;

import org.apache.spark.api.java.function.FlatMapFunction;
import org.apache.spark.api.java.function.MapFunction;
import org.apache.spark.broadcast.Broadcast;
import org.apache.spark.mllib.recommendation.ALS;
import org.apache.spark.sql.Column;
import org.apache.spark.sql.Dataset;
import org.apache.spark.sql.Encoders;
import org.apache.spark.sql.Row;
import org.junit.Test;

import com.esotericsoftware.kryo.serializers.FieldSerializer.Optional;

import scala.Option;
import scala.Tuple2;
import scala.Tuple3;

@SuppressWarnings("serial")
public class RecommenderTestCase extends SharedSparkSession {

	@Test public void test() throws Exception {
//		Dataset<String> raw = spark.read().textFile("/filmy/data/user_artist_data.txt");
//		MapFunction<String, Tuple2<Integer, Integer>> fn = line -> {
//			String[] parts = line.split(" ");
//			return new Tuple2<Integer, Integer>(
//				Integer.valueOf(parts[0]), 
//				Integer.valueOf(parts[1]));
//		};
//		
//		Dataset<Row> ds = raw.map(fn, Encoders.tuple(Encoders.INT(), Encoders.INT())).toDF("user", "artist");
//		ds.agg(
//			min("user"),
//			max("user"),
//			min("artist"),
//			max("artist")).show();
		
		
		
//		Dataset<String> rawArtistData = spark.read().textFile("/filmy/data/artist_data.txt");
//		FlatMapFunction<String, Tuple2<Integer, String>> artistMap = line -> {
//			try {
//				int border = Math.min(line.indexOf("\t"), line.indexOf(" "));			
//				return Itr.of(new Tuple2<Integer, String>(
//					Integer.valueOf(line.substring(0, border)), 
//					line.substring(border)));
//			} catch (Exception e) {
//				return Itr.none();
//			}
//		};
//		System.out.println("COUNT: " + rawArtistData.flatMap(artistMap, Encoders.tuple(Encoders.INT(), Encoders.STRING())).count());
		
		
		Dataset<String> rawArtistAlias = spark.read().textFile("/filmy/data/artist_alias.txt");
		FlatMapFunction<String, Tuple2<Integer, Integer>> artistAliasMap = line -> {			
			try {
				String[] parts = line.split("\t");
				if (parts.length!=2) {
					return Itr.none();
				} else {				
					return Itr.of(new Tuple2<Integer, Integer>(
						Integer.valueOf(parts[0]), 
						Integer.valueOf(parts[1])));
				}		
			} catch (Exception e) {
				return Itr.none();
			}
		};
		Map<Integer, Integer> artistAlias = new HashMap<>();
		rawArtistAlias.flatMap(artistAliasMap, Encoders.tuple(Encoders.INT(), Encoders.INT())).foreach(t -> {
			artistAlias.put(t._1, t._2);
		});		
		Broadcast<Map<Integer, Integer>> a = jsc.broadcast(artistAlias);		
		
		Dataset<String> rawArtistData = spark.read().textFile("/filmy/data/user_artist_data.txt");
		Dataset<Row> counts = buildCounts(rawArtistData, a);		
		
		counts.cache();
		
		new ALS()
			.setSeed(new Random().nextLong())
			.setImplicitPrefs(true)
			.setRank(10)
			.setAlpha(1.0)
			.setIterations(5)
			
			;
	}
	
	
	
	Dataset<Row> buildCounts(Dataset<String> rawArtistData, Broadcast<Map<Integer, Integer>> artistAlias) {
		return rawArtistData.map(line -> {
			String[] parts = line.split(" ");
			Integer artistId = Integer.valueOf(parts[1]);
			artistId = artistAlias.value().getOrDefault(artistId, artistId);
			return new Tuple3<Integer, Integer, Integer>(
				Integer.valueOf(parts[0]), 
				artistId,
				Integer.valueOf(parts[2]));
		}, tuple(INT(), INT(), INT())).toDF();
	}
	
	public static class Itr {
		
		public static <T> Iterator<T> none() {
			return new Iterator<T>() {
				@Override
				public boolean hasNext() {
					return false;
				}
				@Override
				public T next() {
					throw new IllegalStateException();
				}				
			};
		}
		
		public static <T> Iterator<T> of(T value) {
			return new SingleItemIterator<T>(value);
		}
	}
	
	private static final class SingleItemIterator<T> implements Iterator<T> {

		private final T value;
		private boolean first = true;
		
		SingleItemIterator(T value) {
			super();
			this.value = value;
		}
		
		@Override
		public boolean hasNext() {
			return first;
		}

		@Override
		public T next() {
			first = false;
			return value;
		}		
	}
	
}
