package it.pmsc.silviosenese.database;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.Continuation;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

public class EsempioComplesso extends AppCompatActivity {
    private static final String TAG = "EsComplessoActivity";
    private static final int PERMISSION_LOAD_FILE = 1;
    private static final int PICK_PDF_REQUEST = 1;
    EditText etNome;
    EditText etCognome;
    EditText etMatricola;
    TextView tvMessaggio;
    Button button;
    Button btn_selezionaFile;
    Uri risultatoRicercaFile;

    FirebaseDatabase database;
    DatabaseReference myRef;

    FirebaseStorage storage;
    StorageReference storageRef;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_esempio_complesso);

        initUI();
    }

    private void initUI() {
        etNome=findViewById(R.id.etNome);
        etCognome=findViewById(R.id.etCognome);
        etMatricola=findViewById(R.id.etMatricola);
        tvMessaggio=findViewById(R.id.tvMessaggio);
        button=findViewById(R.id.button);
        btn_selezionaFile=findViewById(R.id.btn_seleziona_file);

        database = FirebaseDatabase.getInstance("https://database-40fd1-default-rtdb.europe-west1.firebasedatabase.app/");
        myRef = database.getReference(); //se riporto solo getReference -> nodo radice

        storage = FirebaseStorage.getInstance("gs://database-40fd1.appspot.com");
        storageRef = storage.getReference();
    }

    //Se clicco su Seleziona File mi fa scegliere un file, se clicco su invia guarda se è stato selezionat
    // un pdf e se i campi sono riempiti, se è tutto ok carica l'utente su realtime db e il file su storage
    //Questi due oggetti deo legarli in qualche modo. Magari chiave utente e chiave appunto identifica il "proprietario" dell'appunto
    //Ricordarsi che ogni appunto ha un solo autore e tanti lettori
    //Ogni autore ha tanti appunti
    //Ogni compratore può leggere tanti appunti
    //Da capire qual è il modo migliore

    //Da capire nche se con questo tipo di storage noSQL si riesce a generare delle tabelle per il tool di ricerca
    public void selezionaFile(View view) {
        //Qua bisogna chiedere i permessi per cercare i file
        //ottenuti i permessi servirà un intent con startActivityForResult per tornare con in pancia un file pdf
        //NOTA: bisogna aggiungere alla classe Appunto una voce che contenga questo file pdf?
        //Il caricamento del file sull'app sarebbe da fare in  multithreading e come prima cossa bisognerebbe controllare la dimensione
        Log.i(TAG, "Cliccato su selezionaFile");

        if (!isExternalStorageReadable()){
            Log.i(TAG, "Sono rotto");
            Toast.makeText(this, "Disco non leggibile", Toast.LENGTH_SHORT).show();
            return;
        }

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            //if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.READ_EXTERNAL_STORAGE)) {
              //  Toast.makeText(this, "Il permesso Storage serve a caricare un file dalla memoria del telefono", Toast.LENGTH_SHORT).show();

            //} else {
                //Log.i(TAG, "Permesso negato");
                //Toast.makeText(this, "Permesso negato", Toast.LENGTH_SHORT).show();
            Log.i(TAG, "Dentro checkSelfPermission");
            ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},
                        PERMISSION_LOAD_FILE);

           //}
        }
        else{
            Log.i(TAG, "Permesso già accordato");
            chooseFile();
        }
    }

    private boolean isExternalStorageReadable() {
        String state = Environment.getExternalStorageState();
		    if (Environment.MEDIA_MOUNTED.equals(state)) {
			    return true;
		    }
		return false;
    }

    private void chooseFile(){
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("application/pdf");
        try {
            startActivityForResult(Intent.createChooser(intent, "Select Your .pdf File"), PICK_PDF_REQUEST);
        } catch (ActivityNotFoundException e) {
            Toast.makeText(this, "Please Install a File Manager",Toast.LENGTH_SHORT).show();
        }
    }
    //Il risultato di chooseFile viene intercettato con questo metodo qui sotto
    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if(requestCode==PICK_PDF_REQUEST){
            if(resultCode==RESULT_OK){
                risultatoRicercaFile = data.getData();
                Toast.makeText(EsempioComplesso.this, risultatoRicercaFile.toString(), Toast.LENGTH_SHORT).show();
                Log.i(TAG, "File pdf scelto correttamente: "+risultatoRicercaFile.toString());
            } else{
                Log.i(TAG, "File pdf non scelto");
            }
        }
    }
    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(this, "Permission granted", Toast.LENGTH_SHORT).show();

            if (requestCode==PERMISSION_LOAD_FILE) {
                    chooseFile();
            }

        }else{
            Log.i(TAG, "Permesso negato");
            Toast.makeText(this, "Permesso negato", Toast.LENGTH_SHORT).show();
        }

    }


    public void inviaMessaggio(View view) {
        String nome = etNome.getText().toString();
        String cognome = etCognome.getText().toString();
        String matricola = etMatricola.getText().toString();



        //Penso che si rompa qui
        Log.i(TAG, ""+risultatoRicercaFile.toString());


        //Primo metodo
        //Non va bene perchè sovrascrive sempre sotto studente
        /*
        myRef.child("Studente").child("Nome").setValue(nome);
        myRef.child("Studente").child("Cognome").setValue(cognome);
        myRef.child("Studente").child("Matricola").setValue(matricola);
        */

        //Secondo metodo
         /*
        myRef.child("Studenti").child(matricola).child("Matricola").setValue(matricola);
        myRef.child("Studenti").child(matricola).child("Nome").setValue(nome);
        myRef.child("Studenti").child(matricola).child("Cognome").setValue(cognome);
        Log.i(TAG, "Nome: "+nome+"; Cognome: "+cognome+"; Matricola: "+matricola+";");
        */

        //Terzo metodo=usare un oggetto Java "Studente"
        //Controllo che i campi siano riempiti
        if(controlloCampi()) {
            final String timestamp = "" + System.currentTimeMillis();


            //1)creo lo studente
            Studente studente = new Studente(nome, cognome, matricola);
            //2)
            //myRef.child("Studenti").push().setValue(studente);

            //Ho commentato la riga sopra perchè questo metodo sotto è più completo
            //Creo un riferimento alla sottocategoria "Studenti" sul realtime db
            DatabaseReference studenteAggiunto = myRef.child("Studenti").push();
            //Qui effettivamente aggiungo l'oggetto studente al realtime db
            studenteAggiunto.setValue(studente);
            //Estraggo la chiave assegnata allo studente
            String key = studenteAggiunto.getKey();
            //ora creo una coppia chiave-matricola
            myRef.child("Chiavi").child(matricola).setValue(key);

            Log.i(TAG, "Chiave:"+myRef.getKey());

            final String messagePushID = timestamp;
            //Devo anche caricare l'appunto su storage e devo legarlo all'utente che lo carica,
            // perchè poi devo pagarlo
            // Here we are uploading the pdf in firebase storage with the name of current time

            //final StorageReference filepath = storageRef.child(studente.toString()+"_"+messagePushID+ ".pdf").;
            final StorageReference filepath = storageRef.child("File di Prova1.pdf");

            Toast.makeText(EsempioComplesso.this, filepath.getName(), Toast.LENGTH_SHORT).show();
            filepath.putFile(risultatoRicercaFile).continueWithTask(new Continuation() {
                        @Override
                        public Object then(@NonNull Task task) throws Exception {
                            if (!task.isSuccessful()) {
                                throw task.getException();
                            }
                            return filepath.getDownloadUrl();
                        }
                    }).addOnCompleteListener(new OnCompleteListener<Uri>() {
                @Override
                public void onComplete(@NonNull Task<Uri> task) {
                    if (task.isSuccessful()) {
                        // After uploading is done it progress
                        // dialog box will be dismissed
                        //dialog.dismiss();
                        Uri uri = task.getResult();
                        String myurl;
                        myurl = uri.toString();
                        Toast.makeText(EsempioComplesso.this, "Uploaded Successfully", Toast.LENGTH_SHORT).show();
                    } else {
                        //dialog.dismiss();
                        Toast.makeText(EsempioComplesso.this, "UploadedFailed", Toast.LENGTH_SHORT).show();
                    }
                }
            });
        }

        else
            Log.i(TAG, "Almeno uno dei campi è vuoto");



        //svuotaCampi();

        //leggiChild();

        //conta();

    }

    private boolean controlloCampi() {
        boolean nomeVuoto=true;
        boolean cognomeVuoto=true;
        boolean matricolaVuota=true;
        boolean intentVuoto=true;
        if(etNome.getText().toString().trim().compareTo("")==0){
            nomeVuoto=false;
            Log.i(TAG, "Nome vuoto");
        } else if (etCognome.getText().toString().trim().compareTo("")==0) {
            cognomeVuoto=false;
            Log.i(TAG, "Cognome vuoto");
        } else if (etMatricola.getText().toString().trim().compareTo("")==0) {
            matricolaVuota=false;
            Log.i(TAG, "Cognome vuoto");
        } else if (risultatoRicercaFile==null){
            intentVuoto=false;
            Log.i(TAG, "Intent vuoto");
        }
        if(nomeVuoto && cognomeVuoto && matricolaVuota && intentVuoto)
            return true;
        else
            return false;
    }

    private void svuotaCampi() {
        etNome.setText("");
        etCognome.setText("");
        etMatricola.setText("");
    }

    private void leggi() {
        // Read from the database
        myRef.child("Studenti").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                String studenti="";
                //Qui cicleremo per ogni oggetto di tipo dataSnapshot che sarà il figlio contenuto all'interno di dataSnapshoto.getchildren
                for(DataSnapshot figlio:dataSnapshot.getChildren()){

                    //String value = dataSnapshot.getValue(String.class);

                    Studente studente = figlio.getValue(Studente.class);
                    //Il vantaggio è quello di estrarre l'intero oggetto Studente
                    Log.d(TAG, "Value is: " + studente.toString());
                    studenti += studente.toString()+"\n";
                    Log.d(TAG, "Key is: " + figlio.getKey());
                }
                tvMessaggio.setText(studenti.toString());
            }

            @Override
            public void onCancelled(DatabaseError error) {
                // Failed to read value
                Log.w(TAG, "Failed to read value.", error.toException());
            }
        });
    }

    public void leggiChild(){
        myRef.child("Studenti").addChildEventListener(new ChildEventListener() {
            //Come si comporta quando:
            //un nodo figlio viene aggiunto
            @Override
            public void onChildAdded(@NonNull DataSnapshot snapshot, @Nullable String previousChildName) {
                //in questo caso dataSnapshot è già il figlio
                Studente studente=snapshot.getValue(Studente.class);
                Log.i(TAG, "Aggiunto studente "+studente.toString());
                tvMessaggio.setText("Aggiunto studente "+studente.toString());
                Log.d(TAG, "Key is: " + snapshot.getKey());
            }

            //viene modificato
            @Override
            public void onChildChanged(@NonNull DataSnapshot snapshot, @Nullable String previousChildName) {
                Studente studente=snapshot.getValue(Studente.class);
                Log.i(TAG, "Modificato studente "+studente.toString());
                tvMessaggio.setText("Modificato studente "+studente.toString());
            }

            //viene rimosso
            @Override
            public void onChildRemoved(@NonNull DataSnapshot snapshot) {
                Studente studente=snapshot.getValue(Studente.class);
                Log.i(TAG, "Rimosso studente "+studente.toString());
                tvMessaggio.setText("Rimosso studente "+studente.toString());
            }

            //viene spostato
            @Override
            public void onChildMoved(@NonNull DataSnapshot snapshot, @Nullable String previousChildName) {

            }

            //viene cancellato
            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });
    }

    private void conta(){
        myRef.child("Studenti").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                long children=snapshot.getChildrenCount();
                Log.i(TAG, "Numero di figli: "+String.valueOf(children));
                tvMessaggio.setText(""+String.valueOf(children));
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });
    }

}