Modular2Simple
========================
Modular2Simple is a tool designed to enhance scenario creation for the CARLA simulator by leveraging
existing simple scenarios in the OpenSCENARIO format (.xosc). This tool allows to package a main scenario file
along with the simple scenario files it references into a .mosc file (Modular Open Scenario). It not only
works with simple scenarios (.xosc) but also has the capability to generate modular scenarios from existing modular
scenarios, allowing for generating even more complex modular scenarios. Once the .mosc file is ready, Modular2Simple 
allows to convert the resulting modular scenario (.mosc) file into standard OpenSCENARIO (.xosc) file (that contains
all the necessary data from the simple scenario files), which makes it fully compatible with the ScenarioRunner module
of the CARLA simulator.

Getting the Modular2Simple
---------------------------
To install Modular2Simple, follow these steps:

Download the Modular2Simple
file: [Modular2Simple](https://github.com/NikolaiKhriapov/modular2simple/blob/main/src/Modular2Simple.java)

Navigate to the downloaded file directory.

Compile the program
* javac Modular2Simple.java

Using the Modular2Simple
---------------------------

#### To package a set of .xosc files into a .mosc file, use the following command:

* java Modular2Simple --convert-xosc-to-mosc <input_xosc_(mosc)_files> <output_mosc_file>

Replace <input_xosc_(mosc)_files> with the path to input .xosc (.mosc) files (the main scenario file must be named 
'main.xosc'), and <output_mosc_file> with the desired path for the output modular scenario file.

Example
* java Modular2Simple --convert-xosc-to-mosc main.xosc simple_1.xosc simple_2.xosc modular.mosc

This command will package the 'main.xosc', 'simple_1.xosc' and 'simple_2.xosc' files into the 'modular.mosc' file.

#### To convert a .mosc file to a .xosc scenario file, use the following command:

* java Modular2Simple --convert-mosc-to-xosc <input_mosc_file> <output_xosc_file>

Replace <input_mosc_file> with the path to the input modular package file, and <output_xosc_file> with the desired path
for the output complex scenario file.

Example
* java Modular2Simple --convert-mosc-to-xosc modular.mosc resulting_complex.xosc

This command will convert the 'modular.mosc' file into a scenario file named 'resulting_complex.xosc'.

Documentation
---------------------------
For more details of if you run into problems, check our
[Documentation](http://link-to-documentation). <-- (insert link to documentation) 
