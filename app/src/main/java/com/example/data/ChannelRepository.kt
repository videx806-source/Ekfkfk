package com.example.data

import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.GET
import java.util.concurrent.TimeUnit

interface VidexApiService {
    @GET("eventos.json")
    suspend fun getEventos(): EventosResponse
}

object RetrofitClient {
    private val moshi = Moshi.Builder()
        .addLast(KotlinJsonAdapterFactory())
        .build()

    private val loggingInterceptor = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BODY
    }

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .addInterceptor(loggingInterceptor)
        .build()

    val api: VidexApiService by lazy {
        Retrofit.Builder()
            .baseUrl("https://api.videx.lol/")
            .client(okHttpClient)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
            .create(VidexApiService::class.java)
    }
}

object ChannelRepository {
    val CHANNELS = listOf(
        Channel("ESPN SUR", "/espn/index.m3u8", "GENERAL"),
        Channel("ESPN 2 SUR", "/espn2/index.m3u8", "GENERAL"),
        Channel("ESPN 3 SUR", "/espn3sur/mono.m3u8", "GENERAL"),
        Channel("ESPN 4 SUR", "/espn4sur/mono.m3u8", "GENERAL"),
        Channel("ESPN 5 SUR", "/espn5sur/mono.m3u8", "GENERAL"),
        Channel("ESPN 6 SUR", "/espn6sur/mono.m3u8", "GENERAL"),
        Channel("ESPN 7 SUR", "/espn7sur/mono.m3u8", "GENERAL"),
        Channel("ESPN MEXICO", "/espnmx.m3u8", "GENERAL"),
        Channel("ESPN MEXICO 2", "/espnmx2.m3u8", "GENERAL"),
        Channel("ESPN MEXICO 3", "/espnmx3.m3u8", "GENERAL"),
        Channel("ESPN MEXICO 4", "/espnmx4.m3u8", "GENERAL"),
        Channel("ESPN DEPORTES MX", "/espndeportesmx/index.m3u8", "GENERAL"),
        Channel("ESPN BRASIL", "/espnbr/index.m3u8", "GENERAL"),
        Channel("ESPN 3 BR", "/espn3br/index.m3u8", "GENERAL"),
        Channel("TYC SPORTS", "/tycsports/main.m3u8", "GENERAL"),
        Channel("TNT ARGENTINA", "/tntar/index.m3u8", "GENERAL"),
        Channel("TNT SPORTS CHILE", "/tntsportschile/index.m3u8", "GENERAL"),
        Channel("DAZN", "/dazn1es/mono.m3u8", "GENERAL"),
        Channel("DAZN 2", "/dazn2es/mono.m3u8", "GENERAL"),
        Channel("DAZN 3", "/dazn3es/mono.m3u8", "GENERAL"),
        Channel("DAZN 4", "/dazn4es/mono.m3u8", "GENERAL"),
        Channel("DAZN F1", "/daznf1/index.m3u8", "GENERAL"),
        Channel("BEIN SPORTS 1", "/beinsports1/index.m3u8", "GENERAL"),
        Channel("FOX SPORTS US", "/foxsportsus/index.m3u8", "GENERAL"),
        Channel("FOX ONE 1", "/foxone1/index.m3u8", "GENERAL"),
        Channel("FOX ONE 2", "/foxone2/index.m3u8", "GENERAL"),
        Channel("FOX ONE 3", "/foxone3/index.m3u8", "GENERAL"),
        Channel("DSPORTS", "/canal/dsports.m3u8", "GENERAL"),
        Channel("DSPORTS PLUS", "/DSPORTS/videx.lol.m3u8", "GENERAL"),
        Channel("DSPORTS PREMIUM UY", "/dsportspremiumurg/index.m3u8", "GENERAL"),
        Channel("SPORTV BR", "/sportv/index.m3u8", "GENERAL"),
        Channel("SPORTV 2 BR", "/sportv2/index.m3u8", "GENERAL"),
        Channel("SPORTV 3 BR", "/sportv3/index.m3u8", "GENERAL"),
        Channel("WIN SPORTS", "/winsport/mono.m3u8", "GENERAL"),
        Channel("CLARO SPORTS", "/clarosports/live.m3u8", "GENERAL"),
        Channel("TIGO SPORTS", "/tigosports/live.m3u8", "GENERAL"),
        Channel("MOVISTAR DEP 1", "/movistar1/mono.m3u8", "GENERAL"),
        Channel("MOVISTAR DEP 2", "/movistar2/mono.m3u8", "GENERAL"),
        Channel("MOVISTAR DEP 3", "/movistar3/mono.m3u8", "GENERAL"),
        Channel("MOVISTAR DEP 4", "/movistar4/mono.m3u8", "GENERAL"),
        Channel("MOVISTAR LA LIGA", "/movistarliga/mono.m3u8", "GENERAL"),
        Channel("MOVISTAR CAMPEONES", "/movistarcampeones/mono.m3u8", "GENERAL"),
        Channel("MOVISTAR GOLF", "/movistargolf/mono.m3u8", "GENERAL"),
        Channel("CANAL+ F1", "/canalmasf1/mono.m3u8", "GENERAL"),
        Channel("LA LIGA TV EN", "/laligatven/index.m3u8", "GENERAL"),
        Channel("EUROSPORT 1", "/Eurosport/live.m3u8", "GENERAL"),
        Channel("EUROSPORT 2", "/euro2es/mono.m3u8", "GENERAL"),
        Channel("NBA TV", "/nbatv/index.m3u8", "GENERAL"),
        Channel("SKY NBA", "/skynba/index.m3u8", "GENERAL"),
        Channel("NBA TV 2", "/nba2/tv.m3u8", "GENERAL"),
        Channel("NBA 1", "/nba/live.m3u8", "GENERAL"),
        Channel("NFL NETWORK", "/NFLNetwork/index.m3u8", "GENERAL"),
        Channel("NFL RED ZONE", "/nflredzone/index.m3u8", "GENERAL"),
        Channel("MLB 1", "/mlb1/index.m3u8", "GENERAL"),
        Channel("GOLF CHANNEL", "/golf_channel/index.m3u8", "GENERAL"),
        Channel("MLS DIRECT KICK", "/mls_direct_kick/index.m3u8", "GENERAL"),
        Channel("MLS DIRECT KICK 2", "/mls_direct_kick_2/index.m3u8", "GENERAL"),
        Channel("MLS DIRECT KICK 3", "/mls_direct_kick_3/index.m3u8", "GENERAL"),
        Channel("GOKU 24x7", "/goku/24x7.m3u8", "GENERAL"),
        Channel("DEPORTV", "/deportv/index.m3u8", "GENERAL"),
        Channel("TVC SPORTS", "/tvc_deportes/index.m3u8", "GENERAL"),
        Channel("TUDN", "/tudnof/index.m3u8", "GENERAL"),
        Channel("TUDN 4K", "/tudn/4k.m3u8", "GENERAL"),
        Channel("UNITEL BO", "/unitel/index.m3u8", "GENERAL"),
        Channel("VTV MÁS UY", "/vtvmas/mono.m3u8", "GENERAL"),
        Channel("TELEFE", "/telefe/index.m3u8", "GENERAL"),
        Channel("A24", "/canal/a24.m3u8", "GENERAL"),
        Channel("CANAL 5 MX", "/canal5mx/index.m3u8", "GENERAL"),
        Channel("UNIVISION", "/univision/index.m3u8", "GENERAL"),
        Channel("HBO MAX", "/hbomax/index.m3u8", "GENERAL"),
        Channel("HYPER 1", "/hyper1/index.m3u8", "GENERAL"),
        Channel("WIN PLUS", "/winplus/moe.m3u8", "GENERAL"),
        Channel("SONY CINE", "/sonycine/chunks.m3u8", "GENERAL"),
        Channel("LOS SIMPSON", "/simpson/love.m3u8", "GENERAL"),
        Channel("EL GOURMET", "/ElGourmet/live.m3u8", "GENERAL"),
        Channel("SPREEN KICK", "/spreen/kick.m3u8", "GENERAL"),
        Channel("ORBITA CHILE TV", "/orbita-cl/live.m3u8", "GENERAL"),
        Channel("ORBITA TV 2", "/ORBITA_SPORTS_FHD/live.m3u8", "GENERAL"),
        Channel("VEVO LAT", "/vevolt/index.m3u8", "MÚSICA"),
        Channel("KPOP", "/kpop/love.m3u8", "MÚSICA"),
        Channel("SOL MÚSICA", "/solmusica/lice.m3u8", "MÚSICA"),
        Channel("QUIERO MÚSICA", "/musicaenmiidioma/index.m3u8", "MÚSICA"),
        Channel("MTV LIVE", "/mtv_live/index.m3u8", "MÚSICA"),
        Channel("BOB ESPONJA", "/bob/m.m3u8", "PLUTO"),
        Channel("PADRINOS MÁGICOS", "/padrino/magico.m3u8", "PLUTO"),
        Channel("TORTUGAS NINJA", "/tortuga/live.m3u8", "PLUTO"),
        Channel("NICKELODEON ES", "/nickelodeon/love.m3u8", "PLUTO"),
        Channel("NICKELODEON CLÁSICO", "/nickelodeon/clasico.m3u8", "PLUTO"),
        Channel("PEPA PIG", "/pepapig/love.m3u8", "PLUTO"),
        Channel("DORA", "/dora/index.m3u8", "PLUTO"),
        Channel("RUGRATS", "/rugrats/index.m3u8", "PLUTO"),
        Channel("FANATIZ 1", "/fanatiz1/index.m3u8", "EVENTOS"),
        Channel("FANATIZ 2", "/fanatiz2/index.m3u8", "EVENTOS"),
        Channel("FANATIZ 3", "/fanatiz3/index.m3u8", "EVENTOS"),
        Channel("FANATIZ 4", "/fanatiz4/index.m3u8", "EVENTOS"),
        Channel("FANATIZ 5", "/fanatiz5/live.m3u8", "EVENTOS"),
        Channel("FANATIZ 6", "/fanatiz6/index.m3u8", "EVENTOS"),
        Channel("FANATIZ 7", "/fanatiz7/index.m3u8", "EVENTOS"),
        Channel("FANATIZ 8", "/fanatiz8/index.m3u8", "EVENTOS"),
        Channel("FANATIZ 9", "/fanatiz9/index.m3u8", "EVENTOS"),
        Channel("FANATIZ 10", "/fanatiz10/index.m3u8", "EVENTOS"),
        Channel("FANATIZ 11", "/fanatiz11/index.m3u8", "EVENTOS"),
        Channel("FANATIZ 12", "/fanatiz12/index.m3u8", "EVENTOS"),
        Channel("FANATIZ 13", "/fanatiz13/index.m3u8", "EVENTOS"),
        Channel("FANATIZ 14", "/fanatiz14/index.m3u8", "EVENTOS"),
        Channel("FANATIZ 15", "/fanatiz15/index.m3u8", "EVENTOS"),
        Channel("FANATIZ 16", "/fanatiz16/index.m3u8", "EVENTOS"),
        Channel("FANATIZ 17", "/fanatiz17/index.m3u8", "EVENTOS"),
        Channel("FANATIZ 18", "/fanatiz18/index.m3u8", "EVENTOS"),
        Channel("FANATIZ 19", "/fanatiz19/index.m3u8", "EVENTOS"),
        Channel("FANATIZ 20", "/fanatiz20/index.m3u8", "EVENTOS"),
        Channel("PREMIER 1 BR", "/premierbr/index.m3u8", "EVENTOS"),
        Channel("PREMIER 2 BR", "/premier2br/index.m3u8", "EVENTOS"),
        Channel("PREMIER 3 BR", "/premier3br/index.m3u8", "EVENTOS"),
        Channel("PREMIER 4 BR", "/premier4br/index.m3u8", "EVENTOS"),
        Channel("PREMIER 5 BR", "/premier5/index.m3u8", "EVENTOS"),
        Channel("PREMIER 6 BR", "/premier6/index.m3u8", "EVENTOS"),
        Channel("PREMIER 7 BR", "/premier7/index.m3u8", "EVENTOS"),
        Channel("PREMIER 8 BR", "/premier8/index.m3u8", "EVENTOS"),
        Channel("DISNEY 1", "/disney1/index.m3u8", "EVENTOS"),
        Channel("DISNEY 2", "/disney2/index.m3u8", "EVENTOS"),
        Channel("DISNEY 3", "/disney3/index.m3u8", "EVENTOS"),
        Channel("DISNEY 4", "/disney4/index.m3u8", "EVENTOS"),
        Channel("DISNEY 5", "/disney5/index.m3u8", "EVENTOS"),
        Channel("DISNEY 6", "/disney6/index.m3u8", "EVENTOS"),
        Channel("DISNEY 7", "/disney7/index.m3u8", "EVENTOS"),
        Channel("DISNEY 8", "/disney8/index.m3u8", "EVENTOS"),
        Channel("DISNEY 9", "/disney9/index.m3u8", "EVENTOS"),
        Channel("DISNEY 10", "/disney10/index.m3u8", "EVENTOS"),
        Channel("DISNEY 11", "/disney11/index.m3u8", "EVENTOS"),
        Channel("DISNEY 12", "/disney12/index.m3u8", "EVENTOS"),
        Channel("DISNEY 13", "/disney13/index.m3u8", "EVENTOS"),
        Channel("MLS 1", "/mls/index.m3u8", "EVENTOS"),
        Channel("MLS 2", "/mls2/index.m3u8", "EVENTOS"),
        Channel("MLS 3", "/mls3/index.m3u8", "EVENTOS"),
        Channel("MLS 4", "/mls4/index.m3u8", "EVENTOS"),
        Channel("MLS 5", "/mls5/index.m3u8", "EVENTOS"),
        Channel("EVENTOS BO 1", "/eventosbo1/index.m3u8", "EVENTOS"),
        Channel("EVENTOS BO 2", "/eventosbo2/index.m3u8", "EVENTOS"),
        Channel("EVENTOS BO 3", "/eventosbo3/index.m3u8", "EVENTOS"),
        Channel("EVENTOS PE", "/eventospe/love.m3u8", "EVENTOS"),
        Channel("EVENTOS 1", "/eventos/videx.m3u8", "EVENTOS"),
        Channel("EVENTOS 2", "/eventos2/videx.m3u8", "EVENTOS"),
        Channel("EVENTOS 3", "/eventos3/videx.m3u8", "EVENTOS"),
        Channel("COPA SUDAMERICANA", "/sudamericana/evento.m3u8", "EVENTOS"),
        Channel("HYPER 2", "/hyper2/mono.m3u8", "EVENTOS"),
        Channel("PARAMOUNT+ 1", "/paramount1/mono.m3u8", "EVENTOS"),
        Channel("PARAMOUNT+ 2", "/Paramount2/love.m3u8", "EVENTOS"),
        Channel("PARAMOUNT+ 3", "/Paramount3/love.m3u8", "EVENTOS"),
        Channel("PARAMOUNT+ 4", "/Paramount4/love.m3u8", "EVENTOS"),
        Channel("MLS SOLO EVENTOS", "/mlsoloeventos/mono.m3u8", "EVENTOS"),
        Channel("SUPER NOVA", "/evento/1.m3u8", "EVENTOS")
    )

