import sys

from pyspark import since, keyword_only
from pyspark.ml.param.shared import *
from pyspark.ml.util import JavaMLReadable, JavaMLWritable, MLReader
from pyspark.ml.wrapper import JavaEstimator, JavaModel, JavaTransformer, _jvm, JavaParams

class AlternatingLeastSquareModel(JavaModel, JavaMLReadable, JavaMLWritable):

    _classpath_model = 'mlonspark.AlternatingLeastSquareModel'

    @staticmethod
    def _from_java(java_stage):
        """
        Given a Java object, create and return a Python wrapper of it.
        Used for ML persistence.

        Meta-algorithms such as Pipeline should override this method as a classmethod.
        """
        # Generate a default new instance from the stage_name class.
        py_type = AlternatingLeastSquareModel
        if issubclass(py_type, JavaParams):
            # Load information from java_stage to the instance.
            py_stage = py_type()
            py_stage._java_obj = java_stage
            py_stage._resetUid(java_stage.uid())
            py_stage._transfer_params_from_java()

        return py_stage

    @classmethod
    def read(cls):
        """Returns an MLReader instance for this class."""
        return CustomJavaMLReader(cls, cls._classpath_model)


class AlternatingLeastSquare(JavaEstimator, JavaMLReadable, JavaMLWritable, HasMaxIter, HasPredictionCol, HasRegParam, HasSeed):

    _classpath = 'mlonspark.AlternatingLeastSquare'

    rank = Param(Params._dummy(), "rank", "rank of the factorization",  typeConverter=TypeConverters.toInt)
    alpha = Param(Params._dummy(), "alpha", "alpha for implicit preference", typeConverter=TypeConverters.toFloat)
    userCol = Param(Params._dummy(), "userCol", "userCol", typeConverter=TypeConverters.toString)
    itemCol = Param(Params._dummy(), "itemCol", "itemCol", typeConverter=TypeConverters.toString)
    ratingCol = Param(Params._dummy(), "ratingCol", "ratingCol", typeConverter=TypeConverters.toString)
    numItemBlocks = Param(Params._dummy(), "numItemBlocks", "numItemBlocks", typeConverter=TypeConverters.toInt)
    numUserBlocks = Param(Params._dummy(), "numUserBlocks", "numUserBlocks", typeConverter=TypeConverters.toInt)

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

    def setAlpha(self, value):
        return self._set(alpha=value)

    def getAlpha(self):
        return self.getOrDefault(self.alpha)

    def setNumItemBlocks(self, value):
        return self._set(numItemBlocks=value)

    def getNumItemBlocks(self):
        return self.getOrDefault(self.numItemBlocks)

    def setNumUserBlocks(self, value):
        return self._set(numUserBlocks=value)

    def getNumUserBlocks(self):
        return self.getOrDefault(self.numUserBlocks)

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