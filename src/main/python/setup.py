from setuptools import setup

setup(
    name='mlonspark',
    version='1.0',
    description='ALS module',
    author='Frantisek Hylmar',
    author_email='frantisek.hylmar@gmail.com',
    packages=[
        'mlonspark',
        'mlonspark.utils'
    ]
    #,  #same as name
    #install_requires=['pyspark', 'py4j'], #external packages as dependencies
)