javac -classpath "libs/NetLogo.jar":"libs/commons-math3-3.3.jar":"libs/JRIEngine.jar":"libs/JRI.jar":"libs/REngine.jar":"libs/scala-library.jar":"libs/r.jar"  -d classes src/com/DETect/netlogo/*.java

jar cvfm detect.jar manifest.txt -C classes .

cp detect.jar /home/eamonn/netlogo-5.1.0/extensions/detect/detect.jar



