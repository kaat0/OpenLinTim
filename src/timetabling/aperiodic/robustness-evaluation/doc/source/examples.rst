Examples
========

Setting up a dataset
--------------------
First one needs to set up a :class:`robtim.Dataset` object. It takes care of the 
communication with LinTim. In this example we have moved most configuration 
parameters to a special file `config.py`. The following things are defined there:

**Execution environment**

In order to run `make` properly one has to specify the execution environment of the
shell. This is especially necessary if you want to use third-party solvers like 
Gurobi or Xpress.

.. literalinclude:: ../../config.py
    :linenos:
    :language: python
    :lines: 12-22
    
**Configuration for LinTim**

LinTim comes with many configuration keys. A default configuration may look like this:

.. literalinclude:: ../../config.py
    :linenos:
    :language: python
    :lines: 27-33
    
**Path to the datasets**

The last thing which needs to be specified is the path to the datasets, e.g.

.. literalinclude:: ../../config.py
    :linenos:
    :language: python
    :lines: 35-36

**Initialize dataset**

Now a dataset object can be created in the main file. To reset the configuration
(:func:`robtim.Dataset.resetConfig`) may not be necessary in most cases.

.. literalinclude:: ../../figure_54.py
    :linenos:
    :language: python
    :lines: 4, 7-11
    
Evaluating Robustness
---------------------

For evaluating the robustness of a timetable one needs an instance of an
:class:`robtim.eval.RobustnessEvaluator` object. It comes with several parameters
which are described in the documentation of this class.

The following code sets up an RobustnessEvaluator with the TreeOnTrack model,
i.e. :class:`robtim.eval.TreeOnTrackScenarioGenerator`, and a :class:`robtim.eval.MatrixStatistician`
which collects the field `dm_time_average` from the LinTim component `dm-disposition-timetable-evaluate`
in every iteration. :class:`robtim.eval.ScenarioScheduler` takes care of the amount
of iterations. The scenario generator would otherwise yield infinitely many delay scenarios.

.. literalinclude:: ../../figure_54.py
    :linenos:
    :language: python
    :lines: 18-24
    
Now the robustness can be evaluated using

.. code-block:: python
    
    nominal, delayed = evaluator.evaluate(d)
    
nominal contains the value of `dm_time_average` for the nominal timetable. delayed
is a matrix with rows for every delay scenario. Each row contains the value of
`dm_time_average` for the disposition timetable

Optimizing Robustness
---------------------

For optimizing the timetable with respect to the robustness one neeeds an instance 
of a :class:`robtim.opt.RobustnessOptimizer` object. It comes with several parameters
which are described in the documentation of this class.

The following code sets up an RobustnessOptimizer with :class:`robtim.opt.IncreasingSlackEANGenerator`.
This EANGenerator increases in this example the slack of all activities by 5 min in 
every iteration (first parameter), increases the lower bounds as well as the upper bound (second paramter: True)
and modifies only the `change` activities (third parameter). A :class:`robtim.opt.MatrixRobustnessSupervisor`
is used to provide a stop criterion and collect statistical data. This supervisor will
stop after 15 iterations and uses the RobustnessEvaluator object from the last section to 
compute the robustness of the timetables.

.. literalinclude:: ../../figure_54.py
    :linenos:
    :language: python
    :lines: 27-32
    
After executing these lines, ean contains a dict with information about the 
final ean, see :func:`robtim.opt.EANGenerator.eans`, and report contains a matrix provided by
the supervisor with the robustness information from every iteration, see 
:func:`robtim.opt.Supervisor.report`.
