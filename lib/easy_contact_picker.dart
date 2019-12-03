import 'dart:async';
import 'dart:typed_data';

import 'package:flutter/services.dart';
import 'package:collection/collection.dart';
import 'package:quiver/core.dart';

class EasyContactPicker {
  static const MethodChannel _channel =
      const MethodChannel('plugins.flutter.io/easy_contact_picker');


  /// 获取联系人详细信息
  Future<Iterable<ContactDetail>> getContacts({String query, bool withThumbnails = true}) async {
    Iterable contacts = await _channel.invokeMethod('getContacts', <String, dynamic> {
      'query': query,
      'withThumbnails': withThumbnails
    });
    return contacts.map((m) => ContactDetail.fromMap(m));
  }


  /// 获取通讯录列表
  ///
  /// return list[Contact]。
  Future<List<Contact>> selectContacts() async {
    final List result = await _channel.invokeMethod('selectContactList');
    if (result == null) {
      return null;
    }
    List<Contact> contacts = new List();
    result.forEach((f) {
      contacts.add(new Contact.fromMap(f));
    });
    return contacts;
  }

  /// 打开原生通讯录
  ///
  /// return [Contact]。
  Future<Contact> selectContactWithNative() async {
    final Map<dynamic, dynamic> result =
        await _channel.invokeMethod('selectContactNative');
    if (result == null) {
      return null;
    }
    return new Contact.fromMap(result);
  }
}

/// Represents a contact selected by the user.
class Contact {
  Contact({this.fullName, this.phoneNumber, this.firstLetter});

  factory Contact.fromMap(Map<dynamic, dynamic> map) => new Contact(
        fullName: map['fullName'] != null ? map['fullName'].toString().replaceAll(" ", "") : null,
        phoneNumber: map['phoneNumber'] != null ? map['phoneNumber'].toString().replaceAll(RegExp("[^0-9]"), "") : null,
        firstLetter: map['firstLetter'] != null ? map['firstLetter'].toString().replaceAll(" ", "") : null,
      );

  /// The full name of the contact, e.g. "Dr. Daniel Higgens Jr.".
  final String fullName;

  /// The phone number of the contact.
  final String phoneNumber;

  /// The firstLetter of the fullName.
  final String firstLetter;

  @override
  String toString() => '$fullName: $phoneNumber';
}



class ContactDetail {
  ContactDetail({
    this.givenName,
    this.middleName,
    this.prefix,
    this.suffix,
    this.familyName,
    this.company,
    this.jobTitle,
    this.emails,
    this.phones,
    this.postalAddresses,
    this.avatar,
    this.note
  });

  String identifier, displayName, givenName, middleName, prefix, suffix, familyName, company, jobTitle, note;
  Iterable<Item> emails = [];
  Iterable<Item> phones = [];
  Iterable<PostalAddress> postalAddresses = [];
  Uint8List avatar;

  String initials() {
    return ((this.givenName?.isNotEmpty == true
        ? this.givenName[0]
        : "") +
        (this.familyName?.isNotEmpty == true
            ? this.familyName[0]
            : "")).toUpperCase();
  }

  ContactDetail.fromMap(Map m) {
    identifier = m["identifier"];
    displayName = m["displayName"];
    givenName = m["givenName"];
    middleName = m["middleName"];
    familyName = m["familyName"];
    prefix = m["prefix"];
    suffix = m["suffix"];
    company = m["company"];
    jobTitle = m["jobTitle"];
    emails = (m["emails"] as Iterable)?.map((m) => Item.fromMap(m));
    phones = (m["phones"] as Iterable)?.map((m) => Item.fromMap(m));
    postalAddresses = (m["postalAddresses"] as Iterable)
        ?.map((m) => PostalAddress.fromMap(m));
    avatar = m["avatar"];
    note = m["note"];
  }

