# VDYP
Tools to simplify developing VDYP. 

".BAT" and ".sh" scripts are provided to ease running the tools from the command line. In 
each case, running the scripts with no arguments presents a usage statement. This document 
provides some more details.

All tools make use of these environment variables:
1. **VDYP_TOOLS_PYTHON_HOME**: the location of a python3 installation such as **/usr/bin**.
2. **VDYP_TOOLS_PYTHON**: the location of the python module to be run.

**call_tree**, **browse_usages** and **commons** all require the original VDYP source code to 
be available on the file system of the local machine. The environment variable

**VDYP_MASTER_SOURCE_FOLDER**

is the location of the **Source** folder of that project, such as **C:\source\vdyp\VDYP_Master\Source**.

## Show Deltas
This application compares the output of two VDYP (Forward) runs. The output is supplied in a set
of files, three of which are relevant to this application: the polygon file, the species file and
the utilizations file. These normally have the names **vp_grow2.dat**, **vs_grow2.dat** and **vu_grow2.dat**,
respectively. If the set you're comparing 

The following arguments are required:
1. **vdyp7_folder**: the location of first set of output files to be compared.
2. **vdyp8_folder**: the location of the second set of output files.

The following arguments are optional:
1. **-p <pattern>**: the pattern of the output file names. It is to contain one '%' character, and 'p', 's'
	and 'u' are substituted in place of '%' to get the three filenames. The default is **v%_grow2.dat**. If
	this simple substitution is insufficient, rename the files.
2. **-t <tolerance>**: the tolerance of differences in floating point numbers. This will be a number close
	to 0, such as 0.01 or 0.05. A given pair of numbers (a, b) is considered equal if 

	**abs(a / b) - 1 < tolerance**

	that is, if the ratio of the two numbers, minus one, is no more that **tolerance** away from 0.

The file supplied to this application (the output of a Forward run) have one set of entries of their
type (polygon, species, utilizations) for each year of each polygon. Thus, the polygon file for a given
polygon P, whose data is available for year Y and is to grow for 10 years, will have one line of P at year Y,
a line for P at year Y + 1, etc., up to P at year 10. The same is true of the species and utilizations
files. 

That said, there are VDYP Forward Control Variable 4 values that limit the years for which data is produced. 
If a value other than the default (all years - value 3), it must be that both the runs operate under the 
same settings. See the **ControlVariable** class.

An exception - for the vdyp8 file set only - is that it is possible to ask Forward to "checkpoint" each of
the 14 steps of the grow algorithm. This is done by setting Control Variable 7 to the value "1". This 
results in information produced by Forward for each growth year of each polygon being duplicated 14 times.
This application understands this. This option is not available to vdyp7.

<more to come>.