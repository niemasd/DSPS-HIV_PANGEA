#!/usr/bin/env bash
EXE='DSPS-HIV'
rm -f $EXE
javac -cp ".:commons-math3-3.6.1.jar" ./individualBasedModel/DiscreteSpatialPhyloSimulator.java
echo '#!/usr/bin/env bash' > $EXE
echo 'JAVA=$(which java)' >> $EXE
echo 'PATH="$( cd "$(dirname "$0")" ; pwd -P )"' >> $EXE
echo 'cd $PATH' >> $EXE
echo '$JAVA -cp ".:commons-math3-3.6.1.jar" individualBasedModel/DiscreteSpatialPhyloSimulator' >> $EXE
echo 'cd $ORIG' >> $EXE
chmod a+x $EXE
