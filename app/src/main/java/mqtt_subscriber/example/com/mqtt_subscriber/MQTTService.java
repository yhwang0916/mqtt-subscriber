package mqtt_subscriber.example.com.mqtt_subscriber;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.net.ConnectivityManager;
import android.os.Binder;
import android.os.Environment;
import android.os.IBinder;
import android.util.Log;

import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.ref.WeakReference;


public class MQTTService extends Service
{
    private MqttClient recieverClient = null;
    private MqttConnectOptions connOpts2 = null;
    private Sensor countSensor;

    private String subscribeTopic;
    private String broker;
    private String clientId;
    private MemoryPersistence persistence;

    public class LocalBinder<S> extends Binder
    {
        private WeakReference<S> mService;

        public LocalBinder(S service)
        {
            mService = new WeakReference<S>(service);
        }
        public S getService()
        {
            return mService.get();
        }
        public void close()
        {
            mService = null;
        }
    }

    private LocalBinder<MQTTService> mBinder;

    @Override
    public void onCreate()
    {
        //note: Service not created because function was overrided
        super.onCreate();
        // reset status variable to initial state
        subscribeTopic = "battery";
        broker = "tcp://iot.eclipse.org:1883";
        clientId = "Subscriber";
        persistence = new MemoryPersistence();

        mBinder = new LocalBinder<MQTTService>(this);


    }


    @Override
    public int onStartCommand(final Intent intent, int flags, final int startId)
    {
        new Thread(new Runnable() {
            @Override
            public void run() {
                if (isOnline()){
                    try {
                        initializeAndConnectClients(broker, clientId, persistence);

                    } catch(MqttException me) {
                        System.out.println("reason "+me.getReasonCode());
                        System.out.println("msg "+me.getMessage());
                        System.out.println("loc "+me.getLocalizedMessage());
                        System.out.println("cause "+me.getCause());
                        System.out.println("excep "+me);
                        me.printStackTrace();
                    }

                }

            }
        }, "MQTTservice").start();

        // return START_NOT_STICKY - we want this Service to be left running
        //  unless explicitly stopped, and it's process is killed, we want it to
        //  be restarted
        return START_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent)
    {
        return mBinder;
    }


    private void setUpAcelerometerData(SensorManager senSensorManager) {

        countSensor = senSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        if(countSensor!=null){
            senSensorManager.registerListener((android.hardware.SensorEventListener) this, countSensor , SensorManager.SENSOR_DELAY_NORMAL);
        }
    }

    private static void generateCsvFile(String sFileName, String data) throws IOException, JSONException {
        JSONObject objectToWrite = new JSONObject(data.toString());
        File folder = new File(Environment.getExternalStorageDirectory() + "/Folder");

        boolean var = false;
        if (!folder.exists())
            var = folder.mkdir();

        System.out.println("" + var);


        final String filename = folder.toString() + "/" + sFileName;


        FileWriter writer = new FileWriter(filename,true);

        try
        {

            writer.append( objectToWrite.get("x").toString());
            writer.append(',');
            writer.append(objectToWrite.get("y").toString());
            writer.append(',');
            writer.append(objectToWrite.get("z").toString());
            writer.append('\n');
            writer.flush();
            writer.close();


        }
        catch(IOException e)
        {
            writer.flush();
            writer.close();
            e.printStackTrace();
        } catch (JSONException e) {
            writer.flush();
            writer.close();
            e.printStackTrace();
        }catch(Exception e){
            writer.flush();
            writer.close();
            e.printStackTrace();
        }
    }

    private void setMessageCallbacks() {
        recieverClient.setCallback(new MqttCallback() {
            @Override
            public void connectionLost(Throwable throwable) {

            }

            @Override
            public void messageArrived(String s, MqttMessage mqttMessage) throws Exception {
                //write data to a csv file
                generateCsvFile("subscriber.csv",mqttMessage.toString());
                Log.i("Subscription", "Subscription Received with message:" + s);
                System.out.println("Arrived");
            }

            @Override
            public void deliveryComplete(IMqttDeliveryToken iMqttDeliveryToken) {

            }

        });
    }

    private void initializeAndConnectClients(String broker, String clientId, MemoryPersistence persistence) throws MqttException {
        recieverClient = new MqttClient(broker, "Reciever", persistence);
        connOpts2 = new MqttConnectOptions();
        connOpts2.setCleanSession(true);
        recieverClient.connect(connOpts2);
        setMessageCallbacks();
        recieverClient.subscribe(subscribeTopic);
    }


    private boolean isOnline()
    {
        ConnectivityManager cm = (ConnectivityManager)getSystemService(CONNECTIVITY_SERVICE);
        if(cm.getActiveNetworkInfo() != null &&
                cm.getActiveNetworkInfo().isAvailable() &&
                cm.getActiveNetworkInfo().isConnected())
        {
            return true;
        }
        return false;
    }

}
