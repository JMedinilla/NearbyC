package com.jmd.nearbytestaux;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

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

public class MainActivity extends AppCompatActivity implements
        GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener {

    private final String service_id = "com.jmd.nearbytest";    // app ID
    private final Strategy strategy = Strategy.P2P_STAR;                   // Nearby strategy
    private String device_name = "";                                       // Device name

    private GoogleApiClient googleApiClient;
    private ConnectionLifecycleCallback connectionLifecycleCallback;
    private EndpointDiscoveryCallback endpointDiscoveryCallback;
    private PayloadCallback payloadCallback;

    private String endpoint = "";
    private boolean jmDiscovering = false;
    private boolean jmAdvertising = false;

    private Button btnAdvertising;
    private Button btnDiscovering;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        btnAdvertising = findViewById(R.id.btnAdvertising);
        btnDiscovering = findViewById(R.id.btnDiscovering);
        btnAdvertising.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startAdvertise();
            }
        });
        btnDiscovering.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startDiscover();
            }
        });

        device_name = setDeviceName();

        connectionLifecycleCallback = new ConnectionLifecycleCallback() {
            @Override
            public void onConnectionInitiated(String s, ConnectionInfo connectionInfo) {
                endpoint = s;
                Nearby.Connections.acceptConnection(googleApiClient, endpoint, payloadCallback);
            }

            @Override
            public void onConnectionResult(String s, ConnectionResolution connectionResolution) {
                switch (connectionResolution.getStatus().getStatusCode()) {
                    case ConnectionsStatusCodes.STATUS_OK:
                        break;
                    case ConnectionsStatusCodes.STATUS_CONNECTION_REJECTED:
                        break;
                }
            }

            @Override
            public void onDisconnected(String s) {
            }
        };
        endpointDiscoveryCallback = new EndpointDiscoveryCallback() {
            @Override
            public void onEndpointFound(String s, DiscoveredEndpointInfo discoveredEndpointInfo) {
                Nearby.Connections.requestConnection(googleApiClient, device_name, s, connectionLifecycleCallback)
                        .setResultCallback(new ResultCallback<Status>() {
                            @Override
                            public void onResult(@NonNull Status status) {
                            }
                        });
            }

            @Override
            public void onEndpointLost(String s) {
            }
        };
        payloadCallback = new PayloadCallback() {
            @Override
            public void onPayloadReceived(String s, Payload payload) {
                byte[] bytes = payload.asBytes();
                if (bytes != null) {
                    String msg = new String(bytes);
                    Toast.makeText(MainActivity.this, msg, Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onPayloadTransferUpdate(String s, PayloadTransferUpdate payloadTransferUpdate) {
            }
        };
    }

    @Override
    protected void onStart() {
        super.onStart();

        if (googleApiClient == null) {
            initApi();
        }
        resetApi();
    }

    @Override
    protected void onStop() {
        super.onStop();

        if (googleApiClient != null && googleApiClient.isConnected() && jmAdvertising) {
            Nearby.Connections.stopAdvertising(googleApiClient);
            jmAdvertising = false;
        }
        if (googleApiClient != null && googleApiClient.isConnected() && jmDiscovering) {
            Nearby.Connections.stopDiscovery(googleApiClient);
            jmDiscovering = false;
        }
    }

    private void startAdvertise() {
        connect();
        if (!jmAdvertising) {
            AdvertisingOptions advertisingOptions = new AdvertisingOptions(strategy);
            Nearby.Connections.startAdvertising(googleApiClient, device_name, service_id, connectionLifecycleCallback, advertisingOptions)
                    .setResultCallback(new ResultCallback<Connections.StartAdvertisingResult>() {
                        @Override
                        public void onResult(@NonNull Connections.StartAdvertisingResult startAdvertisingResult) {
                            jmAdvertising = startAdvertisingResult.getStatus().isSuccess();
                        }
                    });
        } else {
            stopAdvertise();
        }
    }

    private void stopAdvertise() {
        Nearby.Connections.stopAdvertising(googleApiClient);
        jmAdvertising = false;
    }

    private void startDiscover() {
        connect();
        if (!jmDiscovering) {
            DiscoveryOptions discoveryOptions = new DiscoveryOptions(strategy);
            Nearby.Connections.startDiscovery(googleApiClient, service_id, endpointDiscoveryCallback, discoveryOptions)
                    .setResultCallback(new ResultCallback<Status>() {
                        @Override
                        public void onResult(@NonNull Status status) {
                            jmDiscovering = status.isSuccess();
                        }
                    });
        } else {
            stopDiscover();
        }

    }

    private void stopDiscover() {
        Nearby.Connections.stopDiscovery(googleApiClient);
        jmDiscovering = false;
    }

    private void resetApi() {
        disconnect();
        connect();
    }

    private void disconnect() {
        if (googleApiClient != null && googleApiClient.isConnected()) {
            googleApiClient.disconnect();
        }
        jmAdvertising = false;
        jmDiscovering = false;
    }

    private void connect() {
        if (!googleApiClient.isConnected()) {
            googleApiClient.connect();
        }
    }

    private void initApi() {
        googleApiClient = new GoogleApiClient.Builder(this)
                .addApi(Nearby.CONNECTIONS_API)
                .addConnectionCallbacks(this)
                .enableAutoManage(this, this)
                .build();
    }

    private String setDeviceName() {
        return String.valueOf(new Random().nextInt(89999) + 10000);
    }

    @Override
    public void onConnected(@Nullable Bundle bundle) {
        btnAdvertising.setEnabled(true);
        btnDiscovering.setEnabled(true);
    }

    @Override
    public void onConnectionSuspended(int i) {
        resetApi();
    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
        resetApi();
    }
}
