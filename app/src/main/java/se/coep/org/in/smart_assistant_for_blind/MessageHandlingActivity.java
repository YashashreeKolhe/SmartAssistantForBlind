package se.coep.org.in.smart_assistant_for_blind;

import android.content.Intent;
import android.database.Cursor;
import android.os.Build;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.speech.tts.TextToSpeech;
import android.speech.tts.UtteranceProgressListener;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.telephony.SmsManager;
import android.util.Log;
import android.widget.EditText;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;

/**
 * Created by dell on 31/3/19.
 */

public class MessageHandlingActivity extends AppCompatActivity {
    private Intent mSpeechRecognizerIntent;
    private SpeechRecognizer mSpeechRecognizer;
    private TextToSpeech tts2;
    String[] namesFromContacts;
    String[] numbersFromContacts;
    boolean senderFlag = false;
    boolean contentsFlag = false;
    HashMap<String, String> senderHash = new HashMap();
    String contactNumber;
    String messageContent;
    boolean textToSpeechIsInitialized = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_message_handling);

        Intent intent = getIntent();
        namesFromContacts = intent.getStringArrayExtra("NamesFromContacts");
        numbersFromContacts = intent.getStringArrayExtra("NumbersFromContacts");

        Log.i("message", Integer.toString(namesFromContacts.length));

        startSTT();

        tts2 = new TextToSpeech(MessageHandlingActivity.this, new TextToSpeech.OnInitListener() {
            @Override
            public void onInit(int status) {
                // TODO Auto-generated method stub
                if (status == TextToSpeech.SUCCESS) {
                    textToSpeechIsInitialized = true;
                    int result = tts2.setLanguage(Locale.US);
                    if (result == TextToSpeech.LANG_MISSING_DATA ||
                            result == TextToSpeech.LANG_NOT_SUPPORTED) {
                        Log.e("error", "This Language is not supported");
                    }
                    senderHash.put(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID,
                            "senderHash");
                    tts2.speak("Please tell the sender's name", TextToSpeech.QUEUE_FLUSH, senderHash);
                } else
                    Log.e("error", "Initilization Failed!");
            }
        });
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH_MR1) {
            tts2.setOnUtteranceProgressListener(new UtteranceProgressListener() {
                @Override
                public void onStart(String s) {

                }

                @Override
                public void onDone(String s) {
                    if (s.equals("senderHash")) {
                        senderFlag = true;
                        Log.i("senderHash", "senderhash");
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                mSpeechRecognizer.startListening(mSpeechRecognizerIntent);
                            }
                        });
                    }else if(s.equals("utteranceId1")) {
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                mSpeechRecognizer.startListening(mSpeechRecognizerIntent);
                            }
                        });
                    } else if (s.equals("contentsHash")) {
                        contentsFlag = true;
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                mSpeechRecognizer.startListening(mSpeechRecognizerIntent);
                            }
                        });
                    }else if (s.equals("aftersend")) {
                        finish();
                    } else if (s.equals("senderNameEcho")) {
                        if (contactNumber == null) {
                            senderHash.put(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, "senderHash");
                            tts2.speak("Incorrect sender name. Say again.", TextToSpeech.QUEUE_FLUSH, senderHash);
                        }else {
                            HashMap<String, String> contentsHash = new HashMap();
                            contentsHash.put(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, "contentsHash");
                            tts2.speak("Please tell message contents", TextToSpeech.QUEUE_FLUSH, contentsHash);
                        }
                    } else if (s.equals("messageContentEcho")) {
                        sendMessage(contactNumber, messageContent);
                    }
                }

                @Override
                public void onError(String s) {
                    //mSpeechRecognizer.startListening(mSpeechRecognizerIntent);
                }
            });
        }
    }

    private void starttts2() {
        Log.i("message", "tts2");

    }

    private void startSTT() {
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

                final ArrayList<String> matches = bundle
                        .getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
                String result = matches.get(0);

                mSpeechRecognizer.stopListening();
                //senderFlag = true;

                if (senderFlag) {
                    senderFlag = false;
                    contactNumber = getContactNumber(result);
                    if(contactNumber == null) {
                        HashMap<String, String> senderNameEcho = new HashMap();
                        senderNameEcho.put(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, "senderNameEcho");
                        tts2.speak(result, TextToSpeech.QUEUE_FLUSH, senderNameEcho);
                    } else {
                        EditText text = findViewById(R.id.senderName);
                        text.setText(result);
                        HashMap<String, String> senderNameEcho = new HashMap();
                        senderNameEcho.put(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, "senderNameEcho");
                        tts2.speak(result, TextToSpeech.QUEUE_FLUSH, senderNameEcho);
                    }
                }
                if (contentsFlag) {
                    contentsFlag = false;
                    messageContent = result;
                    EditText message = findViewById(R.id.message);
                    message.setText(messageContent);
                    HashMap<String, String> messageContentEcho = new HashMap();
                    messageContentEcho.put(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, "messageContentEcho");
                    tts2.speak(result, TextToSpeech.QUEUE_FLUSH, messageContentEcho);
                }
            }

            @Override
            public void onPartialResults(Bundle bundle) {

            }

            @Override
            public void onEvent(int i, Bundle bundle) {

            }
        });
        //mSpeechRecognizer.startListening(mSpeechRecognizerIntent);
    }

    private String getContactNumber(String result) {
        for(int i = 0; i < namesFromContacts.length; i++) {
            if (namesFromContacts[i] != null) {
                if (namesFromContacts[i].replaceAll(" ", "").toLowerCase().equals(result.toLowerCase().replaceAll(" ", ""))) {
                    return numbersFromContacts[i];
                }
            }
        }
        return null;
    }

    private void sendMessage(String contactNumber, String messageContent) {

        try {
            SmsManager smsMgrVar = SmsManager.getDefault();
            smsMgrVar.sendTextMessage(contactNumber, null, messageContent, null, null);
            HashMap<String, String> myHash = new HashMap();
            myHash.put(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID,
                    "aftersend");
            tts2.speak("Message sent successfully.", TextToSpeech.QUEUE_FLUSH, myHash);
        }
        catch (Exception ErrVar)
        {
            Log.e("error", ErrVar.toString());
            HashMap<String, String> myHash = new HashMap();
            myHash.put(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID,
                    "aftersend");
            tts2.speak("Message could not be sent. Try again.", TextToSpeech.QUEUE_FLUSH, myHash);
        }
    }

    @Override
    public void onStop() {
        super.onStop();
        mSpeechRecognizer.stopListening();
        mSpeechRecognizer.destroy();
        tts2.stop();
    }
}
