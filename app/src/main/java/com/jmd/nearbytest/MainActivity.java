package com.jmd.nearbytest;

import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.os.Vibrator;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.nearby.Nearby;
import com.google.android.gms.nearby.connection.AdvertisingOptions;
import com.google.android.gms.nearby.connection.ConnectionInfo;
import com.google.android.gms.nearby.connection.ConnectionLifecycleCallback;
import com.google.android.gms.nearby.connection.ConnectionResolution;
import com.google.android.gms.nearby.connection.Connections;
import com.google.android.gms.nearby.connection.ConnectionsStatusCodes;
import com.google.android.gms.nearby.connection.DiscoveredEndpointInfo;
import com.google.android.gms.nearby.connection.DiscoveryOptions;
import com.google.android.gms.nearby.connection.EndpointDiscoveryCallback;
import com.google.android.gms.nearby.connection.Payload;
import com.google.android.gms.nearby.connection.PayloadCallback;
import com.google.android.gms.nearby.connection.PayloadTransferUpdate;
import com.google.android.gms.nearby.connection.Strategy;

import java.util.Random;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

public class MainActivity extends AppCompatActivity {

    @BindView(R.id.btnAdvertise)
    Button btnAdvertise;
    @BindView(R.id.btnSearch)
    Button btnSearch;
    @BindView(R.id.btnDisconnect)
    Button btnDisconnect;
    @BindView(R.id.btnReset)
    Button btnReset;
    @BindView(R.id.btnSend)
    Button btnSend;
    @BindView(R.id.edtMessage)
    EditText edtMessage;
    @BindView(R.id.txtMessage)
    TextView txtMessage;
    @BindView(R.id.txtInfo)
    TextView txtInfo;

    @OnClick({R.id.btnAdvertise, R.id.btnSearch, R.id.btnSend, R.id.btnDisconnect, R.id.btnReset})
    public void onViewClicked(View view) {
        switch (view.getId()) {
            case R.id.btnAdvertise:
                startAdvertising();
                break;
            case R.id.btnSearch:
                startDiscovering();
                break;
            case R.id.btnSend:
                sendText();
                break;
            case R.id.btnDisconnect:
                disconnectAPI();
                break;
            case R.id.btnReset:
                resetAPI();
                break;
        }
    }

    private final String service_id = "com.jmd.nearbytest";    // app ID
    private final Strategy strategy = Strategy.P2P_STAR;                   // Nearby strategy
    private String device_name = "";                                       // Device name

    private GoogleApiClient.ConnectionCallbacks connectionCallbacks;
    private GoogleApiClient.OnConnectionFailedListener connectionFailedListener;
    private EndpointDiscoveryCallback endpointDiscoveryCallback;
    private ConnectionLifecycleCallback connectionLifecycleCallback;
    private PayloadCallback payloadCallback;
    private GoogleApiClient googleApiClient;

    private String endpt;

    private boolean jmDiscovering = false;
    private boolean jmAdvertising = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ButterKnife.bind(this);
        addInfo("onCreate");

        device_name = setDeviceName();

