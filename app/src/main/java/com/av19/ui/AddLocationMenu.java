package com.av19.ui;

import android.os.Bundle;
import android.widget.Toast;

import androidx.core.view.ViewCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.WindowInsetsCompat;
import androidx.activity.EdgeToEdge;

import com.av19.R;

import org.osmdroid.config.Configuration;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.Marker;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.events.MapEventsReceiver;
import org.osmdroid.views.overlay.MapEventsOverlay;

public class AddLocationMenu extends BaseLocaleActivity {

    private MapView map;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Habilitar Edge-to-Edge
        EdgeToEdge.enable(this);
        setContentView(R.layout.map_menu);

        // Aplicar insets al contenedor principal
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // Inicializar la configuración de osmdroid
        Configuration.getInstance().load(getApplicationContext(), getPreferences(MODE_PRIVATE));

        // Obtener y configurar el MapView
        map = findViewById(R.id.map);
        map.setTileSource(TileSourceFactory.MAPNIK);
        map.setMultiTouchControls(true);
        // Establecer un zoom y centro inicial (por ejemplo, en Vitoria-Gasteiz)
        GeoPoint startPoint = new GeoPoint(42.8467, -2.6731);
        map.getController().setZoom(12.0);
        map.getController().setCenter(startPoint);

        // Agregar un overlay para detectar toques en el mapa
        MapEventsOverlay eventsOverlay = new MapEventsOverlay(new MapEventsReceiver() {
            @Override
            public boolean singleTapConfirmedHelper(GeoPoint p) {
                // Crear el marcador en la posición tocada
                Marker marker = new Marker(map);
                marker.setPosition(p);
                marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);
                // Opcional: asignar un título con las coordenadas
                marker.setTitle("Lat: " + p.getLatitude() + "\nLon: " + p.getLongitude());

                // Agregar listener para mostrar un Toast al pulsar el marcador
                marker.setOnMarkerClickListener((m, mapView) -> {
                    Toast.makeText(AddLocationMenu.this,
                            "Coordenadas: " + m.getTitle(), Toast.LENGTH_SHORT).show();
                    return true;
                });

                // Agregar el marcador al mapa y refrescarlo
                map.getOverlays().add(marker);
                map.invalidate();
                return true;
            }

            @Override
            public boolean longPressHelper(GeoPoint p) {
                // Aquí podrías implementar alguna acción para pulsación larga, si lo deseas
                return false;
            }
        });
        map.getOverlays().add(eventsOverlay);
    }
}
