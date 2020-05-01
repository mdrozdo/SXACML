# SXACML
Semantic extensions to XACML attribute based access control

## Step by step instructions to run the SXACML web site.

1. Clone the SXACML repository. I later assume you clone to c:\sxacml
2. Start IntelliJ Idea. Make sure you have the Scala plugin installed.
3. In IntelliJ, click Open Project and select the folder: c:\sxacml\web\sxacml-admin
4. Select Run->Edit configurations
5. Press the + icon and select SBT. In the task definition set Name to `Run` and task to `run`.
6. Close the window with Ok
7. From the run toolbar or Run menu item, press _Run_.
8. Wait for everything to start up.
9. Point browser to localhost:9000
10. Open the Admin section to be able to upload ontology, create mappings and groups.
11. To issue a request to the PDP, send the XML content in the body of a POST request to: localhost:9000/authorization/pdp