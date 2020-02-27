Evaluating Robustness
=====================

.. automodule:: robtim.eval

The `RobustnessEvaluator`
-------------------------
.. autoclass:: robtim.eval.RobustnessEvaluator
   :members:
   :undoc-members:

Implementations
+++++++++++++++
.. note::
    All classes in this section inherit from :class:`RobustnessEvaluator` and have
    therefore the same functions. This documentation lists only the constructors and 
    their parameters.

.. autoclass:: robtim.eval.TreeOnTrackEvaluator
   

The `ScenarioGenerator`
-----------------------
.. autoclass:: robtim.eval.ScenarioGenerator
   :members:
   :undoc-members:

Helper classes
++++++++++++++
.. note::
    All classes in this section inherit from :class:`ScenarioGenerator` and have
    therefore the same functions. This documentation lists only the constructors and 
    their parameters.

.. autoclass:: robtim.eval.ScenarioScheduler
.. autoclass:: robtim.eval.ConfigurableScenarioGenerator
.. autoclass:: robtim.eval.DistributionScenarioGenerator

Implementation
++++++++++++++
.. note::
    All classes in this section inherit from :class:`ScenarioGenerator` or from 
    classes in the prior section and have therefore the same functions. 
    This documentation lists only the constructors and their parameters.

.. warning:: 
    Note that all scenario generators in this section yield infinetly many delay
    scenarios. Use :class:`ScenarioScheduler` to ensure that the evaluation terminates.

.. autoclass:: robtim.eval.TreeOnTrackScenarioGenerator
.. autoclass:: robtim.eval.NormalDistScenarioGenerator
.. autoclass:: robtim.eval.PoissonDistScenarioGenerator
.. autoclass:: robtim.eval.ScAlbertU1
.. autoclass:: robtim.eval.ScAlbertU2

The `DelayManager`
------------------
.. autoclass:: robtim.eval.DelayManager
   :members:
   :undoc-members:
   
Implementations
+++++++++++++++
.. note::
    All classes in this section inherit from :class:`DelayManager` and have
    therefore the same functions. This documentation lists only the constructors and 
    their parameters.

.. autoclass:: robtim.eval.ConfigurableDelayManager
.. autoclass:: robtim.eval.OrdinaryDelayManager

   
The `Statistician`
------------------
.. autoclass:: robtim.eval.Statistician
   :members:
   :undoc-members:
   
Implementations
++++++++++++++++
.. note::
    All classes in this section inherit from :class:`Statistician` and have
    therefore the same functions. This documentation lists only the constructors and 
    their parameters.

.. autoclass:: robtim.eval.OrdinaryStatistician
.. autoclass:: robtim.eval.MatrixStatistician
.. autoclass:: robtim.eval.CoefficientStatistician