import sys

from pyspark import since, keyword_only
from pyspark.ml.param.shared import *
from pyspark.ml.util import JavaMLReadable, JavaMLWritable, MLReader
from pyspark.ml.wrapper import JavaEstimator, JavaModel, JavaTransformer, _jvm, JavaParams
from pyspark.ml.common import inherit_doc

class AlternatingLeastSquareModel(JavaModel, JavaMLReadable, JavaMLWritable):
    """
    Model fitted by ALS.

    .. versionadded:: 1.4.0
    """

    @property
    @since("1.4.0")
    def rank(self):
        """rank of the matrix factorization model"""
        return self._call_java("rank")

    @property
    @since("1.4.0")
    def userFactors(self):
        """
        a DataFrame that stores user factors in two columns: `id` and
        `features`
        """
        return self._call_java("userFactors")

    @property
    @since("1.4.0")
    def itemFactors(self):
        """
        a DataFrame that stores item factors in two columns: `id` and
        `features`
        """
        return self._call_java("itemFactors")

    @since("2.2.0")
    def recommendForAllUsers(self, numItems):
        """
        Returns top `numItems` items recommended for each user, for all users.

        :param numItems: max number of recommendations for each user
        :return: a DataFrame of (userCol, recommendations), where recommendations are
                 stored as an array of (itemCol, rating) Rows.
        """
        return self._call_java("recommendForAllUsers", numItems)

    @since("2.2.0")
    def recommendForAllItems(self, numUsers):
        """
        Returns top `numUsers` users recommended for each item, for all items.

        :param numUsers: max number of recommendations for each item
        :return: a DataFrame of (itemCol, recommendations), where recommendations are
                 stored as an array of (userCol, rating) Rows.
        """
        return self._call_java("recommendForAllItems", numUsers)

    @since("2.3.0")
    def recommendForUserSubset(self, dataset, numItems):
        """
        Returns top `numItems` items recommended for each user id in the input data set. Note that
        if there are duplicate ids in the input dataset, only one set of recommendations per unique
        id will be returned.

        :param dataset: a Dataset containing a column of user ids. The column name must match
                        `userCol`.
        :param numItems: max number of recommendations for each user
        :return: a DataFrame of (userCol, recommendations), where recommendations are
                 stored as an array of (itemCol, rating) Rows.
        """
        return self._call_java("recommendForUserSubset", dataset, numItems)

    @since("2.3.0")
    def recommendForItemSubset(self, dataset, numUsers):
        """
        Returns top `numUsers` users recommended for each item id in the input data set. Note that
        if there are duplicate ids in the input dataset, only one set of recommendations per unique
        id will be returned.

        :param dataset: a Dataset containing a column of item ids. The column name must match
                        `itemCol`.
        :param numUsers: max number of recommendations for each item
        :return: a DataFrame of (itemCol, recommendations), where recommendations are
                 stored as an array of (userCol, rating) Rows.
        """
        return self._call_java("recommendForItemSubset", dataset, numUsers)



class AlternatingLeastSquare(JavaEstimator, JavaMLReadable, JavaMLWritable, HasMaxIter, HasPredictionCol, HasRegParam, HasSeed):

    _classpath = 'org.apache.spark.ml.recommendation.ALS'

    rank = Param(Params._dummy(), "rank", "rank of the factorization",  typeConverter=TypeConverters.toInt)
    alpha = Param(Params._dummy(), "alpha", "alpha for implicit preference", typeConverter=TypeConverters.toFloat)
    userCol = Param(Params._dummy(), "userCol", "userCol", typeConverter=TypeConverters.toString)
    itemCol = Param(Params._dummy(), "itemCol", "itemCol", typeConverter=TypeConverters.toString)
    ratingCol = Param(Params._dummy(), "ratingCol", "ratingCol", typeConverter=TypeConverters.toString)

    @keyword_only
    def __init__(self):
        super(AlternatingLeastSquare, self).__init__()
        self._java_obj = self._new_java_obj(
            AlternatingLeastSquare._classpath ,
            self.uid
        )
        kwargs = self._input_kwargs
        self.setParams(**kwargs)

    @keyword_only
    def setParams(self):
        kwargs = self._input_kwargs
        return self._set(**kwargs)

    @since("1.4.0")
    def setRank(self, value):
        """
        Sets the value of :py:attr:`rank`.
        """
        return self._set(rank=value)

    @since("1.4.0")
    def getRank(self):
        """
        Gets the value of rank or its default value.
        """
        return self.getOrDefault(self.rank)

    def setUserCol(self, value):
        return self._set(userCol=value)

    def getUserCol(self):
        return self.getOrDefault(self.userCol)

    def setItemCol(self, value):
        return self._set(itemCol=value)

    def getItemCol(self):
        return self.getOrDefault(self.itemCol)

    def setRatingCol(self, value):
        return self._set(ratingCol=value)

    def getRatingCol(self):
        return self.getOrDefault(self.ratingCol)

    def _create_model(self, java_model):
        return AlternatingLeastSquareModel(java_model)


class CustomJavaMLReader(MLReader):

    def __init__(self, clazz, java_class):
        self._clazz = clazz
        self._jread = self._load_java_obj(java_class).read()

    def load(self, path):
        """Load the ML instance from the input path."""
        java_obj = self._jread.load(path)
        return self._clazz._from_java(java_obj)

    @classmethod
    def _load_java_obj(cls, java_class):
        """Load the peer Java object of the ML instance."""
        java_obj = _jvm()
        for name in java_class.split("."):
            java_obj = getattr(java_obj, name)
        return java_obj