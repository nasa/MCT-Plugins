MCT-Plugins
===========

Each plug-in may be built in one of two ways. If building MCT from source, these may be included alongside other projects and packaged by making appropriate changes to pom.xml within an assembly. As a convenience, ant scripts are included to build as stand-alone plugins. An existing platform build (such as the evaluation version) is required and may be specified as a property "mct.dir", i.e.:

ant -Dmct.dir=/Applications/MCT


Ancestor View:

A plug-in for viewing a graph of referencing components within MCT. Select "Ancestor View" to see a graph indicating which components (such as collections) refer to this component, with more information further up the tree.


**Chronology:**

A set of interfaces used for communicating time-stamped information between plug-ins. 


**Notebook:**

A plug-in for making and maintaining notes within MCT. Notes may be annotated with other objects, such as telemetry elements, by dragging and dropping them into the note's text field. Notes are also time-stamped, so they can be viewed in time-enabled views (such as timelines). Depends upon Chronology.


**Timeline:**

A plug-in for viewing time-stamped information (notes, events) in a graphical timeline. The "Timeline" view shows event sequences, such as notebook entries, horizontally in relation to their occurrence in time. These events may be reorganized using drag and drop if the event sequence permits changes. Depends upon Chronology.


**Earth View:**

A plug-in for viewing state vectors relative to the Earth. To view, create an "Orbit" object from the Create menu. You may set initial vectors (units are km and km/s respectively, and position is relative to Earth's center; orbits are approximated at an accelerated rate and are not physically accurate). The resulting collection of state vectors can be viewed as spatial coordinates using the "Orbit" view.

Contains a true-color image of the Earth, owned by NASA, from the Visible Earth catalog. 

http://visibleearth.nasa.gov/view.php?id=73909

R. Stockli, E. Vermote, N. Saleous, R. Simmon and D. Herring (2005). The Blue Marble Next Generation - A true color earth dataset including seasonal dynamics from MODIS. Published by the NASA Earth Observatory. Corresponding author: rstockli@climate.gsfc.nasa.gov


**Quickstart Persistence:**

Provides a simple in-memory persistence service populated with a small number of components and displays. To use, the compiled jar should be placed in the resources/platform of an MCT installation, in lieu of databasePersistence-1.1.0.jar. Note that the example plugin may need to be moved from resources/plugins to resources/platform as well, as this quickstart persistence service is pre-populated with example telemetry components.


**SatelliteTracker:**

This plug-in allows users to create satellites in MCT and track their orbits, in real-time, through various views:
 <UL>
   <LI> Satellite Orbits in 3D via the Earth View plug-in.
   <LI> Real-time locations on a 2D Mercator Projection (A new to MCT; created within this plug-in).
   <LI> All of the views that come standard with the core-MCT distribution (i.e.: MultiColumn View, Plots over time, Alpha, etc.).
 </UL> 

Along with adding an interesting data-source to MCT, the true purpose of this plug-in is that the SatelliteTracker serves as a concrete example on how to write a plug-in for MCT.  As SatelliteTracker is a flagship example for plug-in development, comments have been added throughout the source-files to guide the developer on the design-style and requirements put-forth by MCT's structure (and similarly, a section on the Wiki concerning developing-with MCT references the source contained within SatelliteTracker).

Similar to EarthView, this plug-in contains two true-color image of the Earth (one with and one without snow), owned by NASA, from the Visible Earth catalog.

http://visibleearth.nasa.gov/view.php?id=73909

R. Stockli, E. Vermote, N. Saleous, R. Simmon and D. Herring (2005). The Blue Marble Next Generation - A true color earth dataset including seasonal dynamics from MODIS. Published by the NASA Earth Observatory. Corresponding author: rstockli@climate.gsfc.nasa.gov
