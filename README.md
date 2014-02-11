BLEConnector (In Development)
============

Little library to simplify Bluetooth Low Energy connections using Android.

Bluetooth Low energy support in Android devices is a new feature added since 4.3 version. In my struggle to develop some applications I have faced some problems:

- Bluetooth Low Energy api provides write and read functions which can be called in a asynchronous way but Bluedroid stack does not seem to have a stack of bluetooth commands. I will use a Producer-Consumer solution to give the user an appearance of asynchrony.
- Due to this limitation, if you want to code a non trivial communication between some devices your code is going to be a mess very fast because you have to split your logic into various callbacks. This library is aimed to achieve a better code too.
- There are some troubles when you subscribe to remote notifications and you try to write to a device A while you are receiving notifications from device B. Depending on data length and speed of notifications this may be not a problem but I have been testing with a stress case and this can be happen. My intention is when you write to device A, disable receiveNotifications from other devices and enable them again when write operation is done. Not perfect I know.
- I am going to write a good description for developers about Bluetooth Low Energy for developers in my [blog](http://felhr85.net/) because I feel there is a lack of a good documentation for developers.