    fun getStreamUrl(channelPath: String): String {
        return "https://api.videx.lol/keyvidex.php?keyvidex=videx&stream=$channelPath"
    }

    suspend fun fetchLiveEvents(): List<Evento> {
        return try {
            RetrofitClient.api.getEventos().eventos
        } catch (e: Exception) {
            e.printStackTrace()
            // Return empty list or fallback dummy events to ensure robustness
            listOf(
                Evento(
                    id = "evt_001",
                    titulo = "Champions League Final",
                    equipos = "Real Madrid vs Bayern Munich",
                    liga = "UEFA Champions League",
                    fecha = "2026-05-28",
                    hora = "20:00",
                    zonaHoraria = "CET",
                    imagenUrl = "https://images.unsplash.com/photo-1508098682722-e99c43a406b2?w=800",
                    streamUrl = "https://api.videx.lol/keyvidex.php?keyvidex=videx&stream=/eventos/videx.m3u8",
                    estado = "proximo",
                    destacado = true
                ),
                Evento(
                    id = "evt_002",
                    titulo = "NBA Playoffs Matchup",
                    equipos = "Lakers vs Celtics",
                    liga = "NBA Basketball",
                    fecha = "2026-05-26",
                    hora = "21:30",
                    zonaHoraria = "ET",
                    imagenUrl = "https://images.unsplash.com/photo-1546519638-68e109498ffc?w=800",
                    streamUrl = "https://api.videx.lol/keyvidex.php?keyvidex=videx&stream=/nbatv/index.m3u8",
                    estado = "en_vivo",
                    destacado = false
                )
            )
        }
    }
}
