Optimizing Robustness
=====================

.. automodule:: robtim.opt

The `RobustnessOptimizer`
-------------------------
.. autoclass:: robtim.opt.RobustnessOptimizer
   :members:
   :undoc-members:

The `EANGenerator`
-----------------------
.. autoclass:: robtim.opt.EANGenerator
   :members:
   :undoc-members:

Helper classes
++++++++++++++
.. note::
    All classes in this section inherit from :class:`EANGenerator` and have
    therefore the same functions. This documentation lists only the constructors and 
    their parameters.
   
.. autoclass:: robtim.opt.IncrementalSlackEANGenerator
   :members: distribute 
   
Implementations
+++++++++++++++
.. note::
    All classes in this section inherit from :class:`EANGenerator` and have
    therefore the same functions. This documentation lists only the constructors and 
    their parameters.

.. autoclass:: robtim.opt.IncreasingSlackEANGenerator
.. autoclass:: robtim.opt.GiveTheRichEANGenerator
.. autoclass:: robtim.opt.GiveTheRichWeightedEANGenerator
   
The `Timetabler`
-----------------------
.. autoclass:: robtim.opt.Timetabler
   :members:
   :undoc-members:

Implementations
+++++++++++++++
.. note::
    All classes in this section inherit from :class:`Timetabler` and have
    therefore the same functions. This documentation lists only the constructors and 
    their parameters.

.. autoclass:: robtim.opt.ConfigurableTimetabler
.. autoclass:: robtim.opt.DefaultTimetabler

The `Supervisor`
-----------------------
.. autoclass:: robtim.opt.Supervisor
   :members:
   :undoc-members:
   
Implementations
+++++++++++++++
.. note::
    All classes in this section inherit from :class:`Supervisor` and have
    therefore the same functions. This documentation lists only the constructors and 
    their parameters.

.. autoclass:: robtim.opt.DefaultSupervisor
.. autoclass:: robtim.opt.MatrixRobustnessSupervisor
.. autoclass:: robtim.opt.RobustnessSupervisor