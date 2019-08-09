This folder contains everything needed to build bullet3
from its own CMakeLists.txt files.

BULLET_SOURCE = path to your bullet sources

1. Copy the bullet3 directory to BULLET_SOURCE/build3/Android.
   This should result in 2 directories under build3:
   bullet3 and jni. (The Android.mk files in the jni directory
   only build bullet2).
   
2. Copy bullet3/Extras/CMakeLists.txt into the BULLET_SOURCE/Extras directory
   in your bullet source hierarchy. It removes BulletRobotics
   from the library list. This feature relies on shared memory
   calls (like shmget) which are not available on Android. 
   
3. Open BULLET_SOURCE/build3/Android/bullet3 in Android Studio.
   Build the project for any debug variant and it will build all the debug ABIs
   Build the project for any release variant and it will build all the release ABIs
   The resulting AARs are in BULLET_SOURCE/build3/Android/bullet3/build/output/aar
   They are bullet3-debug.aar and bullet3-release.aar
   
4. Copy the AARs into sxr-physics/src/main/aar.
   The sxr-physics extension no longer uses libBullet.so.
   It extracts the bullet libraries from the AAR and includes
   them in the sxr-physics extension. SXRWorld loads them.
  