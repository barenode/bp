import array
from collections import namedtuple

from pyspark import SparkContext, since
from pyspark.rdd import RDD
from pyspark.mllib.common import JavaModelWrapper, callMLlibFunc, inherit_doc
from pyspark.mllib.util import JavaLoader, JavaSaveable
from pyspark.sql import DataFrame


class NALS(JavaModelWrapper, JavaSaveable, JavaLoader):
    def test(self):
        return self

class Test:
    def __init__(self):
        print("SELF")


class Test2:
    def __init__(self):
        print("SELF")


