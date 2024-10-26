Modular2Simple
========================
Modular2Simple is a a tool designed to simplify the development of complex ADS scenarios. Modular2Simple seamlessly
integrates with the CARLA simulator and is applicable to any software supporting the OpenSCENARIO format. By leveraging
existing simple scenarios in the OpenSCENARIO format, Modular2Simple empowers developers to create modular scenarios
through the combination of multiple simple or modular scenarios. This approach not only facilitates the development of
complex scenarios but also encourages scenario reuse.

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

* java Modular2Simple --convert-xosc-into-mosc <input_xosc_(mosc)_files> <output_mosc_file>

Replace <input_xosc_(mosc)_files> with the path to input .xosc (.mosc) files (the main scenario file must be named
'main.xosc'), and <output_mosc_file> with the desired path for the output modular scenario file.

Example

* java Modular2Simple --convert-xosc-into-mosc main.xosc simple_1.xosc simple_2.xosc modular.mosc

This command will package the 'main.xosc', 'simple_1.xosc' and 'simple_2.xosc' files into the 'modular.mosc' file.

#### To convert a .mosc file to a .xosc scenario file, use the following command:

* java Modular2Simple --convert-mosc-into-xosc <input_mosc_file> <output_xosc_file>

Replace <input_mosc_file> with the path to the input modular package file, and <output_xosc_file> with the desired path
for the output complex scenario file.

Example

* java Modular2Simple --convert-mosc-into-xosc modular.mosc resulting_complex.xosc

This command will convert the 'modular.mosc' file into a scenario file named 'resulting_complex.xosc'.

Environment Variable
---------------------------

If you want to use scenarios from different folders, it is necessary to set an environment variable so that
Modular2Simple can find the library folders. This allows users to choose different locations of scenario files, if they
are not found within the .mosc archive. If no environment variable is specified, then Modular2Simple will search for
scenarios only within the .mosc archive.

To set the environment variable:

1. Open Windows Control Panel and go to 'Advanced System Settings' or search for 'Advanced System Settings' in the
   Windows search bar.
2. On the 'Advanced' panel open 'Environment Variables...'.
3. Click 'New...' to create the variable.
4. Name the variable 'SCENARIO_PATH' and choose the path to the desired folder with scenarios.

Documentation
---------------------------
For more details or if you run into problems, check our
[Documentation](https://github.com/NikolaiKhriapov/modular2simple/tree/main/Docs) 
