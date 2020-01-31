package mlonspark

import java.{util => ju}

import mlonspark.AlternatingLeastSquare.{ALSPartitioner, LocalIndexEncoder}
import mlonspark.util.OpenHashMap
import org.apache.spark.util.collection.OpenHashSet

import scala.collection.mutable
import scala.util.Sorting

class LocalSuite extends org.scalatest.FunSuite {

  test("testBlocks") {
    val dstIds = Array(3L, 3L, 5L, 1L, 1L, 5L)
    val dstIdSet = new OpenHashSet[Long]()
    dstIds.foreach(dstIdSet.add)

    println("dstIdSet: " + dstIdSet.iterator.mkString(", "));

    val sortedDstIds = new Array[Long](dstIdSet.size)
    var i = 0
    var pos = dstIdSet.nextPos(0)
    while (pos != -1) {
      sortedDstIds(i) = dstIdSet.getValue(pos)
      pos = dstIdSet.nextPos(pos + 1)
      i += 1
    }
    Sorting.quickSort(sortedDstIds)

    println("sortedDstIds: " + sortedDstIds.iterator.mkString(", "));

    val dstIdToLocalIndex = new OpenHashMap[Long, Int](sortedDstIds.length)
    i = 0
    while (i < sortedDstIds.length) {
      dstIdToLocalIndex.update(sortedDstIds(i), i)
      i += 1
    }

    println("dstIdToLocalIndex: " + dstIdToLocalIndex.iterator.mkString(", "));

    val dstLocalIndices = dstIds.map(dstIdToLocalIndex.apply)
    println("dstLocalIndices: " + dstLocalIndices.mkString(", "));
  }


  test("compress") {

    val srcIds = Array(1, 1, 2, 2, 5, 6, 3, 1, 2, 3)

    val length = srcIds.length

    val sz = length
    assert(sz > 0, "Empty in-link block should not exist.")
    //sort()
    val uniqueSrcIdsBuilder = mutable.ArrayBuilder.make[Long]
    val dstCountsBuilder = mutable.ArrayBuilder.make[Int]
    var preSrcId = srcIds(0)
    uniqueSrcIdsBuilder += preSrcId
    var curCount = 1
    var i = 1
    while (i < sz) {
      val srcId = srcIds(i)
      if (srcId != preSrcId) {
        uniqueSrcIdsBuilder += srcId
        dstCountsBuilder += curCount
        preSrcId = srcId
        curCount = 0
      }
      curCount += 1
      i += 1
    }
    dstCountsBuilder += curCount
    val uniqueSrcIds = uniqueSrcIdsBuilder.result()
    val numUniqueSrdIds = uniqueSrcIds.length
    val dstCounts = dstCountsBuilder.result()
    val dstPtrs = new Array[Int](numUniqueSrdIds + 1)
    var sum = 0
    i = 0
    while (i < numUniqueSrdIds) {
      sum += dstCounts(i)
      i += 1
      dstPtrs(i) = sum
    }

    println("uniqueSrcIds: " + uniqueSrcIds.mkString(", "));
    println("dstPtrs: " + dstPtrs.mkString(", "));

  }


  test("outBlocks") {
    val dstPart = new ALSPartitioner(5)
    val encoder = new LocalIndexEncoder(dstPart.numPartitions)

    //val x = encoder

    val srcIds = Array(1, 3, 4, 7)
    val dstPtrs = Array(0, 2, 4, 7, 10)
    val dstEncodedIndices = Array(
      encoder.encode(0, 1),
      encoder.encode(1, 3),
      encoder.encode(0, 0),
      encoder.encode(2, 3),
      encoder.encode(1, 1),
      encoder.encode(2, 0),
      encoder.encode(3, 2),
      encoder.encode(1, 4),
      encoder.encode(3, 0),
      encoder.encode(4, 1)
    )

    val activeIds = Array.fill(dstPart.numPartitions)(mutable.ArrayBuilder.make[Int])
    var i = 0
    val seen = new Array[Boolean](dstPart.numPartitions)
    while (i < srcIds.length) {
      var j = dstPtrs(i)
      ju.Arrays.fill(seen, false)
      while (j < dstPtrs(i + 1)) {
        val dstBlockId = encoder.blockId(dstEncodedIndices(j))
        if (!seen(dstBlockId)) {
          activeIds(dstBlockId) += i // add the local index in this out-block
          seen(dstBlockId) = true
        }
        j += 1
      }
      i += 1
    }

    val res = activeIds.map { x =>
      x.result()
    }

    res.foreach { i =>
      println("> [" + i.mkString(", ") + "]")
    }
  }
}
