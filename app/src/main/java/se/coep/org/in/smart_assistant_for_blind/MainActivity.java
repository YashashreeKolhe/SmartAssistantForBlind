package se.coep.org.in.smart_assistant_for_blind;

import android.Manifest;
import android.app.Activity;
import android.app.NotificationManager;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.provider.ContactsContract;
import android.provider.Settings;
import android.service.notification.NotificationListenerService;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.speech.tts.TextToSpeech;
import android.speech.tts.UtteranceProgressListener;
import android.support.annotation.NonNull;
import android.support.annotation.RequiresApi;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.telephony.TelephonyManager;
import android.telephony.PhoneStateListener;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Properties;

import javax.mail.BodyPart;
import javax.mail.Flags;
import javax.mail.Folder;
import javax.mail.Message;
import javax.mail.Multipart;
import javax.mail.Session;
import javax.mail.Store;
import javax.mail.search.FlagTerm;

public class MainActivity extends AppCompatActivity {

    private Intent mSpeechRecognizerIntent;
    private SpeechRecognizer mSpeechRecognizer;
    private TextToSpeech tts;
    private Intent callIntent;
    String aNameFromContacts[];
    String aNumberFromContacts[];
    String anEmailFromContacts[];
    boolean callFlag = false;
    boolean mailFlag = false;
    private boolean isPhoneCalling = false;
    private boolean mismatchFlag = false;
    private boolean messageFlag = true;
    private boolean mailNotificationFlag = true;
    private boolean isThereNotification = false;
    private boolean newNotificationsFlag = false;

    ArrayList<String> mails = new ArrayList<>();
    ArrayList<String> notificationsList = new ArrayList<>();

    private final int OBJECT_DETECTION_ACTIVITY = 1;
    private final int MESSAGE_HANDLING_ACTIVITY = 2;

    int PERMISSION_ALL = 1;
    String[] PERMISSIONS = {
            Manifest.permission.READ_CONTACTS,
            //Manifest.permission.CALL_PHONE,
            Manifest.permission.SEND_SMS,
            Manifest.permission.READ_PHONE_STATE,
            Manifest.permission.RECORD_AUDIO,
            Manifest.permission.INTERNET
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        ComponentName cn = new ComponentName(this, NotificationService.class);
        String flat = Settings.Secure.getString(this.getContentResolver(), "enabled_notification_listeners");
        final boolean enabled = flat != null && flat.contains(cn.flattenToString());

        if (!enabled) {
            startActivity(new Intent("android.settings.ACTION_NOTIFICATION_LISTENER_SETTINGS"));
        }
        if(!hasPermissions(this, PERMISSIONS)){
            ActivityCompat.requestPermissions(this, PERMISSIONS, PERMISSION_ALL);
        } else {
            Cursor contacts = getContentResolver().query(ContactsContract.CommonDataKinds.Phone.CONTENT_URI, null, null, null, null);
            Cursor emailid = getContentResolver().query(ContactsContract.CommonDataKinds.Email.CONTENT_URI, null, null, null, null);

            aNameFromContacts = new String[contacts.getCount()];
            aNumberFromContacts = new String[contacts.getCount()];
            anEmailFromContacts = new String[contacts.getCount()];
            int j = 0;

            if (contacts != null) {
                Log.i("here", "here");
                try {
                    HashSet<String> normalizedNumbersAlreadyFound = new HashSet<>();
                    int indexOfNormalizedNumber = contacts.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NORMALIZED_NUMBER);
                    int indexOfDisplayName = contacts.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME);
                    int indexOfDisplayNumber = contacts.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER);
                    //int indexOfEmail = contacts.getColumnIndex(ContactsContract.CommonDataKinds.Email.DATA6);

                    int nameId = emailid.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME);
                    int emailIdx = emailid.getColumnIndex(ContactsContract.CommonDataKinds.Email.DATA);

