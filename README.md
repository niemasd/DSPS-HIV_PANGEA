# DSPS-HIV_PANGEA
The archived code used to generate 'Village' simulations for the PANGEA_HIV comparison exercise

Please note that this code is *archived* to reflect what was used to create the simulations used in the PANGEA_HIV comparision exercise, and will not be updated. A seperate DSPS-HIV repository will soon be created where the newest version of the code can be found.

No .jar file is provided with this code as there are some hard-coded file references in the source code (bad practice - I know!). Source code must also be changed to active whether an 'acute' stage is simulated (with higher transission probability). See below (sections 2.1 and 2.2) on how to do both of these. 

####A Note on Heritability

Though heritabilty functions can be found in the program, they do not work well when set to values other than 1 (viral load is identical from parent to child) or 0 (viral load is unrelated to parental value, is randomly selected from population).

####XML File

An example XML file is included which specifies running a simulation matching Village simulations 1 and 6 - with a higher transmission probability during the acute stage, 'fast' ART roll-out starting at year 40, and migration from neighbouring 'villages.' Growth is exponential at 1% in this example. 

Code is also provided that allows the user to generate their own XML file similar to those used in the PANGEA_HIV comparison exercise.

####Output

Note that though the DSPS-HIV produces a phylogeny, this was not used for the PANGEA_HIV comparison exercise. Instead, sampling events were generated using the sampling code, inserted into the line-list of events, and a phylogeny produced using VirusTreeSimulator.jar.
(Code for sampling runs is provided)


##2.1 How to Modify Hard-Coded File Paths

####Setting path for the input viral load file

In individualBasedModel/Virus.java, in method getSetpt() the user must provide a file specifying a file that contains the setpoint values of the population being modelled. If heritability is set to 0, values are randomly chosen from this file when the virus is transmitted. However, the file must be provided even if heritability is set to 1. 

On line 191, provide the number of items in the file. 
On line 198, provide a path to the list of viral loads to be read in, one VL per line, in log10 values. 
If heritability is set to 1, you can provide just one value, and the program will be satisified. If heritability is set to 0 but you do not have a list of viral loads from a real population, you can randomly generate some from a distribution.

####Setting path for the MaxTime file

As the DSPS-HIV lags in producing output while it runs, if you force the simulation to stop, you may be missing a lot of informatin about the current run, and will be missing some files such as the trees, which are generated at the end of the run. 

However, you may want to stop a run early but still generate all the information you'd get if the run had gone to your intial MaxTime. In this case, create a one-line file with the new MaxTime. 

In individualBasedModel/Scheduler.java on line 743, set the path to this file. The program will periodically check this file to see if the value in the file is different from what it already has. If so, it will set this new value to be the MaxTime, and run until this time is reached, the exit normally. 


##2.2 How to Turn On Acute Stage

In individualBasedModel/Virus.java, set the global variable 'acute' to 'true' if you want transmission probabilty to be higher during the first 3 months of infection, or 'false' if you do not want higher transmission probability during this time. 
