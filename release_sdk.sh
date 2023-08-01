./gradlew clean aR
rm -rf Output
mkdir Output
mkdir Output/imclient
mkdir Output/imkit   
cp mars-core-release/mars-core-release.aar Output/imclient 
cp client/build/outputs/aar/client-release.aar Output/imclient 
cp avenginekit/avenginekit.aar Output/imkit    
cp emojilibrary/build/outputs/aar/emojilibrary-release.aar Output/imkit 
cp imagepicker/build/outputs/aar/imagepicker-release.aar Output/imkit 
cp uikit/build/outputs/aar/uikit-release.aar Output/imkit 