                    while (contacts.moveToNext()) {
                        String normalizedNumber = contacts.getString(indexOfNormalizedNumber);
                        if (normalizedNumbersAlreadyFound.add(normalizedNumber)) {
                            String displayName = contacts.getString(indexOfDisplayName);
                            Log.i("here", displayName);
                            aNameFromContacts[j] = displayName;
                            String displayNumber = contacts.getString(indexOfDisplayNumber);
                            aNumberFromContacts[j] = displayNumber;

                            anEmailFromContacts[j] = "";

                            emailid.moveToFirst();

                            while(emailid.moveToNext()) {
                                String email = emailid.getString(emailIdx);
                                String name = emailid.getString(nameId);
                                //Log.i("email", email);
                                //Log.i("name", name);
                                if(name.equals(displayName)) {
                                    anEmailFromContacts[j] = email;
                                    Log.i("email", email);
                                    break;
                                }
                            }
                            if (anEmailFromContacts[j].equals(""))
                                anEmailFromContacts[j] = null;
                            j++;
                        }
                    }
                } finally {
                    Log.i("h", aNameFromContacts[0]);
                    contacts.close();
                }
            }

        }
        LocalBroadcastManager.getInstance(this).registerReceiver(onNotice, new IntentFilter("Msg"));
        startSTTandTTS();
    }

    public static boolean hasPermissions(Context context, String... permissions) {
        if (context != null && permissions != null) {
            for (String permission : permissions) {
                if (ActivityCompat.checkSelfPermission(context, permission) != PackageManager.PERMISSION_GRANTED) {
                    return false;
                }
            }
        }
        return true;
    }

    private void startSTTandTTS() {
        tts = new TextToSpeech(getApplicationContext(), new TextToSpeech.OnInitListener() {
            @Override
            public void onInit(int status) {
                // TODO Auto-generated method stub
                if (status == TextToSpeech.SUCCESS) {
                    int result = tts.setLanguage(Locale.US);
                    if (result == TextToSpeech.LANG_MISSING_DATA ||
                            result == TextToSpeech.LANG_NOT_SUPPORTED) {
                        Log.e("error", "This Language is not supported");
                    }
                } else
                    Log.e("error", "Initilization Failed!");
            }
        });

        final EditText editText = findViewById(R.id.editText);
        mSpeechRecognizer = SpeechRecognizer.createSpeechRecognizer(this);
        mSpeechRecognizerIntent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        mSpeechRecognizerIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        mSpeechRecognizerIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE,
                Locale.getDefault());

        mSpeechRecognizer.setRecognitionListener(new RecognitionListener() {
            @Override
            public void onReadyForSpeech(Bundle bundle) {
                Log.i("onreadyforspeech", "hey");
            }

            @Override
            public void onBeginningOfSpeech() {
                Log.i("onbeginningofspeech", "hey");

            }

            @Override
            public void onRmsChanged(float v) {
               // Log.i("onrmschanged", "hey");
            }

            @Override
            public void onBufferReceived(byte[] bytes) {
                Log.i("buffer", "hey");
            }

            @Override
            public void onEndOfSpeech() {
                Log.i("endofspeech", "hey");
            }

            @Override
            public void onError(int i) {
                if ((i == SpeechRecognizer.ERROR_NO_MATCH)
                            || (i == SpeechRecognizer.ERROR_SPEECH_TIMEOUT))
                {
                    Log.d("MainActivity", "didn't recognize anything");
                        // keep going
                    mSpeechRecognizer.startListening(mSpeechRecognizerIntent);
                }
                else
                {
                    Log.d("MainActivity", "FAILED ");
                    }

            }

            @Override
            public void onResults(Bundle bundle) {
                //getting all the matches
                final ArrayList<String> matches = bundle
                        .getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);

                //displaying the first match
                    mSpeechRecognizer.stopListening();
                    if (matches != null)
                        editText.setText(matches.get(0));

                    Log.i("MainActivity", matches.get(0));
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH_MR1) {
                        tts.setOnUtteranceProgressListener(new UtteranceProgressListener() {
                            @Override
                            public void onStart(String s) {

                            }

                            @Override
                            public void onDone(String s) {
                                if (s.equals("utteranceId1")) {
                                    Log.i("MainActivity", "in onDone");
                                    runOnUiThread(new Runnable() {

                                        @Override
                                        public void run() {
                                            String result = matches.get(0);
                                            if ((result.replace(" ", "")).equals("bye") || (result.replace(" ", "")).equals("finish")) {
                                                finish();
                                            } else if (result.startsWith("call")) {
                                                if (mismatchFlag) {
                                                    mismatchFlag = false;
                                                    mSpeechRecognizer.startListening(mSpeechRecognizerIntent);
                                                } else {
                                                    handleCalls(result);
                                                    Log.i("after call", "after call");
                                                }
                                            } else if (result.startsWith("open") || result.startsWith("launch")) {
                                                handleApplications(result);
                                            } else if ((result.replaceAll(" ", "").toLowerCase()).equals("sendmessage") || (result.replaceAll(" ", "").toLowerCase()).equals("sendtextmessage")) {
                                                handleTextMessages();
                                            } else if ((result.replaceAll(" ", "").toLowerCase()).equals("sendmail") || (result.replaceAll(" ", "").replaceAll("-", "").toLowerCase()).equals("sendemail")) {
                                                handleEmail();
                                            } else if ((result.toLowerCase().equals("yes") || result.toLowerCase().equals("no")) && newNotificationsFlag) {
                                                newNotificationsFlag = false;
                                                if (result.toLowerCase().equals("no")) {
                                                    isThereNotification = false;
                                                    mSpeechRecognizer.startListening(mSpeechRecognizerIntent);
                                                } else {
                                                    readNotificationsContent();
                                                    //mSpeechRecognizer.startListening(mSpeechRecognizerIntent);
                                                }
                                            } else if(result.toLowerCase().replaceAll(" ", "").equals("shownotifications")){
                                               // mSpeechRecognizer.stopListening();
                                                if (isThereNotification) {
                                                    speakOutNotifications();
                                                } else {
                                                    HashMap<String, String> noNotifications = new HashMap();
                                                    noNotifications.put(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID,
                                                            "noNotifications");
                                                    tts.speak("No notifications yet.", TextToSpeech.QUEUE_FLUSH, noNotifications);
                                                    //mSpeechRecognizer.startListening(mSpeechRecognizerIntent);
                                                }
                                            } else {
                                                mSpeechRecognizer.startListening(mSpeechRecognizerIntent);
                                            }
                                        }
                                    });
                                } else if(s.equals("noNotifications")) {
                                    runOnUiThread(new Runnable() {

                                        @Override
                                        public void run() {
                                            mSpeechRecognizer.startListening(mSpeechRecognizerIntent);
                                        }
                                    });
                                } else if(s.equals("newNotifications")) {
                                    newNotificationsFlag = true;
                                    runOnUiThread(new Runnable() {

                                        @Override
                                        public void run() {
                                            mSpeechRecognizer.startListening(mSpeechRecognizerIntent);
                                        }
                                    });
                                } else if(s.equals("afterReadingNotifications")) {
                                    runOnUiThread(new Runnable() {

                                        @Override
                                        public void run() {
                                            NotificationManager notificationManager = (NotificationManager) getApplicationContext().getSystemService(Context.NOTIFICATION_SERVICE);
                                            notificationManager.cancelAll();
                                            notificationsList.clear();
                                            mSpeechRecognizer.startListening(mSpeechRecognizerIntent);
                                        }
                                    });
                                } else if(s.equals("end of wakeup message ID")) {

                                }
                            }

                            @Override
                            public void onError(String s) {
                                //mSpeechRecognizer.startListening(mSpeechRecognizerIntent);
                            }
                        });
                    }
                    HashMap<String, String> myHash = new HashMap();
                    myHash.put(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID,
                        "utteranceId1");
                    tts.speak(matches.get(0), TextToSpeech.QUEUE_FLUSH, myHash);
                    Log.i("MainActivity", "here");
            }

            @Override
            public void onPartialResults(Bundle bundle) {
                Log.i("onPartial", "hey");
            }

            @Override
            public void onEvent(int i, Bundle bundle) {
                Log.i("onEvent", "hey");

            }
        });

        /*if (isThereNotification) {
            Log.i("herh", "herh");
            speakOutNotifications();
        }*/

        mSpeechRecognizer.startListening(mSpeechRecognizerIntent);
    }

    private void handleEmail() {
        mailFlag = true;
        Intent emailIntent = new Intent(this, EmailHandlingActivity.class);
        emailIntent.putExtra("anEmailFromContacts", anEmailFromContacts);
        emailIntent.putExtra("aNameFromContacts", aNameFromContacts);
        startActivity(emailIntent);
    }

    private void handleTextMessages() {
        Intent messageIntent = new Intent(this, MessageHandlingActivity.class);
        messageIntent.putExtra("NamesFromContacts", aNameFromContacts);
        messageIntent.putExtra("NumbersFromContacts", aNumberFromContacts);
        messageFlag = true;
        startActivity(messageIntent);
    }

    private void handleApplications(String result) {
        tts.shutdown();
        if (result.toLowerCase().replaceAll(" ", "").equals("opentfdetect")) {
            PackageManager pm = this.getPackageManager();
            Intent intent = pm.getLaunchIntentForPackage("org.tensorflow.demo");
            if (intent != null) {
                this.startActivityForResult(intent, OBJECT_DETECTION_ACTIVITY);
            }
        } else {

        }
    }

    private void handleCalls(String result) {
        Log.i("MainActivity", "call");
        int i;
        Log.i("length", Integer.toString(aNameFromContacts.length));
        for (i = 0; i < aNameFromContacts.length; i++) {
            if (aNameFromContacts[i] != null) {
                if ((aNameFromContacts[i].replaceAll(" ", "").toLowerCase()).equals(result.replaceAll(" ", "").replace("call", "").toLowerCase())) {
                    break;
                }
            }
        }
        if (i == aNameFromContacts.length) {
            mismatchFlag = true;
            HashMap<String, String> myHash = new HashMap();
            myHash.put(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID,
                    "end of wakeup message ID");
            tts.speak("The contact name does not exist. Say again", TextToSpeech.QUEUE_FLUSH, myHash);
            //mSpeechRecognizer.startListening(mSpeechRecognizerIntent);
        } else {
            PhoneCallListener phoneListener = new PhoneCallListener();
            TelephonyManager telephonyManager = (TelephonyManager) this
                    .getSystemService(Context.TELEPHONY_SERVICE);
            telephonyManager.listen(phoneListener,
                    PhoneStateListener.LISTEN_CALL_STATE);

            Intent callIntent = new Intent(Intent.ACTION_CALL);
            callIntent.setData(Uri.parse("tel:" + aNumberFromContacts[i]));
            this.callIntent = callIntent;

            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.CALL_PHONE) != PackageManager.PERMISSION_GRANTED) {
                Log.i("performAction", "error");
                requestForCallPermission(new String[]{Manifest.permission.CALL_PHONE});
            } else {
                Log.i("performAction", "after call");
                startActivity(callIntent);
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        switch (requestCode) {
            case 1:
                if (grantResults.length > 0) {
                    for (int i = 0; i < permissions.length; i++) {
                        if (permissions[i].equals(Manifest.permission.READ_CONTACTS)) {
                            if (grantResults[i] == PackageManager.PERMISSION_GRANTED) {
                                Log.i("permission_granted", "granted");
                                Cursor contacts = getContentResolver().query(ContactsContract.CommonDataKinds.Phone.CONTENT_URI, null, null, null, null);
                                Cursor emailid = getContentResolver().query(ContactsContract.CommonDataKinds.Email.CONTENT_URI, null, null, null, null);

                                aNameFromContacts = new String[contacts.getCount()];
                                aNumberFromContacts = new String[contacts.getCount()];
                                anEmailFromContacts = new String[contacts.getCount()];
                                int j = 0;

                                if (contacts != null) {
                                    Log.i("here", "here");
                                    try {
                                        HashSet<String> normalizedNumbersAlreadyFound = new HashSet<>();
                                        int indexOfNormalizedNumber = contacts.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NORMALIZED_NUMBER);
                                        int indexOfDisplayName = contacts.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME);
                                        int indexOfDisplayNumber = contacts.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER);
                                        //int indexOfEmail = contacts.getColumnIndex(ContactsContract.CommonDataKinds.Email.DATA6);

                                        int nameId = emailid.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME);
                                        int emailIdx = emailid.getColumnIndex(ContactsContract.CommonDataKinds.Email.DATA);

                                        while (contacts.moveToNext()) {
                                            String normalizedNumber = contacts.getString(indexOfNormalizedNumber);
                                            if (normalizedNumbersAlreadyFound.add(normalizedNumber)) {
                                                String displayName = contacts.getString(indexOfDisplayName);
                                                Log.i("here", displayName);
                                                aNameFromContacts[j] = displayName;
                                                String displayNumber = contacts.getString(indexOfDisplayNumber);
                                                aNumberFromContacts[j] = displayNumber;

                                                anEmailFromContacts[j] = "";

                                                emailid.moveToFirst();

                                                while(emailid.moveToNext()) {
                                                    String email = emailid.getString(emailIdx);
                                                    String name = emailid.getString(nameId);
                                                    //Log.i("email", email);
                                                    //Log.i("name", name);
                                                    if(name.equals(displayName)) {
                                                        anEmailFromContacts[j] = email;
                                                        Log.i("email", email);
                                                        break;
                                                    }
                                                }
                                                if (anEmailFromContacts[j].equals(""))
                                                    anEmailFromContacts[j] = null;
                                                j++;
                                            }
                                        }
                                    } finally {
                                        Log.i("h", aNameFromContacts[0]);
                                        contacts.close();
                                    }
                                }
                            }
                        }else if (permissions[i].equals(Manifest.permission.CALL_PHONE)) {
                            if (grantResults[i] == PackageManager.PERMISSION_GRANTED) {
                                startActivity(callIntent);
                            }
                        }else if (permissions[i].equals(Manifest.permission.RECORD_AUDIO)) {
                            if (grantResults[i] == PackageManager.PERMISSION_GRANTED) {
                                startSTTandTTS();
                            }
                        }
                    }
                    break;
                }
        }
    }

    public void requestForCallPermission(String[] permissions)
    {
        if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.CALL_PHONE))
        {
        }
        else {

            ActivityCompat.requestPermissions(this, permissions,1);
        }
    }

    private class PhoneCallListener extends PhoneStateListener {

        String LOG_TAG = "LOGGING 123";

        @Override
        public void onCallStateChanged(int state, String incomingNumber) {

            if (TelephonyManager.CALL_STATE_RINGING == state) {
                // phone ringing
                Log.i(LOG_TAG, "RINGING, number: " + incomingNumber);
            }

            if (TelephonyManager.CALL_STATE_OFFHOOK == state) {
                // active
                Log.i(LOG_TAG, "OFFHOOK");
                callFlag = true;
                isPhoneCalling = true;
            }

            if (TelephonyManager.CALL_STATE_IDLE == state) {
                // run when class initial and phone call ended, need detect flag
                // from CALL_STATE_OFFHOOK
                Log.i(LOG_TAG, "IDLE");
                if (isPhoneCalling) {
                    Log.i(LOG_TAG, "restart app");
                    isPhoneCalling = false;

                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            mSpeechRecognizer.stopListening();
                            mSpeechRecognizer.destroy();
                            tts.shutdown();
                            startSTTandTTS();
                        }
                    });
                }

            }
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch(requestCode) {
            case (OBJECT_DETECTION_ACTIVITY) : {
                if (resultCode == Activity.RESULT_OK) {
                    Log.i("ActivityResult", "TF Detect returned result");
                    mSpeechRecognizer.startListening(mSpeechRecognizerIntent);
                }
                break;
            }
        }
    }

    @Override
    public void onStop() {
        super.onStop();
        Log.i("stop","stop");
        mSpeechRecognizer.stopListening();
        mSpeechRecognizer.destroy();
        tts.stop();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
       // mSpeechRecognizer.stopListening();
        mSpeechRecognizer.destroy();
        tts.shutdown();
    }

    @Override
    public void onPause() {
        super.onPause();
        Log.i("onPause", "onPause");
        mSpeechRecognizer.stopListening();
        mSpeechRecognizer.destroy();
        tts.stop();
        //tts.shutdown();
    }

    @Override
    public void onResume() {
        super.onResume();
        if ((callFlag && isPhoneCalling) || messageFlag || mailFlag) {
            Log.i("resume", "resume");
            callFlag = false;
            messageFlag = false;
            mailFlag = false;
            /*if (isThereNotification) {
                speakOutNotifications();
            }*/
            startSTTandTTS();
            //mSpeechRecognizer.startListening(mSpeechRecognizerIntent);
        }
    }

    private BroadcastReceiver onNotice= new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {
            String pack = intent.getStringExtra("package");
            String title = intent.getStringExtra("title");
            title = title.replaceAll("[^a-zA-Z0-9 ]*", "");
            //title = title.concat(" contains emojis");
            String text = intent.getStringExtra("text");
            String extraBigText = intent.getStringExtra("extraBigText");

            Log.i("package", pack);
            Log.i("text", text);

            /*if (pack.toLowerCase().endsWith("gm")) {
                mailNotificationFlag = true;
            }*/

            if (!(pack.toLowerCase().endsWith("contacts") || pack.toLowerCase().endsWith("systemui"))) {
                notificationsList.add(pack);
                notificationsList.add(title);
                notificationsList.add(text);
                notificationsList.add(extraBigText);

                isThereNotification = true;
            }
        }
    };

    private void speakOutNotifications() {
            HashMap<String, String> newNotifications = new HashMap();
            newNotifications.put(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID,
                    "newNotifications");
            tts.speak("You have new notifications. Do you want to listen to them?", TextToSpeech.QUEUE_FLUSH, newNotifications);
    }

    private void readNotificationsContent() {
        isThereNotification = false;

        mSpeechRecognizer.stopListening();

        for (int i = 0; i<(notificationsList.size()); i+=4) {
            if (notificationsList.get(i).split("\\.")[notificationsList.get(i).split("\\.").length - 1].toLowerCase().equals("mms")) {
                tts.speak("You have a new text message. ", TextToSpeech.QUEUE_ADD, null);
                tts.speak("The sender is " + notificationsList.get(i+1), TextToSpeech.QUEUE_ADD, null);
                tts.speak("The content is " + notificationsList.get(i+2), TextToSpeech.QUEUE_ADD, null);
            } else if (notificationsList.get(i).split("\\.")[notificationsList.get(i).split("\\.").length - 1].toLowerCase().equals("whatsapp")) {
                if (notificationsList.get(i + 2).contains(" messages from ") && notificationsList.get(i + 2).endsWith("chats")) {

                } else {
                    tts.speak("You have a new whatsapp message. ", TextToSpeech.QUEUE_ADD, null);
                    tts.speak("The sender is " + notificationsList.get(i + 1), TextToSpeech.QUEUE_ADD, null);
                    tts.speak("The content is " + notificationsList.get(i + 2), TextToSpeech.QUEUE_ADD, null);
                }
            } else if (notificationsList.get(i).split("\\.")[notificationsList.get(i).split("\\.").length - 1].toLowerCase().equals("gm")) {
                  tts.speak("You have a new mail. ", TextToSpeech.QUEUE_ADD, null);
                  tts.speak("The sender is " + notificationsList.get(i + 1), TextToSpeech.QUEUE_ADD, null);
                  tts.speak("The subject is " + notificationsList.get(i + 2), TextToSpeech.QUEUE_ADD, null);
                  tts.speak("The content is " + notificationsList.get(i + 3).replace(notificationsList.get(i+2), "").trim(), TextToSpeech.QUEUE_ADD, null);

            }
            /*if (notificationsList.get(i+2) != "") {
                tts.speak("The content is" + notificationsList.get(i+2), TextToSpeech.QUEUE_ADD, null);
            }*/
        }
        HashMap<String, String> afterNotifications = new HashMap();
        afterNotifications.put(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID,
                "afterReadingNotifications");
        tts.speak("Done with the notifications", TextToSpeech.QUEUE_ADD, afterNotifications);
    }
}
