Modular2Simple
========================
Modular2Simple is a versatile tool developed to streamline scenario creation for the CARLA simulator by leveraging
existing simple scenarios in the .xosc format (OpenSCENARIO). This tool enables users to package a main scenario file
along with the simple scenario files referenced from within it into a .mosc file (Modular Open Scenario). It not only
works with simple scenarios in the .xosc format (OpenSCENARIO) but also has the capability to generate modular scenarios
from existing modular scenarios, allowing for generating even more complex modular scenarios. Then, it allows to convert
the resulting modular scenario (.mosc) file into standard OpenScenario (.xosc) format file, which makes it fully
compatible with, and, therefore, runnable in, the CARLA simulator.

Getting the Modular2Simple
---------------------------
To install Modular2Simple, follow these steps:

Download the Modular2Simple: [Modular2Simple](https://github.com/NikolaiKhriapov/modular2simple/blob/main/src/Modular2Simple.java)

Navigate to the downloaded file directory.

Compile the program
* javac Modular2Simple.java

Using the Modular2Simple
---------------------------

#### To package a set of .xosc files to a .mosc file, use the following command:

* java Modular2Simple --convert-xosc-to-mosc <input_xosc_files> <output_mosc_file>

Replace <input_xosc_files> with the path to input .xosc files (the main scenario file must be named 'main.xosc'), and <
output_mosc_file> with the desired path for the output modular scenario file.

Example
* java Modular2Simple --convert-mosc-to-xosc main.xosc simple_1.xosc simple_2.xosc modular.mosc

This command will package the modular.mosc file into a scenario file named result.xosc.

#### To convert a .mosc file to a .xosc scenario file, use the following command:

* java Modular2Simple --convert-mosc-to-xosc <input_mosc_file> <output_xosc_file>

Replace <input_mosc_file> with the path to the input modular package file, and <output_xosc_file> with the desired path
for the output complex scenario file.

Example
* java Modular2Simple --convert-mosc-to-xosc modular.mosc resulting_complex.xosc

This command will convert the modular.mosc file into a scenario file named result.xosc.

Documentation
---------------------------
For more details of if you run into problems, check our
[Documentation](http://link-to-documentation). <-- (insert link to documentation) 