  static Map _toMap(ContactDetail contact) {
    var emails = [];
    for (Item email in contact.emails ?? []) {
      emails.add(Item._toMap(email));
    }
    var phones = [];
    for (Item phone in contact.phones ?? []) {
      phones.add(Item._toMap(phone));
    }
    var postalAddresses = [];
    for (PostalAddress address in contact.postalAddresses ?? []) {
      postalAddresses.add(PostalAddress._toMap(address));
    }
    return {
      "identifier": contact.identifier,
      "displayName": contact.displayName,
      "givenName": contact.givenName,
      "middleName": contact.middleName,
      "familyName": contact.familyName,
      "prefix": contact.prefix,
      "suffix": contact.suffix,
      "company": contact.company,
      "jobTitle": contact.jobTitle,
      "emails": emails,
      "phones": phones,
      "postalAddresses": postalAddresses,
      "avatar": contact.avatar,
      "note": contact.note
    };
  }

  Map toMap() {
    return ContactDetail._toMap(this);
  }

  /// The [+] operator fills in this contact's empty fields with the fields from [other]
  operator +(ContactDetail other) => ContactDetail(
      givenName: this.givenName ?? other.givenName,
      middleName: this.middleName ?? other.middleName,
      prefix: this.prefix ?? other.prefix,
      suffix: this.suffix ?? other.suffix,
      familyName: this.familyName ?? other.familyName,
      company: this.company ?? other.company,
      jobTitle: this.jobTitle ?? other.jobTitle,
      note: this.note ?? other.note,
      emails: this.emails == null
          ? other.emails
          : this.emails.toSet().union(other.emails?.toSet() ?? Set()).toList(),
      phones: this.phones == null
          ? other.phones
          : this.phones.toSet().union(other.phones?.toSet() ?? Set()).toList(),
      postalAddresses: this.postalAddresses == null
          ? other.postalAddresses
          : this
          .postalAddresses
          .toSet()
          .union(other.postalAddresses?.toSet() ?? Set())
          .toList(),
      avatar: this.avatar ?? other.avatar);

  /// Returns true if all items in this contact are identical.
  @override
  bool operator ==(Object other) {
    return other is ContactDetail &&
        this.avatar == other.avatar &&
        this.company == other.company &&
        this.displayName == other.displayName &&
        this.givenName == other.givenName &&
        this.familyName == other.familyName &&
        this.identifier == other.identifier &&
        this.jobTitle == other.jobTitle &&
        this.middleName == other.middleName &&
        this.note == other.note &&
        this.prefix == other.prefix &&
        this.suffix == other.suffix &&
        DeepCollectionEquality.unordered().equals(this.phones, other.phones) &&
        DeepCollectionEquality.unordered().equals(this.emails, other.emails) &&
        DeepCollectionEquality.unordered()
            .equals(this.postalAddresses, other.postalAddresses);
  }

  @override
  int get hashCode {
    return hashObjects([
      this.company,
      this.displayName,
      this.familyName,
      this.givenName,
      this.identifier,
      this.jobTitle,
      this.middleName,
      this.note,
      this.prefix,
      this.suffix
    ].where((s) => s != null));
  }
}

class PostalAddress {
  PostalAddress({
    this.label,
    this.street,
    this.city,
    this.postcode,
    this.region,
    this.country
  });
  String label, street, city, postcode, region, country;

  PostalAddress.fromMap(Map m) {
    label = m["label"];
    street = m["street"];
    city = m["city"];
    postcode = m["postcode"];
    region = m["region"];
    country = m["country"];
  }

  @override
  bool operator ==(Object other) {
    return other is PostalAddress &&
        this.city == other.city &&
        this.country == other.country &&
        this.label == other.label &&
        this.postcode == other.postcode &&
        this.region == other.region &&
        this.street == other.street;
  }

  @override
  int get hashCode {
    return hashObjects([
      this.label,
      this.street,
      this.city,
      this.country,
      this.region,
      this.postcode,
    ].where((s) => s != null));
  }

  static Map _toMap(PostalAddress address) => {
    "label": address.label,
    "street": address.street,
    "city": address.city,
    "postcode": address.postcode,
    "region": address.region,
    "country": address.country
  };
}

/// Item class used for contact fields which only have a [label] and
/// a [value], such as emails and phone numbers
class Item {
  Item({this.label, this.value});

  String label, value;

  Item.fromMap(Map m) {
    label = m["label"];
    value = m["value"];
  }

  @override
  bool operator ==(Object other) {
    return other is Item &&
        this.label == other.label &&
        this.value == other.value;
  }

  @override
  int get hashCode => hash2(label ?? "", value ?? "");

  static Map _toMap(Item i) => {"label": i.label, "value": i.value};
}
