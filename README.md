# easy_contact_picker

A Flutter contact picker.Writted with pure dart, supported both iOS and Android.<br>
Get address book permissions before using.

## Use this package
### Depend on it
```
    easy_contact_picker: ^0.0.2
```
### Import it
```
import 'package:easy_contact_picker/easy_contact_picker.dart';
```
### Add permission
#### Android
```
<uses-permission android:name="android.permission.READ_CONTACTS"/>
```
#### iOS<br>
##### info.plist
```
Privacy - Contacts Usage Description
```
##### Tick the right box in the Background Modes Background fetch And Remote notification.
### Sample<br>
#### 1.Open the Native address book.
```
Future<List<Contact>> selectContacts() async {
    final List result =
    await _channel.invokeMethod('selectContactList');
    if (result == null) {
      return null;
    }
    List<Contact> contacts = new List();
    result.forEach((f){
      contacts.add(new Contact.fromMap(f));
    });
    return contacts;
  }
```
##### 2.Get contact list.
```
Future<Contact> selectContactWithNative() async {
    final Map<dynamic, dynamic> result =
    await _channel.invokeMethod('selectContactNative');
    if (result == null) {
      return null;
    }
    return new Contact.fromMap(result);
  }
```