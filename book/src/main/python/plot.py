import numpy
import matplotlib.pyplot as pyplot

if __name__ == '__main__':
    fig = pyplot.figure()
    -- pyplot.plot(numpy.randn(50).cumsum(), 'k--')
    pyplot.savefig('figpath.svg')
    print('plot')