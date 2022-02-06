package fr.celiangarcia.milobella;

import android.Manifest;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Bundle;
import android.speech.RecognizerIntent;
import android.speech.tts.TextToSpeech;
import android.speech.tts.UtteranceProgressListener;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import edu.cmu.pocketsphinx.Assets;
import edu.cmu.pocketsphinx.Hypothesis;
import edu.cmu.pocketsphinx.SpeechRecognizer;
import edu.cmu.pocketsphinx.SpeechRecognizerSetup;

public class MainActivity extends AppCompatActivity implements
        TextToSpeech.OnInitListener, edu.cmu.pocketsphinx.RecognitionListener {
    public static final String ERROR_MESSAGE_USER = "That didn't work!";
    public static final String TTS_UTTERANCE = "TTS_UTTERANCE";
    TextView txtOutput;
    Button bouton;

    private TextToSpeech tts;

    private List<Show> shows = new ArrayList<>();
    private ShowAdapter adapter;

    private SpeechRecognizer recognizer;

    /* Named searches allow to quickly reconfigure the decoder */
    private static final String KWS_SEARCH = "wakeup";

    /* Keyword we are looking for to activate menu */
    private static final String WAKEUPWORD = "milobella";

    /* Used to handle permission request */
    private static final int PERMISSIONS_REQUEST_RECORD_AUDIO = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        tts = new TextToSpeech(this, this);

        txtOutput = findViewById(R.id.text);
        bouton = findViewById(R.id.button);

        ListView mListView = findViewById(R.id.listView);

        bouton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Toast.makeText(getApplicationContext(),
                        "click",
                        Toast.LENGTH_SHORT).show();
                startSpeechToText();
            }
        });

        adapter = new ShowAdapter(MainActivity.this, shows);
        mListView.setAdapter(adapter);

        // Check if user has given permission to record audio
        int permissionCheck = ContextCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.RECORD_AUDIO);
        if (permissionCheck != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.RECORD_AUDIO}, PERMISSIONS_REQUEST_RECORD_AUDIO);
            return;
        }
        // Recognizer initialization is a time-consuming and it involves IO,
        // so we execute it in async task
        new SetupTask(this).execute();

    }

    private static class SetupTask extends AsyncTask<Void, Void, Exception> {
        WeakReference<MainActivity> activityReference;
        SetupTask(MainActivity activity) {
            this.activityReference = new WeakReference<>(activity);
        }
        @Override
        protected Exception doInBackground(Void... params) {
            try {
                Assets assets = new Assets(activityReference.get());
                File assetDir = assets.syncAssets();
                activityReference.get().setupRecognizer(assetDir);
            } catch (IOException e) {
                return e;
            }
            return null;
        }
        @Override
        protected void onPostExecute(Exception result) {
            if (result != null) {
                Toast.makeText(activityReference.get(),
                        "Failed to init recognizer " + result, Toast.LENGTH_SHORT).show();
            } else {
                activityReference.get().startWakeUpWordListening();
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == PERMISSIONS_REQUEST_RECORD_AUDIO) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Recognizer initialization is a time-consuming and it involves IO,
                // so we execute it in async task
                new SetupTask(this).execute();
            } else {
                finish();
            }
        }
    }

    private void setupRecognizer(File assetsDir) throws IOException {
        // The recognizer can be configured to perform multiple searches
        // of different kind and switch between them

        recognizer = SpeechRecognizerSetup.defaultSetup()
                .setAcousticModel(new File(assetsDir, "cmusphinx-fr-ptm-5.2"))
                .setDictionary(new File(assetsDir, "cmudict-fr-fr.dic"))

                .setRawLogDir(assetsDir) // To disable logging of raw audio comment out this call (takes a lot of space on the device)

                .getRecognizer();
        recognizer.addListener(this);

        // Create keyword-activation search.
        recognizer.addKeyphraseSearch(KWS_SEARCH, WAKEUPWORD);

        Log.d("SPHINX", "Recognizer set up");
    }

    private void startSpeechToText() {
        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.FRENCH);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        intent.putExtra(RecognizerIntent.EXTRA_PROMPT,
                "Speak something...");
        try {
            startActivityForResult(intent, 666);
        } catch (ActivityNotFoundException a) {
            Toast.makeText(getApplicationContext(),
                    "Sorry! Speech recognition is not supported in this device.",
                    Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * Callback for speech recognition activity
     */
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch (requestCode) {
            case 666: {
                if (resultCode == RESULT_OK && null != data) {
                    ArrayList<String> result = data
                            .getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
                    String text = result.get(0);
                    makeMilobellaCall(text);
                }
                break;
            }
        }
    }

    protected void makeMilobellaCall(String text) {
        // Instantiate the RequestQueue.
        RequestQueue queue = Volley.newRequestQueue(this);
        String url = "https://milobella.com:10443/api/v1/talk/text";

        try {
            final JSONObject jsonBody = new JSONObject("{\n" +
                    "\t\"text\": \"" + text + "\"\n" +
                    "}");
            // Request a string response from the provided URL.
            JsonObjectRequest stringRequest = new JsonObjectRequest(url, jsonBody,
                    new Response.Listener<JSONObject>() {
                        @Override
                        public void onResponse(JSONObject response) {
                            // Display the first 500 characters of the response string.
                            try {
                                txtOutput.setText(response.getString("vocal"));
                                shows.clear();
                                if (response.has("visu")) {
                                    JSONArray getArray = response.getJSONArray("visu");
                                    for(int i = 0; i < getArray.length(); i++) {
                                        JSONObject obj = getArray.getJSONObject(i);
                                        String title = obj.getString("title");
                                        String display = obj.getString("display");
                                        shows.add(new Show(Color.BLACK, title, display));
                                    }
                                }
                                adapter.notifyDataSetChanged();
                                speakOut();
                            } catch (JSONException e) {
                                txtOutput.setText(ERROR_MESSAGE_USER);
                            }
                        }
                    }, new Response.ErrorListener() {
                @Override
                public void onErrorResponse(VolleyError error) {
                    txtOutput.setText(ERROR_MESSAGE_USER);
                }
            });

            // Add the request to the RequestQueue.
            queue.add(stringRequest);
        } catch (JSONException e) {
            txtOutput.setText(ERROR_MESSAGE_USER);
        }

    }

    @Override
    public void onDestroy() {
        // Don't forget to shutdown tts!
        if (tts != null) {
            tts.stop();
            tts.shutdown();
        }
        super.onDestroy();
    }

    @Override
    public void onInit(int status) {

        if (status == TextToSpeech.SUCCESS) {
            tts.setOnUtteranceProgressListener(new UtteranceProgressListener() {
                @Override
                public void onStart(String utteranceId) {
                    if (TTS_UTTERANCE.equals(utteranceId)) {
                        stopWakeUpWordListening();
                    }
                }

                @Override
                public void onDone(String utteranceId) {
                    if (TTS_UTTERANCE.equals(utteranceId)) {
                        startWakeUpWordListening();
                    }
                }

                @Override
                public void onError(String utteranceId) {
                    if (TTS_UTTERANCE.equals(utteranceId)) {
                        startWakeUpWordListening();
                    }
                }
            });
            int result = tts.setLanguage(Locale.FRENCH);
            if (result == TextToSpeech.LANG_MISSING_DATA
                    || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                Log.e("TTS", "This Language is not supported");
            }
        } else {
            Log.e("TTS", "Initilization Failed!");
        }

    }

    private void speakOut() {
        String text = txtOutput.getText().toString();
        tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, TTS_UTTERANCE);
    }

    @Override
    public void onBeginningOfSpeech() {
        Log.d("SPHINX", "beginning of speech");
    }

    @Override
    public void onEndOfSpeech() {
        Log.d("SPHINX", "end of speech");
    }

    @Override
    public void onPartialResult(Hypothesis hypothesis) {
        if (hypothesis != null) {
            String text = hypothesis.getHypstr();
            Log.d("SPHINX", "Partial result " + text);
            if (WAKEUPWORD.equals(text)) {
                stopWakeUpWordListening();
            }
            Toast.makeText(getApplicationContext(), text, Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onResult(Hypothesis hypothesis) {
        if (hypothesis != null) {
            String text = hypothesis.getHypstr();
            Log.d("SPHINX", "onResult " + text);
            if (WAKEUPWORD.equals(text)) {
                stopWakeUpWordListening();
                startSpeechToText();
            }
            Toast.makeText(getApplicationContext(), text, Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onError(Exception e) {
        Toast.makeText(getApplicationContext(),
                "Error on pocketsphinx speech recognition.",
                Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onTimeout() {
        startWakeUpWordListening();
    }

    private void startWakeUpWordListening() {
        recognizer.startListening(KWS_SEARCH);
    }

    private void stopWakeUpWordListening() {
        recognizer.stop();
    }
}
