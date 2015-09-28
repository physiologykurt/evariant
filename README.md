Evariant Coding Exercise
========================

# Solution Explanation #

I implemented the solution in three languages: R, Python, and Java. The results are the same except for basic formatting differences.

This type of data processing exercise is a natural fit for R or Python+Pandas, so I did it first using those tools. Since the exercise explicitly asked for a Java solution, I ported my solution to Java.

# R Solution Instructions #

First install R, then within R, install the necessary dependencies:

```r
install.packages("stringr")
install.packages("doBy")
```

Then, exit, and from your shell, run the script itself:

```
R CMD BATCH code.r
```

# Python+Pandas Solution Instructions #

First, setup a Python virtualenv where you can safely install pandas without affecting the rest of your system.


```
virtualenv env
. env/bin/activate
pip install pandas
```

Then run the script:

```
python code.py
```

# Java Solution Instructions #

Make sure you have a JDK 8+ installed and a recent version of Gradle. The Java solution has a single third party dependency on the opencsv library. Gradle will automatically install this from the Internet.

To run from your shell, navigate to the `msawetness-java` directory and run:

```
gradle run
```

# Main Caveat: City/State Joining #

The biggest caveat in this is the join between city/state of a wban unit from the station text file to the city/state population data.

For example, in the station text file, there are five wban sensors listed as being in "ATLANTA|GA". I can manually find the corresponding entry in the population spreadsheet which is listed as "Atlanta-Sandy Springs-Roswell, GA" with a population of 5,286,728.

The strings do not directly match. This is a common ETL issue. The most common solution is to lump all three cities into one group. We don't have population data to separate Sandy Springs from Atlanta so there isn't much choice. For the scope of this artifical coding exercise, I'm not going to do extra data cleaning and will limit to simple exact matches.

# Secondary Caveat: Error Handling #

The code is written as a one off exercise and does not handle errors as you would do in a production system. For example, if the input data files are changed and new unexpected or incorrect data formatting is used, the code will break and not necessarily terribly gracefully. Such error handling is typically outisde the bounds of a simple coding exercise like this.
