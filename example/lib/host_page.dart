import 'package:easy_contact_picker/easy_contact_picker.dart';
import 'package:flutter/material.dart';
import 'package:permission_handler/permission_handler.dart';

class HostPage extends StatefulWidget {
  @override
  _HostPageState createState() => _HostPageState();
}

class _HostPageState extends State<HostPage>
    with AutomaticKeepAliveClientMixin {
  Contact _contact = new Contact(fullName: "", phoneNumber: "");
  final EasyContactPicker _contactPicker = new EasyContactPicker();

  _openAddressBook() async {
    // 申请权限
    Map<PermissionGroup, PermissionStatus> permissions =
        await PermissionHandler()
            .requestPermissions([PermissionGroup.contacts]);

    // 申请结果
    PermissionStatus permission = await PermissionHandler()
        .checkPermissionStatus(PermissionGroup.contacts);

    if (permission == PermissionStatus.granted) {
      _getContactData();
    }
  }

  _getContactData() async {
    Contact contact = await _contactPicker.selectContactWithNative();
    setState(() {
      _contact = contact;
    });
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: Text("首页"),
        actions: <Widget>[
          FlatButton(
            child: Text("获取详细联系人"),
            onPressed: () async {

              print("11111111111");

              // 申请权限
              Map<PermissionGroup, PermissionStatus> permissions =
              await PermissionHandler()
                  .requestPermissions([PermissionGroup.contacts]);
              // 申请结果
              PermissionStatus permission = await PermissionHandler()
                  .checkPermissionStatus(PermissionGroup.contacts);
              print("permission = $permission");

              if (permission == PermissionStatus.granted) {
                var _contacts = await _contactPicker.getContacts();

                if (_contacts != null) {
                  List<Map<String, Object>> contactsList =
                      <Map<String, Object>>[];

                  for (var c in _contacts) {
                    Map<String, Object> contactMap = <String, Object>{};

                    contactMap["name"] = c.displayName;
                    contactMap["contactId"] = c.identifier;

                    List<Map<String, Object>> itemList =
                        <Map<String, Object>>[];

                    for (Item phone in c.phones) {
                      Map<String, Object> phoneMap = <String, Object>{};
                      phoneMap["type"] = "PHONE";
                      phoneMap["val"] = phone.value;
                      itemList.add(phoneMap);
                    }

                    for (Item email in c.emails) {
                      Map<String, Object> emailMap = <String, Object>{};
                      emailMap["type"] = "MAIL";
                      emailMap["val"] = email.value;
                      itemList.add(emailMap);
                    }
                    contactMap["itemList"] = itemList;

                    contactsList.add(contactMap);
                  }

                  print("1111111----------------------------");
                  print(contactsList);
                  print("2222222----------------------------");
                }
              }
            },
          )
        ],
      ),
      body: Column(
        children: <Widget>[
          Padding(
            padding: EdgeInsets.fromLTRB(13, 20, 13, 10),
            child: Row(
              children: <Widget>[Text("姓名："), Text(_contact.fullName)],
            ),
          ),
          Padding(
            padding: EdgeInsets.fromLTRB(13, 0, 13, 20),
            child: Row(
              children: <Widget>[Text("手机号："), Text(_contact.phoneNumber)],
            ),
          ),
          // FlatButton(
          //   child: Text("打开通讯录"),
          //   onPressed: _openAddressBook,
          // )
        ],
      ),
    );
  }

  @override
  // TODO: implement wantKeepAlive
  bool get wantKeepAlive => true;
}