        initListeners();
    }

    @Override
    protected void onStart() {
        super.onStart();
        addInfo("onStart");

        if (googleApiClient == null) {
            initApi();
        }
        resetAPI();
    }

    @Override
    protected void onStop() {
        super.onStop();
        addInfo("onStop");
        if (googleApiClient != null && googleApiClient.isConnected() && jmAdvertising) {
            Nearby.Connections.stopAdvertising(googleApiClient);
            jmAdvertising = false;
        }
        if (googleApiClient != null && googleApiClient.isConnected() && jmDiscovering) {
            Nearby.Connections.stopDiscovery(googleApiClient);
            jmDiscovering = false;
        }
    }

    private void initListeners() {
        connectionCallbacks = new GoogleApiClient.ConnectionCallbacks() {
            @Override
            public void onConnected(@Nullable Bundle bundle) {
                addInfo("onConnected");
                btnAdvertise.setEnabled(true);
                btnSearch.setEnabled(true);
                btnDisconnect.setEnabled(true);
                btnReset.setEnabled(true);
            }

            @Override
            public void onConnectionSuspended(int reason) {
                addInfo(String.format("onConnectionSuspended (%s)", reason));
                jmAdvertising = false;
                jmDiscovering = false;
            }
        };

        connectionFailedListener = new GoogleApiClient.OnConnectionFailedListener() {
            @Override
            public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
                addInfo("onConnectionFailed");
                resetAPI();
            }
        };

        endpointDiscoveryCallback = new EndpointDiscoveryCallback() {
            @Override
            public void onEndpointFound(String s, DiscoveredEndpointInfo discoveredEndpointInfo) {
                addInfo("Endpoint Found");

                Nearby.Connections.requestConnection(googleApiClient, device_name, s, connectionLifecycleCallback)
                        .setResultCallback(new ResultCallback<Status>() {
                            @Override
                            public void onResult(@NonNull Status status) {
                                if (status.isSuccess()) {
                                    addInfo("onResult Request Connection (Sent)");
                                } else {
                                    addInfo("onResult Request Connection (Error)");
                                }
                            }
                        });
            }

            @Override
            public void onEndpointLost(String s) {
                addInfo("Endpoint Lost");
            }
        };

        connectionLifecycleCallback = new ConnectionLifecycleCallback() {
            @Override
            public void onConnectionInitiated(String s, ConnectionInfo connectionInfo) {
                endpt = s;
                addInfo("onConnectionInitiated");

                new AlertDialog.Builder(MainActivity.this)
                        .setTitle("Accept connection to " + connectionInfo.getEndpointName())
                        .setMessage("Confirm if the code " + connectionInfo.getAuthenticationToken() + " is also displayed on the other device")
                        .setPositiveButton("Accept", new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                                Nearby.Connections.acceptConnection(googleApiClient, endpt, payloadCallback);
                            }
                        })
                        .setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                                Nearby.Connections.rejectConnection(googleApiClient, endpt);
                            }
                        })
                        .setIcon(android.R.drawable.ic_dialog_alert)
                        .show();
            }

            @Override
            public void onConnectionResult(String s, ConnectionResolution connectionResolution) {
                switch (connectionResolution.getStatus().getStatusCode()) {
                    case ConnectionsStatusCodes.STATUS_OK:
                        addInfo("onConnectionResolution Accepted");
                        break;
                    case ConnectionsStatusCodes.STATUS_CONNECTION_REJECTED:
                        addInfo("onConnectionResolution Rejected");
                        break;
                }
            }

            @Override
            public void onDisconnected(String s) {
                addInfo("onDisconnected");
            }
        };

        payloadCallback = new PayloadCallback() {
            @Override
            public void onPayloadReceived(String s, Payload payload) {
                addInfo("Payload Received");

                byte[] bytes = payload.asBytes();
                if (bytes != null) {
                    String msg = new String(bytes);

                    try {
                        int number = Integer.parseInt(msg);
                        Vibrator v = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
                        if (v != null) {
                            v.vibrate(number);
                        }
                    } catch (NumberFormatException e) {
                        txtMessage.setText(msg);
                    }
                }
            }

            @Override
            public void onPayloadTransferUpdate(String s, PayloadTransferUpdate payloadTransferUpdate) {
                //addInfo("Payload Transfer Update");
            }
        };
    }

    private void initApi() {
        addInfo("Init API");
        googleApiClient = new GoogleApiClient.Builder(this)
                .addApi(Nearby.CONNECTIONS_API)
                .addConnectionCallbacks(connectionCallbacks)
                .enableAutoManage(this, connectionFailedListener)
                .build();
    }

    private void connectAPI() {
        if (!googleApiClient.isConnected()) {
            googleApiClient.connect();
        }
    }

    private void disconnectAPI() {
        if (googleApiClient != null && googleApiClient.isConnected()) {
            googleApiClient.disconnect();
        }
        jmAdvertising = false;
        jmDiscovering = false;
    }

    private void resetAPI() {
        disconnectAPI();
        connectAPI();
    }

    private void startAdvertising() {
        connectAPI();
        if (!jmAdvertising) {
            addInfo("Start Advertise");
            AdvertisingOptions advertisingOptions = new AdvertisingOptions(strategy);
            Nearby.Connections.startAdvertising(googleApiClient, device_name, service_id, connectionLifecycleCallback, advertisingOptions)
                    .setResultCallback(new ResultCallback<Connections.StartAdvertisingResult>() {
                        @Override
                        public void onResult(@NonNull Connections.StartAdvertisingResult startAdvertisingResult) {
                            if (startAdvertisingResult.getStatus().isSuccess()) {
                                addInfo("onResult Advertising (Success)");
                                jmAdvertising = true;
                            } else {
                                addInfo("onResult Advertising (Error) - " + startAdvertisingResult.getStatus().getStatusMessage());
                                jmAdvertising = false;
                            }
                        }
                    });
        } else {
            stopAdvertising();
        }
    }

    private void stopAdvertising() {
        addInfo("Stop Advertise");
        jmAdvertising = false;
        Nearby.Connections.stopAdvertising(googleApiClient);
    }

    private void startDiscovering() {
        connectAPI();
        if (!jmDiscovering) {
            addInfo("Start Discover");
            final DiscoveryOptions discoveryOptions = new DiscoveryOptions(strategy);
            Nearby.Connections.startDiscovery(googleApiClient, service_id, endpointDiscoveryCallback, discoveryOptions)
                    .setResultCallback(new ResultCallback<Status>() {
                        @Override
                        public void onResult(@NonNull Status status) {
                            if (status.isSuccess()) {
                                addInfo("onResult Discovering (Success)");
                                jmDiscovering = true;
                            } else {
                                addInfo("onResult Discovering (Error) - " + status.getStatusMessage());
                                jmDiscovering = false;
                            }
                        }
                    });
        } else {
            stopDiscovering();
        }
    }

    private void stopDiscovering() {
        addInfo("Stop Discover");
        jmDiscovering = false;
        Nearby.Connections.stopDiscovery(googleApiClient);
    }

    private String setDeviceName() {
        return String.valueOf(new Random().nextInt(89999) + 10000);
    }

    private void sendText() {
        addInfo("Send");
        String text = edtMessage.getText().toString();
        Payload payload = Payload.fromBytes(text.getBytes());
        Nearby.Connections.sendPayload(googleApiClient, endpt, payload);
    }

    private void addInfo(String info) {
        String txt = "- " + info + "\n" + txtInfo.getText().toString();
        txtInfo.setText(txt);
    }
}
