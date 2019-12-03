package com.liuwei.easy_contact_picker;

import android.annotation.TargetApi;
import android.app.Activity;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.provider.ContactsContract;
import android.text.TextUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;

import io.flutter.plugin.common.MethodCall;
import io.flutter.plugin.common.MethodChannel;
import io.flutter.plugin.common.MethodChannel.MethodCallHandler;
import io.flutter.plugin.common.MethodChannel.Result;
import io.flutter.plugin.common.PluginRegistry;
import io.flutter.plugin.common.PluginRegistry.Registrar;

/** EasyContactPickerPlugin */
public class EasyContactPickerPlugin implements MethodCallHandler, PluginRegistry.ActivityResultListener {

  private static final String CHANNEL = "plugins.flutter.io/easy_contact_picker";
  // 跳转原生选择联系人页面
  static final String METHOD_CALL_NATIVE = "selectContactNative";
  // 获取联系人列表
  static final String METHOD_CALL_LIST = "selectContactList";
  private Activity mActivity;
  private ContactsCallBack contactsCallBack;


  private final ContentResolver contentResolver;

  // 加个构造函数，入参是Activity
  private EasyContactPickerPlugin(Activity activity, ContentResolver contentResolver) {
    // 存起来
    mActivity = activity;
    this.contentResolver = contentResolver;
  }

  /** Plugin registration. */
  public static void registerWith(Registrar registrar) {
    //传入Activity
    final EasyContactPickerPlugin plugin = new EasyContactPickerPlugin(registrar.activity(), registrar.context().getContentResolver());
    final MethodChannel channel = new MethodChannel(registrar.messenger(), CHANNEL);
    channel.setMethodCallHandler(plugin);
    //添加跳转页面回调
    registrar.addActivityResultListener(plugin);
  }

  @Override
  public void onMethodCall(MethodCall call, final Result result) {
    if (call.method.equals(METHOD_CALL_NATIVE)){
      contactsCallBack = new ContactsCallBack() {
        @Override
        void successWithMap(HashMap<String, String> map) {
          super.successWithMap(map);
          result.success(map);
        }

        @Override
        void error() {
          super.error();
        }
      };
      intentToContact();
    }
    else if (call.method.equals(METHOD_CALL_LIST)){
      contactsCallBack = new ContactsCallBack() {
        @Override
        void successWithList(List<HashMap> contacts) {
          super.successWithList(contacts);
          result.success(contacts);
        }

        @Override
        void error() {
          super.error();
        }
      };
      getContacts();
    }

    else if (call.method.equals("getContacts")){
      this.getContacts((String)call.argument("query"), (boolean)call.argument("withThumbnails"), result);
    }

  }

  /** 跳转到联系人界面. */
  private void intentToContact() {
    Intent intent = new Intent();
    intent.setAction("android.intent.action.PICK");
    intent.addCategory("android.intent.category.DEFAULT");
    intent.setType("vnd.android.cursor.dir/phone_v2");
    mActivity.startActivityForResult(intent, 0x30);
  }

  private void getContacts(){

    //（实际上就是“sort_key”字段） 出来是首字母
    final String PHONE_BOOK_LABEL = "phonebook_label";
    //需要查询的字段
    final String[] CONTACTOR_ION = new String[]{
            ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
            ContactsContract.CommonDataKinds.Phone.NUMBER,
            PHONE_BOOK_LABEL
    };

    List contacts = new ArrayList<>();
    Uri uri = null;
    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.JELLY_BEAN_MR2) {
      uri = ContactsContract.CommonDataKinds.Contactables.CONTENT_URI;
    }
    //获取联系人。按首字母排序
    Cursor cursor = mActivity.getContentResolver().query(uri, CONTACTOR_ION,null,null, ContactsContract.CommonDataKinds.Phone.SORT_KEY_PRIMARY);
    if (cursor != null) {

      while (cursor.moveToNext()) {
        HashMap<String, String> map =  new HashMap<String, String>();
        String name = cursor.getString(cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME));
        String phoneNum = cursor.getString(cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER));
        String firstChar = cursor.getString(cursor.getColumnIndex(PHONE_BOOK_LABEL));
        map.put("fullName", name);
        map.put("phoneNumber", phoneNum);
        map.put("firstLetter", firstChar);

