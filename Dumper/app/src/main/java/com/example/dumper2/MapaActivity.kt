package com.example.dumper2

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import androidx.core.app.ActivityCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices

import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import com.google.firebase.auth.FirebaseAuth
import kotlinx.android.synthetic.main.activity_cadastro_usuario.*
import kotlinx.android.synthetic.main.activity_mapa.*
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class MapaActivity : AppCompatActivity(), OnMapReadyCallback, GoogleMap.OnMarkerClickListener  {

    private lateinit var mMap: GoogleMap
    private lateinit var auth: FirebaseAuth
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    override fun onCreate(savedInstanceState: Bundle?) {
        auth = FirebaseAuth.getInstance()
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_mapa)
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        val mapFragment = supportFragmentManager
            .findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)
        fab.setOnClickListener {
            val intent = Intent(this, ChatBotActivity::class.java)
            startActivity(intent)
        }

        //actionbar
        val actionbar = supportActionBar
        //set actionbar title
        actionbar!!.title = "Mapa"
        //set back button
        actionbar.setDisplayHomeAsUpEnabled(true)
        actionbar.setDisplayHomeAsUpEnabled(true)
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }

    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap

        /*MUDA O TIPO DO MAPA*/
        googleMap.mapType = GoogleMap.MAP_TYPE_NORMAL

        val retrofitClient = DumperAPI.getRetrofitInstance("https://dumper-app.herokuapp.com")
        val endpoint = retrofitClient.create(Endpoint::class.java)
        val callback = endpoint.getPosts()
        var pointOfMap = LatLng(BRASILIA_LATITUDE, BRASILIA_LONGITUDE)

        callback.enqueue(object : Callback<List<PontoUsuario>> {
            override fun onFailure(call: Call<List<PontoUsuario>>, t: Throwable) {
                Toast.makeText(baseContext, t.message, Toast.LENGTH_SHORT).show()
            }
            override fun onResponse(call: Call<List<PontoUsuario>>, response: Response<List<PontoUsuario>>) {
                if (ActivityCompat.checkSelfPermission(this@MapaActivity,
                        Manifest.permission.ACCESS_FINE_LOCATION
                    ) != PackageManager.PERMISSION_GRANTED) {
                    requestPermissions(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                        1
                    )
                    return
                }else if (ActivityCompat.checkSelfPermission(this@MapaActivity,
                        Manifest.permission.ACCESS_FINE_LOCATION
                    ) == PackageManager.PERMISSION_GRANTED) {

                    mMap.isMyLocationEnabled = true
                    fusedLocationClient.lastLocation.addOnSuccessListener(this@MapaActivity) {location ->
                        if (location != null) {
                            val currentLatLng = LatLng(location.latitude, location.longitude)
                            pointOfMap = currentLatLng
                            mMap.setOnMarkerClickListener(this@MapaActivity)
                            response.body()?.forEach {
                                val ponto = LatLng(it.latitude.toDouble(), it.longitude.toDouble())
                                mMap.addMarker(MarkerOptions().position(ponto).title(it.nome).snippet(it.descricao))
                                if (Distance(currentLatLng, ponto) <= 2000) {
                                    //VALIDAR PONTOS
                                }
                            }
                        }
                    }
                }
                else {
                    response.body()?.forEach {
                        val ponto = LatLng(it.latitude.toDouble(), it.longitude.toDouble())
                        mMap.addMarker(MarkerOptions().position(ponto).title(it.nome).snippet(it.descricao))
                    }
                }
            }
        })

        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(pointOfMap, DEFAULT_ZOOM))
    }


    /*===============MENU===============*/
    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        val inflater = menuInflater
        inflater.inflate(R.menu.menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem) = when (item.itemId) {

        R.id.action_mapa -> {
            msgShow("Map")
            val intent = Intent(this, MapaActivity::class.java)
            startActivity(intent)
            true
        }
        R.id.action_add_novos_pontos -> {
            msgShow("Adicionar novos pontos")
            val intent = Intent(this, CadastroDePontoActivity::class.java)
            intent.putExtra("User", auth.currentUser?.email)
            startActivity(intent)
            true
        }
        R.id.action_lista_pontos -> {
            msgShow("Lista de pontos")
            val intent = Intent(this, ListaPontoActivity2::class.java)
            startActivity(intent)
            true
        }
        R.id.action_fale_conosco -> {
            msgShow("Fale conosco")
            val intent = Intent(this, FaleConoscoActivity::class.java)
            startActivity(intent)
            true
        }
        R.id.action_logout -> {
            msgShow("Saindo")
            auth.signOut()
            val intent = Intent(this, LoginActivity::class.java)
            startActivity(intent)
            true
        }
        else -> {
            super.onOptionsItemSelected(item)
        }
    }

    private fun Distance(origin: LatLng, point: LatLng): Float {
        var localOrigin  =  Location(LocationManager.GPS_PROVIDER)
        localOrigin.latitude = origin.latitude
        localOrigin.longitude = origin.longitude

        var localDestination = Location(LocationManager.GPS_PROVIDER)
        localDestination.latitude = point.latitude
        localDestination.longitude = point.longitude

        return localOrigin.distanceTo(localDestination)
    }

    override fun onMarkerClick(p0: Marker?): Boolean = false

    private fun msgShow(msg: String) {
        Toast.makeText(this, msg, Toast.LENGTH_LONG).show()
    }

    companion object{
        const val BRASILIA_LATITUDE : Double = -15.776207
        const val BRASILIA_LONGITUDE : Double = -47.9295729
        const val DEFAULT_ZOOM = 12.0f
    }
}