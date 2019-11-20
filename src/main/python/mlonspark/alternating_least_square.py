import sys

from pyspark.ml.util import MLReader, _jvm

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
        py_type = BucketizerModel
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


class AlternatingLeastSquare(JavaEstimator, JavaMLReadable, JavaMLWritable):

    _classpath = 'mlonspark.AlternatingLeastSquare'

    @keyword_only
    def __init__(self):
        super(Bucketizer, self).__init__()
        self._java_obj = self._new_java_obj(
            Bucketizer._classpath ,
            self.uid
        )
        kwargs = self._input_kwargs
        self.setParams(**kwargs)

    @keyword_only
    def setParams(self):
        kwargs = self._input_kwargs
        return self._set(**kwargs)

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