        contacts.add(map);
      }
      cursor.close();
      contactsCallBack.successWithList(contacts);
    }

  }

  @Override
  public boolean onActivityResult(int requestCode, int resultCode, Intent data) {
    if(requestCode==0x30) {
      if (data != null) {
        Uri uri = data.getData();
        String phoneNum = null;
        String contactName = null;
        // 创建内容解析者
        ContentResolver contentResolver = mActivity.getContentResolver();
        Cursor cursor = null;
        if (uri != null) {
          cursor = contentResolver.query(uri,
                  new String[]{"display_name","data1"},null,null,null);
        }
        while (cursor.moveToNext()) {
          contactName = cursor.getString(cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME));
          phoneNum = cursor.getString(cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER));
        }
        cursor.close();
        //  把电话号码中的  -  符号 替换成空格
        if (phoneNum != null) {
          phoneNum = phoneNum.replaceAll("-", " ");
          // 空格去掉  为什么不直接-替换成"" 因为测试的时候发现还是会有空格 只能这么处理
          phoneNum= phoneNum.replaceAll(" ", "");
        }
        HashMap<String, String> map =  new HashMap<String, String>();
        map.put("fullName", contactName);
        map.put("phoneNumber", phoneNum);
        contactsCallBack.successWithMap(map);
      }
    }
    return false;
  }

  /** 获取通讯录回调. */
  public abstract class ContactsCallBack{
    void successWithList(List<HashMap> contacts){};
    void successWithMap(HashMap<String, String> map){};
    void error(){};
  }



  private static final String[] PROJECTION =
          {
                  ContactsContract.Data.CONTACT_ID,
                  ContactsContract.Profile.DISPLAY_NAME,
                  ContactsContract.Contacts.Data.MIMETYPE,
                  ContactsContract.CommonDataKinds.StructuredName.DISPLAY_NAME,
                  ContactsContract.CommonDataKinds.StructuredName.GIVEN_NAME,
                  ContactsContract.CommonDataKinds.StructuredName.MIDDLE_NAME,
                  ContactsContract.CommonDataKinds.StructuredName.FAMILY_NAME,
                  ContactsContract.CommonDataKinds.StructuredName.PREFIX,
                  ContactsContract.CommonDataKinds.StructuredName.SUFFIX,
                  ContactsContract.CommonDataKinds.Note.NOTE,
                  ContactsContract.CommonDataKinds.Phone.NUMBER,
                  ContactsContract.CommonDataKinds.Phone.TYPE,
                  ContactsContract.CommonDataKinds.Phone.LABEL,
                  ContactsContract.CommonDataKinds.Email.DATA,
                  ContactsContract.CommonDataKinds.Email.ADDRESS,
                  ContactsContract.CommonDataKinds.Email.TYPE,
                  ContactsContract.CommonDataKinds.Email.LABEL,
                  ContactsContract.CommonDataKinds.Organization.COMPANY,
                  ContactsContract.CommonDataKinds.Organization.TITLE,
                  ContactsContract.CommonDataKinds.StructuredPostal.FORMATTED_ADDRESS,
                  ContactsContract.CommonDataKinds.StructuredPostal.TYPE,
                  ContactsContract.CommonDataKinds.StructuredPostal.LABEL,
                  ContactsContract.CommonDataKinds.StructuredPostal.STREET,
                  ContactsContract.CommonDataKinds.StructuredPostal.POBOX,
                  ContactsContract.CommonDataKinds.StructuredPostal.NEIGHBORHOOD,
                  ContactsContract.CommonDataKinds.StructuredPostal.CITY,
                  ContactsContract.CommonDataKinds.StructuredPostal.REGION,
                  ContactsContract.CommonDataKinds.StructuredPostal.POSTCODE,
                  ContactsContract.CommonDataKinds.StructuredPostal.COUNTRY,
          };


  @TargetApi(Build.VERSION_CODES.ECLAIR)
  private void getContacts(String query, boolean withThumbnails, Result result) {
    new GetContactsTask(result, withThumbnails).execute(new String[] {query});
  }

  @TargetApi(Build.VERSION_CODES.CUPCAKE)
  private class GetContactsTask extends AsyncTask<String, Void, ArrayList<HashMap>> {

    private Result getContactResult;
    private boolean withThumbnails;

    public GetContactsTask(Result result, boolean withThumbnails){
      this.getContactResult = result;
      this.withThumbnails = withThumbnails;
    }

    @TargetApi(Build.VERSION_CODES.ECLAIR)
    protected ArrayList<HashMap> doInBackground(String... query) {
      ArrayList<Contact> contacts = getContactsFrom(getCursor(query[0]));
      if (withThumbnails) {
        for(Contact c : contacts){
          setAvatarDataForContactIfAvailable(c);
        }
      }
      //Transform the list of contacts to a list of Map
      ArrayList<HashMap> contactMaps = new ArrayList<>();
      for(Contact c : contacts){
        contactMaps.add(c.toMap());
      }

      return contactMaps;
    }

    protected void onPostExecute(ArrayList<HashMap> result) {
      getContactResult.success(result);
    }
  }

  private Cursor getCursor(String query){
    String selection = ContactsContract.Data.MIMETYPE + "=? OR " + ContactsContract.Data.MIMETYPE + "=? OR " + ContactsContract.Data.MIMETYPE + "=? OR " + ContactsContract.Data.MIMETYPE + "=? OR " + ContactsContract.Data.MIMETYPE + "=? OR " + ContactsContract.Data.MIMETYPE + "=?";
    String[] selectionArgs = new String[]{
            ContactsContract.CommonDataKinds.Note.CONTENT_ITEM_TYPE,
            ContactsContract.CommonDataKinds.Email.CONTENT_ITEM_TYPE,
            ContactsContract.CommonDataKinds.Phone.CONTENT_ITEM_TYPE,
            ContactsContract.CommonDataKinds.StructuredName.CONTENT_ITEM_TYPE,
            ContactsContract.CommonDataKinds.Organization.CONTENT_ITEM_TYPE,
            ContactsContract.CommonDataKinds.StructuredPostal.CONTENT_ITEM_TYPE,
    };
    if(query != null){
      selectionArgs = new String[]{"%" + query + "%"};
      selection = ContactsContract.Contacts.DISPLAY_NAME_PRIMARY + " LIKE ?";
    }
    return contentResolver.query(ContactsContract.Data.CONTENT_URI, PROJECTION, selection, selectionArgs, null);
  }

  /**
   * Builds the list of contacts from the cursor
   * @param cursor
   * @return the list of contacts
   */
  private ArrayList<Contact> getContactsFrom(Cursor cursor) {
    HashMap<String, Contact> map = new LinkedHashMap<>();

    while (cursor != null && cursor.moveToNext()) {
      int columnIndex = cursor.getColumnIndex(ContactsContract.Data.CONTACT_ID);
      String contactId = cursor.getString(columnIndex);

      if (!map.containsKey(contactId)) {
        map.put(contactId, new Contact(contactId));
      }
      Contact contact = map.get(contactId);

      String mimeType = cursor.getString(cursor.getColumnIndex(ContactsContract.Data.MIMETYPE));
      contact.displayName = cursor.getString(cursor.getColumnIndex(ContactsContract.Contacts.DISPLAY_NAME));

      //NAMES
      if (mimeType.equals(ContactsContract.CommonDataKinds.StructuredName.CONTENT_ITEM_TYPE)) {
        contact.givenName = cursor.getString(cursor.getColumnIndex(ContactsContract.CommonDataKinds.StructuredName.GIVEN_NAME));
        contact.middleName = cursor.getString(cursor.getColumnIndex(ContactsContract.CommonDataKinds.StructuredName.MIDDLE_NAME));
        contact.familyName = cursor.getString(cursor.getColumnIndex(ContactsContract.CommonDataKinds.StructuredName.FAMILY_NAME));
        contact.prefix = cursor.getString(cursor.getColumnIndex(ContactsContract.CommonDataKinds.StructuredName.PREFIX));
        contact.suffix = cursor.getString(cursor.getColumnIndex(ContactsContract.CommonDataKinds.StructuredName.SUFFIX));
      }
      // NOTE
      else if (mimeType.equals(ContactsContract.CommonDataKinds.Note.CONTENT_ITEM_TYPE)) {
        contact.note = cursor.getString(cursor.getColumnIndex(ContactsContract.CommonDataKinds.Note.NOTE));
      }
      //PHONES
      else if (mimeType.equals(ContactsContract.CommonDataKinds.Phone.CONTENT_ITEM_TYPE)){
        String phoneNumber = cursor.getString(cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER));
        int type = cursor.getInt(cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.TYPE));
        if (!TextUtils.isEmpty(phoneNumber)){
          contact.phones.add(new Item(Item.getPhoneLabel(type),phoneNumber));
        }
      }
      //MAILS
      else if (mimeType.equals(ContactsContract.CommonDataKinds.Email.CONTENT_ITEM_TYPE)) {
        String email = cursor.getString(cursor.getColumnIndex(ContactsContract.CommonDataKinds.Email.ADDRESS));
        int type = cursor.getInt(cursor.getColumnIndex(ContactsContract.CommonDataKinds.Email.TYPE));
        if (!TextUtils.isEmpty(email)) {
          contact.emails.add(new Item(Item.getEmailLabel(type, cursor),email));
        }
      }
      //ORG
      else if (mimeType.equals(ContactsContract.CommonDataKinds.Organization.CONTENT_ITEM_TYPE)) {
        contact.company = cursor.getString(cursor.getColumnIndex(ContactsContract.CommonDataKinds.Organization.COMPANY));
        contact.jobTitle = cursor.getString(cursor.getColumnIndex(ContactsContract.CommonDataKinds.Organization.TITLE));
      }
      //ADDRESSES
      else if (mimeType.equals(ContactsContract.CommonDataKinds.StructuredPostal.CONTENT_ITEM_TYPE)) {
        contact.postalAddresses.add(new PostalAddress(cursor));
      }
    }
    return new ArrayList<>(map.values());
  }

  private void setAvatarDataForContactIfAvailable(Contact contact) {
    Uri contactUri = ContentUris.withAppendedId(ContactsContract.Contacts.CONTENT_URI, Integer.parseInt(contact.identifier));
    Uri photoUri = Uri.withAppendedPath(contactUri, ContactsContract.Contacts.Photo.CONTENT_DIRECTORY);
    Cursor avatarCursor = contentResolver.query(photoUri,
            new String[] {ContactsContract.Contacts.Photo.PHOTO}, null, null, null);
    if (avatarCursor != null && avatarCursor.moveToFirst()) {
      byte[] avatar = avatarCursor.getBlob(0);
      contact.avatar = avatar;
    }
    if (avatarCursor != null) {
      avatarCursor.close();
    }
  }

}
