package se.coep.org.in.smart_assistant_for_blind;

import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.speech.tts.TextToSpeech;
import android.speech.tts.UtteranceProgressListener;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.widget.EditText;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Locale;

/**
 * Created by dell on 5/4/19.
 */

public class EmailHandlingActivity extends AppCompatActivity {
    private Intent mSpeechRecognizerIntent;
    private SpeechRecognizer mSpeechRecognizer;
    private TextToSpeech tts2;
    HashMap<String, String> senderHash = new HashMap();
    String senderEmail;
    String messageContent;
    String subject;
    boolean contentsFlag = false;
    boolean senderFlag = false;
    boolean subjectFlag = false;
    String[] anEmailFromContacts;
    String[] aNameFromContacts;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_email_handling);

        Intent receivedIntent = getIntent();
        anEmailFromContacts = receivedIntent.getStringArrayExtra("anEmailFromContacts");
        aNameFromContacts = receivedIntent.getStringArrayExtra("aNameFromContacts");

        startSTT();

        tts2 = new TextToSpeech(EmailHandlingActivity.this, new TextToSpeech.OnInitListener() {
            @Override
            public void onInit(int status) {
                // TODO Auto-generated method stub
                if (status == TextToSpeech.SUCCESS) {
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
                    } else if (s.equals("subjectHash")) {
                        //contentsFlag = true;
                        subjectFlag = true;
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
                    }else if (s.equals("afterEmail")) {
                        /*mSpeechRecognizer.stopListening();
                        mSpeechRecognizer.destroy();
                        tts2.stop();*/
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                finish();
                            }
                        });
                    } else if (s.equals("senderNameEcho")) {
                        HashMap<String, String> subjectHash = new HashMap();
                        subjectHash.put(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, "subjectHash");
                        tts2.speak("Please tell mail subject", TextToSpeech.QUEUE_FLUSH, subjectHash);
                    } else if (s.equals("messageContentEcho")) {
                        sendEmail(senderEmail, subject, messageContent);
                    } else if(s.equals("subjectEcho")) {
                        contentsFlag = true;
                        HashMap<String, String> contentsHash = new HashMap();
                        contentsHash.put(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, "subjectHash");
                        tts2.speak("Please tell message contents", TextToSpeech.QUEUE_FLUSH, contentsHash);
                    } else if (s.equals("senderNameError")) {
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                finish();
                                startActivity(getIntent());
                            }
                        });
                    }
                }

                @Override
                public void onError(String s) {
                    //mSpeechRecognizer.startListening(mSpeechRecognizerIntent);
                }
            });
        }
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

                if (result.equals("exit")) {
                    finish();
                }

                Log.i("onResult", "onresult");
                if (senderFlag) {
                    senderFlag = false;
                    if(result == null) {
                        HashMap<String, String> senderNameEcho = new HashMap();
                        senderNameEcho.put(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, "senderNameEcho");
                        tts2.speak(result, TextToSpeech.QUEUE_FLUSH, senderNameEcho);
                    } else {
                        Log.i("result", result);
                        EditText text = findViewById(R.id.editText2);

                        if (result.contains("@")) {
                            senderEmail = parseMailId(result);
                        }
                        else {
                            senderEmail = null;
                            for (int i = 0; i < aNameFromContacts.length; i++) {
                                if (aNameFromContacts[i] != null) {
                                    if (result.toLowerCase().replaceAll(" ", "").equals(aNameFromContacts[i].toLowerCase().replaceAll(" ", ""))) {
                                        if (anEmailFromContacts[i] != null) {
                                            senderEmail = anEmailFromContacts[i];
                                            text.setText(senderEmail);
                                        }else
                                            senderEmail = null;
                                    }
                                }
                            }
                        }

                        if (senderEmail == null) {
                            HashMap<String, String> senderNameError = new HashMap();
                            senderNameError.put(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, null);
                            tts2.speak(result, TextToSpeech.QUEUE_ADD, senderNameError);
                            tts2.speak("No such contact name exists or the email for this contact name is not available. Please enter valid email address", TextToSpeech.QUEUE_ADD, senderNameError);
                        }
                        else {
                            text.setText(senderEmail);
                            HashMap<String, String> senderNameEcho = new HashMap();
                            senderNameEcho.put(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, "senderNameEcho");
                            tts2.speak(result, TextToSpeech.QUEUE_FLUSH, senderNameEcho);
                        }
                    }
                }
                if (subjectFlag) {
                    subjectFlag =false;
                    subject = result;
                    EditText subject = findViewById(R.id.editText3);
                    subject.setText(result);
                    HashMap<String, String> subjectEcho = new HashMap();
                    subjectEcho.put(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, "subjectEcho");
                    tts2.speak(result, TextToSpeech.QUEUE_FLUSH, subjectEcho);

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

    @Override
    public void onStop() {
        super.onStop();
        Log.i("mail activity", "stop");
        mSpeechRecognizer.stopListening();
        mSpeechRecognizer.destroy();
        tts2.stop();
    }

    @Override
    public void onPause() {
        super.onPause();
        Log.i("onapause", "onpause");
        //mSpeechRecognizer.stopListening();
        //mSpeechRecognizer.destroy();
        //tts2.stop();
    }

    private void sendEmail(String senderEmail, String subject, String messageContent) {
        HashMap<String, String> afterEmail = new HashMap();
        afterEmail.put(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, "afterEmail");

        try {
            GMailSender sender = new GMailSender("yashashreekolhe@gmail.com", "quboolhai");
            sender.sendMail(subject,
                    messageContent,
                    "yashashreekolhe@gmail.com",
                    senderEmail);
            tts2.speak("Mail sent successfully", TextToSpeech.QUEUE_FLUSH, afterEmail);
        } catch (Exception e) {
            Log.e("SendMail", e.getMessage(), e);
            tts2.speak("Cannot send mail! Please try again.", TextToSpeech.QUEUE_FLUSH, afterEmail);
        }

        /*try {
            /*this.startActivity(gmailIntent);*/
            //tts2.speak("Mail sent successfully", TextToSpeech.QUEUE_FLUSH, afterEmail);
        /*} catch(ActivityNotFoundException ex) {
            tts2.speak("Cannot send mail! Pleaase try again.", TextToSpeech.QUEUE_FLUSH, afterEmail);
        }*/
    }

    private String parseMailId(String result) {
        String parsed = result.replace(" at ", "@");
        parsed = parsed.replace(" ", "");
        parsed = parsed.toLowerCase();
        parsed = parsed.replace("dot", ".");
        return parsed;

    }
}
