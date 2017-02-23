package com.supcom.testconnection;

import android.app.admin.DevicePolicyManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.media.MediaRecorder;
import android.net.Uri;
import android.os.Environment;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.net.Socket;

import android.app.Activity;
import android.os.Bundle;
import android.os.Handler;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.widget.TextView;
import android.app.Activity;
import android.os.Bundle;
//import android.telephony.gsm.SmsManager;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import com.google.gson.*;
import android.telephony.SmsManager;

import static android.content.Intent.ACTION_CALL_BUTTON;

public class MainActivity extends Activity {

    // On a besoin d'un objet serverSocket car c'est ce qui va nous permettre d'utiliser un port du téléphone
    private ServerSocket serverSocket;

    private String outputFile = null;
    private MediaRecorder myAudioRecorder;



    //Ici on va définir un processus qui va permettre à l'application d'ouvrir un port
    //et d'écouter tout en effectuant d'autres tâches en parallèle notamment l'envoi d'un message
    Handler updateConversationHandler;
    Thread serverThread = null;

    //On a besoin d'un objet TextView pour pouvoir spécifier après ce que va
    //contenir le champ  textuel
    private TextView text;

    // Le serveur écoute sur le port 6050
    public static final int SERVERPORT = 6050;


    @Override
    public void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main_activity2);
        //L'objet TextView créé contiendra l'element TextView de l'interface dont l'id est
        //text2
        Button startbutton = (Button) findViewById(R.id.button);
        startbutton.setOnClickListener(new View.OnClickListener(){
            public void onClick(View v) {
                setContentView(R.layout.activity_main
                );

            }
        });
        text = (TextView) findViewById(R.id.text2);

        outputFile = Environment.getExternalStorageDirectory().getAbsolutePath() + "/OR_recording.wav";
        myAudioRecorder=new MediaRecorder();
        myAudioRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
        myAudioRecorder.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP);
        myAudioRecorder.setAudioEncoder(MediaRecorder.OutputFormat.AMR_NB);
        myAudioRecorder.setOutputFile(outputFile);


        //On lance le processus qui va permettre de continuer à écouter sur le port 6050
        updateConversationHandler = new Handler();
        this.serverThread = new Thread(new ServerThread());
        this.serverThread.start();

        PhoneCallListener phoneListener = new PhoneCallListener();
        TelephonyManager telephonyManager = (TelephonyManager) this.getSystemService(Context.TELEPHONY_SERVICE);
        telephonyManager.listen(phoneListener,PhoneStateListener.LISTEN_CALL_STATE);


    }

    private class PhoneCallListener extends PhoneStateListener {

        private boolean isPhoneCalling = false;


        @Override
        public void onCallStateChanged(int state, String incomingNumber) {



            if (TelephonyManager.CALL_STATE_OFFHOOK == state) {

                  isPhoneCalling = true;
                try{
                    myAudioRecorder.prepare();
                    myAudioRecorder.start();
                }

                catch (IllegalStateException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                }


               // if (TelephonyManager.CALL_STATE_IDLE == state || TelephonyManager.CALL_STATE_RINGING == state ){

                 //   myAudioRecorder.stop();
                   // myAudioRecorder.reset();
                   // myAudioRecorder.release();


//                }
            }
          //  else if (TelephonyManager.CALL_STATE_IDLE == state || TelephonyManager.CALL_STATE_RINGING == state ){

            //    myAudioRecorder.stop();
              //  myAudioRecorder.reset();
               // myAudioRecorder.release();


            }


        //    if (TelephonyManager.CALL_STATE_RINGING == state){

          //      myAudioRecorder.stop();
            //    myAudioRecorder.reset();
              //  myAudioRecorder.release();
           // }
       // }
    }
    // On utilisera cette méthode après, quand on aura besoin de fermer le port qu'on a ouvert
    @Override
    protected void onStop() {
        super.onStop();
        try {
            serverSocket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

    }


    class ServerThread implements Runnable {

        public void run() {
            Socket socket = null;
            try {
                //création d'un objet qui permettra de gérer le port 6000
                //SERVERPORT est juste un entier contenant la valeur 6000
                serverSocket = new ServerSocket(SERVERPORT);
            } catch (IOException e) {
                e.printStackTrace();
            }
            while (!Thread.currentThread().isInterrupted()) {

                try {
                    //Ici on accepte des données sur le port 6000
                    socket = serverSocket.accept();
                    //Ici on crée et on lance un autre processus qui va permettre de recevoir les données envoyées
                    //par le PC
                    CommunicationThread commThread = new CommunicationThread(socket);
                    new Thread(commThread).start();

                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    class CommunicationThread implements Runnable {
        private Socket clientSocket;
        private BufferedReader input;

        public CommunicationThread(Socket clientSocket) {
            this.clientSocket = clientSocket;
            try {
                //cette partie permet d'avoir le contenu envoyé par le PC au téléphone
                this.input = new BufferedReader(new InputStreamReader(this.clientSocket.getInputStream()));
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        public void run() {

            while (!Thread.currentThread().isInterrupted()) {

                try {

                    String read = input.readLine();
                    //Après lecture du contenu envoyé par le PC on lance le processus qui va filtrer
                    // le contenu et envoyer le SMS
                    updateConversationHandler.post(new updateUIThread(read));

                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }


        class updateUIThread implements Runnable {
            private String num;

            public updateUIThread(String str) {
                this.num = str;

            }

            @Override
            public void run() {
                if (num != null) {
                    //Ici on a préféré traiter les données sous le format Json puisqu'on sera
                    //obligé de l'utiliser quand l'application va évoluer

                    JsonParser parser = new JsonParser();
                    JsonObject obj = parser.parse(num).getAsJsonObject();
                    String msg1 = obj.get("msg").getAsString();
                    String num = obj.get("num").getAsString();

                    //Cette ligne permet d'envoyer le SMS au destinataire
                    //text.setText("Le message suivant ** "+msg1+" ** a été envoyé au numéro ** "+num+" **");
                    //SmsManager.getDefault().sendTextMessage(num, null, msg1, null, null);
                    //SmsManager.getDefault().sendTextMessage("27872299", null, "hello", null, null);

                    Intent intent = new Intent(Intent.ACTION_CALL);

                    intent.setData(Uri.parse("tel:" + num));
                    startActivity(intent);

                   }


            }
        }
    }
}
