import sys

from pyspark import since, keyword_only
from pyspark.ml.param.shared import *
from pyspark.ml.wrapper import JavaEstimator, JavaModel, JavaTransformer, _jvm, JavaParams

class ScalerModel(JavaModel):

    _classpath_model = 'mlonspark.ScalerModel'

    @staticmethod
    def _from_java(java_stage):
        """
        Given a Java object, create and return a Python wrapper of it.
        Used for ML persistence.

        Meta-algorithms such as Pipeline should override this method as a classmethod.
        """
        # Generate a default new instance from the stage_name class.
        py_type = ScalerModel
        if issubclass(py_type, JavaParams):
            # Load information from java_stage to the instance.
            py_stage = py_type()
            py_stage._java_obj = java_stage
            py_stage._resetUid(java_stage.uid())
            py_stage._transfer_params_from_java()

        return py_stage


class Scaler(JavaEstimator, HasInputCol, HasOutputCol):

    groupCol = Param(Params._dummy(), "groupCol", "groupCol", typeConverter=TypeConverters.toString)

    _classpath = 'mlonspark.Scaler'

    @keyword_only
    def __init__(self):
        super(Scaler, self).__init__()
        self._java_obj = self._new_java_obj(
            Scaler._classpath ,
            self.uid
        )
        kwargs = self._input_kwargs
        self.setParams(**kwargs)

    @keyword_only
    def setParams(self):
        kwargs = self._input_kwargs
        return self._set(**kwargs)

    def _create_model(self, java_model):
        return ScalerModel(java_model)

    def setGroupCol(self, value):
        return self._set(groupCol=value)

    def getGroupCol(self):
        return self.getOrDefault(self.groupCol)

    def setOutputCol(self, value):
        return self._set(outputCol=value)

    def getOutputCol(self):
        return self.getOrDefault(self.outputCol)

    def setInputCol(self, value):
        return self._set(inputCol=value)

    def getInputCol(self):
        return self.getOrDefault(self.inputCol)
