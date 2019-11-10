from setuptools import setup

setup(
    name='mlonspark',
    version='1.0',
    description='A useful module',
    author='Frantisek Hylmar',
    author_email='frantisek.hylmar@gmail.com',
    packages=[
        'mlonspark',
        'mlonspark.feature',
        'mlonspark.utils'
    ]
    #,  #same as name
    #install_requires=['pyspark', 'py4j'], #external packages as dependencies
)