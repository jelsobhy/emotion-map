# emotion-map

## Motivation

The aim of the EmoMap-App is to show emotions of people on a Google Map, so that a person who is searching for pretty places can have quick information about a special place. The easiest way to realize that is to vizualize the emotions as smileys.

## Fundamentals

### Android
The EmoMap-App is developed with Android. An amazing guide for developers is available at http://developer.android.com/. The app is developed with Android 4.0.3 with the Eclipse framework. You need to install the ADT plugin and download the Android and Google SDK from the Android SDK manager.

### Augmented Reality
Another goal in this project was to vizualize the smileys in an augmented reality envi- ronment. The main class for this purpose is the AugmentedRealityActivity class.

### Google Maps
We used Google Maps V2 2 for visualizing the cart in which the smileys are created in. You have to create an API-key and put it in the google console by creating a personal fingerprint. This API-key has to be set in the Android Manifest file. To spare you this work you can use a shared debug keystore, that I have set in the project, so that multiple users can develope the app. Go to eclipse through Preferences ? Android ? Build 2.1.

### Libraries
I have used two libraries: the android-map-extensions library 3 and the Actionbarsherlock library 4. The map extension supports clustering of markers and the actionbarsherlock supports action bar for all android levels. When you import the two libraries you have to do that through Android preferences 2.2.

NOTE: You have to check whether the checkboxes in the order and export category are en- abled like in the figure 2.3. You also have to delete the android-support-library in the Emotion Broadcasting project from the libs folder and use the support library from android-maps-extensions library.

## Implementation

The implementation is seperated into android and server application.

### Android App
The main class for the google map is the MapViewActivity. The lifecycle of the google map, the map extensions library and the marker management are included. The android smartphone has to be connected to google play services. Otherwise the google map is not displayed. You have to be connect to the internet and enable GPS. The first screen displays the smileys in a circle. The user can click on a smiley that expresses his emotion and the smiley is set on the map.

### Settings
On the first screen the user can find an android menu. You can choose between displaying the map without putting a smiley on the map, augmented reality or settings view. Entering the settings view you can handle some preferences like enabling clustering, delete all personal smileys, map type, etc.

### Filter
You also have the ability to filter smileys on the map by choosing the smileys on the view.

### Share
The user can share his smileys, that he has set on the map.

### Controller
There are two controller classes: LocationService and a Servermanager. The LocationService handles the current position of the phone and where the smiley has to be set.

### Database
There are two databases: The server database and the internal phone database. The phone database is used to visualize the personal smileys of the user. The server stores all smileys that were setted on the map. The classes that handle the database queries and operations are DatabaseOpenHelper, DBManager and Queries.

### Server
All smileys are stored in a database on the EmoMap-server.
Host: emo.qu.tu-berlin.de/Emo_Map


## Summary and Further Work
Currently the app has the basic functionality of displaying different features in the map and the augmented reality. The user can have overview of the map, share his emotions with other applications, filter his smileys with different features. If you want to develop the app to a signed application that can be released in the Google Play store, you need to extend the basic functionalities with better appearance and design of the app. Some of the features are not intuitive like displaying personal and global smileys. I think the first screen has to be changed in a way that the user understands how to interact with the application. Furthermore the clustering functionality can be improved. One way to achieve this is to source the clustering computation to the server out.
 





