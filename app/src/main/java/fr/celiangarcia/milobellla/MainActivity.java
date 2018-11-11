package fr.celiangarcia.milobellla;

import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.graphics.Color;
import android.speech.RecognizerIntent;
import android.speech.tts.TextToSpeech;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
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

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class MainActivity extends AppCompatActivity implements
        TextToSpeech.OnInitListener {
    TextView txtOutput;
    Button bouton;

    private TextToSpeech tts;

    private ListView mListView;
    private List<Show> shows;
    private ShowAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        tts = new TextToSpeech(this, this);

        txtOutput = (TextView) findViewById(R.id.text);
        bouton = (Button) findViewById(R.id.button);

        mListView = (ListView) findViewById(R.id.listView);

        bouton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Toast.makeText(getApplicationContext(),
                        "click",
                        Toast.LENGTH_SHORT).show();
                startSpeechToText();
            }
        });

        shows = genererTweets();

        adapter = new ShowAdapter(MainActivity.this, shows);
        mListView.setAdapter(adapter);

    }

    private void startSpeechToText() {
        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault());
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
        String url = "http://192.168.1.21:9100/talk/text";

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
                                txtOutput.setText("That didn't work!");
                            }
                        }
                    }, new Response.ErrorListener() {
                @Override
                public void onErrorResponse(VolleyError error) {
                    txtOutput.setText("That didn't work!");
                }


            });

            // Add the request to the RequestQueue.
            queue.add(stringRequest);
        } catch (JSONException e) {
            txtOutput.setText("That didn't work!");
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

        tts.speak(text, TextToSpeech.QUEUE_FLUSH, null);
    }

    private List<Show> genererTweets(){
        List<Show> shows = new ArrayList<Show>();
        shows.add(new Show(Color.BLACK, "Florent", "Mon premier tweet !"));
        shows.add(new Show(Color.BLUE, "Kevin", "C'est ici que ça se passe !"));
        shows.add(new Show(Color.GREEN, "Logan", "Que c'est beau..."));
        shows.add(new Show(Color.RED, "Mathieu", "Il est quelle heure ??"));
        shows.add(new Show(Color.GRAY, "Willy", "On y est presque"));
        return shows;
    }

}