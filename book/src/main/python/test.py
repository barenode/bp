import pandas
import numpy
import numpy.linalg as linalg
import matplotlib.pyplot as pyplot

class Test:

    def __init__(self):
        self.test = 0
        self.test2 = 1

    def call(self):
        self.test2 = 2
        x = numpy.array([
            [1, 2],
            [4, 5]
        ])
        z = x.transpose()
        f = z.dot(x)
        print(f)
        y = linalg.inv(z)
        print(y)

    def load(self):
        a = numpy.loadtxt('matrix.dat')
        print(a)
        print(numpy.size(a, 0))
        print(numpy.size(a, 1))
        x = numpy.random.random((2, numpy.size(a, 0)))

        rmse_array = []

        for i in range(15):
            y = linalg.inv(x.dot(x.transpose())).dot(x).dot(a)
            x = linalg.inv(y.dot(y.transpose())).dot(y).dot(a.transpose())
            p = x.transpose().dot(y)
            rmse = numpy.sqrt(numpy.mean((a - p) ** 2))
            rmse_array.append(rmse)

        print("RMSE: ", rmse_array)
        print(numpy.around(x.transpose(), decimals=2))
        print("RMSE: ", rmse_array)
        print(numpy.around(y, decimals=2))
        print("RMSE: ", rmse_array)
        print(numpy.around(p, decimals=2))
        # print("RMSE: ", rmse_array)
        pyplot.figure(figsize=(9, 4))
        pyplot.plot(rmse_array)
        pyplot.xlabel('Iterace')
        pyplot.ylabel('RMSE')
        pyplot.savefig('figpath.svg')
        return a

if __name__ == '__main__':
    Test().load()
