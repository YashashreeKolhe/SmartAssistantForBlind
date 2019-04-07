package se.coep.org.in.smart_assistant_for_blind;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.provider.ContactsContract;
import android.provider.Settings;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.speech.tts.TextToSpeech;
import android.speech.tts.UtteranceProgressListener;
import android.support.annotation.NonNull;
import android.support.annotation.RequiresApi;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
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

public class MainActivity extends AppCompatActivity {

    private Intent mSpeechRecognizerIntent;
    private SpeechRecognizer mSpeechRecognizer;
    private TextToSpeech tts;
    private Intent callIntent;
    String aNameFromContacts[];
    String aNumberFromContacts[];
    boolean callFlag = false;
    private boolean isPhoneCalling = false;
    private boolean mismatchFlag = false;
    private boolean messageFlag = true;

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

        if(!hasPermissions(this, PERMISSIONS)){
            ActivityCompat.requestPermissions(this, PERMISSIONS, PERMISSION_ALL);
        } else {
            Cursor contacts = getContentResolver().query(ContactsContract.CommonDataKinds.Phone.CONTENT_URI, null, null, null, null);
            aNameFromContacts = new String[contacts.getCount()];
            aNumberFromContacts = new String[contacts.getCount()];
            int j = 0;

            if (contacts != null) {
                Log.i("here", "here");
                try {
                    HashSet<String> normalizedNumbersAlreadyFound = new HashSet<>();
                    int indexOfNormalizedNumber = contacts.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NORMALIZED_NUMBER);
                    int indexOfDisplayName = contacts.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME);
                    int indexOfDisplayNumber = contacts.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER);

                    while (contacts.moveToNext()) {
                        String normalizedNumber = contacts.getString(indexOfNormalizedNumber);
                        if (normalizedNumbersAlreadyFound.add(normalizedNumber)) {
                            String displayName = contacts.getString(indexOfDisplayName);
                            Log.i("here", displayName);
                            aNameFromContacts[j] = displayName;
                            String displayNumber = contacts.getString(indexOfDisplayNumber);
                            aNumberFromContacts[j] = displayNumber;
                            j++;
                        }
                    }
                } finally {
                    Log.i("h", aNameFromContacts[0]);
                    contacts.close();
                }
            }

        }

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

            }

            @Override
            public void onBeginningOfSpeech() {

            }

            @Override
            public void onRmsChanged(float v) {

            }

            @Override
            public void onBufferReceived(byte[] bytes) {

            }

            @Override
            public void onEndOfSpeech() {

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
                                            } else {
                                                mSpeechRecognizer.startListening(mSpeechRecognizerIntent);
                                            }
                                        }
                                    });
                                } else if(s.equals("utteranceId2")) {

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

            }

            @Override
            public void onEvent(int i, Bundle bundle) {

            }
        });
        mSpeechRecognizer.startListening(mSpeechRecognizerIntent);
    }

    private void handleEmail() {
        Intent emailIntent = new Intent(this, EmailHandlingActivity.class);
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
                                aNameFromContacts = new String[contacts.getCount()];
                                aNumberFromContacts = new String[contacts.getCount()];
                                int j = 0;

                                if (contacts != null) {
                                    Log.i("here", "here");
                                    try {
                                        HashSet<String> normalizedNumbersAlreadyFound = new HashSet<>();
                                        int indexOfNormalizedNumber = contacts.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NORMALIZED_NUMBER);
                                        int indexOfDisplayName = contacts.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME);
                                        int indexOfDisplayNumber = contacts.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER);

                                        while (contacts.moveToNext()) {
                                            String normalizedNumber = contacts.getString(indexOfNormalizedNumber);
                                            if (normalizedNumbersAlreadyFound.add(normalizedNumber)) {
                                                String displayName = contacts.getString(indexOfDisplayName);
                                                Log.i("here", displayName);
                                                aNameFromContacts[j] = displayName;
                                                String displayNumber = contacts.getString(indexOfDisplayNumber);
                                                aNumberFromContacts[j] = displayNumber;
                                                j++;
                                                //haven't seen this number yet: do something with this contact!
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
        if ((callFlag && isPhoneCalling) || messageFlag) {
            Log.i("resume", "resume");
            callFlag = false;
            messageFlag = false;
            startSTTandTTS();
            //mSpeechRecognizer.startListening(mSpeechRecognizerIntent);
        }
    }
}
