package it.pmsc.silviosenese.database;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = "MainActivity";
    EditText etMessaggio;
    TextView tvMessaggio;
    Button button;

    FirebaseDatabase database = FirebaseDatabase.getInstance("https://database-40fd1-default-rtdb.europe-west1.firebasedatabase.app/");
    DatabaseReference myRef = database.getReference("message"); //se riporto solo getReference -> nodo radice


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        initUI();
    }

    private void initUI() {
        etMessaggio = (EditText) findViewById(R.id.etNome);
        tvMessaggio = (TextView) findViewById(R.id.tvMessaggio);
        button = (Button) findViewById(R.id.button);

    }

    public void inviaMessaggio(View view) {
        String messaggio=etMessaggio.getText().toString();
        Log.i(TAG, "Messaggio: "+messaggio);

        //Scrivo il messaggio sul DB
        Log.i(TAG, "Messaggio: "+myRef);
        myRef.setValue(messaggio);

        leggiMessaggio();


    }

    private void leggiMessaggio() {
    // Read from the database
        myRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                // This method is called once with the initial value and again
                // whenever data at this location is updated.
                String value = dataSnapshot.getValue(String.class);
                Log.d(TAG, "Value is: " + value);
                tvMessaggio.setText(value);
            }

            @Override
            public void onCancelled(DatabaseError error) {
                // Failed to read value
                Log.w(TAG, "Failed to read value.", error.toException());
            }
        });
    }
}